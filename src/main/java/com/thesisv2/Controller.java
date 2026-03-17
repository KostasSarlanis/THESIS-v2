package com.thesisv2;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class Controller {

    //~~~~~ CLOSE HANDLER ~~~~~                                                                                        .
    @FXML
    private void HandleCloseButton(ActionEvent event) {
        Stage stage = (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
        stage.close();
    }









}



