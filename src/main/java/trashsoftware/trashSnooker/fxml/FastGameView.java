package trashsoftware.trashSnooker.fxml;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.training.Challenge;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.util.ConfigLoader;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.GeneralSaveManager;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.ResourceBundle;

public class FastGameView extends ChildInitializable {

    @FXML
    Button resumeButton;

    @FXML
    Button player1InfoButton, player2InfoButton;

    @FXML
    ComboBox<Integer> totalFramesBox;

    @FXML
    ComboBox<GameRule> gameRuleBox;

    @FXML
    ComboBox<TableMetrics.TableBuilderFactory> tableMetricsBox;

    @FXML
    ComboBox<BallMetrics> ballMetricsBox;

    @FXML
    ComboBox<TableCloth.Smoothness> clothSmoothBox;

    @FXML
    ComboBox<TableCloth.Goodness> clothGoodBox;

    @FXML
    ComboBox<PocketSize> holeSizeBox;

    @FXML
    ComboBox<PocketDifficulty> pocketDifficultyBox;

    @FXML
    ComboBox<PlayerPerson> player1Box, player2Box;

    @FXML
    ComboBox<CueItem> player1CueBox, player2CueBox;

    @FXML
    ComboBox<PlayerType> player1Player, player2Player;
    @FXML
    ComboBox<CategoryItem> player1CatBox, player2CatBox;

    @FXML
    CheckBox devModeBox;

    @FXML
    ToggleGroup gameTrainToggleGroup;
    @FXML
    RadioButton gameRadioBtn, trainRadioBtn;
    @FXML
    Label vsText, trainingItemText;
    @FXML
    ComboBox<TrainType> trainingItemBox;
    @FXML
    CheckBox trainChallengeBox;

    private Stage stage;
    private ResourceBundle strings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.strings = resources;

        initTypeSelectionToggle();
        initGameTypeBox();
        initTotalFramesBox();
        initClothBox();
        loadPlayerList();
        loadCueList();

        resumeButton.setDisable(!GeneralSaveManager.getInstance().hasSavedGame());

        gameRuleBox.getSelectionModel().select(0);
        tableMetricsBox.getSelectionModel().select(0);
        ballMetricsBox.getSelectionModel().select(0);
    }

    public void reloadPlayerList() {
        loadPlayerList();
    }

    private void initTypeSelectionToggle() {
        gameTrainToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            changeTrainGameVisibility(Objects.equals(newValue.getUserData(), "TRAIN"));
        });
    }

    private void changeTrainGameVisibility(boolean isTrain) {
        trainingItemText.setVisible(isTrain);
        trainingItemBox.setVisible(isTrain);
        trainChallengeBox.setVisible(isTrain);

        totalFramesBox.setDisable(isTrain);

        vsText.setVisible(!isTrain);
        player2InfoButton.setVisible(!isTrain);
        player2Box.setVisible(!isTrain);
        player2Player.setVisible(!isTrain);
        player2CueBox.setVisible(!isTrain);
        player2CatBox.setVisible(!isTrain);
    }

    private void reloadTrainingItemByGameType(GameRule rule) {
        trainingItemBox.getItems().clear();
        trainingItemBox.getItems().addAll(rule.supportedTrainings);
        trainingItemBox.getSelectionModel().select(0);
    }

    private void initTotalFramesBox() {
        for (int i = 1; i <= 41; i += 2) {
            totalFramesBox.getItems().add(i);
        }
        totalFramesBox.getSelectionModel().select(0);
    }

    private void initClothBox() {
        clothSmoothBox.getItems().addAll(TableCloth.Smoothness.values());
        clothGoodBox.getItems().addAll(TableCloth.Goodness.values());
        clothSmoothBox.getSelectionModel().select(1);
        clothGoodBox.getSelectionModel().select(1);
    }

    private void initGameTypeBox() {
        gameRuleBox.getItems().addAll(
                GameRule.values()
        );
        tableMetricsBox.getItems().addAll(
                TableMetrics.TableBuilderFactory.values()
        );
        ballMetricsBox.getItems().addAll(
                BallMetrics.SNOOKER_BALL,
                BallMetrics.POOL_BALL
        );

        gameRuleBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            switch (newValue) {
                case SNOOKER:
                case MINI_SNOOKER:
                    tableMetricsBox.getSelectionModel().select(TableMetrics.TableBuilderFactory.SNOOKER);
                    ballMetricsBox.getSelectionModel().select(BallMetrics.SNOOKER_BALL);
                    break;
                case CHINESE_EIGHT:
                case LIS_EIGHT:
                    tableMetricsBox.getSelectionModel().select(TableMetrics.TableBuilderFactory.CHINESE_EIGHT);
                    ballMetricsBox.getSelectionModel().select(BallMetrics.POOL_BALL);
                    break;
                case SIDE_POCKET:
                    tableMetricsBox.getSelectionModel().select(TableMetrics.TableBuilderFactory.SIDE_POCKET);
                    ballMetricsBox.getSelectionModel().select(BallMetrics.POOL_BALL);
                    break;
            }
            reloadTrainingItemByGameType(newValue);
        }));

        tableMetricsBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                holeSizeBox.getItems().clear();
                holeSizeBox.getItems().addAll(newValue.supportedHoles);
                holeSizeBox.getSelectionModel().select(newValue.supportedHoles.length / 2);

                pocketDifficultyBox.getItems().clear();
                pocketDifficultyBox.getItems().addAll(newValue.supportedDifficulties);
                pocketDifficultyBox.getSelectionModel().select(newValue.supportedDifficulties.length / 2);
            }
        });
    }

    private void loadCueList() {
        refreshCueList(player1CueBox);
        refreshCueList(player2CueBox);
    }

    private void refreshCueList(ComboBox<CueItem> box) {
        box.getItems().clear();
        for (Cue cue : DataLoader.getInstance().getCues().values()) {
            if (!cue.privacy) {
                box.getItems().add(new CueItem(cue, cue.getName()));
            }
        }
        box.getSelectionModel().select(0);
    }

    private void loadPlayerList() {
        player1CatBox.getItems().addAll(CategoryItem.values());
        player2CatBox.getItems().addAll(CategoryItem.values());
        
        addCatBoxProperty(player1CatBox, player1Box);
        addCatBoxProperty(player2CatBox, player2Box);
        
//        Collection<PlayerPerson> playerPeople = DataLoader.getInstance().getActualPlayers();
//        player1Box.getItems().clear();
//        player2Box.getItems().clear();
//        player1Box.getItems().addAll(playerPeople);
//        player2Box.getItems().addAll(playerPeople);

        addPlayerBoxProperty(player1Box, player1CueBox, player1InfoButton);
        addPlayerBoxProperty(player2Box, player2CueBox, player2InfoButton);

        player1Player.getItems().addAll(PlayerType.PLAYER, PlayerType.COMPUTER);
        player2Player.getItems().addAll(PlayerType.PLAYER, PlayerType.COMPUTER);
        player1Player.getSelectionModel().select(0);
        player2Player.getSelectionModel().select(0);
    }
    
    private void addCatBoxProperty(ComboBox<CategoryItem> catBox,
                                   ComboBox<PlayerPerson> playerBox) {
        catBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                playerBox.getItems().clear();
                Collection<PlayerPerson> catPlayers = DataLoader.getInstance().filterActualPlayersByCategory(newValue.cat);
                playerBox.getItems().addAll(catPlayers);
            }
        });
        catBox.getSelectionModel().select(0);
    }

    private void addPlayerBoxProperty(ComboBox<PlayerPerson> playerBox,
                                      ComboBox<CueItem> cueBox,
                                      Button infoButton) {
        playerBox.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        infoButton.setDisable(false);
                        refreshCueList(cueBox);
                        boolean sel = false;
                        for (Cue cue : newValue.getPrivateCues()) {
                            cueBox.getItems().add(new CueItem(cue, cue.getName()));
                            if (!sel) {
                                // 有私杆的人默认选择第一根私杆
                                cueBox.getSelectionModel().select(cueBox.getItems().size() - 1);
                                sel = true;
                            }
                        }
                    } else {
                        infoButton.setDisable(true);
                    }
                }));
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void playerInfoAction(ActionEvent event) {
        ComboBox<PlayerPerson> personBox;
        if (Objects.equals(event.getSource(), player1InfoButton)) {
            personBox = player1Box;
        } else {
            personBox = player2Box;
        }
        PlayerPerson person = personBox.getValue();
        if (person != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("abilityView.fxml"),
                        strings
                );
                Parent root = loader.load();
                root.setStyle(App.FONT_STYLE);

                Stage stage = new Stage();
                stage.initOwner(this.stage);
                stage.initModality(Modality.WINDOW_MODAL);

                Scene scene = new Scene(root);
                stage.setScene(scene);

                stage.show();

                AbilityView controller = loader.getController();
                controller.setup(scene, person);
            } catch (IOException e) {
                EventLogger.error(e);
            }
        }
    }

    @FXML
    void resumeAction() {
        EntireGame game = GeneralSaveManager.getInstance().getSave();
        if (game != null) {
            startGame(game, devModeBox.isSelected());
        } else {
            throw new RuntimeException("???");
        }
    }

    @FXML
    void startGameAction() {
        TableCloth cloth = new TableCloth(clothGoodBox.getValue(), clothSmoothBox.getValue());

        TableMetrics.TableBuilderFactory tableMetricsFactory =
                tableMetricsBox.getValue();
        TableMetrics tableMetrics = tableMetricsFactory
                .create()
                .pocketDifficulty(pocketDifficultyBox.getValue())
                .holeSize(holeSizeBox.getValue())
                .build();

        GameRule rule = gameRuleBox.getValue();
        BallMetrics ballMetrics = ballMetricsBox.getValue();

        // todo: 检查规则兼容性

        GameValues values = new GameValues(rule, tableMetrics, ballMetrics);
        
        TrainType trainType = getTrainType();
        if (trainType != null) {
            Challenge challenge = null;
            if (trainChallengeBox.isSelected()) {
                challenge = new Challenge(rule, trainType);
            }

            values.setTrain(getTrainType(), challenge);
        }

        showGame(values, cloth);
    }
    
    private TrainType getTrainType() {
        if (gameTrainToggleGroup.getSelectedToggle().getUserData().equals("TRAIN")) {
            return trainingItemBox.getValue();
        } else {
            return null;
        }
    }

    private void showGame(GameValues gameValues, TableCloth cloth) {
        PlayerPerson p1 = player1Box.getValue();
        PlayerPerson p2 = player2Box.getValue();
        if (p1 == null) {
            System.out.println("No enough players");
            return;
        }
        InGamePlayer igp1;
        InGamePlayer igp2;
        if (gameValues.isTraining()) {
            igp1 = new InGamePlayer(p1, player1CueBox.getValue().cue, player1Player.getValue(), 1, 1.0);
            igp2 = new InGamePlayer(p1, player1CueBox.getValue().cue, player1Player.getValue(), 2, 1.0);
        } else {
            if (p2 == null) {
                System.out.println("No enough players");
                return;
            }
            Cue stdBreakCue = DataLoader.getInstance().getStdBreakCue();
            if (stdBreakCue == null ||
                    gameValues.rule == GameRule.SNOOKER ||
                    gameValues.rule == GameRule.MINI_SNOOKER) {
                igp1 = new InGamePlayer(p1, player1CueBox.getValue().cue, player1Player.getValue(), 1, 1.0);
                igp2 = new InGamePlayer(p2, player2CueBox.getValue().cue, player2Player.getValue(), 2, 1.0);
            } else {
                igp1 = new InGamePlayer(p1, stdBreakCue, player1CueBox.getValue().cue, player1Player.getValue(), 1, 1.0);
                igp2 = new InGamePlayer(p2, stdBreakCue, player2CueBox.getValue().cue, player2Player.getValue(), 2, 1.0);
            }
        }

        EntireGame game = new EntireGame(igp1, igp2, gameValues, totalFramesBox.getValue(), cloth, null);
        startGame(game, devModeBox.isSelected());
    }

    private void startGame(EntireGame entireGame, boolean devMode) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("gameView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            AiCueResult.setAiPrecisionFactor(ConfigLoader.getInstance().getDouble("fastGameAiStrength", 1.0));

            GameView gameView = loader.getController();
            gameView.setup(stage, entireGame, devMode);
            gameView.setAimingLengthFactor(ConfigLoader.getInstance().getDouble("fastGameAiming", 1.0));

            stage.show();
        } catch (Exception e) {
            EventLogger.error(e);
        }
    }

    public static class CueItem {
        public final Cue cue;
        private final String string;

        CueItem(Cue cue, String string) {
            this.cue = cue;
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }
    
    public enum CategoryItem {
        ALL("All"),
        PROFESSIONAL("Professional"),
        AMATEUR("Amateur"),
        NOOB("Noob"),
        GOD("God");
        
        private final String cat;
        
        CategoryItem(String cat) {
            this.cat = cat;
        }

        @Override
        public String toString() {
            return PlayerPerson.getPlayerCategoryShown(cat, App.getStrings());
        }
    }
}
