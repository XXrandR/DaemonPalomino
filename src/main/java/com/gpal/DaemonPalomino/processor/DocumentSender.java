package com.gpal.DaemonPalomino.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import com.gpal.DaemonPalomino.database.HikariBase;
import com.gpal.DaemonPalomino.network.HttpClientSender;

public class DocumentSender {

    private final HikariBase database;
    private final HttpClientSender httpClientSender;

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSender.class);
    private final ScheduledExecutorService scheduler;

    @Inject
    public DocumentSender(HikariBase connection, HttpClientSender httpClientSender) {
        this.database = connection;
        this.httpClientSender = httpClientSender;
        this.scheduler = Executors.newScheduledThreadPool(4);
    }

    public void startSendDocuments(int timeSendDocuments, int timeValidatingDocuments, int timeSendAnuDocuments,
            int timeValidateAnulated) {
        scheduler.scheduleWithFixedDelay(this::sendDocuments, 1, timeSendDocuments, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(this::validateDocuments, 1, timeValidatingDocuments, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(this::sendAnulatedDocuments, 1, timeSendAnuDocuments, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(this::valAnulatedDocuments, 1, timeValidateAnulated, TimeUnit.SECONDS);
    }

    private void sendDocuments() {
        LOGGER.info("Sending documents !!!");
    }

    private void validateDocuments() {
        LOGGER.info("Validating the documents !!!");
    }

    private void sendAnulatedDocuments() {
        LOGGER.info("Send Anulated documents !!!");
    }

    private void valAnulatedDocuments() {
        LOGGER.info("Validating Anulated documents !!!");
    }
}
