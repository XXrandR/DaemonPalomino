package com.gpal.DaemonPalomino.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class HikariBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(HikariBase.class);
    private static final HikariConfig config = new HikariConfig();
    private static DataSource dataSource;

    public HikariBase(){
        config.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
        config.addDataSourceProperty("url", "jdbc:sqlserver://172.16.10.200:1433;database=Empresa;encrypt=false;trustServerCertificate=true");
        config.addDataSourceProperty("user", "sa");
        config.addDataSourceProperty("password", "palomino.123");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(300000); // 30 seconds
        config.setIdleTimeout(600000); // 1 minute
        dataSource = new HikariDataSource(config);
    }

    public java.sql.Connection getConnection(){
        try (java.sql.Connection conn = dataSource.getConnection()) {
            LOGGER.info("Connected to the database");
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Connected to the database");
            return null;
        }
    }

}
