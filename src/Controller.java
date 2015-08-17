import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.KeyManagerUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

public class Controller {

    private FTPClient ftpClient;
    private FTPSClient ftpsClient;
    private boolean isSecure;
    private Stage stage;

    @FXML
    private TextField userNameField;

    @FXML
    private TextField passwordField;

    @FXML
    private TextField downloadDirectoryField;

    @FXML
    private TextField downloadFileField;

    @FXML
    private TextField sendFileField;

    @FXML
    private CheckBox secureCheckBox;

    @FXML
    private TextArea progressTextArea;

    @FXML
    void login(ActionEvent event) {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        } else if (ftpsClient != null && ftpsClient.isConnected()) {
            try {
                ftpsClient.logout();
                ftpsClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        isSecure = secureCheckBox.isSelected();
        boolean loggedIn = false;
        try {
            if (isSecure) {
                ftpsClient = new FTPSClient(true);
                Path keystorePath = Paths.get(System.getProperty("user.home") + "/ftpHome/client.jks");
                if (Files.notExists(keystorePath)) {
                    try {
                        Files.copy(getClass().getResourceAsStream("client.jks"), keystorePath);
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                    }
                }
                ftpsClient.setKeyManager(KeyManagerUtils.createClientKeyManager(
                        keystorePath.toFile(), "password"));
                // Connecting to server
                ftpsClient.connect("localhost", 29746);
                // Logging in using inputs from user
                loggedIn = ftpsClient.login(userNameField.getText(), passwordField.getText());
                // Setting file type and passive mode to ensure file transfer does not fail
                ftpsClient.enterLocalPassiveMode();
                ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                ftpClient = new FTPClient();
                // Connecting to server
                ftpClient.connect("localhost", 29745);
                // Logging in using inputs from user
                loggedIn = ftpClient.login(userNameField.getText(), passwordField.getText());
                // Setting file type and passive mode to ensure file transfer does not fail
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            }
            if (loggedIn) {
                progressTextArea.appendText("Logged In. \n");
            } else {
                progressTextArea.appendText("failed to login. \n");
            }
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
            progressTextArea.appendText("failed to logout.\n");
            e.printStackTrace(System.err);
        }
    }

    @FXML
    void receiveBrowse(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Download Directory");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selectedDirectory = chooser.showDialog(stage);
        if (!(selectedDirectory == null)) {
            downloadDirectoryField.setText(selectedDirectory.toString());
        }
    }

    @FXML
    void sendBrowse(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Send File");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selectedDirectory = chooser.showOpenDialog(stage);
        if (!(selectedDirectory == null)) {
            sendFileField.setText(selectedDirectory.toString());
        }
    }

    @FXML
    void receiveFile(ActionEvent event) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final String remoteFile = downloadFileField.getText();
                final String localFile = downloadDirectoryField + File.separator + downloadFileField.getText();
                File downloadedFile = new File(localFile);
                try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadedFile))) {
                    progressTextArea.appendText("Started downloading file.\n");
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
                                        ("The file is downloaded successfully.\n");
                            } else {
                                progressTextArea.appendText
                                        ("The file could not be downloaded.\n");
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

    @FXML
    void sendFile(ActionEvent event) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String file = sendFileField.getText();
                // Extracting file name from file path
                String fileName = file.substring(file.lastIndexOf(File.separator + 1));
                try (InputStream inputStream = new FileInputStream(file)) {
                    progressTextArea.appendText("Started uploading file.\n");
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
                                        ("File is uploaded successfully.\n");
                            } else {
                                progressTextArea.appendText
                                        ("File could not be uploaded.\n");
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

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
