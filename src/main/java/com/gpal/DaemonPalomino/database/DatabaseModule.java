package com.gpal.DaemonPalomino.database;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Module
public class DatabaseModule {

    @Provides
    @Singleton
    public DataSource provideHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
        config.addDataSourceProperty("url",
                "jdbc:sqlserver://172.16.10.200:1433;database=Empresa;encrypt=false;trustServerCertificate=true");
        config.addDataSourceProperty("user", "sa");
        config.addDataSourceProperty("password", "palomino.123");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(300000); // 30 seconds
        config.setIdleTimeout(600000); // 1 minute
        return new HikariDataSource(config);
    }

    @Provides
    @Singleton
    public HikariBase provideHiraki(DataSource dataSource) {
        return new HikariBase(dataSource);
    }

}
