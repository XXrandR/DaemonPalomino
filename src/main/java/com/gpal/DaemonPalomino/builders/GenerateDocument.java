
package com.gpal.DaemonPalomino.builders;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.gpal.DaemonPalomino.models.FirmSignature;
import com.gpal.DaemonPalomino.models.GenericDocument;
import com.gpal.DaemonPalomino.models.PendingDocument;
import com.gpal.DaemonPalomino.models.TicketDocument;
import com.gpal.DaemonPalomino.utils.DataUtil;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateDocument {

    private final VelocityEngine velocityEngine;

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
            FirmSignature daa = generateXMLUnsigned(dataSource, data, location);
            if (daa != null) {
                documentsToFirm.add(daa);
            }
        });
        log.debug("The size: " + documentBrws.toString());
        return documentsToFirm;
    }

    private <GD extends GenericDocument> void generateFile(GD document, StringWriter writer, String location) {
        try (java.io.FileWriter fileWriter = new java.io.FileWriter(
                location + document.getCompanyID() + "-" + document.getDocumentTypeId() + "-" + document.getNuDocu()
                        + ".xml")) {
            fileWriter.write(writer.toString());
            log.info("Generated " + location + document.getCompanyID() + "-" + document.getDocumentTypeId() + "-"
                    + document.getNuDocu() + ".xml");
        } catch (Exception ex) {
            log.error("Error writing file...", ex);
        }
    }

    private FirmSignature generateXMLUnsigned(DataSource dataSource, PendingDocument pendingDocument, String location) {
        List<Object> input = new ArrayList<>();
        input.add(pendingDocument.getNU_DOCU());
        input.add(pendingDocument.getTI_DOCU());
        input.add(pendingDocument.getCO_EMPR());
        input.add(pendingDocument.getCO_ORIG());
        log.debug("Generating xml for {},{},{},{}", pendingDocument.getNU_DOCU(), pendingDocument.getTI_DOCU(),
                pendingDocument.getCO_EMPR(), pendingDocument.getCO_ORIG());

        location = location.concat("/unsigned/");

        switch (pendingDocument.getTI_DOCU()) {
            case "BOL" -> {
                List<TicketDocument> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU ?,?,?,?",
                        input,
                        TicketDocument.class);
                if (dbDocuments != null) {
                    if (dbDocuments.isEmpty() == false) {
                        log.debug("DOCUMENTS BEING FOUND: {}", dbDocuments.toString());
                        TicketDocument document = dbDocuments.get(0);
                        VelocityContext context = new VelocityContext();
                        log.debug("DEBUG OF DIGEST GEN DOCU: {}", document.getDigestValue());
                        context.put("document", document);
                        Template template = velocityEngine.getTemplate("/templates/xml/pasajes/ticket.vm");
                        StringWriter writer = new StringWriter();
                        template.merge(context, writer);
                        generateFile(document, writer, location);

                        return dbDocuments.get(0);
                    } else {
                        log.debug("DOCUMENTS NOT FOUND: {},{},{},{}", pendingDocument.getNU_DOCU(),
                                pendingDocument.getTI_DOCU(),
                                pendingDocument.getCO_EMPR(), pendingDocument.getCO_ORIG());
                        return null;
                    }
                }
            }
            case "FAC" ->
                log.info("NOT YET IMPLEMENTED...{},{}", pendingDocument.getTI_DOCU(), pendingDocument.getNU_DOCU());
            case "NCR" ->
                log.info("NOT YET IMPLEMENTED...{},{}", pendingDocument.getTI_DOCU(), pendingDocument.getNU_DOCU());
            case "NCD" ->
                log.info("NOT YET IMPLEMENTED...{},{}", pendingDocument.getTI_DOCU(), pendingDocument.getNU_DOCU());
            default -> log.info("Tipo de documento no identificado...");
        }
        return null;
    }

}
