package trashsoftware.trashSnooker.fxml;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampDataManager;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class NewCareerView implements Initializable {
    @FXML
    Pane basePane;
    @FXML
    ComboBox<Hand> handBox;
    @FXML
    TextField nameField;
    @FXML
    ComboBox<PlayerPerson> existingPlayersBox;
    @FXML
    Button usePlayerButton, playerInfoBtn;
    @FXML
    Label promptLabel;
    @FXML ComboBox<PlayerPerson.Sex> sexBox;
    @FXML ComboBox<Double> heightBox;
    @FXML ComboBox<Difficulty> aiGoodnessBox;
    @FXML ComboBox<Difficulty> playerGoodnessBox;

    private EntryView entryView;
    private Stage owner, thisStage;
    private ResourceBundle strings;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
        
        handBox.getItems().addAll(Hand.values());
        handBox.getSelectionModel().select(1);

        fillBox();

        ChampDataManager.getInstance();
    }

    public void setup(EntryView entryView, Stage owner, Stage thisStage) {
        this.entryView = entryView;
        this.owner = owner;
        this.thisStage = thisStage;
    }

    private void fillBox() {
        existingPlayersBox.getItems().addAll(DataLoader.getInstance().getActualPlayers());
        existingPlayersBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            usePlayerButton.setDisable(newValue == null);
            playerInfoBtn.setDisable(newValue == null);
        }));
        
        sexBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                heightValues(newValue.minHeight, newValue.maxHeight, newValue.stdHeight);
            }
        }));
        sexBox.getItems().addAll(PlayerPerson.Sex.values());
        sexBox.getSelectionModel().select(0);
        
        playerGoodnessBox.getItems().addAll(
                new Difficulty("difEasiest", 3.0),
                new Difficulty("difEasy", 1.5),
                new Difficulty("difMedium", 1.0),
                new Difficulty("difHard", 0.75)
        );
        playerGoodnessBox.getSelectionModel().select(2);
        
        aiGoodnessBox.getItems().addAll(
                new Difficulty("aiGoodNoob", 0.15),
                new Difficulty("aiGoodBad", 0.4),
                new Difficulty("aiGoodNormal", 1.0),
                new Difficulty("aiGoodGood", 2.0),
                new Difficulty("aiGoodExtreme", 10.0)
        );
        
        aiGoodnessBox.getSelectionModel().select(2);
    }
    
    private void heightValues(double from, double to, double select) {
        heightBox.getItems().clear();
        for (double i = from; i <= to; i += 1) {
            heightBox.getItems().add(i);
        }
        
        heightBox.getSelectionModel().select(select);
    }

    @FXML
    public void createPlayer() {
        String name = nameField.getText();
        if (name.isBlank()) return;
        
        String generatedId = DataLoader.generateIdByName(name);
        if (DataLoader.getInstance().hasPlayer(generatedId)) {
            promptLabel.setText(strings.getString("playerPersonAlreadyExists"));
            return;
        } else {
            promptLabel.setText("");
        }

        boolean leftHanded = handBox.getValue() == Hand.LEFT;

        PlayerPerson playerPerson = PlayerPerson.randomPlayer(
                generatedId,
                name,
                leftHanded,
                70.0,
                80.0,
                true,
                heightBox.getValue(),
                sexBox.getValue()
        );

        DataLoader.getInstance().addPlayerPerson(playerPerson);
        createCareerInfo(playerPerson);
    }

    @FXML
    public void usePlayerAction() {
        createCareerInfo(existingPlayersBox.getValue());
    }

    @FXML
    public void playerInfoAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("abilityView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.initOwner(thisStage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();

            AbilityView controller = loader.getController();
            controller.setup(scene, existingPlayersBox.getValue());
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    private void launchNext() {
        thisStage.close();
        entryView.refreshGui();
        entryView.startCareerView(owner, new Stage());
    }

    private void createCareerInfo(PlayerPerson person) {
        Service<Void> service = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        CareerManager.createNew(person, 
                                playerGoodnessBox.getValue().multiplier, 
                                aiGoodnessBox.getValue().multiplier);
                        System.out.println("Start simulating");
                        long st = System.currentTimeMillis();
                        CareerManager.getInstance().simulateMatchesInPastTwoYears();
                        System.out.println("Simulation ends in " + (System.currentTimeMillis() - st) + " ms");
                        return null;
                    }
                };
            }
        };
        service.setOnSucceeded(event -> launchNext());
        service.setOnFailed(event -> EventLogger.error(event.getSource().getException()));

        basePane.setDisable(true);
        promptLabel.setText(strings.getString("initializingCareer"));

        service.start();
    }

    public enum Hand {
        LEFT,
        RIGHT;

        @Override
        public String toString() {
            return App.getStrings().getString(Util.toLowerCamelCase(name()));
        }
    }
    
    class Difficulty {
        double multiplier;
        String key;
        
        Difficulty(String shown, double multiplier) {
            this.multiplier = multiplier;
            this.key = shown;
        }

        @Override
        public String toString() {
            return strings.getString(key);
        }
    }
}
