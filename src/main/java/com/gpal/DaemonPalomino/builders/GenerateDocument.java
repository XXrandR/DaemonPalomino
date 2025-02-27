package com.gpal.DaemonPalomino.builders;

import java.util.Objects;
import java.util.Arrays;
import java.io.StringWriter;
import java.util.List;
import javax.sql.DataSource;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.models.dao.PendingDocument;
import com.gpal.DaemonPalomino.models.BolDocument;
import com.gpal.DaemonPalomino.utils.DataUtil;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;

@Slf4j
public class GenerateDocument {

    private final VelocityEngine velocityEngine;

    @Inject
    public GenerateDocument(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
    }

    public List<GenericDocument> generateDocuments(int sizeBatch, DataSource dataSource, String location) {
        return DataUtil.executeProcedure(dataSource,
                "EXEC SP_TTHELP_DOCU01 ?,?,?",
                Arrays.asList(sizeBatch, "BOL", "105"),
                PendingDocument.class)
                .stream().map(doc -> generateXMLUnsigned(dataSource, doc, location))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<GenericDocument> generateDocumentUnique(DataSource dataSource, String nu_docu, String ti_docu,
            String co_empr, String location, String coOrig) {
        return DataUtil.executeProcedure(dataSource, "EXEC SP_TTHELP_DOCU01 2,?,?,?,?",
                Arrays.asList(ti_docu, (coOrig == null) ? "105" : coOrig, co_empr, nu_docu),
                PendingDocument.class).stream().map(doc -> generateXMLUnsigned(dataSource, doc, location))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private GenericDocument generateXMLUnsigned(DataSource dataSource, PendingDocument pendingDocument,
            String location) {
        List<Object> input = Arrays.asList(pendingDocument.getNU_DOCU(), pendingDocument.getTI_DOCU(),
                pendingDocument.getCO_EMPR(), pendingDocument.getCO_ORIG());

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
                // "/templates/xml/pasajes/FDocument.vm");
            }
            case "NCR" -> {
                log.info("NOT YET IMPLEMENTED...{},{}", pendingDocument.getTI_DOCU(),
                        pendingDocument.getNU_DOCU());
                // return generateDoc(dataSource, input, pendingDocument, location,
                // NcrDocument.class,
                // "/templates/xml/pasajes/NCDocument.vm");
            }
            case "NCD" -> {
                log.info("NOT YET IMPLEMENTED...{},{}", pendingDocument.getTI_DOCU(),
                        pendingDocument.getNU_DOCU());
                // generateDoc(dataSource, input, pendingDocument, location, NcdDocument.class,
                // "/templates/xml/pasajes/NDDocument.vm");
            }
            default -> log.info("Tipo de documento no identificado...");
        }
        return null;
    }

    public <T extends GenericDocument> T generateDoc(DataSource dataSource, List<Object> input,
            PendingDocument pendingDocument,
            String location, Class<T> clazz, String templ) {
        List<T> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU ?,?,?,?",
                input,
                clazz);

        return dbDocuments.stream().map(docu -> {
            log.debug("Documents processing : {}", docu.getNU_DOCU() + " " + docu.getTI_DOCU());
            // T document = docu.get(0);
            VelocityContext context = new VelocityContext();
            context.put("document", docu);
            Template template = velocityEngine.getTemplate(templ);
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            docu.setNU_DOCU(pendingDocument.getNU_DOCU());
            docu.setTI_DOCU(pendingDocument.getTI_DOCU());
            docu.setCO_EMPR(pendingDocument.getCO_EMPR());
            docu.setCO_ORIG(pendingDocument.getCO_ORIG());
            DataUtil.generateFile(docu, writer, location);
            return docu;
        }).findFirst().get();
    }

}
