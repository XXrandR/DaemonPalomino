package com.gpal.DaemonPalomino.processor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.builders.DocumentSender;
import com.gpal.DaemonPalomino.builders.FirmDocument;
import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.builders.PdfDataDocument;
import com.gpal.DaemonPalomino.utils.FolderManagement;
import lombok.extern.slf4j.Slf4j;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.network.FtpRemote;

@Slf4j
public class DocumentUnique {

    private final GenerateDocument generateDocument;
    private final DataSource dataSource;
    private final String locationDocuments;
    private final FirmDocument firmDocument;
    private final PdfDataDocument pdfDocument;
    private final DocumentSender documentSender;
    private final FtpRemote ftpRemote;
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newSingleThreadExecutor();

    public DocumentUnique(GenerateDocument generateDocument, DataSource dataSource, FirmDocument firmDocument,
            PdfDataDocument pdfDocument, DocumentSender documentSender, FtpRemote ftpRemote) {
        this.generateDocument = generateDocument;
        this.dataSource = dataSource;
        this.firmDocument = firmDocument;
        this.pdfDocument = pdfDocument;
        this.documentSender = documentSender;
        this.ftpRemote = ftpRemote;
        // set location of unsigned,signed,pdf, and cdr
        try (InputStream inputStream = DocumentUnique.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (inputStream == null)
                throw new RuntimeException("Unable to find application.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            String locationDocuments = properties.getProperty("location.documents");
            this.locationDocuments = locationDocuments;
            FolderManagement.createFolders();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public boolean downloadCdr(String co_seri, String nu_docu, String ti_docu, String co_empr) {
        return documentSender.downloadCdr(co_seri, nu_docu, ti_docu, co_empr);
    }

    public List<GenericDocument> assembleLifecycle(String NU_DOCU, String TI_DOCU, String CO_EMPR, String tiOper) {

        log.info("Processing document {},{},{}.", NU_DOCU, TI_DOCU, CO_EMPR);

        // first obtain documents(1), generic
        List<GenericDocument> documentsPending = generateDocument.generateDocumentUnique(dataSource, NU_DOCU, TI_DOCU,
                CO_EMPR, locationDocuments, tiOper);

        // then sign document(2), generic
        List<GenericDocument> documentsPending1 = firmDocument.signDocuments(dataSource, documentsPending);

        // generate pdf
        List<GenericDocument> documentsPending2 = pdfDocument.generatePdfDocument(dataSource, documentsPending1,
                locationDocuments + "/pdf/");

        //// to not wait for these processes that are basically optional because can be reprocessed by the backround thread
        //ASYNC_EXECUTOR.submit(() -> {
        //    try {
        //        // send bizlinks data
        //        List<GenericDocument> documentsPending3 = documentSender.sendDocument(documentsPending2);
        //        // send resources to server
        //        if (!ftpRemote.saveData(documentsPending3).isEmpty()) {
        //            log.info("Successfully processed");
        //        } else {
        //            log.info("Empty list to send..");
        //        }
        //    } catch (Exception ex) {
        //        ex.printStackTrace();
        //    }
        //});

        return documentsPending2;

    }

}
