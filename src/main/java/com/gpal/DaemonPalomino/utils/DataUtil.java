package com.gpal.DaemonPalomino.utils;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.lang.reflect.Field;

@Slf4j
public class DataUtil {

    public static <T> List<T> executeProcedure(DataSource dataSource, String procedureQuery, List<?> data,
            Class<T> mClass) {
        try {
            List<T> mTs = new ArrayList<>();
            var dataSourc = dataSource.getConnection();
            PreparedStatement statement = dataSourc.prepareStatement(procedureQuery);
            for (int i = 0; i < data.size(); i++) {
                Object parameter = data.get(i);
                if (parameter instanceof Integer) {
                    statement.setInt(i + 1, (Integer) parameter);
                } else if (parameter instanceof String) {
                    statement.setString(i + 1, (String) parameter);
                } else if (parameter instanceof Long) {
                    statement.setLong(i + 1, (Long) parameter);
                } else if (parameter instanceof Double) {
                    statement.setDouble(i + 1, (Double) parameter);
                } else if (parameter instanceof Boolean) {
                    statement.setBoolean(i + 1, (Boolean) parameter);
                } else {
                    throw new UnsupportedOperationException("Unsupported parameter type: " + parameter.getClass());
                }
            }
            ResultSet rsSet = statement.executeQuery();
            while (rsSet.next()) {
                T document = mClass.getDeclaredConstructor().newInstance();
                for (Field field : mClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        field.set(document, rsSet.getObject(field.getName()));
                    } catch (Exception ex) {
                        log.error("Error mapping the class.." + mClass.getName(), ex);
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
}
