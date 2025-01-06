package com.gpal.DaemonPalomino;

import java.util.logging.Logger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Daemon palomino
 */
public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            try {
                logger.info("Task executed");
            } catch (Exception e) {
                logger.severe("Exception caught: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);

    }
}
