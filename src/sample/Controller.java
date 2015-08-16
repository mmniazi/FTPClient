package sample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.KeyManagerUtils;

import java.io.*;
import java.security.GeneralSecurityException;

public class Controller {

    private FTPClient ftpClient;
    private FTPSClient ftpsClient;
    private boolean isSecure;

    @FXML
    private TextField userNameField;

    @FXML
    private TextField passwordField;

    @FXML
    private CheckBox secureCheckBox;

    @FXML
    private TextArea progressTextArea;

    @FXML
    void login(ActionEvent event) {
        isSecure = secureCheckBox.isSelected();
        try {
            if (isSecure) {
                ftpsClient = new FTPSClient(true);
                // Setting a key store for storing Client public and private keys
                ftpsClient.setKeyManager(KeyManagerUtils.createClientKeyManager(
                        new File("src/sample/client.jks"), "password"));
                // Connecting to server
                ftpsClient.connect("localhost", 29746);
                // Logging in using inputs from user
                ftpsClient.login(userNameField.getText(), passwordField.getText());
                // Setting file type and passive mode to ensure file transfer does not fail
                ftpsClient.enterLocalPassiveMode();
                ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                ftpClient = new FTPClient();
                // Connecting to server
                ftpClient.connect("localhost", 29745);
                // Logging in using inputs from user
                ftpClient.login(userNameField.getText(), passwordField.getText());
                // Setting file type and passive mode to ensure file transfer does not fail
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            }
            progressTextArea.appendText("Logged In. \n");
        } catch (IOException | GeneralSecurityException e) {
            progressTextArea.appendText("failed to login. \n");
            e.printStackTrace(System.err);
        }
    }

    @FXML
    void logout(ActionEvent event) {
        try {
            if (isSecure) {
                ftpsClient.logout();
                ftpsClient.disconnect();
            } else {
                ftpClient.logout();
                ftpClient.disconnect();
            }
            progressTextArea.appendText("Logged Out.\n");
        } catch (IOException e) {
            progressTextArea.appendText("failed to logout.");
            e.printStackTrace(System.err);
        }
    }

    @FXML
    void receiveLgFile(ActionEvent event) {
        // Download the large file to current user directory.
        // In windows it corresponds to C:\\Users\\{Your UserName}\\
        receiveFile("/Drivers.zip", System.getProperty("user.home") + "/Drivers.zip", "large");
    }

    @FXML
    void receiveMdFile(ActionEvent event) {
        // Download the medium file to current user directory.
        // In windows it corresponds to C:\\Users\\{Your UserName}\\
        receiveFile("/commons.zip", System.getProperty("user.home") + "/commons.zip", "medium");
    }

    @FXML
    void receiveSmFile(ActionEvent event) {
        // Download the small file to current user directory.
        // In windows it corresponds to C:\\Users\\{Your UserName}\\
        receiveFile("/dropbox.exe", System.getProperty("user.home") + "/dropbox.exe", "small");
    }

    @FXML
    void sendLgFile(ActionEvent event) {
        // Send upload large file to ftp server and store it as Drivers.zip
        sendFile("SampleFiles/LG-Mobile-Driver_v3.14.1.zip", "Drivers.zip", "large");
    }

    @FXML
    void sendMdFile(ActionEvent event) {
        // Send upload medium file to ftp server and store it as commons.zip
        sendFile("SampleFiles/commons-net-3.3-bin.zip", "commons.zip", "medium");
    }

    @FXML
    void sendSmFile(ActionEvent event) {
        // Send upload small file to ftp server and store it as dropbox.exe
        sendFile("SampleFiles/DropboxInstaller.exe", "dropbox.exe", "small");
    }

    private void receiveFile(final String remoteFile, final String localFile, final String fileSize) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                File downloadedFile = new File(localFile);
                try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadedFile))) {
                    progressTextArea.appendText("Started downloading " + fileSize + " file.\n");
                    // Receiving file from server
                    final boolean success;
                    if (isSecure) {
                        success = ftpsClient.retrieveFile(remoteFile, outputStream);
                    } else {
                        success = ftpClient.retrieveFile(remoteFile, outputStream);
                    }
                    // To change GUI from any thread other then GUI thread we need to call Platform.runLater
                    Platform.runLater(new Runnable() {
                        public void run() {
                            if (success) {
                                progressTextArea.appendText
                                        ("The " + fileSize + " file is downloaded successfully.\n");
                            } else {
                                progressTextArea.appendText
                                        ("The " + fileSize + " file could not be downloaded.\n");
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        });
        thread.start();
    }

    private void sendFile(final String fileLocation, final String fileName, final String fileSize) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (InputStream inputStream = getClass().getResourceAsStream(fileLocation)) {
                    progressTextArea.appendText("Started uploading " + fileSize + " file.\n");
                    // Storing file on server
                    final boolean success;
                    if (isSecure) {
                        success = ftpsClient.storeFile(fileName, inputStream);
                    } else {
                        success = ftpClient.storeFile(fileName, inputStream);
                    }
                    // To change GUI from any thread other then GUI thread we need to call Platform.runLater
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                progressTextArea.appendText
                                        ("The " + fileSize + " file is uploaded successfully.\n");
                            } else {
                                progressTextArea.appendText
                                        ("The " + fileSize + " file could not be uploaded.\n");
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        });
        thread.start();
    }
}
