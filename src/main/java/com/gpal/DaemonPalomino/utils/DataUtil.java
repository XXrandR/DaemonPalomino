package com.gpal.DaemonPalomino.utils;

import javax.sql.DataSource;
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
import java.util.zip.ZipOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Slf4j
public class DataUtil {

    public static <T> List<T> executeProcedure(DataSource dataSource, String procedureQuery, List<?> data,
            Class<T> mClass) {
        try {
            List<T> mTs = new ArrayList<>();
            var dataSourc = dataSource.getConnection();
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
                location + document.getCompanyID() + "-"
                        + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                        + document.getNU_DOCU() + ".xml")) {
            fileWriter.write(writer.toString());
            log.info("Generated : " + location + document.getCompanyID() + "-"
                    + (document.getTI_DOCU().equals("BOL") ? "03" : "01") + "-"
                    + document.getNU_DOCU() + ".xml");
        } catch (Exception ex) {
            log.error("Error writing file...", ex);
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
            log.info("XML content before compression:\n" + xmlContent);

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

}
