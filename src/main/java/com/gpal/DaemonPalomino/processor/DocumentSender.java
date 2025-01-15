package com.gpal.DaemonPalomino.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.builders.FirmDocument;
import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.database.HikariBase;
import com.gpal.DaemonPalomino.network.HttpClientSender;

public class DocumentSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSender.class);
    private final HttpClientSender httpClientSender;
    private final GenerateDocument generateDocument;
    private final ScheduledExecutorService scheduler;
    private DataSource dataSource;
    private Integer sizeBatch;

    @Inject
    public DocumentSender(DataSource dataSource, GenerateDocument generateDocument, HttpClientSender httpClientSender) {
        this.dataSource = dataSource;
        this.httpClientSender = httpClientSender;
        this.generateDocument = generateDocument;
        int numCores = Runtime.getRuntime().availableProcessors();
        this.scheduler = Executors.newScheduledThreadPool(numCores);
    }

    public static void createDirectory(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
            System.out.println("Directory created: " + path);
        } else {
            System.out.println("Directory already exists: " + path);
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
        scheduler.scheduleWithFixedDelay(this::sendDocuments, 1, timeSendDocuments, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(this::validateDocuments, 1, timeValidatingDocuments, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(this::sendAnulatedDocuments, 1, timeSendAnuDocuments, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(this::valAnulatedDocuments, 1, timeValidateAnulated, TimeUnit.SECONDS);
    }

    // TODO:
    // - Generate a folder by day in each folder appropiatedly
    // - Obtain documents
    // - Generate XML Unsigned
    // - Send to sign
    private void sendDocuments() {
        try {
            LOGGER.info("Reading documents !!!");
            generateDocument.generateDocument(sizeBatch, dataSource);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void validateDocuments() {
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
