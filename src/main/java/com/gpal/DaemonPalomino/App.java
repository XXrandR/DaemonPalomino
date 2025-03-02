package com.gpal.DaemonPalomino;

import com.gpal.DaemonPalomino.BaseComponent.CoreComponent;
import com.gpal.DaemonPalomino.BaseComponent.DaggerCoreComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * Daemon palomino
 */
@Slf4j
public class App {

    public static void main(String[] args) {
        // construction of the CoreComponent
        CoreComponent coreComp = DaggerCoreComponent.builder().build();
        if (args.length > 0 && args[0].equals("SERVER")) {
            handleServCommand(args, coreComp);
        } else if (args.length > 1 && args[0].equals("UNIQUE")) {
            handleUniqueCommand(args, coreComp);
            log.info("Parameter UNIQUE.");
        } else if (args.length > 1 && args[0].equals("CANCEL")) {
            handleCancelCommand(args, coreComp);
            log.debug("Parameter to CANCEL");
        } else {
            log.warn("Parameter unknown.");
        }
    }

    private static void handleServCommand(String[] args, CoreComponent coreComp) {
        if (args.length < 8) {
            printServUsage();
            return;
        }
        try {
            int sizeBatch = Integer.parseInt(args[1]);
            int firmInterval = Integer.parseInt(args[2]);
            int validationInterval = Integer.parseInt(args[3]);
            int anulationSendInterval = Integer.parseInt(args[4]);
            int anulationValidateInterval = Integer.parseInt(args[5]);
            int summaryHour = Integer.parseInt(args[6]);
            int summaryMin = Integer.parseInt(args[7]);
            coreComp.daemonScheduler().startSendDocuments(
                    sizeBatch,
                    firmInterval,
                    validationInterval,
                    anulationSendInterval,
                    anulationValidateInterval,
                    summaryHour,
                    summaryMin);
            log.info("Document sending process launched successfully.");
        } catch (Exception e) {
            log.error("Invalid number format in arguments. Please provide integer values.");
            e.printStackTrace();
            printServUsage();
        }
    }

    private static void printServUsage() {
        log.info(
                "Usage: java -jar DaemonPalomino.jar SERVER <sizeBatch> <timeSendDocuments> <timeValidatingDocuments> <timeSendAnuDocuments> <timeValidateAnulated> <summaryHour> <summaryMin>");
        log.info("  sizeBatch: Size of the batch for document processing");
        log.info("  timeSendDocuments: Time interval (in minutes) for generating and signing documents");
        log.info("  timeValidatingDocuments: Time interval (in days) for sending non-BOL documents");
        log.info("  timeSendAnuDocuments: Time interval (in seconds) for sending annulled documents");
        log.info("  timeValidateAnulated: Time interval (in seconds) for validating annulled documents");
        log.info("  summaryHour: Time exactly (in hours) to stablish the hour");
        log.info("  summaryMin: Time exactly (in minutes) to stablish the minutes");
    }

    private static void handleCancelCommand(String[] args, CoreComponent coreComp) {
        // FORMAT: TI_DOCU,NU_DOCU,CO_EMPR
        if (args.length < 4) {
            printCancelUsage();
            return;
        }
        try {
            String nu_docu = String.valueOf(args[1]);
            String ti_docu = String.valueOf(args[2]);
            String co_empr = String.valueOf(args[3]);
            if (coreComp.documentAnulate().anulateDocument(nu_docu, ti_docu, co_empr)) {
                log.info("Document anul process finished successfully.");
            } else {
                log.info("Document anul have not finished successfully.");
            }
        } catch (Exception ex) {
            log.error("Invalid number format in arguments. Please provide integer values.");
            ex.printStackTrace();
            printUniqueUsage();
        }
    }

    private static void printCancelUsage() {
        log.info("Usage: java -jar DaemonPalomino.jar CANCEL <nu_docu> <ti_docu> <co_empr>");
        log.info("  nu_docu: Number of the document");
        log.info("  ti_docu: Type of document");
        log.info("  timeValidatingDocuments: Code of the Business");
    }

    private static void handleUniqueCommand(String[] args, CoreComponent coreComp) {
        // FORMAT: TI_DOCU,NU_DOCU,CO_EMPR
        if (args.length < 4) {
            printUniqueUsage();
            return;
        }
        try {
            String nu_docu = String.valueOf(args[1]);
            String ti_docu = String.valueOf(args[2]);
            String co_empr = String.valueOf(args[3]);
            coreComp.documentUnique().assembleLifecycle(nu_docu, ti_docu, co_empr, null);
            log.info("Document sending process finished successfully.");
            System.exit(0);
        } catch (Exception ex) {
            log.error("Invalid number format in arguments. Please provide integer values.");
            ex.printStackTrace();
            printUniqueUsage();
        }
    }

    private static void printUniqueUsage() {
        log.info("Usage: java -jar DaemonPalomino.jar UNIQUE <NuDocu> <TiDocu> <CoEmpr>");
        log.info("  NuDocu: The Number of the Document with the format (B644-8874).");
        log.info("  TiDocu: The type of document to BOL,FAC,NCR,NCD.");
        log.info("  CoEmpr: Business code, i mean 005,004, etc.");
    }

}
