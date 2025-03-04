# Daemon Palomino
This daemon have two parts, the recurrent daemon, and the listening port.

The daemon search into all tables looking for documents that have not being sended, 
and for those that are sended, just validate using the method getStatus and getStatusCdr
and for cancelation of the documents the program itself creates an NCR and send it to SUNAT.

## How to start the daemon in SERVER mode
```bash
java -jar target/DaemonPalomino-ALPHA1.0-jar-with-dependencies.jar SERVER 5 1 1 1 1 1 10 59
```
- Usage: java -jar DaemonPalomino.jar SERVER <sizeBatch> <timeSendDocuments> <timeValidatingDocuments> <timeSendAnuDocuments> <timeValidateAnulated> <summaryHour> <summaryMin>
-   sizeBatch: Size of the batch for document processing                                                                                                                    
-   timeSendDocuments: Time interval (in minutes) for generating and signing documents                                                                                      
-   timeValidatingDocuments: Time interval (in days) for sending non-BOL documents                                                                                          
-   timeSendAnuDocuments: Time interval (in seconds) for sending annulled documents                                                                                         
-   timeValidateAnulated: Time interval (in seconds) for validating annulled documents                                                                                      
-   summaryHour: Time exactly (in hours) to stablish the hour                                                                                                               
-   summaryMin: Time exactly (in minutes) to stablish the minutes

## How to start the daemon in UNIQUE mode
```bash
java -jar target/DaemonPalomino-ALPHA1.0-jar-with-dependencies.jar UNIQUE B644-8874 BOL 005
```
- Usage: java -jar DaemonPalomino.jar UNIQUE <NuDocu> <TiDocu> <CoEmpr> 
-   NuDocu: The Number of the Document with the format (B644-8874).     
-   TiDocu: The type of document to BOL,FAC,NCR,NCD.
-   CoEmpr: Business code, i mean 005,004, etc.
-   Parameter UNIQUE.

## How to start the daemon to CANCEL document
```bash
java -jar target/DaemonPalomino-ALPHA1.0-jar-with-dependencies.jar CANCEL B644-8874 BOL 005
```
- Usage: java -jar DaemonPalomino.jar  <NuDocu> <TiDocu> <CoEmpr> 
-   NuDocu: The Number of the Document with the format (B644-8874).     
-   TiDocu: The type of document to BOL,FAC,NCR,NCD.
-   CoEmpr: Business code, i mean 005,004, etc.
-   Parameter UNIQUE.

## To download only the CDR
```bash
java -jar target/DaemonPalomino-ALPHA1.0-jar-with-dependencies.jar CDR F305 0000919 07 20417931393
```
- Usage: java -jar DaemonPalomino.jar  <NuDocu> <TiDocu> <CoEmpr> 
-   CDR: Parameter that it's principal
-   Serie: Code like F502 or B502
-   NuDocu: Number that represents the number of document(correlative)
-   CoEmpr: Ruc of Business

## How the HTTP server works
```bash
java -jar target/DaemonPalomino-ALPHA1.0-jar-with-dependencies.jar SERVER 5 1 1 1 1 1 10 59
```
When the server it's invoked the app starts an server which listens in the port 8080
it awaits in two endpoints:
- {localhost:other}:8080/daemon/api/generate/{nro}/{tipoOperacion}
- {localhost:other}:8080/daemon/api/cancel/{nro}/{tipoOperacion}

## Convert .jks into .PEM
```bash
java -jar target/DaemonPalomino-ALPHA1-jar-with-dependencies.jar SERVER 5 1 1 1 1 1 1 10 59
```

## From .p12 into an .pem file
```bash
keytool -importkeystore -srckeystore <keystore.jks> -destkeystore <keystore.p12> -srcstoretype JKS -deststoretype PKCS12
openssl pkcs12 -in exp-palomino.p12 -nokeys -out exp-palomino.pem
```

# in this part only let the cert of palomino and delete the rest
```bash
openssl pkcs12 -in exp-palomino.p12 -nocerts -nodes -out private.pem
```

# join the previous two in one .pem file using
```bash
cat certificate.pem private.pem > combined.pem
```
# then validate
```bash
openssl x509 -noout -text -in combined.pem
openssl rsa -check -noout -in combined.pem
```

# FOR SSH CONFIG
ssh-keyscan -H 172.16.10.15 >> ~/.ssh/known_hosts
ssh-keyscan -H 172.16.10.15 >> ~/.ssh/administrator

