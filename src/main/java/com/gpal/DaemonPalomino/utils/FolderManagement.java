package com.gpal.DaemonPalomino.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FolderManagement {

    public static void createFolders() {
        // set location of unsigned,signed,pdf,and cdr
        try (InputStream inputStream = FolderManagement.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (inputStream == null)
                throw new RuntimeException("Unable to find application.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            String locationDocuments = properties.getProperty("location.documents");
            createDirectory(locationDocuments);
            createDirectory(locationDocuments + "/unsigned");
            createDirectory(locationDocuments + "/signed");
            createDirectory(locationDocuments + "/pdf");
            createDirectory(locationDocuments + "/cdr");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    private static void createDirectory(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdir();
            log.debug("Directory created: " + path);
        } else {
            log.debug("Directory already exists: " + path);
        }
    }

}
