package trashsoftware.trashSnooker.fxml.alert;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class Alert implements Initializable {
    protected Stage stage;
    @FXML
    Label headerText, contentText;
    @FXML
    Button yesButton, noButton;
    @FXML
    VBox additionalPane;
    private boolean active = true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    private void setOnClose(Stage stage) {
        stage.setOnCloseRequest(e -> active = false);
    }

    public void setupInfo(Stage stage,
                          String header, String content) {
        functionalWindow(stage, header, content, null, null);
    }
    
    public void setHeaderText(String text) {
        headerText.setText(text);
    }
    
    public void setContentText(String text) {
        contentText.setText(text);
    }
    
    public void setPositiveButton(String text, Runnable callback) {
        yesButton.setText(text);
        yesButton.setOnAction(e -> Platform.runLater(callback));
    }

    public void functionalWindow(Stage stage,
                                 String header, String content, 
                                 String positiveText,
                                 Runnable callback) {
        this.stage = stage;
        setOnClose(stage);

        this.headerText.setText(header);
        this.contentText.setText(content);
        if (positiveText != null) this.yesButton.setText(positiveText);
        this.noButton.setVisible(false);
        this.noButton.setManaged(false);
        this.yesButton.setOnAction(e -> {
            if (callback != null) Platform.runLater(callback);
            else stage.close();
        });
    }
    
    public void close() {
        stage.close();
    }

    public void setupAdditional(Node additionalContent) {
        this.additionalPane.getChildren().add(additionalContent);
    }

    public void setupConfirm(Stage stage, String header, String content,
                             String yesText, String noText,
                             Runnable yes, Runnable no) {
        this.stage = stage;
        setOnClose(stage);

        this.headerText.setText(header);
        this.contentText.setText(content);
        this.yesButton.setText(yesText);
        this.yesButton.setOnAction(e -> {
            stage.close();
            if (yes != null) Platform.runLater(yes);
        });
        this.noButton.setText(noText);
        this.noButton.setOnAction(e -> {
            stage.close();
            if (no != null) Platform.runLater(no);
        });
    }

    public boolean isActive() {
        return active;
    }
}
