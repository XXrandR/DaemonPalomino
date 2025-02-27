package com.gpal.DaemonPalomino.processor;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.models.dao.PendingDocument;
import com.gpal.DaemonPalomino.network.WsService;
import com.gpal.DaemonPalomino.utils.DataUtil;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocumentAnulate {

    private final DataSource dataSource;

    @Inject
    public DocumentAnulate(DataSource dataSource, WsService wsService) {
        this.dataSource = dataSource;
    }

    public Boolean anulateDocument(String nu_docu, String ti_docu, String co_empr) {
        List<Object> input = new ArrayList<>();
        input.add(nu_docu);
        input.add(ti_docu);
        input.add(co_empr);
        List<PendingDocument> documents = DataUtil.executeProcedure(dataSource, "EXEC SP_ANUL_DOCU_XML ?,?,?", input,
                PendingDocument.class);
        if (!documents.isEmpty()) {
            log.info("Document anulated, {}", documents.get(0).toString());
        } else {
            log.info("No documents to anulate.");
        }
        return !documents.isEmpty();
    }

}
