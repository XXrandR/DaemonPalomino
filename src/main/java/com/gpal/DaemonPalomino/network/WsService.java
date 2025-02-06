package com.gpal.DaemonPalomino.network;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.models.FacDocument;
import com.gpal.DaemonPalomino.models.NcdDocument;
import com.gpal.DaemonPalomino.models.NcrDocument;
import com.gpal.DaemonPalomino.models.SummaryDocument;
import com.gpal.DaemonPalomino.models.dao.PendingDocument;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.network.helpers.WSSEHeaderSOAPHandler;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.BizlinksOSE;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.BizlinksOSE_Service;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.SOAPException_Exception;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.StatusResponse;
import com.gpal.DaemonPalomino.utils.DataUtil;
import com.gpal.DaemonPalomino.utils.PropertiesHelper;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.HandlerResolver;
import jakarta.xml.ws.handler.PortInfo;
import com.sun.xml.ws.fault.ServerSOAPFaultException;

@Slf4j
public class WsService {

    private final BizlinksOSE_Service bService;
    private BizlinksOSE bs;
    private URL wsdlURL;
    Properties properties;
    private DataSource dataSource;

    @Inject
    public WsService(DataSource dataSource) {
        try {
            wsdlURL = new URL("https://testose2.bizlinks.com.pe/ol-ti-itcpe/billService?wsdl");
            bService = new BizlinksOSE_Service(wsdlURL);
            properties = new Properties();
            properties = PropertiesHelper.obtainProps();
            this.dataSource = dataSource;
        } catch (Exception ex) {
            log.error("Logging into WsService...", ex);
            throw new RuntimeException("Failed to load application.properties", ex);
        }
    }

    public void sendDocuments(String pathBase, List<FirmSignature> lSignatures) {
        try {
            lSignatures.forEach(data -> {
                if (data instanceof SummaryDocument summaryDocument) {
                    log.debug("SUMMARIE type document found... sending.");
                    sendSummarie(pathBase, summaryDocument);
                } else if (data instanceof FacDocument facDocument) {
                    log.debug("FAC type document found... sending.");
                    sendDocument(pathBase, facDocument);
                } else if (data instanceof NcrDocument ncrDocument) {
                    log.debug("NCR type document found... sending.");
                    sendDocument(pathBase, ncrDocument);
                } else if (data instanceof NcdDocument ncdDocument) {
                    log.debug("NCD type document found... sending.");
                    sendDocument(pathBase, ncdDocument);
                }
            });
        } catch (Exception ex) {
            log.error("Error sending documents different: ", ex);
        }
    }

    private void sendDocument(String pathBase, GenericDocument signature) {
        try {
            String fileName = signature.getTI_DOCU() + "-" + signature.getCO_EMPR() + "-"
                    + signature.getNU_DOCU() + ".xml";
            bs.sendBill(fileName, DataUtil.obtainFileDataHandler(pathBase + fileName));
        } catch (Exception ex) {
            log.error("SOAP Exception " + signature.getNU_DOCU() + ", sendDocument: ", ex);
        }
    }

    private void obtainStatusDocument(String ticket) {
        try {
            StatusResponse sresponse = bs.getStatus(ticket);
            log.info("INFO of StatusDocument: code: {}, content: {}", sresponse.getStatusCode(),
                    sresponse.getContent());
        } catch (Exception ex) {
            log.error("Error Status Document..", ex);
        }
    }

    public void sendSummarie(String pathBase, FirmSignature lSignatures) {
        log.debug("Trying to send summaries...");
        if (lSignatures instanceof SummaryDocument summaryDocument) {
            try {
                String user = properties.getProperty("keys." + summaryDocument.getCO_EMPR() + ".user");
                String pass = properties.getProperty("keys." + summaryDocument.getCO_EMPR() + ".pass");
                log.info("The User: {} and Pass: {}", user, pass);

                bService.setHandlerResolver(new HandlerResolver() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public List<Handler> getHandlerChain(PortInfo portInfo) {
                        List<Handler> handlerChain = new ArrayList<>();
                        handlerChain.add(new WSSEHeaderSOAPHandler(user, pass));
                        return handlerChain;
                    }
                });
                bs = bService.getBizlinksOSEPort();

                String fileName = summaryDocument.getCompanyRuc() + "-"
                        + summaryDocument.getNU_DOCU();
                log.info("FILENAME _BASE THING : {}", fileName);
                log.info("Info response summary: {}",
                        bs.sendSummary(fileName + ".zip", DataUtil.obtainFileDataHandlerZip(pathBase + fileName)));
            } catch (SOAPException_Exception | ServerSOAPFaultException ex) {
                processError(ex, summaryDocument);
                log.warn("SOAP Exception in :" + summaryDocument.getNU_DOCU() + ", sendSummarie: {}", ex.getMessage());
            }
        } else {
            log.debug("Document type unknown...");
        }
    }

    // In case of an error save into the server that error, register it
    private void processError(Exception ex, SummaryDocument summaryDocument) {
        Pattern pattern = Pattern.compile("\\b\\d{4}\\b");
        Matcher matcher = pattern.matcher(ex.getMessage());
        if (matcher.find()) {
            String errorNumber = matcher.group();
            System.out.println("Error number: " + errorNumber);
            List<Object> input = new ArrayList<>();
            input.add(errorNumber);
            input.add(summaryDocument.getNU_DOCU());
            input.add(summaryDocument.getTI_DOCU());
            input.add(summaryDocument.getCO_EMPR());
            log.info("Input Parameters to anulate...{}", input.toString());
            DataUtil.executeProcedure(dataSource, "EXEC SP_TMDOCU_XML_U02 ?,?,?,?", input,
                    PendingDocument.class);
        } else {
            System.out.println("No error number found.");
        }
    }

}
