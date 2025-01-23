package com.gpal.DaemonPalomino.processor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.builders.FirmDocument;
import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.builders.SummaryDocumentProcess;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import com.gpal.DaemonPalomino.network.WsService;
import com.gpal.DaemonPalomino.utils.ChronoUtils;
import com.gpal.DaemonPalomino.utils.FolderManagement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocumentScheduler {

    private final GenerateDocument documentGenerator;
    private final ScheduledExecutorService scheduler;
    private DataSource dataSource;
    private Integer sizeBatch;
    private String locationDocuments;
    private final FirmDocument firmDocument;
    private final SummaryDocumentProcess sDocumentProcess;
    private final WsService wService;

    @Inject
    public DocumentScheduler(SummaryDocumentProcess sDocumentProcess, FirmDocument firmDocument, DataSource dataSource,
            GenerateDocument generateDocument, WsService wsService) {
        this.dataSource = dataSource;
        this.documentGenerator = generateDocument;
        this.sDocumentProcess = sDocumentProcess;
        int numCores = Runtime.getRuntime().availableProcessors();
        this.scheduler = Executors.newScheduledThreadPool(numCores);
        this.firmDocument = firmDocument;
        this.wService = wsService;

        FolderManagement.createFolders();

        try (InputStream inputStream = DocumentScheduler.class.getClassLoader()
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
    }

    private void generateAndFirmDocuments() {
        try {
            log.info("Generate and Firm documents");
            List<FirmSignature> documentsPending = documentGenerator.generateDocument(sizeBatch, dataSource,
                    locationDocuments);
            if (!documentsPending.isEmpty()) {
                firmDocument.signDocument(dataSource, documentsPending);
                // try to send the FAC,NCR,NCD(inmediately)
                wService.sendDocuments("",documentsPending);
            } else {
                log.info("No documents pending to firm.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendSummaries() {
        try {
            log.info("Send summaries documents !!!");
            List<FirmSignature> fSignature = sDocumentProcess.sendDocuments(0, dataSource,
                    locationDocuments + "/unsigned/");
            if (!fSignature.isEmpty()) {
                firmDocument.signDocument(dataSource, fSignature);
                log.info("Sending documents...");
                wService.sendSummarie(locationDocuments + "/unsigned/", fSignature);
            } else {
                log.info("No resumes left to firm.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendDocumentsNotBol() {
        try {
            // bs.getStatus("");
            log.info("Validate documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendAnulatedDocuments() {
        try {
            // bs.sendSummary("", null);
            log.info("Send Anulated documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void valAnulatedDocuments() {
        try {
            // bs.sendPack("", null);
            log.info("Validating Anulated documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
