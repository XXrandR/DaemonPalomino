package com.gpal.DaemonPalomino.builders;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.Base64;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import java.security.Signature;
import com.gpal.DaemonPalomino.models.DBDocument;
import com.gpal.DaemonPalomino.models.FirmSignature;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FirmDocument {

    private VelocityEngine velocityEngine;
    private String locationDocuments;
    private final X509Certificate cX509Certificate;
    private final String certificatePath;

    @Inject
    public FirmDocument(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;

        // set location of unsigned,signed,pdf, and cdr
        try (InputStream inputStream = FirmDocument.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            // for location
            Properties properties = new Properties();
            properties.load(inputStream);
            locationDocuments = properties.getProperty("location.documents") + "/signed/";
            // for certificated
            certificatePath = properties.getProperty("name.certificate");
            cX509Certificate = loadCertificate(properties.getProperty("name.certificate"));

        } catch (Exception e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }

    }

    public void signDocument(List<FirmSignature> documentsToFirm) {
        PrivateKey privateKey = loadPrivateKey(certificatePath);

        // put the certificate to each item
        documentsToFirm.forEach(item -> {
            item.setSignatureValue(generateSignature("MY XML FILE", privateKey));
            item.setDigestValue(generateDigest("MY XML FILE"));
            item.setCertificate(getCertificateString(cX509Certificate));
        });

        // then generate the files, accordingly
        documentsToFirm.forEach(item -> {
            if (item instanceof DBDocument) {
                generateXMLSigned((DBDocument) item, "TBDocument.vm");
            }
        });
    }

    private void generateXMLSigned(DBDocument document, String nameTempl) {
        VelocityContext context = new VelocityContext();
        log.debug("DEBUG OF DIGEST FIRM DOCU: {}",document.getDigestValue());
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
            log.info("Generated " + location + document.getCompanyID() + document.getNuDocu() + ".xml");
        } catch (Exception ex) {
            log.error("Error writing file...", ex);
        }
    }

    /* FROM HERE HELPERS TO SET CORRECTLY */
    private X509Certificate loadCertificate(String pemFile) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream is = FirmDocument.class.getClassLoader()
                .getResourceAsStream(pemFile);
        if (is == null) {
            throw new RuntimeException("Failed to load " + pemFile + " from classpath");
        }
        log.info("NAME CERTIFICATE : {}", pemFile);
        return (X509Certificate) certFactory.generateCertificate(is);
    }

    private String generateSignature(String data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes());
            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private String generateDigest(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            md.update(data.getBytes());
            byte[] digestBytes = md.digest();
            return Base64.getEncoder().encodeToString(digestBytes);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private String getCertificateString(X509Certificate certificate) {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private PrivateKey loadPrivateKey(String pemFile) {
        Security.addProvider(new BouncyCastleProvider());
        ClassLoader classLoader = FirmDocument.class.getClassLoader();
        try (Reader reader = new InputStreamReader(classLoader.getResourceAsStream(pemFile));
                PEMParser pemParser = new PEMParser(reader)) {

            Object bouncyCastleResult;
            PrivateKeyInfo info = null;

            while ((bouncyCastleResult = pemParser.readObject()) != null) {
                if (bouncyCastleResult instanceof X509CertificateHolder) {
                    continue;
                } else if (bouncyCastleResult instanceof PrivateKeyInfo) {
                    info = (PrivateKeyInfo) bouncyCastleResult;
                    break;
                } else if (bouncyCastleResult instanceof PEMKeyPair) {
                    PEMKeyPair keys = (PEMKeyPair) bouncyCastleResult;
                    info = keys.getPrivateKeyInfo();
                    break;
                } else {
                    throw new Exception("No private key found in the provided file");
                }
            }

            if (info == null) {
                throw new Exception("No private key found in the provided file");
            }

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            PrivateKey privateKey = converter.getPrivateKey(info);

            return privateKey;
        } catch (Exception ex) {
            System.err.println("Error loading private key: " + ex.getMessage());
            return null;
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

    // private PrivateKey loadPrivateKey(String pemFile) {
    // Security.addProvider(new BouncyCastleProvider());
    // ClassLoader classLoader = FirmDocument.class.getClassLoader();
    // Reader reader = new
    // InputStreamReader(classLoader.getResourceAsStream(pemFile));
    // try (PEMParser pemParser = new PEMParser(reader)) {
    // Object bouncyCastleResult = pemParser.readObject();
    // log.info("Parsed object: " + bouncyCastleResult);
    // PrivateKeyInfo info;
    // if (bouncyCastleResult instanceof PrivateKeyInfo) {
    // info = (PrivateKeyInfo) bouncyCastleResult;
    // } else if (bouncyCastleResult instanceof PEMKeyPair) {
    // PEMKeyPair keys = (PEMKeyPair) bouncyCastleResult;
    // info = keys.getPrivateKeyInfo();
    // } else {
    // log.error("No private key found in the provided file. Parsed object type: " +
    // bouncyCastleResult.getClass());
    // throw new Exception("No private key found in the provided file");
    // }
    // JcaPEMKeyConverter converter = new
    // JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
    // PrivateKey privateKey = converter.getPrivateKey(info);
    //
    // return privateKey;
    // } catch (Exception ex) {
    // log.error("Error loadPrivateKey..", ex);
    // return null;
    // }
    // }

    //
    // ClassLoader classLoader = FirmDocument.class.getClassLoader();
    // Reader reader = new
    // InputStreamReader(classLoader.getResourceAsStream(pemFile));
    //
    // PEMParser reader = new PEMParser(new FileReader(pemFile));
    // Object bouncyCastleResult = reader.readObject();
    // PrivateKeyInfo info;
    // if (bouncyCastleResult instanceof PrivateKeyInfo) {
    // info = (PrivateKeyInfo) bouncyCastleResult;
    // } else if (bouncyCastleResult instanceof PEMKeyPair) {
    // PEMKeyPair keys = (PEMKeyPair) bouncyCastleResult;
    // info = keys.getPrivateKeyInfo();
    // } else {
    // throw new Exception("No private key found in the provided file");
    // }
    // JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
    // return converter.getPrivateKey(info);

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
