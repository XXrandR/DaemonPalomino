package com.gpal.DaemonPalomino.network;

import java.net.URL;
import java.util.List;
import com.gpal.DaemonPalomino.models.FacDocument;
import com.gpal.DaemonPalomino.models.NcdDocument;
import com.gpal.DaemonPalomino.models.NcrDocument;
import com.gpal.DaemonPalomino.models.SummaryDocument;
import com.gpal.DaemonPalomino.models.BolDocument;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.BizlinksOSE;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.BizlinksOSE_Service;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.SOAPException_Exception;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.StatusCdr;
import com.gpal.DaemonPalomino.utils.DataUtil;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WsService {

    private final BizlinksOSE_Service bService;
    private final BizlinksOSE bs;
    private URL wsdlURL;

    @Inject
    public WsService() {
        try {
            wsdlURL = new URL("https://testose2.bizlinks.com.pe/ol-ti-itcpe/billService?wsdl");
            bService = new BizlinksOSE_Service(wsdlURL);
            bs = bService.getBizlinksOSEPort();
        } catch (Exception ex) {
            log.error("Logging into WsService...", ex);
            throw new RuntimeException("Failed to load application.properties", ex);
        }
    }

    public void sendDocuments(String pathBase, List<FirmSignature> lSignatures) {
        try {
            lSignatures.forEach(data -> {
                if (data instanceof BolDocument) {
                    log.debug("BOL type document found... omiting.");
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
            bs.getStatus(ticket);
        } catch (Exception ex) {
            log.error("Error Status Document..", ex);
        }
    }

    private void obtainStatusCdr(String ticket) {
        try {
            String numeroComprobante = "";
            String rucComprobante = "";
            String serieComprobante = "";
            String tipoComprobante = "";

            var a = new StatusCdr();
            a.setRucComprobante(rucComprobante);
            a.setTipoComprobante(tipoComprobante);
            a.setSerieComprobante(serieComprobante);
            a.setNumeroComprobante(numeroComprobante);
            bs.getStatusCdr(a);
        } catch (Exception ex) {
            log.error("Error Status Document..", ex);
        }
    }

    public void sendSummarie(String pathBase, List<FirmSignature> lSignatures) {
        if (lSignatures.isEmpty()) {
            log.debug("Empty list to upload summaries...");
            return;
        }
        lSignatures.forEach(data -> {
            if (data instanceof SummaryDocument summaryDocument) {
                log.debug("SUMMARY type document found... omitting.");
                try {
                    String fileName = summaryDocument.getTI_DOCU() + "-" + summaryDocument.getCO_EMPR() + "-"
                            + summaryDocument.getNU_DOCU() + ".xml";
                    String response = bs.sendSummary(fileName, DataUtil.obtainFileDataHandler(pathBase + fileName));
                    log.info("INFO RESPONSE SUMMARY: {}",response);
                } catch (SOAPException_Exception ex) {
                    log.error("SOAP Exception" + summaryDocument.getNU_DOCU() + ", sendSummarie: ", ex);
                }
            } else {
                log.debug("Document type unknown...");
            }
        });
    }

}
