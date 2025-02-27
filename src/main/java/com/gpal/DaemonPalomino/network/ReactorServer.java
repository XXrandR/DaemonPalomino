package com.gpal.DaemonPalomino.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;
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
        return Mono.fromCallable(() -> {
            String nro = hRequest.param("nro");
            String tipoOperacion = hRequest.param("tipoOperacion");
            Map<String, Object> dt = new HashMap<>();
            dt.put("pdf", obtainBase64Pdf());
            dt.put("qr", obtainQRCode());
            dt.put("hash", obtainHashCode());
            Stream<DataPreVFact> mdata = DataUtil.executeProcedure(dataSource, "EXEC SP_TTHELP_DOCU02 ?,?",
                    List.of(nro, tipoOperacion), DataPreVFact.class).stream();
            mdata.map(item -> {
                documentUnique.assembleLifecycle(item.getNU_DOCU(), item.getTI_DOCU(), item.getCO_EMPR(),
                        tipoOperacion.equals("B") ? "001" : "002");
                return item;
            });
            log.debug("INFO of JSON: nro={}, tipoOperacion={}", nro, tipoOperacion);
            return dt.toString();
        }).subscribeOn(Schedulers.boundedElastic());
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
