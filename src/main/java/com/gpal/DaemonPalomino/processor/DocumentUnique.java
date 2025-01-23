package com.gpal.DaemonPalomino.processor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.builders.FirmDocument;
import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import com.gpal.DaemonPalomino.utils.FolderManagement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocumentUnique {

    private final GenerateDocument generateDocument;
    private final DataSource dataSource;
    private final String locationDocuments;
    private final FirmDocument firmDocument;

    public DocumentUnique(GenerateDocument generateDocument, DataSource dataSource, FirmDocument firmDocument) {
        this.generateDocument = generateDocument;
        this.dataSource = dataSource;
        this.firmDocument = firmDocument;

        // set location of unsigned,signed,pdf, and cdr
        try (InputStream inputStream = DocumentScheduler.class.getClassLoader()
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

    // IN THIS OCATION JUST THE SAME, and by default it's going to write on top of the file if there's already one
    public void sendDocument(String NU_DOCU, String TI_DOCU, String CO_EMPR) {
        List<FirmSignature> documentsPending = generateDocument.generateDocumentUnique(dataSource, NU_DOCU, TI_DOCU,
                CO_EMPR, locationDocuments);
        if (!documentsPending.isEmpty()) {
            firmDocument.signDocument(dataSource, documentsPending);
        } else {
            log.info("No documents pending to firm.");
        }
    }

}
