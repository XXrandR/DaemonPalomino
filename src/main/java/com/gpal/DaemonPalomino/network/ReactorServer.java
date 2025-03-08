package com.gpal.DaemonPalomino.network;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import javax.sql.DataSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.gpal.DaemonPalomino.models.BolDocument;
import com.gpal.DaemonPalomino.models.FacDocument;
import com.gpal.DaemonPalomino.models.NcdDocument;
import com.gpal.DaemonPalomino.models.dao.DataPreVFact;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.processor.DocumentUnique;
import com.gpal.DaemonPalomino.utils.DataUtil;
import com.gpal.DaemonPalomino.utils.PropertiesHelper;
import io.netty.handler.codec.http.HttpHeaderValues;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import reactor.netty.http.HttpProtocol;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;

/*
 *
 * Why this part ?, to receive messages from other parts of the system,
 * to firm the document then to generate the qr and all that 
 * stuff and then to let the server do the part of sendsummary 
 * and assemble the anulated documents 
 * and all that stuff 
 * 
 * */
@Slf4j
public class ReactorServer {

    static final boolean SECURE = System.getProperty("secure") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SECURE ? "8443" : "8084"));
    static final boolean WIRETAP = System.getProperty("wiretap") != null;
    static final boolean COMPRESS = System.getProperty("compress") != null;
    static final boolean HTTP2 = System.getProperty("http2") != null;
    private final DocumentUnique documentUnique;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();
    Properties properties = new Properties();
    private final String locationPdfs;

    @Inject
    public ReactorServer(DocumentUnique documentUnique, DataSource dataSource) {
        properties = PropertiesHelper.obtainProps();
        locationPdfs = properties.getProperty("location.documents") + "/pdf/";
        this.documentUnique = documentUnique;
        this.dataSource = dataSource;
    }

    public void startServer() throws CertificateException {
        HttpServer server = HttpServer.create()
                .port(PORT)
                .wiretap(WIRETAP)
                .compress(COMPRESS)
                .route(r -> r.get("/daemon/wsservice/{nro}/{tipoOperacion}",
                        (req, res) -> res.header(CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                                .sendString(assembleResponseJson(req))));
        if (HTTP2) {
            server = server.protocol(HttpProtocol.H2);
        }
        log.info("Server opened in port {}", PORT);
        server.bindNow()
                .onDispose()
                .block();
    }

    private Mono<String> assembleResponseJson(HttpServerRequest hRequest) {
        return Mono.defer(() -> {
            String nro = hRequest.param("nro");
            String tipoOperacion = hRequest.param("tipoOperacion");
            if (nro == null || tipoOperacion == null) {
                return Mono.error(new IllegalArgumentException("Missing required parameters: nro and tipoOperacion"));
            }
            return Mono.from(
                    getBlockingOperation(() -> DataUtil.executeProcedure(
                            dataSource,
                            "EXEC SP_TTHELP_DOCU02 ?,?",
                            List.of(nro, tipoOperacion),
                            DataPreVFact.class)))
                    .flatMap(tuple -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        List<GenericDocument> genericDocument = tuple.stream()
                                .map(item -> documentUnique.assembleLifecycle(
                                        item.getNU_DOCU(),
                                        item.getTI_DOCU(),
                                        item.getCO_EMPR(),
                                        switch (tipoOperacion) {
                                            case "B" -> "001";
                                            case "E" -> "002";
                                            case "C" -> "003";
                                            case "D" -> "004";
                                            default -> "";
                                        }))
                                .findFirst().get();
                        if (genericDocument != null && !genericDocument.isEmpty()) {
                            GenericDocument document = genericDocument.get(0);
                            response.put("pdf", obtainBase64Pdf(document));
                            response.put("qr", obtainQRCode(document));
                            response.put("hash", obtainHashCode(document));
                        } else {
                            log.info("Document already accepted or does not exist.");
                            response.put("message", "Document already accepted or does not exist.");
                        }
                        log.debug("Constructed response for nro={}, tipoOperacion={}", nro, tipoOperacion);
                        try {
                            return Mono.just(objectMapper.writeValueAsString(response));
                        } catch (JsonProcessingException e) {
                            return Mono.error(e);
                        }
                    });
        }).onErrorMap(data -> {
            data.printStackTrace();
            return data;
        });
    }

    private <T> Mono<T> getBlockingOperation(Supplier<T> supplier) {
        return Mono.fromSupplier(supplier).subscribeOn(Schedulers.boundedElastic());
    }

    private String obtainBase64Pdf(GenericDocument genericDocument) {
        return DataUtil.obtainBase64(
                locationPdfs + DataUtil.obtainNameByTypeDocumentNotXml(genericDocument) + ".pdf");
    }

    private String obtainQRCode(GenericDocument genericDocument1) {
        if (genericDocument1 instanceof BolDocument genericDocument) {
            return assembleQr(
                    genericDocument.getCompanyID() + "|01|" + genericDocument.getSeries() + "|"
                            + genericDocument.getNumber()
                            + "|0.00|" + genericDocument.getDueDate() + "|" + genericDocument.getTI_DOCU() + "|"
                            + genericDocument.getCustomerId() + "|"
                            + genericDocument.getDigestValue() + "|"
                            + genericDocument.getPayableAmount() + "|" + genericDocument.getDigestValue() + "|"
                            + genericDocument.getSignatureValue(),
                    300, 300);
        } else if (genericDocument1 instanceof FacDocument genericDocument) {
            return assembleQr(
                    genericDocument.getCompanyID() + "|01|" + genericDocument.getSeries() + "|"
                            + genericDocument.getNumber()
                            + "|0.00|" + genericDocument.getDueDate() + "|" + genericDocument.getTI_DOCU() + "|"
                            + genericDocument.getCustomerId() + "|"
                            + genericDocument.getDigestValue() + "|"
                            + genericDocument.getPayableAmount() + "|" + genericDocument.getDigestValue() + "|"
                            + genericDocument.getSignatureValue(),
                    300, 300);
        } else if (genericDocument1 instanceof NcdDocument genericDocument) {
            return assembleQr(
                    genericDocument.getCompanyID() + "|01|" + genericDocument.getSeries() + "|"
                            + genericDocument.getNumber()
                            + "|0.00|" + genericDocument.getDueDate() + "|" + genericDocument.getTI_DOCU() + "|"
                            + genericDocument.getDigestValue() + "|"
                            + genericDocument.getPayableAmount() + "|" + genericDocument.getDigestValue() + "|"
                            + genericDocument.getSignatureValue(),
                    300, 300);
        } else {
            return "";
        }
    }

    private String obtainHashCode(GenericDocument genericDocument) {
        return genericDocument.getDigestValue();
    }

    private String assembleQr(String data, int height, int width) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            BitMatrix matrix = qrCodeWriter.encode(new String(data.getBytes("UTF-8"), "UTF-8"),
                    BarcodeFormat.QR_CODE, width, height);
            MatrixToImageWriter.writeToStream(matrix, "PNG", pngOutputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        } catch (WriterException | IOException ex) {
            log.error("IO Error: captured in assembleQr in PdfDocument class..", ex);
            return null;
        }
    }

}
