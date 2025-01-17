package com.gpal.DaemonPalomino.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataUtil {

    public static <T> List<T> executeProcedure(DataSource dataSource, String procedureQuery, List<?> data,
            Class<T> mClass) {
        try {
            List<T> mTs = new ArrayList<>();
            try (Connection dataSourc = dataSource.getConnection()) {
                PreparedStatement statement = dataSourc.prepareStatement(procedureQuery);
                for (int i = 0; i < data.size(); i++) {
                    Object parameter = data.get(i);
                    if (parameter instanceof Integer integer) {
                        statement.setInt(i + 1, integer);
                    } else if (parameter instanceof String string) {
                        statement.setString(i + 1, string);
                    } else if (parameter instanceof Long aLong) {
                        statement.setLong(i + 1, aLong);
                    } else if (parameter instanceof Double aDouble) {
                        statement.setDouble(i + 1, aDouble);
                    } else if (parameter instanceof Boolean aBoolean) {
                        statement.setBoolean(i + 1, aBoolean);
                    } else {
                        throw new UnsupportedOperationException("Unsupported parameter type: " + parameter.getClass());
                    }
                }
                try (ResultSet rsSet = statement.executeQuery()) {
                    while (rsSet.next()) {
                        T document = mClass.getDeclaredConstructor().newInstance();
                        for (Field field : mClass.getDeclaredFields()) {
                            field.setAccessible(true);
                            try {
                                field.set(document, rsSet.getObject(field.getName()));
                            } catch (IllegalAccessException | IllegalArgumentException | SQLException ex) {
                                log.error("Error mapping the class.." + mClass.getName(), ex);
                            }
                        }
                        mTs.add(document);
                    }
                }
            }
            return mTs;
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | UnsupportedOperationException | InvocationTargetException | SQLException ex) {
            log.error("Error DataUtil ex: ", ex);
            return null;
        }
    }
}
