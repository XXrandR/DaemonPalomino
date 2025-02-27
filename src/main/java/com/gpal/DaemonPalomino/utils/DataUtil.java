package com.gpal.DaemonPalomino.utils;

import javax.sql.DataSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import com.gpal.DaemonPalomino.models.BolDocument;
import com.gpal.DaemonPalomino.models.FacDocument;
import com.gpal.DaemonPalomino.models.NcdDocument;
import com.gpal.DaemonPalomino.models.NcrDocument;
import com.gpal.DaemonPalomino.models.SummaryDocument;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class DataUtil {

    public static <T> List<T> executeProcedure(DataSource dataSource, String procedureQuery, List<?> data,
            Class<T> mClass) {
        try {

            List<T> mTs = new ArrayList<>();
            var dataSourc = dataSource.getConnection();

            log.info("Procedure: {},{}", procedureQuery, data.toString());

            PreparedStatement statement = dataSourc.prepareStatement(procedureQuery);
            IntStream.range(0, data.size()).forEach(i -> {
                try {
                    statement.setObject(i + 1, data.get(i));
                } catch (SQLException ex) {
                    log.error("Error mapping the class,input.." + mClass.getName(), ex);
                }
            });
            ResultSet rsSet = statement.executeQuery();
            while (rsSet.next()) {
                T document = mClass.getDeclaredConstructor().newInstance();
                for (Field field : mClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        field.set(document, rsSet.getObject(field.getName()));
                    } catch (Exception ex) {
                        log.error("Error mapping the class,output.." + mClass.getName(), ex);
                    }
                }
                mTs.add(document);
            }
            rsSet.close();
            dataSourc.close();
            return mTs;

        } catch (Exception ex) {
            log.error("Error DataUtil ex: ", ex);
            return null;
        }
    }

    public static <T extends GenericDocument> void generateFile(T document, StringWriter writer, String location) {
        try (FileWriter fileWriter = new FileWriter(
                location + obtainNameByTypeDocument(document))) {
            fileWriter.write(writer.toString());
            log.info("Generated : " + location + obtainNameByTypeDocument(document));
        } catch (Exception ex) {
            log.error("Error writing file...", ex);
        }
    }

    public static <T> String obtainCompanyId(T obj) {
        if (obj instanceof SummaryDocument sDocument) {
            return sDocument.getCompanyID();
        } else if (obj instanceof BolDocument document) {
            return document.getCompanyID();
        } else if (obj instanceof FacDocument document) {
            return document.getCompanyID();
        } else if (obj instanceof NcrDocument document) {
            return document.getCompanyID();
        } else if (obj instanceof NcdDocument document) {
            return document.getCompanyID();
        } else {
            throw new RuntimeException("Error obtaining name of document: " + obj.toString());
        }
    }

    public static <T> String obtainNameByTypeDocument(T obj) {
        if (obj instanceof SummaryDocument sDocument) {
            return sDocument.getCompanyRuc() + "-" + sDocument.getNU_DOCU() + ".xml";
        } else if (obj instanceof BolDocument document) {
            return document.getCompanyID() + "-"
                    + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + document.getNU_DOCU() + ".xml";
        } else if (obj instanceof FacDocument document) {
            return document.getCompanyID() + "-"
                    + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + document.getNU_DOCU() + ".xml";
        } else if (obj instanceof NcrDocument document) {
            return document.getCompanyID() + "-"
                    + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + document.getNU_DOCU() + ".xml";
        } else if (obj instanceof NcdDocument document) {
            return document.getCompanyID() + "-"
                    + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + document.getNU_DOCU() + ".xml";
        } else {
            throw new RuntimeException("Error obtaining name of document: " + obj.toString());
        }
    }

    public static <T> String obtainNameByTypeDocumentNotXml(T obj) {
        if (obj instanceof SummaryDocument sDocument) {
            return sDocument.getCompanyRuc() + "-" + sDocument.getNU_DOCU();
        } else if (obj instanceof BolDocument document) {
            return document.getCompanyID() + "-"
                    + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + document.getNU_DOCU();
        } else if (obj instanceof FacDocument document) {
            return document.getCompanyID() + "-"
                    + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + document.getNU_DOCU();
        } else if (obj instanceof NcrDocument document) {
            return document.getCompanyID() + "-"
                    + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + document.getNU_DOCU();
        } else if (obj instanceof NcdDocument document) {
            return document.getCompanyID() + "-"
                    + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + document.getNU_DOCU();
        } else {
            throw new RuntimeException("Error obtaining name of document: " + obj.toString());
        }
    }

    public static <T extends SummaryDocument> void generateFileSummarie(T document, StringWriter writer,
            String location) {
        try (FileWriter fileWriter = new FileWriter(
                location + document.getCompanyRuc() + "-" + document.getNU_DOCU() + ".xml")) {
            fileWriter.write(writer.toString());
            log.info("Generated  Summary: " +
                    location + document.getCompanyRuc() + "-" + document.getNU_DOCU() + ".xml");
        } catch (Exception ex) {
            log.error("Error writing file...", ex);
        }
    }

    public static DataHandler obtainFileDataHandler(String locationFile) {
        try {
            File myObj = new File(locationFile);
            byte[] fileContent = Files.readAllBytes(myObj.toPath());
            return new DataHandler(new ByteArrayDataSource(fileContent, "application/octet-stream"));
        } catch (Exception ex) {
            log.error("Error reading the file..", ex);
            return null;
        }
    }

    public static DataHandler obtainFileDataHandlerZip(String locationFile) {
        try {
            File sourceFile = new File(locationFile + ".xml");
            if (!sourceFile.exists()) {
                throw new FileNotFoundException("Source file not found: " + locationFile);
            }

            String xmlContent = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
            log.debug("XML content before compression:\n" + xmlContent);

            String zipFileName = locationFile + ".zip";
            FileOutputStream fStream = new FileOutputStream(zipFileName);
            ZipOutputStream zip = new ZipOutputStream(fStream);

            FileInputStream fis = new FileInputStream(sourceFile);
            ZipEntry zipEntry = new ZipEntry(sourceFile.getName());
            zip.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zip.write(bytes, 0, length);
            }

            zip.closeEntry();
            fis.close();
            zip.close();

            return DataUtil.obtainFileDataHandler(zipFileName);
        } catch (Exception ex) {
            log.error("Error creating the ZIP file", ex);
            return null;
        }
    }

    public static void unzipFiles(String location, byte[] unzipFile) {
        try (
                ByteArrayInputStream bStream = new ByteArrayInputStream(unzipFile);
                ZipInputStream zis = new ZipInputStream(bStream);) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();
                File outputFile = new File(location, fileName);
                log.info("File to UNZIP: {}", fileName);

                // Create directories if the entry is a directory
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    // Ensure parent directories exist
                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null) {
                        parentDir.mkdirs();
                    }

                    // Write file content
                    try (FileOutputStream fos = new FileOutputStream(outputFile);
                            BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String obtainBase64(String locationFile) {
        try {
            File myObj = new File(locationFile);
            byte[] fileContent = Files.readAllBytes(myObj.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (Exception ex) {
            log.error("Error obtaining base64..", ex);
            return null;
        }
    }

    public static String getStringFromDataHandler(DataHandler dataHandler) {
        try {
            InputStream inputStream = dataHandler.getInputStream();
            return convertInputStreamToString(inputStream);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private static String convertInputStreamToString(InputStream inputStream) {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    public static void saveXml(Document doc, String outputPath) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outputPath));
        transformer.transform(source, result);
        log.info("Generated : " + outputPath);
    }

    public static boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);

            if (!Files.isRegularFile(path)) {
                System.err.println("Error: Not a regular file or does not exist.");
                return false;
            }

            boolean deleted = Files.deleteIfExists(path);
            if (!deleted) {
                System.err.println("Warning: File does not exist.");
            }
            return deleted;
        } catch (NoSuchFileException e) {
            System.err.println("Error: File does not exist: " + e.getMessage());
            return false;
        } catch (DirectoryNotEmptyException e) {
            System.err.println("Error: Target is a non-empty directory.");
            return false;
        } catch (IOException e) {
            System.err.println("I/O Error: Unable to delete - " + e.getMessage());
            return false;
        } catch (SecurityException e) {
            System.err.println("Security Error: Permission denied - " + e.getMessage());
            return false;
        }
    }

    public static void writeBytes(byte[] fileBytes, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
