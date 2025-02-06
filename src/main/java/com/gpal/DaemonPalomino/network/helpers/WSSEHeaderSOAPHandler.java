package com.gpal.DaemonPalomino.network.helpers;

import java.io.ByteArrayOutputStream;
import java.util.Set;
import java.util.UUID;
import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WSSEHeaderSOAPHandler implements SOAPHandler<SOAPMessageContext> {

    private String username;
    private String password;

    public WSSEHeaderSOAPHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outbound) {
            try {
                SOAPMessage soapMsg = context.getMessage();
                SOAPEnvelope soapEnv = soapMsg.getSOAPPart().getEnvelope();
                SOAPHeader soapHeader = soapEnv.getHeader();

                if (soapHeader == null) {
                    soapHeader = soapEnv.addHeader();
                }

                // Create Security element with correct namespace
                SOAPElement security = soapHeader.addChildElement("Security", "wsse",
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");

                // Add wsu namespace declaration properly
                security.addNamespaceDeclaration("wsu",
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

                // Add mustUnderstand attribute with correct SOAP namespace
                QName mustUnderstandQName = new QName(
                        "http://schemas.xmlsoap.org/soap/envelope/",
                        "mustUnderstand",
                        "soapenv");
                security.addAttribute(mustUnderstandQName, "1");

                // Create UsernameToken with wsu:Id
                SOAPElement usernameToken = security.addChildElement("UsernameToken", "wsse");
                QName wsuIdQName = new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd",
                        "Id",
                        "wsu");
                usernameToken.addAttribute(wsuIdQName, "UsernameToken-" + UUID.randomUUID().toString());

                // Add Username
                SOAPElement usernameElement = usernameToken.addChildElement("Username", "wsse");
                usernameElement.addTextNode(username);

                // Add Password with Type attribute
                SOAPElement passwordElement = usernameToken.addChildElement("Password", "wsse");
                passwordElement.addAttribute(
                        new QName("Type"),
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
                passwordElement.addTextNode(password);

                soapMsg.saveChanges();
            } catch (SOAPException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        logXML(context);
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    private void logXML(SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        String direction = outboundProperty ? "Outgoing" : "Incoming";
        try {
            SOAPMessage message = context.getMessage();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            message.writeTo(out);
            log.info(direction + " XML:\n" + out.toString());
        } catch (Exception e) {
            log.info("Error logging XML: " + e.getMessage());
        }
    }

}
