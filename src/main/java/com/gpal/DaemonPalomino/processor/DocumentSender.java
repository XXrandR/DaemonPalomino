package com.gpal.DaemonPalomino.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import com.gpal.DaemonPalomino.database.HikariBase;

public class DocumentSender{

    private final HikariBase database;

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSender.class);

    private final ScheduledExecutorService scheduler;

    @Inject
    public DocumentSender(HikariBase connection){
        this.database = connection;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startSendingDocuments(){
        scheduler.scheduleWithFixedDelay(this::sendDocuments,0,5,TimeUnit.SECONDS);
    }

    private void sendDocuments(){
        LOGGER.info("Hello World!!!");
    }

}
