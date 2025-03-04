package com.gpal.DaemonPalomino.database;

import dagger.Module;
import dagger.Provides;
import java.util.Properties;
import javax.inject.Singleton;
import javax.sql.DataSource;
import com.gpal.DaemonPalomino.utils.PropertiesHelper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Module
public class DatabaseModule {

    @Provides
    @Singleton
    public DataSource provideHikariConfig() {
        HikariConfig config = new HikariConfig();
        Properties props = PropertiesHelper.obtainProps();
        config.setDataSourceClassName(props.getProperty("db.datasource"));
        config.addDataSourceProperty("url", props.getProperty("db.url"));
        config.addDataSourceProperty("user", props.getProperty("db.user"));
        config.addDataSourceProperty("password", props.getProperty("db.password"));

        // config.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
        // config.addDataSourceProperty("url","jdbc:sqlserver://172.16.20.8\\TPALOMINOSQL;database=Empresa;encrypt=false;trustServerCertificate=true");
        // config.addDataSourceProperty("user", "sa");
        // config.addDataSourceProperty("password", "palomino.123");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(300000); // 30 seconds
        config.setIdleTimeout(600000); // 1 minute
        return new HikariDataSource(config);
    }

}
