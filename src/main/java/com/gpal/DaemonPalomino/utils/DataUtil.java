package com.gpal.DaemonPalomino.utils;

import javax.sql.DataSource;

import com.gpal.DaemonPalomino.models.GenericDocument;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.FileWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

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
                    statement.setObject(i+1, data.get(i));
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
                location + document.getCompanyID() + "-" + document.getDocumentTypeId() + "-" + document.getNuDocu()
                        + ".xml")) {
            fileWriter.write(writer.toString());
            log.info("Generated " + location + document.getCompanyID() + "-" + document.getDocumentTypeId() + "-"
                    + document.getNuDocu() + ".xml");
        } catch (Exception ex) {
            log.error("Error writing file...", ex);
        }
    }

}
