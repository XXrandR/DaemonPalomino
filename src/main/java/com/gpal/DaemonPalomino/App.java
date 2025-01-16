package com.gpal.DaemonPalomino;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gpal.DaemonPalomino.BaseComponent.CoreComponent;
import com.gpal.DaemonPalomino.BaseComponent.DaggerCoreComponent;
import com.gpal.DaemonPalomino.processor.DocumentScheduler;

/**
 * Daemon palomino
 */
public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        // construction of the CoreComponent
        CoreComponent coreComp = DaggerCoreComponent.builder().build();

        if (args.length > 0 && args[0].equals("SERV")) {
            DocumentScheduler documentSender = coreComp.documentSender();
            // args[1],args[2] -- time,batchSize
            documentSender.startSendDocuments(Integer.valueOf(args[2]), Integer.valueOf(args[1]),
                    Integer.valueOf(args[1]), Integer.valueOf(args[1]), Integer.valueOf(args[1]));
            LOGGER.info("Process launched.");
        } else if (args.length > 1 && args[0].equals("ALON")) {

            LOGGER.info("Parameter ALON.");
        } else if (args.length > 1 && args[0].equals("ANUL")) {

            LOGGER.debug("Parameter to ANUL");
        } else {

            LOGGER.warn("Parameter unknown.");
        }

    }
}
