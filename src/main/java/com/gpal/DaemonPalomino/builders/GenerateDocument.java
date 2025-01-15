
package com.gpal.DaemonPalomino.builders;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import com.gpal.DaemonPalomino.models.DBDocument;
import com.gpal.DaemonPalomino.models.FirmSignature;
import com.gpal.DaemonPalomino.models.PendingDocument;
import com.gpal.DaemonPalomino.utils.DataUtil;
import javax.sql.DataSource;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateDocument {

    private VelocityEngine velocityEngine;

    @Inject
    public GenerateDocument(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
    }

    public List<FirmSignature> generateDocument(int sizeBatch, DataSource dataSource, String location) {

        List<Object> input = new ArrayList<>();
        input.add(sizeBatch);
        input.add("001"); // for only BOL

        // Obtaining restant documents
        List<PendingDocument> documentBrws = DataUtil.executeProcedure(dataSource, "EXEC SP_TTHELP_DOCU01 ?,?", input,
                PendingDocument.class);

        List<FirmSignature> documentsToFirm = new ArrayList<>();
        documentBrws.forEach(data -> {
            documentsToFirm.add(generateXMLUnsigned(dataSource, data, location));
        });
        log.info("The size: " + documentBrws.toString());
        return documentsToFirm;
    }

    private void generateFile(DBDocument document, StringWriter writer, String location) {
        try (java.io.FileWriter fileWriter = new java.io.FileWriter(
                location + document.getCompanyID() + document.getNuDocu() + ".xml")) {
            fileWriter.write(writer.toString());
            System.out
                    .println("Generated " + location + document.getDateIssue() + document.getCompanyID()
                            + document.getNuDocu() + ".xml");
        } catch (Exception ex) {
            log.error("Error writing file...", ex);
        }
    }

    private FirmSignature generateXMLUnsigned(DataSource dataSource, PendingDocument pendingDocument, String location) {
        log.info("Generating xml for {}", pendingDocument.getNU_DOCU());
        List<Object> input = new ArrayList<>();
        input.add(pendingDocument.getNU_DOCU());
        input.add(pendingDocument.getTI_DOCU());
        input.add(pendingDocument.getCO_EMPR());
        input.add(pendingDocument.getCO_ORIG());

        location = location.concat("/unsigned/");

        if (pendingDocument.getTI_DOCU().equals("BOL")) {

            List<DBDocument> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU ?,?,?,?", input,
                    DBDocument.class);

            if (dbDocuments != null) {

                log.info("DOCUMENTS BEING FOUND: {}", dbDocuments.toString());
                DBDocument document = dbDocuments.get(0);
                VelocityContext context = new VelocityContext();
                context.put("document", document);
                Template template = velocityEngine.getTemplate("/templates/TBDocument.vm");
                StringWriter writer = new StringWriter();
                template.merge(context, writer);

                generateFile(document, writer, location);
            }
            return dbDocuments.get(0);

        } else if (pendingDocument.getTI_DOCU().equals("FAC")) {

            List<DBDocument> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU ?,?,?,?", input,
                    DBDocument.class);

            DBDocument document = dbDocuments.get(0);

        } else if (pendingDocument.getTI_DOCU().equals("NCR")) {

            List<DBDocument> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU ?,?,?,?", input,
                    DBDocument.class);

            DBDocument document = dbDocuments.get(0);

        } else if (pendingDocument.getTI_DOCU().equals("NCD")) {

            List<DBDocument> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU ?,?,?,?", input,
                    DBDocument.class);

            DBDocument document = dbDocuments.get(0);

        } else {
            log.info("Tipo de documento no identificado...");
        }
        return null;
    }

}
