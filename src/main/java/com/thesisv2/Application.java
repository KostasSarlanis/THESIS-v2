package com.thesisv2;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import java.io.IOException;

public class Application extends javafx.application.Application {@Override
    public void start(Stage stage) throws IOException {

        boolean configReady = AppConfig.exists();

        if (!configReady) {
            boolean saved = AppConfig.showDatabaseSettingsDialog();
            if (!saved) {
                return;
            }
        }

        boolean connected = false;

        while (!connected) {
            try {
                DBConnection dbConnection = new DBConnection();
                dbConnection.getConnection().close();
                connected = true;
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Σφάλμα σύνδεσης");
                alert.setHeaderText("Αποτυχία σύνδεσης με τη βάση");
                alert.setContentText("Θα ανοίξουν οι ρυθμίσεις βάσης για διόρθωση.");
                alert.showAndWait();

                boolean saved = AppConfig.showDatabaseSettingsDialog();
                if (!saved) {
                    return;
                }
            }
        }

        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        stage.setTitle("Thesis V2.0");
        stage.setScene(scene);
        stage.show();
    }
}
