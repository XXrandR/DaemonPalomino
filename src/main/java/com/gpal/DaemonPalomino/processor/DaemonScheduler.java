package com.gpal.DaemonPalomino.processor;

import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import java.util.concurrent.ScheduledExecutorService;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.builders.DocumentSender;
import com.gpal.DaemonPalomino.builders.FirmDocument;
import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.builders.PdfDataDocument;
import com.gpal.DaemonPalomino.builders.SummaryDocumentProcess;
import com.gpal.DaemonPalomino.network.FtpRemote;
import com.gpal.DaemonPalomino.network.ReactorServer;
import com.gpal.DaemonPalomino.utils.FolderManagement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DaemonScheduler {

    private final GenerateDocument documentGenerator;
    private final ScheduledExecutorService scheduler;
    private DataSource dataSource;
    private Integer sizeBatch;
    private String locationDocuments;
    private final FirmDocument firmDocument;
    private final PdfDataDocument pdfDocument;
    private final ReactorServer reactorServer;
    private final DocumentSender documentSender;
    private final FtpRemote ftpRemote;

    @Inject
    public DaemonScheduler(ReactorServer reactorServer, SummaryDocumentProcess sDocumentProcess,
            FirmDocument firmDocument, DataSource dataSource,
            GenerateDocument generateDocument, PdfDataDocument pdfDocument, DocumentSender documentSender,
            FtpRemote ftpRemote) {

        this.reactorServer = reactorServer;
        this.dataSource = dataSource;
        this.documentGenerator = generateDocument;
        this.documentSender = documentSender;
        int numCores = Runtime.getRuntime().availableProcessors();
        this.scheduler = Executors.newScheduledThreadPool(numCores);
        this.firmDocument = firmDocument;
        this.pdfDocument = pdfDocument;
        this.ftpRemote = ftpRemote;
        FolderManagement.createFolders();
        try (InputStream inputStream = DaemonScheduler.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            // for location
            Properties properties = new Properties();
            properties.load(inputStream);
            locationDocuments = properties.getProperty("location.documents");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }

    }

    public void startSendDocuments(int sizeBatch, int firmInterval, int validationInterval,
            int anulationSendInterval,
            int anulationValidateInterval, int summaryHour, int summaryMin) {

        this.sizeBatch = sizeBatch;

        // scheduler.scheduleWithFixedDelay(this::generateAndFirmDocuments, 0,
        // firmInterval, TimeUnit.MINUTES);
        // scheduler.scheduleWithFixedDelay(this::getStatus, 1, anulationSendInterval,
        // TimeUnit.MINUTES);

        try {
            reactorServer.startServer();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    // process to firm the document and send it
    private void assembleLifecycle() {

        try {

            log.info("Generate and Firm documents");
            // first obtain documents(1), generic
            List<GenericDocument> documentsPending = documentGenerator.generateDocuments(sizeBatch, dataSource,
                    locationDocuments);

            // then firm documents(2), generic
            List<GenericDocument> documentsPending1 = firmDocument
                    .signDocuments(dataSource, documentsPending);

            // then generate pdf
            List<GenericDocument> documentsPending2 = pdfDocument.generatePdfDocument(dataSource, documentsPending1,
                    locationDocuments + "/pdf/");

            //// send bizlinks data
            List<GenericDocument> documentsPending3 = documentSender.sendDocument(documentsPending2);

            // send resources to server
            if (!ftpRemote.saveData(documentsPending3).isEmpty()) {
                log.info("Successfully processed");
            } else {
                log.error("Some error while processing");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error generating and firming documents..", ex);
        }

    }

    public void getStatus() {

        try {
            log.info("Send Anulated documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error generating and firming documents..", ex);
        }

    }

}
