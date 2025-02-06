package com.gpal.DaemonPalomino.builders;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.models.dao.PendingDocument;
import com.gpal.DaemonPalomino.models.BolDocument;
import com.gpal.DaemonPalomino.models.NcdDocument;
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
        input.add("001"); // for only pasajes
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

    public List<FirmSignature> generateDocumentUnique(DataSource dataSource, String nu_docu, String ti_docu,
            String co_empr, String location, String tiOper) {
        List<Object> input = new ArrayList<>();
        if (tiOper == null) {
            tiOper = "105";
        }
        input.add(tiOper);
        input.add(co_empr);
        input.add(nu_docu);
        // Obtaining restant documents
        List<PendingDocument> documentBrws = DataUtil.executeProcedure(dataSource, "EXEC SP_TTHELP_DOCU01 0,?,?,?",
                input,
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
                return generateDoc(dataSource, input, pendingDocument, location, BolDocument.class,
                        "/templates/xml/pasajes/BDocument.vm");
            }
            case "FAC" -> {
                log.info("NOT YET IMPLEMENTED...{},{}", pendingDocument.getTI_DOCU(),
                        pendingDocument.getNU_DOCU());
                // return generateDoc(dataSource, input, pendingDocument, location,
                // FacDocument.class,
                // "/templates/xml/pasajes/ticket.vm");
            }
            case "NCR" -> {
                log.info("NOT YET IMPLEMENTED...{},{}", pendingDocument.getTI_DOCU(),
                        pendingDocument.getNU_DOCU());
                // return generateDoc(dataSource, input, pendingDocument, location,
                // NcrDocument.class,
                // "/templates/xml/pasajes/ticket.vm");
            }
            case "NCD" -> {
                // log.info("NOT YET IMPLEMENTED...{},{}", pendingDocument.getTI_DOCU(),
                // pendingDocument.getNU_DOCU());
                return generateDoc(dataSource, input, pendingDocument, location, NcdDocument.class,
                        "/templates/xml/pasajes/ticket.vm");
            }
            default -> log.info("Tipo de documento no identificado...");
        }
        return null;
    }

    public <T extends GenericDocument> FirmSignature generateDoc(DataSource dataSource, List<Object> input,
            PendingDocument pendingDocument,
            String location, Class<T> clazz, String templ) {
        List<T> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU ?,?,?,?",
                input,
                clazz);

        if (dbDocuments != null) {
            if (dbDocuments.isEmpty() == false) {
                log.debug("DOCUMENTS BEING FOUND: {}", dbDocuments.toString());
                T document = dbDocuments.get(0);
                VelocityContext context = new VelocityContext();
                log.debug("DEBUG OF DIGEST GEN DOCU: {} (if it's null it's correct)",
                        document.getDigestValue());
                context.put("document", document);
                Template template = velocityEngine.getTemplate(templ);
                StringWriter writer = new StringWriter();
                template.merge(context, writer);
                dbDocuments.get(0).setNU_DOCU(pendingDocument.getNU_DOCU());
                dbDocuments.get(0).setTI_DOCU(pendingDocument.getTI_DOCU());
                dbDocuments.get(0).setCO_EMPR(pendingDocument.getCO_EMPR());
                dbDocuments.get(0).setCO_ORIG(pendingDocument.getCO_ORIG());
                DataUtil.generateFile(document, writer, location);
                return dbDocuments.get(0);
            } else {
                log.debug("DOCUMENTS NOT FOUND: {},{},{},{}", pendingDocument.getNU_DOCU(),
                        pendingDocument.getTI_DOCU(),
                        pendingDocument.getCO_EMPR(), pendingDocument.getCO_ORIG());
                return null;
            }
        } else {
            log.debug("EMPTY DOCUMENTS: {},{},{},{}", pendingDocument.getNU_DOCU(),
                    pendingDocument.getTI_DOCU(),
                    pendingDocument.getCO_EMPR(), pendingDocument.getCO_ORIG());
            return null;
        }
    }

}
