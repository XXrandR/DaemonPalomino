package com.gpal.DaemonPalomino.builders;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.models.dao.PendingDocument;
import com.gpal.DaemonPalomino.builders.helpers.XmlSec;
import com.gpal.DaemonPalomino.utils.DataUtil;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FirmDocument {

    @Inject
    public FirmDocument() {
    }

    public List<GenericDocument> signDocuments(DataSource dataSource, List<GenericDocument> documentsToFirm) {
        return documentsToFirm.stream().map(item -> {

            // firm the document
            XmlSec documnt = new XmlSec();
            GenericDocument item1 = documnt.firmDocument(item);

            // save the status on DB
            DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU_I01 ?,?,?,?,?,?",
                    Arrays.asList("FIR", "FIRMADO", item.getNU_DOCU(), item.getTI_DOCU(),
                            item.getCO_EMPR(),
                            item.getCO_ORIG()),
                    PendingDocument.class);

            log.info("1 Processing document: {}", item1);
            return item1;

        }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
