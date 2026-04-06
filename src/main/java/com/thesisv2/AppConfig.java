package com.thesisv2;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;

public class AppConfig {

    private static final String SECRET_KEY = "ThesisV2AESKey!!"; // 16 chars
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),".thesisv2", "dbsettings.enc");

    public static boolean exists() {
        return Files.exists(CONFIG_PATH);
    }

    public static Properties load() {
        Properties props = new Properties();

        if (!exists()) {
            return props;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(CONFIG_PATH);
            byte[] encrypted = Base64.getDecoder().decode(fileBytes);
            byte[] decrypted = decrypt(encrypted);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(decrypted)) {
                props.load(bais);
            }

            return props;

        } catch (Exception e) {
            throw new RuntimeException("Αποτυχία φόρτωσης ρυθμίσεων βάσης.", e);
        }
    }

    public static void save(Properties props) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            props.store(baos, "DB Settings");

            byte[] encrypted = encrypt(baos.toByteArray());
            byte[] encoded = Base64.getEncoder().encode(encrypted);

            Files.write(CONFIG_PATH, encoded);

        } catch (Exception e) {
            throw new RuntimeException("Αποτυχία αποθήκευσης ρυθμίσεων βάσης.", e);
        }
    }

    public static boolean testConnection(
            String host,
            String port,
            String dbName,
            String user,
            String password
    ) {
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + dbName;

        try (Connection ignored = DriverManager.getConnection(url, user, password)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean showDatabaseSettingsDialog() {
        Properties current = load();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ρυθμίσεις βάσης δεδομένων");
        dialog.setHeaderText("Εισαγωγή στοιχείων σύνδεσης");

        ButtonType saveBtn = new ButtonType("Αποθήκευση", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Ακύρωση", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType testBtn = new ButtonType("Έλεγχος σύνδεσης", ButtonBar.ButtonData.LEFT);

        dialog.getDialogPane().getButtonTypes().addAll(testBtn, saveBtn, cancelBtn);

        TextField hostField = new TextField(current.getProperty("db.host", "127.0.0.1"));
        TextField portField = new TextField(current.getProperty("db.port", "3306"));
        TextField dbNameField = new TextField(current.getProperty("db.name", "thesisdb"));
        TextField userField = new TextField(current.getProperty("db.user", "root"));
        PasswordField passwordField = new PasswordField();
        passwordField.setText(current.getProperty("db.password", "Password"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.setAlignment(Pos.CENTER_LEFT);

        grid.add(new Label("Host:"), 0, 0);
        grid.add(hostField, 1, 0);

        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);

        grid.add(new Label("Όνομα βάσης:"), 0, 2);
        grid.add(dbNameField, 1, 2);

        grid.add(new Label("Χρήστης:"), 0, 3);
        grid.add(userField, 1, 3);

        grid.add(new Label("Κωδικός:"), 0, 4);
        grid.add(passwordField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Node testButtonNode = dialog.getDialogPane().lookupButton(testBtn);
        testButtonNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();

            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String dbName = dbNameField.getText().trim();
            String user = userField.getText().trim();
            String pass = passwordField.getText();

            if (host.isBlank() || port.isBlank() || dbName.isBlank() || user.isBlank()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Ελλιπή στοιχεία");
                alert.setHeaderText(null);
                alert.setContentText("Συμπλήρωσε όλα τα υποχρεωτικά πεδία.");
                alert.showAndWait();
                return;
            }

            boolean ok = testConnection(host, port, dbName, user, pass);

            Alert alert = new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            alert.setTitle("Έλεγχος σύνδεσης");
            alert.setHeaderText(null);
            alert.setContentText(ok
                    ? "Η σύνδεση με τη βάση ήταν επιτυχής."
                    : "Αποτυχία σύνδεσης. Έλεγξε τα στοιχεία και ξαναπροσπάθησε.");
            alert.showAndWait();
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isEmpty() || result.get() != saveBtn) {
            return false;
        }

        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        String dbName = dbNameField.getText().trim();
        String user = userField.getText().trim();
        String pass = passwordField.getText();

        if (host.isBlank() || port.isBlank() || dbName.isBlank() || user.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ελλιπή στοιχεία");
            alert.setHeaderText(null);
            alert.setContentText("Συμπλήρωσε όλα τα υποχρεωτικά πεδία.");
            alert.showAndWait();
            return false;
        }

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", port);
        props.setProperty("db.name", dbName);
        props.setProperty("db.user", user);
        props.setProperty("db.password", pass);

        save(props);
        return true;
    }

    private static byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec keySpec = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                "AES"
        );
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    private static byte[] decrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec keySpec = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                "AES"
        );
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }
}