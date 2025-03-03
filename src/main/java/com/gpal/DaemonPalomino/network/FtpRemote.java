package com.gpal.DaemonPalomino.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.inject.Inject;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import com.gpal.DaemonPalomino.utils.DataUtil;
import com.gpal.DaemonPalomino.utils.PropertiesHelper;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FtpRemote {

    private final String host;
    private final String username;
    private final String password;
    private final int port;
    private static Session session;
    private final String pathKey;
    private final String knownHosts;
    Properties properties = new Properties();

    @Inject
    public FtpRemote() {
        properties = PropertiesHelper.obtainProps();
        username = properties.getProperty("ssh.username");
        password = properties.getProperty("ssh.password");
        host = properties.getProperty("ssh.host");
        pathKey = properties.getProperty("ssh.path.key");
        knownHosts = properties.getProperty("ssh.known_hosts");
        port = Integer.valueOf(properties.getProperty("ssh.port"));
    }

    public void openConnection() {
        try {
            if (session == null || !session.isConnected()) {
                JSch jsch = new JSch();
                jsch.addIdentity(pathKey, password);
                jsch.setKnownHosts(knownHosts);
                session = jsch.getSession(username, host, port);
                // For simplicity; consider using known hosts file in production
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                config.put("debug", "all");
                session.setConfig(config);
                session.connect();
            } else {
                log.info("SCP already connected.");
            }
        } catch (JSchException ex) {
            ex.printStackTrace();
        }
    }

    public <T extends GenericDocument> List<T> saveData(List<T> documentBase) {
        openConnection();
        Properties props = PropertiesHelper.obtainProps();
        // upload list of xml
        return documentBase.stream()
                .map(document -> {
                    upload(props.getProperty("location.documents") + "/pdf/"
                            + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".pdf",
                            props.getProperty("location.remote") + "/pdf/"
                                    + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".pdf");
                    upload(
                            props.getProperty("location.documents") + "/signed/"
                                    + DataUtil.obtainNameByTypeDocument(document),
                            props.getProperty("location.remote") + "/signed/"
                                    + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".xml");
                    upload(
                            props.getProperty("location.documents") + "/cdr/R-"
                                    + DataUtil.obtainNameByTypeDocument(document),
                            props.getProperty("location.remote") + "/cdr/R-"
                                    + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".xml");
                    return document;
                }).collect(Collectors.toList());
    }

    public <T extends GenericDocument> List<T> getData(List<T> documentBase) {
        openConnection();
        Properties props = PropertiesHelper.obtainProps();
        // upload list of xml
        return documentBase.stream()
                .map(document -> {
                    download(props.getProperty("location.remote") + "/pdf/"
                            + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".pdf",
                            props.getProperty("location.documents") + "/pdf/"
                                    + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".pdf");
                    download(props.getProperty("location.remote") + "/cdr/R-"
                            + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".xml",
                            props.getProperty("location.documents") + "/cdr/R-"
                                    + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".xml");
                    download(props.getProperty("location.remote") + "/signed/"
                            + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".xml",
                            props.getProperty("location.documents") + "/signed/"
                                    + DataUtil.obtainNameByTypeDocumentNotXml(document) + ".xml");
                    return document;
                }).collect(Collectors.toList());
    }

    private boolean upload(String localPath, String remotePath) {
        log.info("Uploading xml and pdf for {}, into {}.", localPath, remotePath);
        ChannelSftp sftp = null;
        try {
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            ensureDirectoriesExist(sftp, remotePath);
            if (Files.exists(Paths.get(localPath))) {
                try (InputStream inputStream = new FileInputStream(localPath)) {
                    sftp.put(inputStream, remotePath);
                    return DataUtil.deleteFile(localPath);
                }
            } else {
                System.out.println("The file does not exist or is a directory.");
                return false;
            }
        } catch (IOException | JSchException | SftpException ex) {
            log.error("Error: ", ex);
            return false;
        } finally {
            if (sftp != null && sftp.isConnected()) {
                log.info("Disconnecting...");
                sftp.disconnect();
            }
        }
    }

    public boolean download(String remotePath, String localPath) {
        log.info("Uploading xml and pdf for {}, into {}.", remotePath, localPath);
        ChannelSftp sftp = null;
        try {
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            Path localFilePath = Paths.get(localPath);
            File parentDir = localFilePath.getParent().toFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                log.error("Failed to create parent directories for {}", localPath);
                return false;
            }

            sftp.get(remotePath, localPath);
            return true;
        } catch (JSchException | SftpException ex) {
            log.error("Error: ", ex);
            return false;
        } finally {
            if (sftp != null && sftp.isConnected()) {
                log.info("Disconnecting...");
                sftp.disconnect();
            }
        }
    }

    private void ensureDirectoriesExist(ChannelSftp sftp, String dir) throws SftpException {
        String[] folders = dir.split("/");
        StringBuilder currentDir = new StringBuilder();
        for (int i = 0; i < folders.length - 1; i++) {
            if (i == folders.length - 1) {
                break;
            }
            String folder = folders[i];
            if (!folder.isEmpty()) {
                currentDir.append("/").append(folder);
                try {
                    sftp.cd(currentDir.toString());
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        sftp.mkdir(currentDir.toString()); // Create directory if it doesn't exist
                        sftp.cd(currentDir.toString());
                    } else {
                        throw e; // Re-throw unexpected exceptions
                    }
                }
            }
        }
    }

}
