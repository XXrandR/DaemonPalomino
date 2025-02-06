package com.gpal.DaemonPalomino.builders;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Base64;
import java.util.List;
import javax.sql.DataSource;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.gpal.DaemonPalomino.models.BolDocument;
import com.gpal.DaemonPalomino.models.FacDocument;
import com.gpal.DaemonPalomino.models.NcdDocument;
import com.gpal.DaemonPalomino.models.NcrDocument;
import com.gpal.DaemonPalomino.models.dao.DataPdfDocument;
import com.gpal.DaemonPalomino.models.dao.DataPdfDocument.DetBolPdfDocument;
import com.gpal.DaemonPalomino.models.dao.DataPdfDocument.DetBolPdfDocument.DetBolPdfDocumentBuilder;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import com.itextpdf.html2pdf.HtmlConverter;

@Slf4j
public class PdfDocument {

    private final VelocityEngine velocityEngine;

    @Inject
    public PdfDocument(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
    }

    public Boolean generatePdfDocument(DataSource dataSource, FirmSignature genericDocument, String basePath) {
        if (genericDocument instanceof FacDocument item1) {
            return assemblePdf(createDtoFac(item1), basePath + item1.getCompanyID() + "-" + (item1.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + item1.getNU_DOCU() + ".pdf");
        } else if (genericDocument instanceof BolDocument item1) {
            return assemblePdf(createDtoBol(item1), basePath + item1.getCompanyID() + "-" + (item1.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + item1.getNU_DOCU() + ".pdf");
        } else if (genericDocument instanceof NcdDocument item1) {
            return assemblePdf(createDtoNcd(item1), basePath + item1.getCompanyID() + "-" + (item1.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + item1.getNU_DOCU() + ".pdf");
        } else if (genericDocument instanceof NcrDocument item1) {
            return assemblePdf(createDtoNcr(item1), basePath + item1.getCompanyID() + "-" + (item1.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + item1.getNU_DOCU() + ".pdf");
        } else {
            log.info("Type of document... unknown.");
            return false;
        }
    }

    private DataPdfDocument createDtoBol(BolDocument item1) {
        DetBolPdfDocumentBuilder dt = DetBolPdfDocument.builder();
        dt.cant("1");
        dt.noUnid("NIU");
        dt.description(item1.getDescription());
        dt.model("");
        dt.lote("");
        dt.serie("");
        dt.priceUnit(String.valueOf(item1.getPayableAmount()));
        dt.dto("");
        dt.total(String.valueOf(item1.getPayableAmount()));
        return DataPdfDocument.builder()
                .nuDocu(item1.getSeries() + "-" + item1.getNumber())
                .businessName(item1.getCompanyName())
                .businessID(item1.getCompanyID())
                .tiDocu("BOL")
                .direction(item1.getEstablishmentDepartamentName() + " " + item1.getEstablishmentDistrictName())
                .centTelf("")
                .dateEmition(item1.getIssueDate())
                .dateVenc(item1.getDueDate())
                .datClie(item1.getCustomerName())
                .docuClie(item1.getCustomerId())
                .directionClie("")
                .opExoneradas(String.valueOf(item1.getPayableAmount()))
                .igvAmount(String.valueOf(item1.getTaxAmount()))
                .totaPag(String.valueOf(item1.getPayableAmount()))
                .totaPagLetters(item1.getAmountInLetters())
                .codHash(item1.getDigestValue())
                .condPag("")
                .qrBase64(assembleQr(item1.getCompanyID() + "|01" + item1.getSeries() + "|" + item1.getNumber()
                        + "|0.00|" + item1.getDueDate() + "|" + item1.getTI_DOCU() + "|"
                        + item1.getCustomerId() + "|"
                        + item1.getDigestValue() + "|"
                        + item1.getPayableAmount() + "|" + item1.getDigestValue(), 300, 300))
                .documents(List.of(dt.build())).build();
    }

    private DataPdfDocument createDtoFac(FacDocument item1) {
        return null;
    }

    private DataPdfDocument createDtoNcr(NcrDocument item1) {
        return DataPdfDocument.builder().build();
    }

    private DataPdfDocument createDtoNcd(NcdDocument item1) {
        return DataPdfDocument.builder().build();
    }

    private Boolean assemblePdf(DataPdfDocument document, String pathAndFileName) {
        try {
            VelocityContext context = new VelocityContext();
            context.put("document", document);
            StringWriter writer = new StringWriter();
            Template template = velocityEngine.getTemplate("/templates/html/pasajes/pdf_template.vm");
            template.merge(context, writer);
            String html = writer.toString(); // This contains the merged HTML from Velocity
            HtmlConverter.convertToPdf(html, new FileOutputStream(pathAndFileName));
            System.out.println("PDF created successfully at: " + pathAndFileName);
            return true;
        } catch (FileNotFoundException e) {
            log.error("File not found...", e);
            return false;
        }
    }

    private String assembleQr(String data, int height, int width) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            BitMatrix matrix = qrCodeWriter.encode(new String(data.getBytes("UTF-8"), "UTF-8"),
                    BarcodeFormat.QR_CODE, width, height);
            MatrixToImageWriter.writeToStream(matrix, "PNG", pngOutputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        } catch (WriterException | IOException ex) {
            log.error("IO Error: captured in assembleQr in PdfDocument class..", ex);
            return null;
        }
    }

}
