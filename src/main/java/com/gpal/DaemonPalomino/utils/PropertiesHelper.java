package com.gpal.DaemonPalomino.utils;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesHelper {

    public static Properties obtainProps() {
        try (InputStream inputStream = PropertiesHelper.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

}
