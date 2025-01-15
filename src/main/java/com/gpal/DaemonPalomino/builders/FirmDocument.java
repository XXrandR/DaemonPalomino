package com.gpal.DaemonPalomino.builders;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.gpal.DaemonPalomino.models.DB.Invoice;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.BizlinksOSE;
import com.gpal.DaemonPalomino.osebizlinks.wsdl.BizlinksOSE_Service;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.ws.BindingProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FirmDocument {
    private final BizlinksOSE_Service bService;
    private final BizlinksOSE bs;
    private String locationUnsigned;
    private URL wsdlURL;

    public FirmDocument() {
        try (InputStream inputStream = FirmDocument.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (inputStream == null)
                throw new RuntimeException("Unable to find application.properties");
            Properties properties = new Properties();
            properties.load(inputStream);

            locationUnsigned = properties.getProperty("location.unsigned");
            wsdlURL = new URL(properties.getProperty("wsdl.url")); // Assuming wsdl.url is in your properties file

            log.info("WSDL: " + wsdlURL.toString());
            bService = new BizlinksOSE_Service(wsdlURL);
            bs = bService.getBizlinksOSEPort();
            ((BindingProvider) bs).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, locationUnsigned);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public void sendDocuments() {
        try {
            JAXBContext context = JAXBContext.newInstance(Invoice.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            File xmlFile = obtainFile();
            Invoice invoice = (Invoice) unmarshaller.unmarshal(xmlFile);
            log.info("THE BOLETA XML: " + invoice.toString());
            DataHandler dataZip = zipXmlDto(invoice);
            byte[] data = bs.sendBill(xmlFile.getName(), dataZip);
            log.info("DATA AFTER SENDING BILL: " + data.toString());
        } catch (Exception ex) {
            log.error("ERROR processing document general...", ex);
        }
    }

    private DataHandler zipXmlDto(Invoice invoice) {
        try {
            JAXBContext context = JAXBContext.newInstance(Invoice.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            ByteArrayOutputStream xmlOutputStream = new ByteArrayOutputStream();
            System.out.println("THE EXPECTED CONVERSION OF XML: " + xmlOutputStream.toString());
            marshaller.marshal(invoice, xmlOutputStream);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.w3.org/2001/XMLSchema-instance");

            // Step 2: Compress the XML File and write to a file
            String zipFileName = "document.zip"; // Name of the zip file
            FileOutputStream fos = new FileOutputStream(zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zos.putNextEntry(new ZipEntry("document.xml")); // Name of the file inside the ZIP
            zos.write(xmlOutputStream.toByteArray());
            zos.closeEntry();
            zos.close();
            fos.close();

            // Optional: Return as DataHandler if needed
            DataSource dataSource = new FileDataSource(zipFileName);
            return new DataHandler(dataSource);
        } catch (Exception ex) {
            log.error("ERROR in compression...", ex);
            return null;
        }
    }

    private File obtainFile() {
        return new File(
                "/home/maximus/hitherebuddy/hitherebuddy/turismopalomino/storage/app/tenancy/tenants/tenancy_palomino/unsigned/20515659324-03-B662-6341.xml");
    }

}
