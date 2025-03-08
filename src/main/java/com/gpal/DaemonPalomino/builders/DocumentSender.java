package com.gpal.DaemonPalomino.builders;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.models.dao.PendingDocument;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.network.WsService;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.SOAPException_Exception;
import com.gpal.DaemonPalomino.utils.DataUtil;
import com.gpal.DaemonPalomino.utils.PropertiesHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocumentSender {

    private final WsService wsService;
    private final DataSource dataSource;
    private final String pathBase;
    Properties properties = new Properties();

    @Inject
    public DocumentSender(WsService wsService, DataSource dataSource) {
        properties = PropertiesHelper.obtainProps();
        this.wsService = wsService;
        this.dataSource = dataSource;
        this.pathBase = properties.getProperty("location.documents");
    }

    // to assemble the request
    public List<GenericDocument> sendDocument(List<GenericDocument> genericDocuments) {
        return genericDocuments.stream()
                .map(item -> sendDocument(item))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    public boolean downloadCdr(String co_seri, String nu_docu, String ti_docu, String co_empr) {
        try {
            wsService.getStatusCdr(co_seri, String.valueOf(Integer.valueOf(nu_docu)), ti_docu, co_empr);
            return true;
        } catch (SOAPException_Exception ex) {
            processError(ex, null);
            return false;
        }
    }

    private GenericDocument sendDocument(GenericDocument object) {
        try {
            GenericDocument document = wsService.sendDocument(object,
                    DataUtil.obtainNameByTypeDocumentNotXml(object) + ".zip",
                    DataUtil.obtainFileDataHandlerZip(
                            pathBase + "/signed/" + DataUtil.obtainNameByTypeDocumentNotXml(object)));
            GenericDocument document1 = wsService.getStatusCdr(document);

            // save the status on DB
            DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU_I01 ?,?,?,?,?,?",
                    Arrays.asList("ACE", "ACEPTADO", object.getNU_DOCU(), object.getTI_DOCU(),
                            object.getCO_EMPR(),
                            object.getCO_ORIG()),
                    PendingDocument.class);
            return document1;
        } catch (SOAPException_Exception ex) {
            processError(ex, object);
            return null;
        }
    }

    // In case of an error save into the server that error, register it
    private void processError(SOAPException_Exception ex, GenericDocument document) {
        Pattern pattern = Pattern.compile("\\b\\d{4}\\b");
        Matcher matcher = pattern.matcher(ex.getMessage());
        if (matcher.find()) {
            String errorNumber = matcher.group();
            log.info("Error processing ...{},{},{},{}", document.getNU_DOCU(), document.getTI_DOCU(),
                    document.getCO_EMPR(), errorNumber);
            DataUtil.executeProcedure(dataSource, "EXEC SP_TMDOCU_XML_U02 ?,?,?,?",
                    Arrays.asList(errorNumber, document.getNU_DOCU(), document.getTI_DOCU(), document.getCO_EMPR()),
                    PendingDocument.class);
        } else {
            log.info("No error number found.");
        }
    }

}
