package com.gpal.DaemonPalomino.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocumentSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSender.class);
    private final HttpClientSender httpClientSender;
    private final GenerateDocument documentGenerator;
    private final ScheduledExecutorService scheduler;
    private DataSource dataSource;
    private Integer sizeBatch;
    private String locationDocuments;
    private final FirmDocument firmDocument;

    @Inject
    public DocumentSender(FirmDocument firmDocument, DataSource dataSource, GenerateDocument generateDocument,
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
            log.info("Directory created: " + path);
        } else {
            log.info("Directory already exists: " + path);
        }
    }

    public void createFolders() {
        // set location of unsigned,signed,pdf, and cdr
        try (InputStream inputStream = DocumentSender.class.getClassLoader()
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
        //scheduleFixedTime(this::sendSummaries, Date.from(Instant.now().plusSeconds(5)));
    }

    public void scheduleFixedTime(Runnable runnable, Date time) {
        class Helper extends TimerTask {
            public static int i = 0;
            @Override
            public void run() {
                log.info("Timer ran " + ++i);
                runnable.run();
            }
        }
        Timer timer = new Timer();
        TimerTask timerTask = new Helper();
        timer.schedule(timerTask, time, 24 * 60 * 60 * 1000);
    }

    // TODO:
    // - Generate a folder by day in each folder appropiatedly
    // - Obtain documents
    // - Generate XML Unsigned
    // - Send to sign
    private void generateAndFirmDocuments() {
        try {
            LOGGER.info("Reading documents !!!");
            List<FirmSignature> documentsPending = documentGenerator.generateDocument(sizeBatch, dataSource,
                    locationDocuments);
            firmDocument.signDocument(documentsPending);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendSummaries() {
        try {
            // bs.getStatus("");
            LOGGER.info("Send summaries documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendDocumentsNotBol() {
        try {
            // bs.getStatus("");
            LOGGER.info("Validate documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendAnulatedDocuments() {
        try {
            // bs.sendSummary("", null);
            LOGGER.info("Send Anulated documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void valAnulatedDocuments() {
        try {
            // bs.sendPack("", null);
            LOGGER.info("Validating Anulated documents !!!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
