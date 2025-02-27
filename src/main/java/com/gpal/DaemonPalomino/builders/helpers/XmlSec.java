package com.gpal.DaemonPalomino.builders.helpers;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.gpal.DaemonPalomino.models.firm.FirmSignature;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.utils.DataUtil;
import com.gpal.DaemonPalomino.utils.PropertiesHelper;
import lombok.extern.slf4j.Slf4j;

// to open, to firm and to save the firmed document
@Slf4j
public class XmlSec {

    private final String locationUnsignedDocuments;
    private final String locationSignedDocuments;
    Properties properties = new Properties();

    public XmlSec() {
        properties = PropertiesHelper.obtainProps();
        locationUnsignedDocuments = properties.getProperty("location.documents") + "/unsigned/";
        locationSignedDocuments = properties.getProperty("location.documents") + "/signed/";
    }

    public <T extends GenericDocument> T firmDocument(T sDocument) {
        try {
            log.info("Opening document to firm..");
            Document doc = openDocument(sDocument);

            log.info("Loading keys and certs..");
            String certificatePathBase;
            String certificatePathFile;
            certificatePathBase = properties.getProperty("location.firms");
            certificatePathFile = properties
                    .getProperty("name." + ((GenericDocument) sDocument).getCO_EMPR() + ".certificate");
            if (certificatePathFile == null) {
                throw new RuntimeException("Error in loading keys in firm..");
            }
            log.info("Searching the PEM file.. {}", certificatePathBase + certificatePathFile);
            Object[] keyCert = loadKeyAndCert(certificatePathBase + certificatePathFile);
            PrivateKey privateKey = (PrivateKey) keyCert[0];
            X509Certificate certificate = (X509Certificate) keyCert[1];

            log.info("Injecting Signatures..");
            injectSignature(doc, privateKey, certificate);

            saveFirmedDocumentDto(sDocument);

            log.info("Saving Document..");
            DataUtil.saveXml(doc, locationSignedDocuments + DataUtil.obtainNameByTypeDocument(sDocument));

            log.info("Validating Firm...");
            XMLSignatureValidator.validateXMLSignature(
                    locationSignedDocuments + DataUtil.obtainNameByTypeDocument(sDocument));
            return sDocument;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private <T> void saveFirmedDocumentDto(T sDocument) {
        if (sDocument instanceof FirmSignature firmedDto) {
            firmedDto.setCertificate(null);
            firmedDto.setDigestValue(null);
            firmedDto.setSignatureValue(null);
        }
    }

    private static void injectSignature(Document doc, PrivateKey privateKey, X509Certificate certificate)
            throws Exception {
        // Locate the ExtensionContent element where the signature should be placed
        NodeList extensionContentList = doc.getElementsByTagNameNS(
                "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2", "ExtensionContent");
        Element nodeSign = null;
        for (int i = 0; i < extensionContentList.getLength(); i++) {
            Element element = (Element) extensionContentList.item(i);
            if (element.getTextContent().trim().isEmpty()) {
                nodeSign = element;
                break;
            }
        }

        if (nodeSign == null) {
            nodeSign = doc.getDocumentElement();
        }

        // Create XMLSignature object
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        List<Transform> transforms = Arrays.asList(
                fac.newTransform(Transform.ENVELOPED,
                        (TransformParameterSpec) null));

        // Build Reference
        Reference ref = fac.newReference(
                "",
                fac.newDigestMethod(DigestMethod.SHA1, null),
                transforms,
                null,
                null);

        // Create SignedInfo with inclusive canonicalization
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(
                        CanonicalizationMethod.INCLUSIVE,
                        (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                Collections.singletonList(ref));

        // Build KeyInfo
        KeyInfoFactory kif = KeyInfoFactory.getInstance();
        X509Data xd = kif.newX509Data(Collections.singletonList(certificate));
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

        // Configure context *before* signing
        DOMSignContext dsc = new DOMSignContext(privateKey, nodeSign);
        dsc.setDefaultNamespacePrefix("ds");

        // Generate & sign
        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        NodeList signatureList = doc.getElementsByTagNameNS(XMLSignature.XMLNS,
                "Signature");

        if (signatureList.getLength() > 0) {
            Element signatureElement = (Element) signatureList.item(0);
            if (!signatureElement.hasAttribute("Id")) {
                signatureElement.setAttribute("Id", "signatureFACTURALOPERU");
                signatureElement.setIdAttribute("Id", true);
            }
        }
    }

    private static Object[] loadKeyAndCert(String certificatePath) throws Exception {
        org.apache.xml.security.Init.init();
        ClassLoader classLoader = XmlSec.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(certificatePath);
                PEMParser pemParser = new PEMParser(new InputStreamReader(inputStream))) {
            PrivateKey privateKey = null;
            X509Certificate certificate = null;
            Object pemObject;

            while ((pemObject = pemParser.readObject()) != null) {
                if (pemObject instanceof X509CertificateHolder certHolder) {
                    certificate = new JcaX509CertificateConverter().getCertificate(certHolder);
                } else if (pemObject instanceof PEMKeyPair keyPair) {
                    privateKey = new JcaPEMKeyConverter().getPrivateKey(keyPair.getPrivateKeyInfo());
                } else if (pemObject instanceof PrivateKeyInfo privateKeyInfo) {
                    privateKey = new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
                }
            }

            if (privateKey == null || certificate == null) {
                throw new RuntimeException("Missing key/cert in PEM file");
            }

            return new Object[] { privateKey, certificate };
        }
    }

    // into a DOM element
    private <T extends GenericDocument> Document openDocument(T sDocument) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new File(
                    locationUnsignedDocuments + DataUtil.obtainNameByTypeDocument(sDocument)));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
