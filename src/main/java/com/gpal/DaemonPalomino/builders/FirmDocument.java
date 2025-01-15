package com.gpal.DaemonPalomino.builders;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Base64;
import java.security.PrivateKey;
import javax.security.cert.X509Certificate;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import java.security.Signature;
import com.gpal.DaemonPalomino.models.DBDocument;
import com.gpal.DaemonPalomino.models.FirmSignature;
import com.gpal.DaemonPalomino.models.PendingDocument;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FirmDocument {

    private VelocityEngine velocityEngine;
    private String locationDocuments;

    @Inject
    public FirmDocument(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;

        // set location of unsigned,signed,pdf, and cdr
        try (InputStream inputStream = FirmDocument.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            locationDocuments = properties.getProperty("location.documents") + "/signed/";
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    // private final BizlinksOSE_Service bService;
    // private final BizlinksOSE bs;
    // private String locationUnsigned;
    // private URL wsdlURL;

    // public FirmDocument() {
    // try (InputStream inputStream = FirmDocument.class.getClassLoader()
    // .getResourceAsStream("application.properties")) {
    // if (inputStream == null)
    // throw new RuntimeException("Unable to find application.properties");
    // Properties properties = new Properties();
    // properties.load(inputStream);
    //
    // locationUnsigned = properties.getProperty("location.unsigned");
    // wsdlURL = new URL(properties.getProperty("wsdl.url")); // Assuming wsdl.url
    // is in your properties file
    //
    // log.info("WSDL: " + wsdlURL.toString());
    // bService = new BizlinksOSE_Service(wsdlURL);
    // bs = bService.getBizlinksOSEPort();
    // ((BindingProvider)
    // bs).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
    // locationUnsigned);
    // } catch (IOException e) {
    // throw new RuntimeException("Failed to load application.properties", e);
    // }
    // }

    public List<PendingDocument> signDocument(List<FirmSignature> documentsToFirm) {
        documentsToFirm.forEach(item -> {
            item.setCertificate("MY GENERATED SIGNATURE");
            item.setSignatureValue("MY GENERATED SIGNATURE");
            item.setCertificate("MY GENERATED CERTIFICATE");
        });
        documentsToFirm.forEach(item -> {
            if (item instanceof DBDocument) {
                generateXMLSigned((DBDocument) item, "TBDocument.vm");
            }
            // else if (item instanceof DBDocument) {
            // generateXMLSigned((DBDocument) item, "TFDocument.vm");
            // } else if (item instanceof DBDocument) {
            // generateXMLSigned((DBDocument) item, "TBDocument.vm");
            // }

        });
        return null;
    }

    private void generateXMLSigned(DBDocument document, String nameTempl) {
        VelocityContext context = new VelocityContext();
        context.put("document", document);
        Template template = velocityEngine.getTemplate("/templates/" + nameTempl);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        generateFile(document, writer, locationDocuments);
    }

    private void generateFile(DBDocument document, StringWriter writer, String location) {
        try (java.io.FileWriter fileWriter = new java.io.FileWriter(
                location + document.getCompanyID() + document.getNuDocu() + ".xml")) {
            fileWriter.write(writer.toString());
            log.info("Generated " + location + document.getDateIssue() + document.getCompanyID()
                    + document.getNuDocu() + ".xml");
        } catch (Exception ex) {
            log.error("Error writing file...", ex);
        }
    }

    /* FROM HERE HELPERS TO SET CORRECTLY */
    private static java.security.cert.X509Certificate loadCertificate(String pemFile) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        FileInputStream is = new FileInputStream(pemFile);
        return (X509Certificate) certFactory.generateCertificate(is);
    }

    private String generateSignature(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        byte[] signedBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signedBytes);
    }

    private String generateDigest(String data) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
        md.update(data.getBytes());
        byte[] digestBytes = md.digest();
        return Base64.getEncoder().encodeToString(digestBytes);
    }

    private String getCertificateString(X509Certificate certificate) {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    // public void sendDocuments() {
    // try {
    // JAXBContext context = JAXBContext.newInstance(Invoice.class);
    // Unmarshaller unmarshaller = context.createUnmarshaller();
    // File xmlFile = obtainFile();
    // Invoice invoice = (Invoice) unmarshaller.unmarshal(xmlFile);
    // log.info("THE BOLETA XML: " + invoice.toString());
    // DataHandler dataZip = zipXmlDto(invoice);
    // byte[] data = bs.sendBill(xmlFile.getName(), dataZip);
    // log.info("DATA AFTER SENDING BILL: " + data.toString());
    // } catch (Exception ex) {
    // log.error("ERROR processing document general...", ex);
    // }
    // }
    //
    // private DataHandler zipXmlDto(Invoice invoice) {
    // try {
    // JAXBContext context = JAXBContext.newInstance(Invoice.class);
    // Marshaller marshaller = context.createMarshaller();
    // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    // ByteArrayOutputStream xmlOutputStream = new ByteArrayOutputStream();
    // System.out.println("THE EXPECTED CONVERSION OF XML: " +
    // xmlOutputStream.toString());
    // marshaller.marshal(invoice, xmlOutputStream);
    // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    // marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
    // marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
    // "http://www.w3.org/2001/XMLSchema-instance");
    //
    // // Step 2: Compress the XML File and write to a file
    // String zipFileName = "document.zip"; // Name of the zip file
    // FileOutputStream fos = new FileOutputStream(zipFileName);
    // ZipOutputStream zos = new ZipOutputStream(fos);
    // zos.putNextEntry(new ZipEntry("document.xml")); // Name of the file inside
    // the ZIP
    // zos.write(xmlOutputStream.toByteArray());
    // zos.closeEntry();
    // zos.close();
    // fos.close();
    //
    // // Optional: Return as DataHandler if needed
    // DataSource dataSource = new FileDataSource(zipFileName);
    // return new DataHandler(dataSource);
    // } catch (Exception ex) {
    // log.error("ERROR in compression...", ex);
    // return null;
    // }
    // }
    //
    // private File obtainFile() {
    // return new File(
    // "/home/maximus/hitherebuddy/hitherebuddy/turismopalomino/storage/app/tenancy/tenants/tenancy_palomino/unsigned/20515659324-03-B662-6341.xml");
    // }

}
