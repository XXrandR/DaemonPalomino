package com.gpal.DaemonPalomino.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;

public class HikariBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(HikariBase.class);
    private final DataSource dataSource;

    public HikariBase(DataSource dataSource){
        this.dataSource = dataSource;
    }

    public java.sql.Connection getConnection(){
        try (java.sql.Connection conn = dataSource.getConnection()) {
            LOGGER.info("Connected to the database");
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Error connecting to the database", e);
            return null;
        }
    }

}
