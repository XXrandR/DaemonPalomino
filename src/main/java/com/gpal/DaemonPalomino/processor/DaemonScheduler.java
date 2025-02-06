package com.gpal.DaemonPalomino.processor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.builders.FirmDocument;
import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.builders.PdfDocument;
import com.gpal.DaemonPalomino.builders.SummaryDocumentProcess;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import com.gpal.DaemonPalomino.network.ReactorServer;
import com.gpal.DaemonPalomino.network.WsService;
import com.gpal.DaemonPalomino.utils.ChronoUtils;
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
    private final SummaryDocumentProcess sDocumentProcess;
    private final WsService wService;
    private final PdfDocument pdfDocument;
    private final ReactorServer reactorServer;

    @Inject
    public DaemonScheduler(ReactorServer reactorServer, SummaryDocumentProcess sDocumentProcess,
            FirmDocument firmDocument, DataSource dataSource,
            GenerateDocument generateDocument, WsService wsService, PdfDocument pdfDocument) {

        this.reactorServer = reactorServer;
        this.dataSource = dataSource;
        this.documentGenerator = generateDocument;
        this.sDocumentProcess = sDocumentProcess;
        int numCores = Runtime.getRuntime().availableProcessors();
        this.scheduler = Executors.newScheduledThreadPool(numCores);
        this.firmDocument = firmDocument;
        this.wService = wsService;
        this.pdfDocument = pdfDocument;
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

        // this needs to work every 10 min or by preference
        scheduler.scheduleWithFixedDelay(this::generateAndFirmDocuments, 0, firmInterval, TimeUnit.MINUTES);
        // this needs to work every 30 min or by preference
        scheduler.scheduleWithFixedDelay(this::sendDocumentsNotBol, 1, validationInterval, TimeUnit.DAYS);
        // this needs to work every 30 min or by preference
        scheduler.scheduleWithFixedDelay(this::sendAnulatedDocuments, 1, anulationSendInterval, TimeUnit.MINUTES);
        // this needs to work every 30 min or by preference
        scheduler.scheduleWithFixedDelay(this::valAnulatedDocuments, 1, anulationValidateInterval, TimeUnit.MINUTES);
        // this works every 24 hours, depending on the hour and min
        ChronoUtils.scheduleFixedTime(this::sendSummaries, summaryHour, summaryMin);

        try {
            reactorServer.startServer();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void generateAndFirmDocuments() {
        try {
            log.info("Generate and Firm documents");
            List<FirmSignature> documentsPending = documentGenerator.generateDocument(sizeBatch, dataSource,
                    locationDocuments);
            if (!documentsPending.isEmpty()) {
                firmDocument.signDocuments(dataSource, documentsPending);
                // if all it's ok, we generate the pdf files in the folder pdf
                documentsPending.forEach(
                        item -> pdfDocument.generatePdfDocument(dataSource, item, locationDocuments + "/pdf/"));
            } else {
                log.info("No documents pending to firm.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error generating and firming documents..", ex);
        }
    }

    private void sendSummaries() {
        try {
            log.info("Send summaries documents !!!");
            List<FirmSignature> fSignature = sDocumentProcess.generateDocuments(0, dataSource,
                    locationDocuments + "/unsigned/");
            if (!fSignature.isEmpty()) {
                firmDocument.signDocuments(dataSource, fSignature);
                log.info("Sending documents...");
                wService.sendDocuments(locationDocuments + "/signed/", fSignature);
            } else {
                log.info("No resumes left to firm.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error generating and firming documents..", ex);
        }
    }

    private void sendDocumentsNotBol() {
        try {
            // bs.getStatus("");
            log.info("Validate documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error generating and firming documents..", ex);
        }
    }

    private void sendAnulatedDocuments() {
        try {
            // bs.sendSummary("", null);
            log.info("Send Anulated documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error generating and firming documents..", ex);
        }
    }

    private void valAnulatedDocuments() {
        try {
            // bs.sendPack("", null);
            log.info("Validating Anulated documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Error generating and firming documents..", ex);
        }
    }

}
