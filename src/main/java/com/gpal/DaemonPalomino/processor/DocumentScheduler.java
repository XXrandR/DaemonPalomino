package com.gpal.DaemonPalomino.processor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.builders.FirmDocument;
import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.models.FirmSignature;
import com.gpal.DaemonPalomino.network.HttpClientSender;
import com.gpal.DaemonPalomino.utils.ChronoUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocumentScheduler {

    private final HttpClientSender httpClientSender;
    private final GenerateDocument documentGenerator;
    private final ScheduledExecutorService scheduler;
    private DataSource dataSource;
    private Integer sizeBatch;
    private String locationDocuments;
    private final FirmDocument firmDocument;

    @Inject
    public DocumentScheduler(FirmDocument firmDocument, DataSource dataSource, GenerateDocument generateDocument,
            HttpClientSender httpClientSender) {
        this.dataSource = dataSource;
        this.httpClientSender = httpClientSender;
        this.documentGenerator = generateDocument;
        int numCores = Runtime.getRuntime().availableProcessors();
        this.scheduler = Executors.newScheduledThreadPool(numCores);
        this.firmDocument = firmDocument;
    }

    public static void createDirectory(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
            log.debug("Directory created: " + path);
        } else {
            log.debug("Directory already exists: " + path);
        }
    }

    public void createFolders() {
        // set location of unsigned,signed,pdf, and cdr
        try (InputStream inputStream = DocumentScheduler.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (inputStream == null)
                throw new RuntimeException("Unable to find application.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            String locationDocuments = properties.getProperty("location.documents");
            this.locationDocuments = locationDocuments;
            createDirectory(locationDocuments + "/unsigned");
            createDirectory(locationDocuments + "/signed");
            createDirectory(locationDocuments + "/pdf");
            createDirectory(locationDocuments + "/cdr");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public void startSendDocuments(int sizeBatch, int timeSendDocuments, int timeValidatingDocuments,
            int timeSendAnuDocuments,
            int timeValidateAnulated) {
        this.sizeBatch = sizeBatch;
        createFolders();

        // schedulers
        scheduler.scheduleWithFixedDelay(this::generateAndFirmDocuments, 0, timeSendDocuments, TimeUnit.MINUTES);
        scheduler.scheduleWithFixedDelay(this::sendDocumentsNotBol, 1, timeValidatingDocuments, TimeUnit.DAYS);
        scheduler.scheduleWithFixedDelay(this::sendAnulatedDocuments, 1, timeSendAnuDocuments, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(this::valAnulatedDocuments, 1, timeValidateAnulated, TimeUnit.SECONDS);

        // specific time
        ChronoUtils.scheduleFixedTime(this::sendSummaries, Date.from(Instant.now().plusSeconds(5)));
    }

    private void generateAndFirmDocuments() {
        try {
            log.debug("Reading documents !!!");
            List<FirmSignature> documentsPending = documentGenerator.generateDocument(sizeBatch, dataSource,
                    locationDocuments);
            if (!documentsPending.isEmpty()) {
                firmDocument.signDocument(documentsPending);
            } else {
                log.info("No documents pending to firm.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendSummaries() {
        try {
            // bs.getStatus("");
            log.info("Send summaries documents !!!");
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
