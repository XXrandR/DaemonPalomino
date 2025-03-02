package com.gpal.DaemonPalomino.network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpal.DaemonPalomino.models.dao.DataPreVFact;
import com.gpal.DaemonPalomino.processor.DocumentUnique;
import com.gpal.DaemonPalomino.utils.DataUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import reactor.netty.http.HttpProtocol;
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
    static final int PORT = Integer.parseInt(System.getProperty("port", SECURE ? "8443" : "8080"));
    static final boolean WIRETAP = System.getProperty("wiretap") != null;
    static final boolean COMPRESS = System.getProperty("compress") != null;
    static final boolean HTTP2 = System.getProperty("http2") != null;
    private final DocumentUnique documentUnique;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public ReactorServer(DocumentUnique documentUnique, DataSource dataSource) {
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
                        response.put("pdf", obtainBase64Pdf());
                        response.put("qr", obtainQRCode());
                        response.put("hash", obtainHashCode());

                        tuple.stream()
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
                                .collect(Collectors.toList());

                        log.debug("Constructed response for nro={}, tipoOperacion={}", nro, tipoOperacion);

                        try {
                            return Mono.just(objectMapper.writeValueAsString(response));
                        } catch (JsonProcessingException e) {
                            return Mono.error(e);
                        }
                    });
        });
    }

    // private Mono<String> assembleResponseJson(HttpServerRequest hRequest) {
    // return Mono.fromCallable(() -> {
    // String nro = hRequest.param("nro");
    // String tipoOperacion = hRequest.param("tipoOperacion");
    // Map<String, Object> dt = new HashMap<>();
    // dt.put("pdf", obtainBase64Pdf());
    // dt.put("qr", obtainQRCode());
    // dt.put("hash", obtainHashCode());
    // List<DataPreVFact> mdata = DataUtil.executeProcedure(dataSource, "EXEC
    // SP_TTHELP_DOCU02 ?,?",
    // List.of(nro, tipoOperacion), DataPreVFact.class);
    // var a = mdata.stream().map(item -> {
    // return documentUnique.assembleLifecycle(item.getNU_DOCU(), item.getTI_DOCU(),
    // item.getCO_EMPR(),
    // tipoOperacion.equals("B") ? "001" : "002");
    // });
    // log.debug("INFO of JSON: nro={}, tipoOperacion={}", nro, tipoOperacion);
    // return dt.toString();
    // }).subscribeOn(Schedulers.boundedElastic());
    // }

    private <T> Mono<T> getBlockingOperation(Supplier<T> supplier) {
        return Mono.fromSupplier(supplier).subscribeOn(Schedulers.boundedElastic());
    }

    private String obtainBase64Pdf() {
        return "";
    }

    private String obtainQRCode() {
        return "";
    }

    private String obtainHashCode() {
        return "";
    }

}
