package com.gpal.DaemonPalomino.builders;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.models.dao.PendingDocument;
import com.gpal.DaemonPalomino.models.SummaryDocument;
import com.gpal.DaemonPalomino.models.BolDocument;
import com.gpal.DaemonPalomino.utils.DataUtil;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FirmDocument {

    private VelocityEngine velocityEngine;
    private String locationDocuments;
    private String locationUnsignedDocuments;
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
            locationUnsignedDocuments = properties.getProperty("location.documents") + "/unsigned/";
            // for certificated
            certificatePath = properties.getProperty("name.certificate");
            cX509Certificate = loadCertificate(properties.getProperty("name.certificate"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public void signDocument(DataSource dataSource, List<FirmSignature> documentsToFirm) {
        PrivateKey privateKey = loadPrivateKey(certificatePath);

        // put the certificate to each item
        documentsToFirm.forEach(item -> {
            // obtaining the xml to firm
            GenericDocument item1 = (GenericDocument) item;
            String locationFile = locationUnsignedDocuments + item1.getTI_DOCU() + "-" + item1.getCO_EMPR() + "-"
                    + item1.getNU_DOCU() + ".xml";
            item.setSignatureValue(generateSignature(DataUtil.obtainBase64(locationFile), privateKey));
            item.setDigestValue(generateDigest(DataUtil.obtainBase64(locationFile)));
            item.setCertificate(getCertificateString(cX509Certificate));
        });

        // then generate the files, accordingly
        documentsToFirm.forEach(item -> {
            if (item instanceof BolDocument ticketDocument) {
                generateXMLSigned(ticketDocument, "xml/pasajes/ticket.vm");
                // making the register of the data being firmed
                List<Object> input = new ArrayList<>();
                var item1 = (GenericDocument) item;
                input.add("FIR");
                input.add("FIRMADO");
                input.add(item1.getNU_DOCU());
                input.add(item1.getTI_DOCU());
                input.add(item1.getCO_EMPR());
                input.add(item1.getCO_ORIG());
                DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU_I01 ?,?,?,?,?,?", input, PendingDocument.class);
            } else if (item instanceof SummaryDocument summaryDocument) {
                generateXMLSigned(summaryDocument, "xml/pasajes/SummaryDocument.vm");
                // making the register of the data being firmed
                List<Object> input = new ArrayList<>();
                var item1 = (GenericDocument) item;
                input.add("FIR");
                input.add("FIRMADO");
                input.add(item1.getNU_DOCU());
                input.add(item1.getTI_DOCU());
                input.add(item1.getCO_EMPR());
                input.add("105");
                DataUtil.executeProcedure(dataSource, "EXEC SP_OBT_DOCU_I01 ?,?,?,?,?,?", input, PendingDocument.class);
            }
        });
    }

    private <T extends GenericDocument> void generateXMLSigned(T document, String nameTempl) {
        VelocityContext context = new VelocityContext();
        log.debug("DEBUG OF DIGEST FIRM DOCU: {}", document.getDigestValue());
        context.put("document", document);
        Template template = velocityEngine.getTemplate("/templates/" + nameTempl);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        DataUtil.generateFile(document, writer, locationDocuments);
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
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
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
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private String getCertificateString(X509Certificate certificate) {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (CertificateEncodingException ex) {
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
                } else if (bouncyCastleResult instanceof PrivateKeyInfo privateKeyInfo) {
                    info = privateKeyInfo;
                    break;
                } else if (bouncyCastleResult instanceof PEMKeyPair keys) {
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

}
