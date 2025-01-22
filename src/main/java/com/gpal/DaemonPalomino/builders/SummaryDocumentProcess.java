package com.gpal.DaemonPalomino.builders;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import com.gpal.DaemonPalomino.models.DetSummaryDocument;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import com.gpal.DaemonPalomino.models.SummaryDocument;
import com.gpal.DaemonPalomino.utils.DataUtil;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SummaryDocumentProcess {

    private final VelocityEngine velocityEngine;

    @Inject
    public SummaryDocumentProcess(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
    }

    public List<FirmSignature> sendDocuments(int sizeBatch, DataSource dataSource, String location) {
        List<Object> input = new ArrayList<>();
        input.add("");
        input.add("BOL");
        input.add("005");
        input.add("105");
        input.add("GEN_SUM");

        List<DetSummaryDocument> documentBrws = DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU ?,?,?,?,?",
                input, DetSummaryDocument.class);

        List<SummaryDocument> summaries = documentBrws.stream()
                .collect(Collectors.groupingBy(doc -> new SummaryDocument(
                        doc.getNumSummary(),
                        doc.getDateRefe(),
                        doc.getIssueDate(),
                        doc.getCompanyId(),
                        doc.getCompanyName()),
                        Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    SummaryDocument summary = entry.getKey();
                    summary.setDocuments(entry.getValue());
                    return summary;
                }).collect(Collectors.toList());

        List<FirmSignature> sm = new ArrayList<>();

        summaries.forEach(data -> {
            log.info("Summary obtained: {}", data.toString());
            data.setNU_DOCU(data.getNumSummary());
            data.setTI_DOCU("SUM");
            data.setCO_EMPR(data.getCompanyId());
            VelocityContext context = new VelocityContext();
            context.put("document", summaries);
            Template template = velocityEngine.getTemplate("/templates/xml/pasajes/SummaryDocument.vm");
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            DataUtil.generateFile(data, writer, location);
            sm.add(data);
        });
        return sm;
    }

}
