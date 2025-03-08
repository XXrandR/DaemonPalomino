package com.gpal.DaemonPalomino.network;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.network.security.WSSEHeaderSOAPHandler;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.BizlinksOSE;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.BizlinksOSE_Service;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.SOAPException_Exception;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.StatusCdr;
import com.gpal.DaemonPalomino.utils.DataUtil;
import com.gpal.DaemonPalomino.utils.PropertiesHelper;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.HandlerResolver;
import jakarta.xml.ws.handler.PortInfo;

@Slf4j
public class WsService {

    private final BizlinksOSE_Service bService;
    private BizlinksOSE bislinksOse;
    private URL wsdlURL;
    Properties properties;
    private final String documentsCdr;

    @Inject
    public WsService() {
        try {
            properties = new Properties();
            properties = PropertiesHelper.obtainProps();
            wsdlURL = new URL(properties.getProperty("wsdl.url"));
            documentsCdr = properties.getProperty("location.documents");
            bService = new BizlinksOSE_Service(wsdlURL);
        } catch (Exception ex) {
            log.error("Logging into WsService...", ex);
            throw new RuntimeException("Failed to load application.properties", ex);
        }
    }

    // THROUGH NORMAL LIFECYCLE
    public BizlinksOSE putAuthentication(BizlinksOSE_Service service, GenericDocument document) {
        String user = properties.getProperty("keys." + document.getCompanyID() + ".user");
        String pass = properties.getProperty("keys." + document.getCompanyID() + ".pass");
        log.info("The User: {} and Pass: {}", user, pass);
        service.setHandlerResolver(new HandlerResolver() {
            @SuppressWarnings("rawtypes")
            @Override
            public List<Handler> getHandlerChain(PortInfo portInfo) {
                List<Handler> handlerChain = new ArrayList<>();
                handlerChain.add(new WSSEHeaderSOAPHandler(user, pass));
                return handlerChain;
            }
        });
        return bService.getBizlinksOSEPort();
    }

    // DIRECT CDR
    public BizlinksOSE putAuthentication(BizlinksOSE_Service service, String user, String pass) {
        log.info("The User: {} and Pass: {}", user, pass);
        service.setHandlerResolver(new HandlerResolver() {
            @SuppressWarnings("rawtypes")
            @Override
            public List<Handler> getHandlerChain(PortInfo portInfo) {
                List<Handler> handlerChain = new ArrayList<>();
                handlerChain.add(new WSSEHeaderSOAPHandler(user, pass));
                return handlerChain;
            }
        });
        return bService.getBizlinksOSEPort();
    }

    public GenericDocument sendDocument(GenericDocument document, String fileName,
            jakarta.activation.DataHandler contentFile)
            throws SOAPException_Exception {
        log.info("Sending {}.", fileName);
        bislinksOse = putAuthentication(bService, document);
        byte[] data = bislinksOse.sendBill(fileName, contentFile);
        DataUtil.unzipFiles(documentsCdr + "/cdr/", data);
        log.debug("Result of sending into OSE: {}", data);
        return document;
    }

    // for the lifecycle complete
    public GenericDocument getStatusCdr(GenericDocument document) throws SOAPException_Exception {
        bislinksOse = putAuthentication(bService, document);
        StatusCdr status = new StatusCdr();
        log.info("Document to cdr: Ruc {}, Numero {}, Tipo {}, Serie {}", DataUtil.obtainCompanyId(document),
                String.valueOf(Integer.valueOf(document.getNU_DOCU().substring(5, document.getNU_DOCU().length()))),
                document.getTI_DOCU().equals("BOL") ? "03" : "01",
                document.getNU_DOCU().substring(0, 4));
        status.setRucComprobante(DataUtil.obtainCompanyId(document));
        status.setNumeroComprobante(String.valueOf(Integer.valueOf(document.getNU_DOCU().substring(5, document.getNU_DOCU().length()))));
        status.setTipoComprobante(document.getTI_DOCU().equals("BOL") ? "03" : "01");
        status.setSerieComprobante(document.getNU_DOCU().substring(0, 4));
        byte[] data = bislinksOse.getStatusCdr(status);
        DataUtil.unzipFiles(documentsCdr + "/cdr/", data);
        return document;
    }

    // for only CDR
    public boolean getStatusCdr(String co_seri, String nu_docu, String ti_docu, String co_empr) throws SOAPException_Exception {
        String user = properties.getProperty("keys." + co_empr + ".user");
        String pass = properties.getProperty("keys." + co_empr + ".pass");
        bislinksOse = putAuthentication(bService, user, pass);
        StatusCdr status = new StatusCdr();
        log.info("Document to cdr: Ruc {}, Numero {}, Tipo {}, Serie {}", co_empr,nu_docu, ti_docu, co_seri);
        status.setRucComprobante(co_empr);
        status.setNumeroComprobante(nu_docu);
        status.setTipoComprobante(ti_docu);
        status.setSerieComprobante(co_seri);
        byte[] data = bislinksOse.getStatusCdr(status);
        DataUtil.unzipFiles(documentsCdr + "/cdr/", data);
        return true;
    }

    public boolean getStatus(GenericDocument document) throws SOAPException_Exception {
        bislinksOse = putAuthentication(bService, document);
        bislinksOse.getStatus("");
        return true;
    }
    
    // Don't fckn remove this part it's for summaries

    // public void sendDocuments(String pathBase, List<FirmSignature> lSignatures) {
    // try {
    // lSignatures.forEach(data -> {
    // if (data instanceof SummaryDocument summaryDocument) {
    // log.debug("SUMMARIE type document found... sending.");
    // sendSummarie(pathBase, summaryDocument);
    // } else if (data instanceof FacDocument facDocument) {
    // log.debug("FAC type document found... sending.");
    // sendDocument(pathBase, facDocument);
    // } else if (data instanceof NcrDocument ncrDocument) {
    // log.debug("NCR type document found... sending.");
    // sendDocument(pathBase, ncrDocument);
    // } else if (data instanceof NcdDocument ncdDocument) {
    // log.debug("NCD type document found... sending.");
    // sendDocument(pathBase, ncdDocument);
    // }
    // });
    // } catch (Exception ex) {
    // log.error("Error sending documents different: ", ex);
    // }
    // }

    // public void obtainStatusDocument(String ticket) {
    // try {
    // StatusResponse sresponse = bs.getStatus(ticket);
    // log.info("INFO of StatusDocument: code: {}, content: {}",
    // sresponse.getStatusCode(),
    // sresponse.getContent());
    // } catch (Exception ex) {
    // log.error("Error Status Document..", ex);
    // }
    // }

    // public void obtainCdr(String rucComprobante, String tipoComprobante, String
    // serieComprobante,
    // String numeroComprobante) {
    // try {
    // StatusCdr statusCdr = new StatusCdr();
    // statusCdr.setRucComprobante(rucComprobante);
    // statusCdr.setTipoComprobante(tipoComprobante);
    // statusCdr.setSerieComprobante(serieComprobante);
    // statusCdr.setNumeroComprobante(numeroComprobante);
    // byte[] sresponse = bs.getStatusCdr(statusCdr);
    // log.info("INFO of StatusDocument content: {}", sresponse.toString());
    // } catch (Exception ex) {
    // log.error("Error Status Document..", ex);
    // }
    // }

    // public void sendSummarie(String pathBase, GenericDocument lSignatures) {
    // log.debug("Trying to send summaries...");
    // if (lSignatures instanceof SummaryDocument summaryDocument) {
    // try {
    // String user = properties.getProperty("keys." + summaryDocument.getCO_EMPR() +
    // ".user");
    // String pass = properties.getProperty("keys." + summaryDocument.getCO_EMPR() +
    // ".pass");
    // log.info("The User: {} and Pass: {}", user, pass);

    // bService.setHandlerResolver(new HandlerResolver() {
    // @SuppressWarnings("rawtypes")
    // @Override
    // public List<Handler> getHandlerChain(PortInfo portInfo) {
    // List<Handler> handlerChain = new ArrayList<>();
    // handlerChain.add(new WSSEHeaderSOAPHandler(user, pass));
    // return handlerChain;
    // }
    // });
    // bislinksOse = bService.getBizlinksOSEPort();

    // String fileName = summaryDocument.getCompanyRuc() + "-" +
    // summaryDocument.getNU_DOCU();

    // log.info("FILENAME _BASE THING : {}", fileName);
    // log.info("Info response summary: {}",
    // bislinksOse.sendSummary(fileName + ".zip",
    // DataUtil.obtainFileDataHandlerZip(pathBase + fileName)));
    // } catch (SOAPException_Exception | ServerSOAPFaultException ex) {
    // processError(ex, summaryDocument);
    // log.warn("SOAP Exception in :" + summaryDocument.getNU_DOCU() +
    // ",sendSummarie: {}", ex.getMessage());
    // }
    // } else {
    // log.debug("Document type unknown...");
    // }
    // }

    // private void sendDocument(String pathBase, GenericDocument signature) {
    // try {
    // String fileName = signature.getTI_DOCU() + "-" + signature.getCO_EMPR() + "-"
    // + signature.getNU_DOCU() + ".zip";
    // bs.sendBill(fileName, DataUtil.obtainFileDataHandlerZip(pathBase +
    // fileName));
    // } catch (Exception ex) {
    // log.error("SOAP Exception " + signature.getNU_DOCU() + ", sendDocument: ",
    // ex);
    // }
    // }

}
