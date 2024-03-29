package trashsoftware.trashSnooker.fxml;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import trashsoftware.trashSnooker.audio.GameAudio;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCueBallPlacer;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerMatch;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeMatch;
import trashsoftware.trashSnooker.core.career.championship.PlayerVsAiMatch;
import trashsoftware.trashSnooker.core.career.championship.SnookerChampionship;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.Rule;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallGame;
import trashsoftware.trashSnooker.core.scoreResult.ChineseEightScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.NineBallScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.SnookerScoreResult;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;
import trashsoftware.trashSnooker.core.table.ChineseEightTable;
import trashsoftware.trashSnooker.core.table.NumberedBallTable;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.drawing.CueModel;
import trashsoftware.trashSnooker.fxml.drawing.GameLoop;
import trashsoftware.trashSnooker.fxml.drawing.PredictionQuality;
import trashsoftware.trashSnooker.fxml.projection.BallProjection;
import trashsoftware.trashSnooker.fxml.projection.CushionProjection;
import trashsoftware.trashSnooker.fxml.projection.ObstacleProjection;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;
import trashsoftware.trashSnooker.recorder.ActualRecorder;
import trashsoftware.trashSnooker.recorder.CueRecord;
import trashsoftware.trashSnooker.recorder.GameReplay;
import trashsoftware.trashSnooker.recorder.TargetRecord;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class GameView implements Initializable {
    public static final Color GLOBAL_BACKGROUND = Color.WHITESMOKE;  // 似乎正好是javafx默认背景色

    public static final Color WHITE = Color.WHITE;
    public static final Color BLACK = Color.BLACK;
    public static final Color CUE_AIMING_CROSS = Color.LIGHTGRAY;
    public static final Color CUE_POINT = Color.RED;
    public static final Color INTENT_CUE_POINT = Color.NAVY;
    public static final Color CUE_TIP_COLOR = Color.LIGHTSEAGREEN;
    public static final Color REST_METAL_COLOR = Color.GOLDENROD;
    public static final Font POOL_NUMBER_FONT = new Font(8.0);
    public static final double HAND_DT_TO_MAX_PULL = 30.0;
    public static final double MIN_CUE_BALL_DT = 30.0;  // 运杆时杆头离白球的最小距离
    public static final double MAX_CUE_ANGLE = 75.0;
    private static final double DEFAULT_POWER = 30.0;
    private static final double WHITE_PREDICT_LEN_AFTER_WALL = 1000.0;  // todo: 根据球员
    private static final long DEFAULT_REPLAY_GAP = 1000;
    //    public static double scale;
    public static int productionFrameRate = 60;  // 这两个是管物理运算的存档率的，也就是movement和回放文件的帧率
    public static double frameTimeMs = 1000.0 / productionFrameRate;
    private static double uiFrameTimeMs = 10.0;
    //    private double minRealPredictLength = 300.0;
    private static double defaultMaxPredictLength = 1200;
    private final List<Node> disableWhenCuing = new ArrayList<>();  // 出杆/播放动画时不准按的东西
    @FXML
    GamePane gamePane;  // 球和桌子画在这里
    @FXML
    Canvas cueAngleCanvas;
    @FXML
    Label cueAngleLabel;
    @FXML
    Canvas cuePointCanvas;
    @FXML
    Slider powerSlider;
    @FXML
    Label powerLabel;
    @FXML
    Button cueButton;
    @FXML
    Button replayNextCueButton, replayLastCueButton;
    @FXML
    CheckBox replayAutoPlayBox;
    @FXML
    VBox gameButtonBox, replayButtonBox;
    @FXML
    Label singlePoleLabel;
    @FXML
    Canvas singlePoleCanvas;
    @FXML
    Label player1Label, player2Label, player1ScoreLabel, player2ScoreLabel;
    @FXML
    Label player1FramesLabel, totalFramesLabel, player2FramesLabel;
    @FXML
    Label snookerScoreDiffLabel, snookerScoreRemainingLabel;
    @FXML
    Label fpsLabel;
    @FXML
    Canvas player1TarCanvas, player2TarCanvas;
    @FXML
    Menu gameMenu;
    @FXML
    MenuItem withdrawMenu, replaceBallInHandMenu, letOtherPlayMenu, repositionMenu, pushOutMenu;
    @FXML
    SeparatorMenuItem gameMenuSep1;
    @FXML
    CheckMenuItem aiHelpPlayMenuItem;
    @FXML
    Menu debugMenu;
    @FXML
    MenuItem debugModeMenu;
    //            , saveGameMenu, newGameMenu;
    @FXML
    ToggleGroup player1SpeedToggle, player2SpeedToggle;
    @FXML
    Menu player1SpeedMenu, player2SpeedMenu;
    @FXML
    CheckMenuItem aiAutoPlayMenuItem;
    @FXML
    CheckMenuItem drawAiPathItem;
    CheckMenuItem predictPlayerPathItem = new CheckMenuItem();
    @FXML
    VBox handSelectionBox;
    @FXML
    ToggleGroup handSelectionToggleGroup;
    @FXML
    RadioButton handSelectionLeft, handSelectionRight, handSelectionRest;
    boolean debugMode = false;
    boolean devMode = true;
    PredictionQuality predictionQuality = PredictionQuality.fromKey(
            ConfigLoader.getInstance().getString("performance", "veryHigh"));
    //    private Timeline timeline;
    GameLoop gameLoop;
    //    AnimationTimer animationTimer;
    private double minPredictLengthPotDt = 2000;
    private double maxPredictLengthPotDt = 100;
    private double ballDiameter;
    private double ballRadius;
    private double cueCanvasWH = 80.0;
    private double cueAreaRadius = 36.0;
    private double cueRadius = 4.0;
    private GraphicsContext cuePointCanvasGc;
    private GraphicsContext cueAngleCanvasGc;
    private Pane basePane;  // 杆是画在这个pane上的
    private Stage stage;
    private InGamePlayer player1;
    private InGamePlayer player2;
    private EntireGame game;
    private GameReplay replay;
    private GameValues gameValues;
    private double cursorDirectionUnitX, cursorDirectionUnitY;
    private double targetPredictionUnitX, targetPredictionUnitY;
    private Ball predictedTargetBall;
    private Movement movement;
    private boolean playingMovement = false;
    private GamePlayStage currentPlayStage;  // 只对game有效, 并且不要直接access这个变量, 请用getter
    private PotAttempt lastPotAttempt;
    private DefenseAttempt curDefAttempt;
    // 用于播放运杆动画时持续显示预测线（含出杆随机偏移）
    private ObstacleProjection obstacleProjection;
    private double mouseX, mouseY;
    // 杆法的击球点。注意: cuePointY用在击球点的canvas上，值越大杆法越低，而unitFrontBackSpin相反
    private double cuePointX, cuePointY;  // 杆法的击球点
    private double intentCuePointX = -1, intentCuePointY = -1;  // 计划的杆法击球点
    private double cueAngleDeg = 5.0;
    private double cueAngleBaseVer = 10.0;
    private double cueAngleBaseHor = 10.0;
    private CueAnimationPlayer cueAnimationPlayer;
    private boolean isDragging;
    private double lastDragAngle;
    //    private double predictionMultiplier = 2000.0;
    private double maxRealPredictLength = defaultMaxPredictLength;
    private boolean enablePsy = true;  // 由游戏决定心理影响
    private boolean aiCalculating;
    private boolean aiAutoPlay = true;
    private boolean printPlayStage = false;
    private boolean tableGraphicsChanged = true;
    private Ball debuggingBall;
    private long replayStopTime;
    private long replayGap = DEFAULT_REPLAY_GAP;
    private boolean drawStandingPos = false;
    private boolean drawTargetRefLine = false;
    private boolean miscued = false;
    private PlayerPerson.HandSkill currentHand;
    private CareerMatch careerMatch;
    private PredictionDrawing cursorDrawer;

    private ResourceBundle strings;

    private double p1PlaySpeed = 1.0;
    private double p2PlaySpeed = 1.0;
    private boolean aiHelpPlay = false;

    private List<double[]> aiWhitePath;  // todo: debug用的
    private List<double[]> suggestedPlayerWhitePath;

    private Map<Cue, CueModel> cueModelMap = new HashMap<>();

    private static double pullDtOf(PlayerPerson person, double personPower) {
        return (person.getMaxPullDt() - person.getMinPullDt()) *
                personPower + person.getMinPullDt();
    }

    private static double extensionDtOf(PlayerPerson person, double personPower) {
        return (person.getMaxExtension() - person.getMinExtension()) *
                personPower + person.getMinExtension();
    }

    private static double[] handPosition(double handDt,
                                         double cueAngleDeg,
                                         double whiteX, double whiteY,
                                         double trueAimingX, double trueAimingY) {
        double cueAngleCos = Math.cos(Math.toRadians(cueAngleDeg));
        double handX = whiteX - handDt * trueAimingX * cueAngleCos;
        double handY = whiteY - handDt * trueAimingY * cueAngleCos;
        return new double[]{handX, handY};
    }

    private static double aimingOffsetOfPlayer(PlayerPerson person, double selectedPower) {
        double playerAimingOffset = person.getAimingOffset();
        // 这个比较固定，不像出杆扭曲那样，发暴力时歪得夸张
        return (playerAimingOffset * (selectedPower / 100.0) + playerAimingOffset) / 8.0;
    }

    private static double getPersonPower(double selectedPower, PlayerPerson person) {
        return selectedPower / person.getMaxPowerPercentage();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.strings = resources;

        cueModelMap.clear();

        player1SpeedToggle.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            if (t1 != null)
                player1SpeedMenu.setText(strings.getString("p1PlaySpeedMenu") + " " + t1.getUserData() + "x");
        });
        player2SpeedToggle.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            if (t1 != null)
                player2SpeedMenu.setText(strings.getString("p2PlaySpeedMenu") + " " + t1.getUserData() + "x");
        });

        player1SpeedToggle.selectToggle(player1SpeedToggle.getToggles().get(3));
        player2SpeedToggle.selectToggle(player2SpeedToggle.getToggles().get(3));
        drawAiPathItem.selectedProperty().addListener((observable, oldValue, newValue) ->
                tableGraphicsChanged = true);

        cuePointCanvasGc = cuePointCanvas.getGraphicsContext2D();
        cuePointCanvasGc.setTextAlign(TextAlignment.CENTER);
        cuePointCanvasGc.setFont(App.FONT);

        cueAngleCanvasGc = cueAngleCanvas.getGraphicsContext2D();

        boolean aa = ConfigLoader.getInstance().getAntiAliasing().canvasAA;
        cuePointCanvasGc.setImageSmoothing(aa);
        cueAngleCanvasGc.setImageSmoothing(aa);

        player1TarCanvas.getGraphicsContext2D().setImageSmoothing(aa);
        player2TarCanvas.getGraphicsContext2D().setImageSmoothing(aa);
        singlePoleCanvas.getGraphicsContext2D().setImageSmoothing(aa);

        // todo: 喊mac用户来测试
        player1TarCanvas.getGraphicsContext2D().setFont(App.FONT);
        player2TarCanvas.getGraphicsContext2D().setFont(App.FONT);

        addListeners();

        disableWhenCuing.addAll(List.of(
                cueButton,
                cuePointCanvas,
                cueAngleCanvas,
                handSelectionLeft,
                handSelectionRight,
                handSelectionRest
        ));

        powerSlider.setShowTickLabels(true);
    }

    private void generateScales(GameValues gameValues) {
        gamePane.setupPane(gameValues);
        double scale = gamePane.getScale();

//        double topLeftY = (canvasHeight - innerHeight) / 2;
        ballDiameter = gameValues.ball.ballDiameter * scale;
        ballRadius = ballDiameter / 2;
//        cornerArcDiameter = values.cornerArcDiameter * scale;

        double[] actualResolution = ConfigLoader.getInstance().getEffectiveResolution();
        double zoomRatio = actualResolution[1] / 864;
        if (zoomRatio < 1.0) {
            cueCanvasWH *= zoomRatio;
            cueAreaRadius *= zoomRatio;
            cueRadius *= zoomRatio;

            cueAngleBaseHor *= zoomRatio;
            cueAngleBaseVer *= zoomRatio;
        }
        powerSlider.setPrefHeight(powerSlider.getPrefHeight() * zoomRatio);

        cuePointCanvas.setWidth(cueCanvasWH);
        cuePointCanvas.setHeight(cueCanvasWH);
        cueAngleCanvas.setWidth(cueCanvasWH);
        cueAngleCanvas.setHeight(cueCanvasWH);

        player1TarCanvas.setHeight(ballDiameter * 1.2);
        player1TarCanvas.setWidth(ballDiameter * 1.2);
        player2TarCanvas.setHeight(ballDiameter * 1.2);
        player2TarCanvas.setWidth(ballDiameter * 1.2);
        singlePoleCanvas.setHeight(ballDiameter * 1.2);
        if (gameValues.rule.snookerLike())
            singlePoleCanvas.setWidth(ballDiameter * 7 * 1.2);
        else if (gameValues.rule == GameRule.CHINESE_EIGHT || gameValues.rule == GameRule.LIS_EIGHT)
            singlePoleCanvas.setWidth(ballDiameter * 8 * 1.2);
        else if (gameValues.rule == GameRule.AMERICAN_NINE)
            singlePoleCanvas.setWidth(ballDiameter * 9 * 1.2);
        else throw new RuntimeException("nmsl");

        singlePoleCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        singlePoleCanvas.getGraphicsContext2D().setStroke(WHITE);
        player1TarCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        player1TarCanvas.getGraphicsContext2D().setStroke(WHITE);
        player2TarCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        player2TarCanvas.getGraphicsContext2D().setStroke(WHITE);
    }

    private void setupDebug() {
        System.out.println("Debug: " + devMode);
        debugMenu.setVisible(devMode);

        if (devMode) {
            drawAiPathItem.setSelected(true);
        }

        setupCheckMenus();
    }

    private void setupCheckMenus() {
        aiAutoPlayMenuItem.selectedProperty().addListener((observableValue, aBoolean, t1) -> aiAutoPlay = t1);
        aiAutoPlayMenuItem.setSelected(aiAutoPlay);

        predictPlayerPathItem.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (t1) {
                if (game.getGame().isEnded()
                        || cueAnimationPlayer != null || playingMovement || aiCalculating) return;
                Player currentPlayer = game.getGame().getCuingPlayer();
                if (currentPlayer.getInGamePlayer().getPlayerType() == PlayerType.PLAYER) {
                    predictPlayerPath(currentPlayer);
                }
            } else {
                suggestedPlayerWhitePath = null;
            }
        });

        if (replay != null) {
            // disable all
            aiAutoPlayMenuItem.setVisible(false);
            drawAiPathItem.setVisible(false);
            predictPlayerPathItem.setVisible(false);
        }
    }

    public void setupReplay(Stage stage, GameReplay replay) {
        this.replay = replay;
        this.gameValues = replay.gameValues;
        this.devMode = false;
        this.stage = stage;

        this.player1 = replay.getP1();
        this.player2 = replay.getP2();

        this.basePane = (Pane) stage.getScene().getRoot();

        gameButtonBox.setVisible(false);
        gameButtonBox.setManaged(false);

        productionFrameRate = replay.getFrameRate();
        frameTimeMs = 1000.0 / productionFrameRate;

        setupNameLabels(player1.getPlayerPerson(), player2.getPlayerPerson());
        totalFramesLabel.setText(String.format("(%d)", replay.getItem().totalFrames));

        generateScales(replay.gameValues);
        restoreCuePoint();
        restoreCueAngle();
//        setupCanvas();
        drawTargetBoard(true);

        setupPowerSlider();
        gameMenu.getItems().clear();
        setUiFrameStart();
        setupDebug();
        setupHandSelection();

        setupBalls();

        setOnHidden();

//        for (CueModel cueModel : CueModel.getAllCueModels()) {
//            basePane.getChildren().add(cueModel);
//        }

        startAnimation();

//        while (replay.loadNext() && replay.getCurrentFlag() == GameRecorder.FLAG_HANDBALL) {
//            // do nothing
//        }
//        replayCue();
    }

    public void setup(Stage stage, EntireGame entireGame, boolean devMode) {
        this.game = entireGame;
        this.gameValues = entireGame.gameValues;
        this.player1 = entireGame.getPlayer1();
        this.player2 = entireGame.getPlayer2();
        this.stage = stage;
        this.devMode = devMode;
        this.aiAutoPlay = !devMode;

        this.basePane = (Pane) stage.getScene().getRoot();

        setFrameRate(ConfigLoader.getInstance().getFrameRate());
        setKeyboardActions();

        generateScales(entireGame.gameValues);
        restoreCuePoint();
        restoreCueAngle();
//        setupCanvas();
//        drawTargetBoard(true);

        setupHandSelection();
        startNextFrame();
//        game.startNextFrame();  // fixme: 问题 game.game不是null的时候就渲染不出球
//        drawScoreBoard(game.getGame().getCuingPlayer(), true);
//        cursorDrawer = new PredictionDrawing();

        replayButtonBox.setVisible(false);
        replayButtonBox.setManaged(false);

        setupNameLabels(player1.getPlayerPerson(), player2.getPlayerPerson());
        totalFramesLabel.setText(String.format("(%d)", entireGame.totalFrames));

        updatePlayStage();

        setupPowerSlider();
        updatePowerSlider(game.getGame().getCuingPlayer().getPlayerPerson());

        setupGameMenu();
        setupAiHelper();
//        setUiFrameStart();
        setupDebug();

//        setupBalls();

        stage.setOnCloseRequest(e -> {
            if (!game.isFinished()) {
                e.consume();

                AlertShower.askConfirmation(stage,
                        strings.getString("closeWindowConcede"),
                        strings.getString("notEndExitWarning"),
                        () -> {
//                            game.quitGame(careerMatch != null);
//                            timeline.stop();
//                            gameLoop.cancel();
                            System.out.println("Close request");
                            gameLoop.stop();
                            stage.close();
                        },
                        null);
            }
        });

        setOnHidden();

//        for (CueModel cueModel : CueModel.getAllCueModels()) {
//            basePane.getChildren().add(cueModel);
//        }

        startAnimation();
    }

    private void setupGameMenu() {
        gameMenu.getItems().clear();
        gameMenu.setDisable(replay != null);
        if (careerMatch != null) {
            gameMenu.getItems().addAll(aiHelpPlayMenuItem, gameMenuSep1);
        }
        
        gameMenu.getItems().addAll(withdrawMenu, replaceBallInHandMenu);

        GameRule rule = gameValues.rule;
//        if (rule.hasRule(Rule.FOUL_LET_OTHER_PLAY)) {
        gameMenu.getItems().add(letOtherPlayMenu);
//        }
        if (rule.hasRule(Rule.FOUL_AND_MISS)) {
            gameMenu.getItems().add(repositionMenu);
        }
        if (rule.hasRule(Rule.PUSH_OUT)) {
            gameMenu.getItems().add(pushOutMenu);
        }
    }
    
    private void setupAiHelper() {
        aiHelpPlayMenuItem.selectedProperty().addListener(((observableValue, aBoolean, t1) -> {
            if (t1) {
                AlertShower.askConfirmation(
                        stage,
                        strings.getString("aiHelpPlayerPrompt"),
                        strings.getString("aiHelpPlayerTitle"),
                        () -> aiHelpPlay = true,
                        () -> aiHelpPlayMenuItem.setSelected(false));
            } else {
                aiHelpPlay = false;
            }
        }));
    }

    private void setupHandSelection() {
        if (replay != null) {
            handSelectionBox.setVisible(false);
            handSelectionBox.setManaged(false);
        } else {
            handSelectionToggleGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
                if (game == null || game.getGame() == null) return;
                PlayerPerson.Hand selected = PlayerPerson.Hand.valueOf(String.valueOf(t1.getUserData()));
                PlayerPerson person = game.getGame().getCuingPlayer().getPlayerPerson();
                currentHand = person.handBody.getHandSkillByHand(selected);
                createPathPrediction();
            });

            handSelectionToggleGroup.selectToggle(handSelectionRight);
        }
    }
    
    private void updateHandSelectionToggleByData(PlayerPerson.Hand hand) {
        Toggle toggle = switch (hand) {
            case LEFT -> handSelectionLeft;
            case RIGHT -> handSelectionRight;
            default -> handSelectionRest;
        };
        
        handSelectionToggleGroup.selectToggle(toggle);
    }

    private void updateHandSelection(boolean forceChangeHand) {
        Ball cueBall = game.getGame().getCueBall();
        InGamePlayer igp = game.getGame().getCuingPlayer().getInGamePlayer();
        if (igp.getPlayerType() == PlayerType.COMPUTER) return;
        PlayerPerson playingPerson = igp.getPlayerPerson();

        List<PlayerPerson.Hand> playAbles = CuePlayParams.getPlayableHands(
                cueBall.getX(), cueBall.getY(),
                cursorDirectionUnitX, cursorDirectionUnitY,
                gameValues.table,
                playingPerson
        );

        boolean leftWasUsable = !handSelectionLeft.isDisable();
        boolean rightWasUsable = !handSelectionRight.isDisable();

        handSelectionLeft.setDisable(!playAbles.contains(PlayerPerson.Hand.LEFT));
        handSelectionRight.setDisable(!playAbles.contains(PlayerPerson.Hand.RIGHT));
        handSelectionRest.setDisable(!playAbles.contains(PlayerPerson.Hand.REST));  // 事实上架杆永远可用

        boolean anyChange = forceChangeHand ||
                (leftWasUsable == handSelectionLeft.isDisabled()) ||
                (rightWasUsable == handSelectionRight.isDisabled());

        if (anyChange) {
            // 如果这次update改变了任何“一只手”的可用性，刷新为可用的第一顺位手
            handSelectionToggleGroup.selectToggle(
                    handButtonOfHand(playAbles.get(0))
            );
            currentHand = playingPerson.handBody.getHandSkillByHand(playAbles.get(0));  // 防止由于toggle没变的原因导致不触发换手
        }
    }

    private RadioButton handButtonOfHand(PlayerPerson.Hand hand) {
        switch (hand) {
            case LEFT:
                return handSelectionLeft;
            case RIGHT:
                return handSelectionRight;
            case REST:
                return handSelectionRest;
            default:
                throw new RuntimeException("If this happens then go fuck the developer");
        }
    }

    private void setupNameLabels(PlayerPerson p1, PlayerPerson p2) {
        String p1n = p1.getName();
        String p2n = p2.getName();

        if (careerMatch != null && careerMatch.getChampionship() != null) {
            p1n += " (" + careerMatch.getChampionship().getCareerSeedMap().get(p1.getPlayerId()) + ")";
            p2n += " (" + careerMatch.getChampionship().getCareerSeedMap().get(p2.getPlayerId()) + ")";
        }

        player1Label.setText(p1n);
        player2Label.setText(p2n);
    }

    public void setupCareerMatch(Stage stage,
                                 CareerMatch careerMatch) {
        this.careerMatch = careerMatch;

        setup(stage, careerMatch.getGame(), false);

        double playerGoodness = CareerManager.getInstance().getPlayerGoodness();
        setAimingLengthFactor(playerGoodness);
    }

    public void setAimingLengthFactor(double aimingLengthFactor) {
        maxRealPredictLength = defaultMaxPredictLength * aimingLengthFactor;
    }

    public void setFrameRate(int frameRate) {
        uiFrameTimeMs = 1000.0 / frameRate;
    }

    private void keyboardAction(KeyEvent e) {
        if (replay != null || aiCalculating || playingMovement || cueAnimationPlayer != null) {
            return;
        }
        switch (e.getCode()) {
            case SPACE:
                if (!cueButton.isDisabled()) {
                    cueButton.fire();
                }
                break;
            case LEFT:
                turnDirectionDeg(-0.5);
                break;
            case RIGHT:
                turnDirectionDeg(0.5);
                break;
            case COMMA:
                turnDirectionDeg(-0.01);
                break;
            case PERIOD:
                turnDirectionDeg(0.01);
                break;
            case A:
                setCuePoint(cuePointX - 1, cuePointY, true);
                break;
            case D:
                setCuePoint(cuePointX + 1, cuePointY, true);
                break;
            case W:
                setCuePoint(cuePointX, cuePointY - 1, true);
                break;
            case S:
                setCuePoint(cuePointX, cuePointY + 1, true);
                break;
            case Q:
                setCueAngleDeg(cueAngleDeg + 1);
                break;
            case E:
                setCueAngleDeg(cueAngleDeg - 1);
                break;
        }
    }

    private void setKeyboardActions() {
        powerSlider.setBlockIncrement(1.0);

        basePane.setOnKeyPressed(this::keyboardAction);

        for (Toggle toggle : handSelectionToggleGroup.getToggles()) {
            RadioButton rb = (RadioButton) toggle;
            rb.setOnKeyPressed(this::keyboardAction);
        }
    }

    private void turnDirectionDeg(double deg) {
        double rad = Math.toRadians(deg);
        double cur = Algebra.thetaOf(cursorDirectionUnitX, cursorDirectionUnitY);
        cur += rad;
        double[] nd = Algebra.unitVectorOfAngle(cur);
        cursorDirectionUnitX = nd[0];
        cursorDirectionUnitY = nd[1];
        recalculateUiRestrictions();
    }

    private void setOnHidden() {
        this.stage.setOnHidden(e -> {
            System.out.println("Hide");
//            DataLoader.getInstance().invalidate();
//            basePane.getChildren().clear();

            if (replay != null) {
                try {
                    replay.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                if (!game.getGame().getRecorder().isFinished()) {
                    game.getGame().getRecorder().stopRecording(false);
                }
                if (!game.isFinished() && !game.getGame().isEnded()) {
                    Player p1 = game.getGame().getPlayer1();
//                    game.quitGame(careerMatch != null);
                    InGamePlayer winner;
                    if (careerMatch != null) {
                        winner = player1.getPlayerType() == PlayerType.PLAYER ?
                                player2 : player1;
                    } else {
                        winner =
                                (game.getGame().getCuingPlayer() == game.getGame().getPlayer1() ?
                                        game.getGame().getPlayer2() : game.getGame().getPlayer1())
                                        .getInGamePlayer();
                    }

                    if (careerMatch != null) {
//                        game.saveTo(PlayerVsAiMatch.getMatchSave());
                        boolean matchFinish = game.playerWinsAframe(winner);
                        if (matchFinish) {
                            if (careerMatch instanceof ChallengeMatch) {
                                ((ChallengeMatch) careerMatch).setScore(p1.getScore());
                                careerMatch.finish(game.getGame().getPlayer2().getPlayerPerson(),
                                        0, 1);
                            } else {
                                careerMatch.finish(winner.getPlayerPerson(),
                                        game.getP1Wins(),
                                        game.getP2Wins());  // 这里没用endFrame或者withdraw，因为不想影响数据库
                            }
                        } else {
                            careerMatch.saveMatch();
                            careerMatch.saveAndExit();
                        }
                    } else {
                        if (game.getGame().isStarted() || (game.getP1Wins() + game.getP2Wins() > 0)) {
                            boolean matchFinish = game.playerWinsAframe(winner);
                            if (!matchFinish) game.generalSave();  // 这局SL了，还有下一局
                        } else {
                            // 上来第一局没开球就sl，删记录了
                            game.quitGameDeleteRecord();
                            game.getGame().getRecorder().deleteRecord();
                        }
                    }
//                    else {
//                        game.generalSave();
//                    }
                }
            }
//            timeline.stop();
//            gameLoop.cancel();
            gameLoop.stop();
        });
    }

    private void setUiFrameStart() {
        InGamePlayer igp;
        if (replay != null) {
            if (replay.getCueRecord() != null) {
                igp = replay.getCueRecord().cuePlayer;
            } else {
                return;
            }
        } else {
            igp = game.getGame().getCuingPlayer().getInGamePlayer();
        }

        PlayerPerson playerPerson = igp.getPlayerPerson();

        updatePowerSlider(playerPerson);
        if (replay == null) {
            cueButton.setDisable(false);
            if (igp.getPlayerType() == PlayerType.COMPUTER) {
                cueButton.setText(strings.getString("aiCueText"));
            } else {
                cueButton.setText(strings.getString("cueText"));
                enableDisabledUi();
            }
        }
        updateScoreDiffLabels();
    }

    private void updateScoreDiffLabels() {
        if (gameValues.rule.snookerLike()) {
            if (replay != null) {

            } else {
                AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
                snookerScoreDiffLabel.setText(String.format(strings.getString("scoreDiff"),
                        asg.getScoreDiffAbs()));
                snookerScoreRemainingLabel.setText(String.format(strings.getString("scoreRem"),
                        asg.getRemainingScore(asg.isDoingSnookerFreeBll())));
            }
        }
    }

    public void finishCueReplay() {
        replayStopTime = System.currentTimeMillis();

        drawScoreBoard(null, true);
        drawTargetBoard(true);
        restoreCuePoint();
        restoreCueAngle();
        updateScoreDiffLabels();

//        replayNextCueButton.setDisable(false);
        replayNextCueButton.setText(strings.getString("replayNextCue"));
        replayNextCueButton.setOnAction(this::replayNextCueAction);
        replayLastCueButton.setDisable(false);

        tableGraphicsChanged = true;
    }

    private void replayLoadNext() {
        if (replay.loadNext()) {
            drawTargetBoard(false);
            replayCue();
        } else {
            System.out.println("Replay finished!");
        }
    }

    private void updatePowerSlider(PlayerPerson cuingPlayer) {
        powerSlider.setValue(DEFAULT_POWER);
        powerSlider.setMajorTickUnit(
                cuingPlayer.getControllablePowerPercentage());
    }

    public void finishCue(Player justCuedPlayer, Player nextCuePlayer) {
//        updateCuePlayerSinglePole(justCuedPlayer);
        if (cueAnimationPlayer != null) {
            endCueAnimation();
        }
        oneFrame();
        drawScoreBoard(justCuedPlayer, true);
        drawTargetBoard(true);
        restoreCuePoint();
        restoreCueAngle();
        updateScoreDiffLabels();
        Platform.runLater(() -> updatePowerSlider(nextCuePlayer.getPlayerPerson()));
        setButtonsCueEnd(nextCuePlayer);
        obstacleProjection = null;
        printPlayStage = true;

        if (curDefAttempt != null && curDefAttempt.isSolvingSnooker()) {
            curDefAttempt.setSolveSuccess(!game.getGame().isThisCueFoul());
        }
        
        ScoreResult scoreResult = game.getGame().makeScoreResult(justCuedPlayer);
        
        game.getGame().getRecorder().recordScore(scoreResult);
        game.getGame().getRecorder().recordNextTarget(makeTargetRecord(nextCuePlayer));
        game.getGame().getRecorder().writeCueToStream();

        AchManager.getInstance().updateAfterCueFinish(gamePane, game.getGame(), scoreResult,
                lastPotAttempt, curDefAttempt, gamePlayStage());

        FoulInfo foulInfo = game.getGame().getThisCueFoul();
        if (foulInfo.isFoul()) {
            String foulReason0 = game.getGame().getFoulReason();
            if (game.getGame() instanceof AbstractSnookerGame) {
                if (foulInfo.isMiss()) {
                    foulReason0 = strings.getString("foulAndMiss") + foulReason0;
                }
            }
            String foulReason = foulReason0;
            String headerReason = foulInfo.getHeaderReason(strings);

            Platform.runLater(() -> {
                AlertShower.showInfo(
                        stage,
                        foulReason,
                        headerReason,
                        3000
                );
                if (game.getGame().isEnded()) {
                    endFrame();
                } else {
                    finishCueNextStep(nextCuePlayer);
                }
            });
        } else {
            if (game.getGame().isEnded()) {
                endFrame();
            } else {
                finishCueNextStep(nextCuePlayer);
            }
        }
    }

    private void autoAimEasiestNextBall(Player nextCuePlayer) {
        if (game.getGame().getCueBall().isPotted()) return;
        Ball tgt = game.getGame().getEasiestTarget(nextCuePlayer);
        if (tgt == null) return;

        double dx = tgt.getX() - game.getGame().getCueBall().getX();
        double dy = tgt.getY() - game.getGame().getCueBall().getY();

        double[] unit = Algebra.unitVector(dx, dy);
        cursorDirectionUnitX = unit[0];
        cursorDirectionUnitY = unit[1];
        recalculateUiRestrictions(true);

        tableGraphicsChanged = true;
    }

    @Deprecated
    private void predictPlayerPath(Player humanPlayer) {
        Thread thread = new Thread(() -> {
            System.out.println("ai predicting human player path");
            long st = System.currentTimeMillis();
            if (game.getGame().isBallInHand()) {
                return;
            }
            AiCueResult cueResult = game.getGame().aiCue(humanPlayer, game.predictPhy);
            System.out.println("ai predicting human player path in " + (System.currentTimeMillis() - st) +
                    " ms, result: " + cueResult);
            if (cueResult != null) {
                suggestedPlayerWhitePath = cueResult.getWhitePath();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void finishCueNextStep(Player nextCuePlayer) {
        cuePointCanvas.setDisable(false);
        cueAngleCanvas.setDisable(false);

        cursorDrawer.synchronizeGame();  // 刷新白球预测的线程池

        Ball.enableGearOffset();
        aiWhitePath = null;
        miscued = false;
        if (nextCuePlayer.getInGamePlayer().getPlayerType() == PlayerType.PLAYER && !aiHelpPlay) {
            boolean autoAim = true;

            if (game.getGame().canReposition()) {
                System.out.println("Solvable snooker");
                autoAim = false;  // 把autoAim交给askReposition的不复位分支
                repositionMenu.setDisable(false);
                askReposition();
            }
            if (game.getGame() instanceof NeedBigBreak nbb) {
                if (nbb.isJustAfterBreak() && nbb.wasIllegalBreak()) {
//                    autoAim = false;  // 把autoAim交给askAfterBreakLoseChance的不复位分支
//                    askAfterBreakLoseChance();
                    letOtherPlayMenu.setDisable(false);
                }
            }
            if (game.getGame().getThisCueFoul().isFoul()) {
                if (game.getGame().getGameValues().rule.hasRule(Rule.FOUL_LET_OTHER_PLAY)) {
                    letOtherPlayMenu.setDisable(false);
                }
            }
            if (game.getGame() instanceof AmericanNineBallGame g) {
                if (g.currentlyCanPushOut()) {
                    pushOutMenu.setDisable(false);
                }
                if (g.lastCueWasPushOut()) {
                    letOtherPlayMenu.setDisable(false);
                }
            }

            if (autoAim) autoAimEasiestNextBall(nextCuePlayer);
            if (predictPlayerPathItem.isSelected()) {
                predictPlayerPath(nextCuePlayer);
            }
        } else {
            if (!game.isFinished() &&
                    aiAutoPlay) {
                Platform.runLater(() -> aiCue(nextCuePlayer));
            }
        }
        updatePlayStage();
        recalculateUiRestrictions();

        tableGraphicsChanged = true;
    }

    private void updateChampionshipBreaks(SnookerChampionship sc,
                                          PlayerVsAiMatch pva,
                                          SnookerPlayer player,
                                          int nFrameFrom1) {
        for (Integer breakScore : player.getSinglePolesInThisGame()) {
            sc.updateBreakScore(player.getPlayerPerson().getPlayerId(), pva.stage, breakScore, false,
                    pva.metaMatchInfo.toString(), nFrameFrom1);
        }
    }

    private void startNextFrame() {
        game.startNextFrame();
        setupBalls();
        
        predictedTargetBall = null;
        aiWhitePath = null;
        suggestedPlayerWhitePath = null;

        drawScoreBoard(game.getGame().getCuingPlayer(), true);
        drawTargetBoard(true);
        setUiFrameStart();

        endCueAnimation();
        if (cursorDrawer == null) {
            cursorDrawer = new PredictionDrawing();
        } else {
            cursorDrawer.synchronizeGame();
        }

        Player breakPlayer = game.getGame().getCuingPlayer();
        updateHandSelection(true);
        updatePowerSlider(breakPlayer.getPlayerPerson());

        tableGraphicsChanged = true;
        
        AchManager.getInstance().showAchievementPopup();
    }

    private void endFrame() {
        hideCue();
        tableGraphicsChanged = true;
        Player p1 = game.getGame().getPlayer1();
        Player wonPlayer = game.getGame().getWiningPlayer();
        boolean entireGameEnd = game.playerWinsAframe(wonPlayer.getInGamePlayer());
        drawScoreBoard(game.getGame().getCuingPlayer(), false);
        game.getGame().getRecorder().stopRecording(true);
        
        AchManager.getInstance().showAchievementPopup();

        if (gameValues.isTraining()) {
            boolean success = wonPlayer.getInGamePlayer().getPlayerNumber() == 1;
            String title = success ? strings.getString("challengeSuccess") :
                    strings.getString("challengeFailed");

            final String content;
            if (careerMatch != null) {
                ((ChallengeMatch) careerMatch).setScore(p1.getScore());
                content = ((ChallengeMatch) careerMatch).challengeSet.getName();
                careerMatch.finish(wonPlayer.getPlayerPerson(), success ? 1 : 0, success ? 0 : 1);
            } else {
                content = gameValues.getTrainType().toString();
            }
            Platform.runLater(() ->
            {
                AlertShower.showInfo(stage,
                        content,
                        title);
                AlertShower.askConfirmation(stage,
                        strings.getString("finishAndCloseHint"),
                        strings.getString("finishAndClose"),
                        this::closeWindowAction,
                        null);
            });
        } else {
            int frameNFrom1 = game.getP1Wins() + game.getP2Wins();  // 上面已经更新了

            if (careerMatch != null) {
                if (careerMatch instanceof PlayerVsAiMatch pva) {
                    if (gameValues.rule.snookerLike()) {
                        SnookerChampionship sc = (SnookerChampionship) pva.getChampionship();
                        SnookerPlayer sp1 = (SnookerPlayer) game.getGame().getPlayer1();
                        SnookerPlayer sp2 = (SnookerPlayer) game.getGame().getPlayer2();
                        updateChampionshipBreaks(sc, pva, sp1, frameNFrom1);
                        updateChampionshipBreaks(sc, pva, sp2, frameNFrom1);
                    }
                }
                careerMatch.saveMatch();
            } else {
                game.generalSave();
            }

            Platform.runLater(() -> {
                AlertShower.showInfo(stage,
                        String.format("%s  %d (%d) : (%d) %d  %s",
                                game.getPlayer1().getPlayerPerson().getName(),
                                game.getGame().getPlayer1().getScore(),
                                game.getP1Wins(),
                                game.getP2Wins(),
                                game.getGame().getPlayer2().getScore(),
                                game.getPlayer2().getPlayerPerson().getName()),
                        String.format(strings.getString("winsAFrame"), wonPlayer.getPlayerPerson().getName()),
                        3000);

                if (entireGameEnd) {
                    if (careerMatch != null) {
                        careerMatch.finish(wonPlayer.getPlayerPerson(), game.getP1Wins(), game.getP2Wins());
                    }
                    AchManager.getInstance().updateAfterMatchEnds(game);

                    AlertShower.showInfo(stage,
                            String.format("%s (%d) : (%d) %s",
                                    game.getPlayer1().getPlayerPerson().getName(),
                                    game.getP1Wins(),
                                    game.getP2Wins(),
                                    game.getPlayer2().getPlayerPerson().getName()),
                            String.format(strings.getString("winsAMatch"), wonPlayer.getPlayerPerson().getName()));

                    AlertShower.askConfirmation(stage,
                            strings.getString("finishAndCloseHint"),
                            strings.getString("finishAndClose"),
                            this::closeWindowAction,
                            null);
                } else {
                    //                            if (game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) {
                    //                                ss
                    //                            }
                    AlertShower.askConfirmation(
                            stage,
                            strings.getString("ifStartNextFrameContent"),
                            strings.getString("ifStartNextFrame"),
                            strings.getString("yes"),
                            strings.getString("saveAndExit"),
                            this::startNextFrame,
                            () -> {
                                if (careerMatch != null) {
                                    careerMatch.saveMatch();
                                    careerMatch.saveAndExit();
                                } else {
                                    game.generalSave();
                                }
                                stage.hide();
                            }
                    );
                }
            });
        }
    }

    private GameHolder getActiveHolder() {
        if (replay != null) return replay;
        else return game.getGame();
    }

    private void setupBalls() {
        GameHolder gameHolder = getActiveHolder();
        gamePane.setupBalls(gameHolder, true);
    }

    private void askReposition() {
        Platform.runLater(() ->
                AlertShower.askConfirmation(stage,
                        strings.getString("ifReposition"),
                        strings.getString("oppoFoul"),
                        this::repositionAction,
                        this::notReposition));
    }

    private void notReposition() {
        AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
        asg.notReposition();

        letOtherPlayMenu.setDisable(false);
        autoAimEasiestNextBall(game.getGame().getCuingPlayer());
    }

    private void onCanvasClicked(MouseEvent mouseEvent) {
        if (isDragging) {
            onDragEnd(mouseEvent);
            return;
        }

        if (mouseEvent.getClickCount() == 1) {
            onSingleClick(mouseEvent);
        }
    }

    private double getRatioOfCueAndBall() {
        return game.getGame().getCuingPlayer().getInGamePlayer()
                .getCurrentCue(game.getGame()).getCueTipWidth() /
                gameValues.ball.ballDiameter;
    }

    private double getCuePointRelX(double x) {
        return (x - cueCanvasWH / 2) / cueAreaRadius;
    }

    private double getCuePointRelY(double y) {
        return (y - cueCanvasWH / 2) / cueAreaRadius;
    }

    private double getCuePointCanvasX(double x) {
        return x * cueAreaRadius + cueCanvasWH / 2;
    }

    private double getCuePointCanvasY(double y) {
        return y * cueAreaRadius + cueCanvasWH / 2;
    }

    private void setCuePoint(double x, double y, boolean forceMove) {
        if (Algebra.distanceToPoint(x, y, cueCanvasWH / 2, cueCanvasWH / 2) <
                cueAreaRadius - cueRadius) {
            double ratioCueAndBall = getRatioOfCueAndBall();
            if (obstacleProjection == null
//                    || true
                    || obstacleProjection.cueAble(
                    getCuePointRelX(x), getCuePointRelY(y), ratioCueAndBall)) {
                cuePointX = x;
                cuePointY = y;
                recalculateUiRestrictions();
            } else if (forceMove) {
                // obstacleProjection 一定!= null
                boolean curCueAble = obstacleProjection.cueAble(
                        getCuePointRelX(cuePointX), getCuePointRelY(cuePointY), ratioCueAndBall);
                boolean newCueAble = obstacleProjection.cueAble(
                        getCuePointRelX(x), getCuePointRelY(y), ratioCueAndBall);
                if (newCueAble || !curCueAble) {
                    // 只要不是从可以打的地方调到打不了的地方，都允许
                    cuePointX = x;
                    cuePointY = y;
                    recalculateUiRestrictions();
                }
            }
        }
    }

    private void setCueAnglePoint(double x, double y) {
        double relX = x - cueAngleBaseHor;
        double relY = cueAngleCanvas.getHeight() - cueAngleBaseVer - y;
        double rad = Math.atan2(relY, relX);
        double deg = Math.toDegrees(rad);
        setCueAngleDeg(deg);
    }

    private void setCueAngleDeg(double newDeg) {
        cueAngleDeg = Math.min(MAX_CUE_ANGLE, Math.max(0, newDeg));
        recalculateUiRestrictions();
        setCueAngleLabel();
    }

    private void restoreCueAngle() {
        cueAngleDeg = 5.0;
        recalculateUiRestrictions();
        setCueAngleLabel();
    }

    private void setCueAngleLabel() {
        cueAngleLabel.setText(String.format("%.1f°", cueAngleDeg));
    }

    private void onCuePointCanvasClicked(MouseEvent mouseEvent) {
        if (playingMovement || aiCalculating || cueAnimationPlayer != null) return;

        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            setCuePoint(mouseEvent.getX(), mouseEvent.getY(), false);
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            restoreCuePoint();
        }
    }

    private void onCuePointCanvasDragged(MouseEvent mouseEvent) {
        if (playingMovement || aiCalculating || cueAnimationPlayer != null) return;

        setCuePoint(mouseEvent.getX(), mouseEvent.getY(), false);
    }

    private void onCueAngleCanvasClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            setCueAnglePoint(mouseEvent.getX(), mouseEvent.getY());
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            restoreCueAngle();
        }
    }

    private void onCueAngleCanvasDragged(MouseEvent mouseEvent) {
        setCueAnglePoint(mouseEvent.getX(), mouseEvent.getY());
    }

    private void debugClick(MouseEvent mouseEvent) {
        double realX = gamePane.realX(mouseEvent.getX());
        double realY = gamePane.realY(mouseEvent.getY());
        if (debuggingBall == null) {
            for (Ball ball : game.getGame().getAllBalls()) {
                if (!ball.isPotted()) {
                    if (Algebra.distanceToPoint(realX, realY, ball.getX(), ball.getY()) < gameValues.ball.ballRadius) {
                        debuggingBall = ball;
                        ball.setPotted(true);
                        cursorDrawer.synchronizeGame();
                        break;
                    }
                }
            }
        } else {
            double[] ballRealPos = gamePane.getRealPlaceCanPlaceBall(mouseX, mouseY);
            if (game.getGame().isInTable(ballRealPos[0], ballRealPos[1]) &&
                    !game.getGame().isOccupied(ballRealPos[0], ballRealPos[1])) {
                debuggingBall.setX(ballRealPos[0]);
                debuggingBall.setY(ballRealPos[1]);
                debuggingBall.setPotted(false);
                debuggingBall = null;
                cursorDrawer.synchronizeGame();
            }
        }
    }

    private void onSingleClick(MouseEvent mouseEvent) {
        System.out.println("Clicked!");
        if (replay != null) return;
        if (playingMovement) return;
        if (aiCalculating) return;
        if (cueAnimationPlayer != null) return;
        if (game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() ==
                PlayerType.COMPUTER) {
            if (debugMode) {
                debugClick(mouseEvent);
            }
            return;
        }

        if (debugMode) {
            debugClick(mouseEvent);
        } else if (game.getGame().getCueBall().isPotted()) {
            // 放置手中球
            double[] ballRealPos = gamePane.getRealPlaceCanPlaceBall(mouseEvent.getX(), mouseEvent.getY());

            game.getGame().placeWhiteBall(ballRealPos[0], ballRealPos[1]);
            game.getGame().getRecorder().writeBallInHandPlacement();

            replaceBallInHandMenu.setDisable(false);
            cursorDrawer.synchronizeGame();
        } else if (!game.getGame().isCalculating() && movement == null) {
            Ball whiteBall = game.getGame().getCueBall();
            double[] unit = Algebra.unitVector(
                    new double[]{
                            gamePane.realX(mouseEvent.getX()) - whiteBall.getX(),
                            gamePane.realY(mouseEvent.getY()) - whiteBall.getY()
                    });
            cursorDirectionUnitX = unit[0];
            cursorDirectionUnitY = unit[1];
            recalculateUiRestrictions();
            System.out.println("New direction: " + cursorDirectionUnitX + ", " + cursorDirectionUnitY);
        }
    }

    private void onMouseMoved(MouseEvent mouseEvent) {
        mouseX = mouseEvent.getX();
        mouseY = mouseEvent.getY();
    }

    private void onDragStarted(MouseEvent mouseEvent) {
        if (playingMovement) return;
        if (replay != null) return;
        if (aiCalculating) return;
        if (cueAnimationPlayer != null) return;
        if (game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() ==
                PlayerType.COMPUTER) return;

        Ball white = game.getGame().getCueBall();
        if (white.isPotted()) return;
        isDragging = true;
        double xDiffToWhite = gamePane.realX(mouseEvent.getX()) - white.getX();
        double yDiffToWhite = gamePane.realY(mouseEvent.getY()) - white.getY();
        lastDragAngle = Algebra.thetaOf(new double[]{xDiffToWhite, yDiffToWhite});
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) {
            double[] unitVec = Algebra.unitVector(xDiffToWhite, yDiffToWhite);
            cursorDirectionUnitX = unitVec[0];
            cursorDirectionUnitY = unitVec[1];
            recalculateUiRestrictions();
        }
        stage.getScene().setCursor(Cursor.CLOSED_HAND);
    }

    private void onDragging(MouseEvent mouseEvent) {
        if (!isDragging) {
            return;
        }
        Ball white = game.getGame().getCueBall();
        if (white.isPotted()) return;
        double xDiffToWhite = gamePane.realX(mouseEvent.getX()) - white.getX();
        double yDiffToWhite = gamePane.realY(mouseEvent.getY()) - white.getY();
        double distanceToWhite = Math.hypot(xDiffToWhite, yDiffToWhite);  // 光标离白球越远，移动越慢
        double currentAngle =
                Algebra.thetaOf(new double[]{xDiffToWhite, yDiffToWhite});
        double changedAngle =
                Algebra.normalizeAngle
                        (currentAngle - lastDragAngle) / (distanceToWhite / 500.0);

        double aimingAngle = Algebra.thetaOf(new double[]{cursorDirectionUnitX, cursorDirectionUnitY});
        double resultAngle = aimingAngle + changedAngle;
        double[] newUnitVector = Algebra.unitVectorOfAngle(resultAngle);
        cursorDirectionUnitX = newUnitVector[0];
        cursorDirectionUnitY = newUnitVector[1];
        recalculateUiRestrictions();

        lastDragAngle = currentAngle;
    }

    private void onDragEnd(MouseEvent mouseEvent) {
        isDragging = false;
        lastDragAngle = 0.0;
        stage.getScene().setCursor(Cursor.DEFAULT);
    }

    @FXML
    public void closeWindowAction() {
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @FXML
    void terminateAction() {
        game.getGame().forcedTerminate();
        movement = null;
        playingMovement = false;
        if (cueAnimationPlayer != null) {
            endCueAnimation();
        }
        finishCueNextStep(game.getGame().getCuingPlayer());
//        setButtonsCueEnd(game.getGame().getCuingPlayer());
    }

    @FXML
    void debugModeAction() {
        debugMode = !debugMode;
        if (debugMode) {
            debugModeMenu.setText("normal mode");
        } else {
            debugModeMenu.setText("debug mode");
        }
    }

    @FXML
    void testAction() {
        movement = game.getGame().collisionTest();
        playMovement();
    }

    @FXML
    void tieTestAction() {
        game.getGame().tieTest();
        drawTargetBoard(true);
        drawScoreBoard(game.getGame().getCuingPlayer(), true);
    }

    @FXML
    void clearRedBallsAction() {
        game.getGame().clearRedBallsTest();
        drawTargetBoard(true);
    }

    @FXML
    void p1AddScoreAction() {
        game.getGame().getPlayer1().addScore(10);
    }

    @FXML
    void p2AddScoreAction() {
        game.getGame().getPlayer2().addScore(10);
    }

    @FXML
    void withdrawAction() {
        if (game.getGame() instanceof AbstractSnookerGame) {
            AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
            SnookerPlayer curPlayer = asg.getCuingPlayer();
            int diff = asg.getScoreDiff(curPlayer);
            String behindText = diff <= 0 ? strings.getString("scoreBehind") : strings.getString("scoreAhead");
            AlertShower.askConfirmation(
                    stage,
                    String.format(strings.getString("confirmWithdrawContent"), behindText, Math.abs(diff),
                            asg.getRemainingScore(asg.isDoingSnookerFreeBll())),
                    String.format(strings.getString("confirmWithdraw"), curPlayer.getPlayerPerson().getName()),
                    () -> withdraw(curPlayer),
                    null
            );
        } else {
            Player curPlayer = game.getGame().getCuingPlayer();
            AlertShower.askConfirmation(
                    stage,
                    "......",
                    String.format(strings.getString("confirmWithdraw"), curPlayer.getPlayerPerson().getName()),
                    () -> withdraw(curPlayer),
                    null
            );
        }
    }

    private void withdraw(Player curPlayer) {
        game.getGame().withdraw(curPlayer);
        endFrame();
    }

    @FXML
    void replayFastForwardAction(ActionEvent event) {
        playingMovement = false;
        movement = null;
        cueAnimationPlayer = null;
        finishCueReplay();
//        hideCue();
    }

    @FXML
    void replayNextCueAction(ActionEvent event) {
        replayLoadNext();
    }

    @FXML
    void replayLastCueAction() {
        replay.loadLast();
        playingMovement = false;
        movement = null;
        cueAnimationPlayer = null;

        drawTargetBoard(false);
        drawScoreBoard(null, false);
        restoreCuePoint();
        restoreCueAngle();
        updateScoreDiffLabels();
    }

    @FXML
    void pushOutAction() {
        if (game.getGame() instanceof AmericanNineBallGame g) {
            cursorDirectionUnitX = 0.0;
            cursorDirectionUnitY = 0.0;
            hideCue();
            tableGraphicsChanged = true;

            g.pushOut();

            cursorDrawer.synchronizeGame();

            drawScoreBoard(game.getGame().getCuingPlayer(), true);
            drawTargetBoard(true);
            updatePowerSlider(game.getGame().getCuingPlayer().getPlayerPerson());
            draw();
        } else {
            System.err.println("Game rule no push out!");
        }
    }

    @FXML
    void repositionAction() {
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;
        hideCue();
        tableGraphicsChanged = true;

        game.getGame().reposition();
        repositionMenu.setDisable(true);
        letOtherPlayMenu.setDisable(true);
        
        updateHandSelection(true);
        cursorDrawer.synchronizeGame();
        drawScoreBoard(game.getGame().getCuingPlayer(), true);
        drawTargetBoard(true);
        updatePowerSlider(game.getGame().getCuingPlayer().getPlayerPerson());
        draw();
        if (game.getGame() instanceof AbstractSnookerGame) {
            AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
            if (asg.isNoHitThreeWarning()) {
                showThreeNoHitWarning();
            }
        }
        if (aiAutoPlay &&
                game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) {
//                                Platform.runLater(() -> aiCue(game.getGame().getCuingPlayer()));
            aiCue(game.getGame().getCuingPlayer());
        }
    }

    @FXML
    void letOtherPlayAction() {
        letOtherPlayMenu.setDisable(true);
        repositionMenu.setDisable(true);
        restoreCuePoint();
        restoreCueAngle();
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;

        hideCue();

//        for (CueModel cueModel : CueModel.getAllCueModels()) {
//            cueModel.hide();
//        }
//        game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame()).
        game.getGame().switchPlayer();

        if (game.getGame() instanceof AbstractSnookerGame) {
            AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
            asg.cancelFreeBall();  // 让杆了你还打自由球？
        }

        if (game.getGame().isPlacedHandBallButNoHit()) {
            // 哪有自己摆好球再让对手打的
            game.getGame().setBallInHand();
        }

        Player willPlayPlayer = game.getGame().getCuingPlayer();
        updatePowerSlider(willPlayPlayer.getPlayerPerson());
        setButtonsCueEnd(willPlayPlayer);
        drawScoreBoard(willPlayPlayer, true);
        drawTargetBoard(true);
        updateScoreDiffLabels();

        cursorDrawer.synchronizeGame();

        if (aiAutoPlay && willPlayPlayer.getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) {
            aiCue(willPlayPlayer, false);  // 老子给你让杆，你复位？豁哥哥
        }
    }

    @FXML
    void replaceBallInHandAction() {
        restoreCuePoint();
        restoreCueAngle();
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;
        hideCue();
        game.getGame().setBallInHand();

        cursorDrawer.synchronizeGame();
    }

    @FXML
    void cueAction() {
        if (game.getGame().isEnded() || cueAnimationPlayer != null) return;

        if (replay != null) {
            return;
        }

        suggestedPlayerWhitePath = null;
        tableGraphicsChanged = true;

        replaceBallInHandMenu.setDisable(true);
        letOtherPlayMenu.setDisable(true);
        repositionMenu.setDisable(true);
        pushOutMenu.setDisable(true);

        Player player = game.getGame().getCuingPlayer();
        if (player.getInGamePlayer().getPlayerType() == PlayerType.COMPUTER || aiHelpPlay) {
            setButtonsCueStart();
            aiCue(player);
        } else {
            if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;
            setButtonsCueStart();
            playerCue(player);
        }
    }

    private CuePlayParams applyRandomCueError(Player player) {
        Random random = new Random();
        return applyCueError(player,
                random.nextGaussian(), random.nextGaussian(), random.nextGaussian(), true,
                currentHand);
    }

    /**
     * 三个factor都是指几倍标准差
     */
    private CuePlayParams applyCueError(Player player,
                                        double powerFactor,
                                        double frontBackSpinFactor,
                                        double sideSpinFactor,
                                        boolean mutate,
                                        PlayerPerson.HandSkill handSkill) {
        Cue cue = player.getInGamePlayer().getCurrentCue(game.getGame());
        PlayerPerson playerPerson = player.getPlayerPerson();

        // 用架杆影响打点精确度
        double handSdMul = PlayerPerson.HandBody.getSdOfHand(handSkill);
        frontBackSpinFactor *= handSdMul;
        sideSpinFactor *= handSdMul;
        // power的包含在下面了
        
        CueParams desiredParams = CueParams.createBySelected(
                getSelectedPower(),
                getSelectedFrontBackSpin(),
                getSelectedSideSpin(),
                game.getGame(),
                player.getInGamePlayer(),
                handSkill
        );
//        System.out.println("FB spin: " + desiredParams.selectedFrontBackSpin());

        double maxSelPower = playerPerson.getMaxPowerPercentage();

        final double origSelPower = desiredParams.selectedPower();
        double selPower = origSelPower;
//        double power = desiredParams.actualPower();

        // 因为力量控制导致的力量偏差
        powerFactor = powerFactor * playerPerson.getPowerSd(selPower, handSkill);  // 用力越大误差越大
        powerFactor *= cue.powerMultiplier;  // 发力范围越大的杆控力越粗糙
        if (enablePsy) {
            double psyPowerMul = getPsyControlMultiplier(playerPerson);
            powerFactor /= psyPowerMul;
        }
        double powerMul = 1 + powerFactor;
        double maxDev = 1.5;
        if (powerMul > maxDev) {
            powerMul = maxDev;
        } else if (powerMul < 1 / maxDev) {
            powerMul = 1 / maxDev;
        }

//        power *= powerMul;
        selPower *= powerMul;

        if (selPower > maxSelPower) {
            selPower = maxSelPower;  // 控不了力也不可能打出怪力吧
        }
        if (mutate) System.out.println("Want power: " + origSelPower + ", actual power: " + selPower);

//        if (mutate) {
        intentCuePointX = cuePointX;
        intentCuePointY = cuePointY;
        // 因为出杆质量而导致的打点偏移
//        }

        // todo: 高低杆偏差稍微小点，斯登大点

        double cpx = cuePointX;
        double cpy = cuePointY;

        int counter = 0;
        while (counter < 10) {
            double xError = sideSpinFactor;
            double yError = frontBackSpinFactor;
            double[] muSigXy = playerPerson.getCuePointMuSigmaXY();
            double xSig = muSigXy[1];
            double ySig = -muSigXy[3];

            double mulWithPower = playerPerson.getErrorMultiplierOfPower(origSelPower);

            xError = xError * xSig + muSigXy[0];
            yError = yError * ySig + muSigXy[2];
            xError = xError * mulWithPower * cueAreaRadius / 200;
            yError = yError * mulWithPower * cueAreaRadius / 200;
            cpx = intentCuePointX + xError;
            cpy = intentCuePointY + yError;
            if (mutate) {
                cuePointX = cpx;
                cuePointY = cpy;
            }

            if (obstacleProjection == null || obstacleProjection.cueAble(
                    getCuePointRelX(cpx), getCuePointRelY(cpy),
                    getRatioOfCueAndBall())) {
                break;
            }

            counter++;
        }
        if (mutate && counter == 10) {
            System.out.println("Failed to find a random cueAble position");
            cuePointX = intentCuePointX;
            cuePointY = intentCuePointY;
        }

        if (mutate) {
            System.out.print("intent: " + intentCuePointX + ", " + intentCuePointY + "; ");
            System.out.println("actual: " + cuePointX + ", " + cuePointY);
        }

//        System.out.println(cpx + " " + cuePointX);
//        double unitSideSpin = getUnitSideSpin(cpx);

        boolean slidedCue = false;
        if (mutate) {
            if (isMiscue()) {
//                power /= 4;
//                unitSideSpin *= 10;
                System.out.println("Miscued!");
                AchManager.getInstance().addAchievement(Achievement.MISCUED, game.getGame().getCuingIgp());
                slidedCue = true;
            }
            miscued = slidedCue;
        }

//        double[] unitXYWithSpin = getUnitXYWithSpins(unitSideSpin, power);

        return generateCueParams(selPower, getSelectedFrontBackSpin(cpy), getSelectedSideSpin(cpx), cueAngleDeg, slidedCue);
    }

    private boolean isMiscue() {
        return Algebra.distanceToPoint(cuePointX, cuePointY, cueCanvasWH / 2, cueCanvasWH / 2)
                > cueAreaRadius - cueRadius;
    }

    private CueRecord makeCueRecord(Player cuePlayer, CuePlayParams paramsWithError) {
        return new CueRecord(cuePlayer.getInGamePlayer(),
                game.getGame().isBreaking(),
                paramsWithError.cueParams.selectedPower(),
                paramsWithError.cueParams.actualPower(),
                cursorDirectionUnitX,
                cursorDirectionUnitY,
                getCuePointRelY(intentCuePointY),
                getCuePointRelX(intentCuePointX),
                getCuePointRelY(cuePointY),
                getCuePointRelX(cuePointX),
                cueAngleDeg,
                gamePlayStage(),
                currentHand.hand);
    }

    private TargetRecord makeTargetRecord(Player willCuePlayer) {
        return new TargetRecord(willCuePlayer.getInGamePlayer().getPlayerNumber(),
                game.getGame().getCurrentTarget(),
                game.getGame().isDoingSnookerFreeBll());
    }

    private void disableUiWhenCuing() {
        for (Node control : disableWhenCuing) {
            control.setDisable(true);
        }
    }

    private void enableDisabledUi() {
        for (Node control : disableWhenCuing) {
            control.setDisable(false);
        }
    }

    private void playerCue(Player player) {
        updateBeforeCue();
        if (game.gameValues.rule.snookerLike()) {
            AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
            System.out.println(asg.getCurrentTarget() + " " + predictedTargetBall);
            if (asg.getCurrentTarget() == 0) {
                // 判断斯诺克打彩球时的实际目标球
                GridPane container = new GridPane();
                container.setHgap(10.0);
                container.setVgap(10.0);
                ToggleGroup toggleGroup = new ToggleGroup();
                Map<RadioButton, Integer> buttonVal = new HashMap<>();

                for (int i = 2; i <= 7; i++) {
                    RadioButton rb = new RadioButton(AbstractSnookerGame.ballValueToColorName(i, strings));
                    toggleGroup.getToggles().add(rb);

                    // 是弹出框，只运行一次，不用担心
                    Canvas tarCan = new Canvas();
                    tarCan.setHeight(ballDiameter * 1.2);
                    tarCan.setWidth(ballDiameter * 1.2);
                    tarCan.getGraphicsContext2D().setFill(GLOBAL_BACKGROUND);
                    tarCan.getGraphicsContext2D().fillRect(0, 0, tarCan.getWidth(), tarCan.getHeight());
                    drawSnookerTargetBall(tarCan, i, false);

                    container.add(tarCan, 0, i - 2);
                    container.add(rb, 1, i - 2);

                    buttonVal.put(rb, i);

                    if (i == 7) toggleGroup.selectToggle(rb);
                }

                if (predictedTargetBall == null) {
                    AlertShower.askConfirmation(
                            stage,
                            "",
                            strings.getString("askSnookerTarget"),
                            strings.getString("confirm"),
                            strings.getString("cancel"),
                            () -> {
                                RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
                                asg.setIndicatedTarget(buttonVal.get(selected));
                                playerCueEssential(player);
                                tableGraphicsChanged = true;
                            },
                            null,
                            container
                    );
                    return;
                } else {
                    asg.setIndicatedTarget(predictedTargetBall.getValue());
                }
            }
        }
        playerCueEssential(player);
    }

    private void playerCueEssential(Player player) {
        disableUiWhenCuing();

        // 判断是否为进攻杆
        PlayerPerson.HandSkill usedHand = currentHand;
        PotAttempt currentAttempt = null;
        boolean snookered = game.getGame().isSnookered();

        CuePlayParams params = applyRandomCueError(player);
//        CuePlayParams params = applyCueError(player, 0, 0, 0,true, currentHand);
//        System.out.println("Final params: " + params.cueParams);
        
        if (!snookered && predictedTargetBall != null) {
            List<double[][]> holeDirectionsAndHoles =
                    game.getGame().directionsToAccessibleHoles(predictedTargetBall);
            for (double[][] directionHole : holeDirectionsAndHoles) {
                double pottingDirection = Algebra.thetaOf(directionHole[0]);
                double aimingDirection =
                        Algebra.thetaOf(targetPredictionUnitX, targetPredictionUnitY);

                double angleBtw = Math.abs(pottingDirection - aimingDirection);

                if (angleBtw <= Game.MAX_ATTACK_DECISION_ANGLE) {
                    currentAttempt = new PotAttempt(
                            gameValues,
                            params,
                            game.getGame().getCuingPlayer().getPlayerPerson(),
                            predictedTargetBall,
                            new double[]{game.getGame().getCueBall().getX(),
                                    game.getGame().getCueBall().getY()},
                            new double[]{predictedTargetBall.getX(), predictedTargetBall.getY()},
                            directionHole
                    );
                    System.out.printf("Angle is %f, attacking!\n",
                            Math.toDegrees(Math.abs(pottingDirection - aimingDirection)));
                    break;
                }
            }
        }

        double whiteStartingX = game.getGame().getCueBall().getX();
        double whiteStartingY = game.getGame().getCueBall().getY();

        // 先开始放动画
        beginCueAnimationOfHumanPlayer(whiteStartingX, whiteStartingY);

        final var attempt = currentAttempt;
        Thread thread = new Thread(() ->
                playerCueCalculations(params, player, attempt, usedHand, snookered));
        thread.start();
    }

    private void playerCueCalculations(CuePlayParams params,
                                       Player player,
                                       PotAttempt currentAttempt,
                                       PlayerPerson.HandSkill usedHand,
                                       boolean snookered) {
        gameValues.estimateMoveTime(
                game.playPhy,
                CuePlayParams.getSpeedOfPower(getActualPowerPercentage(), 0),
                2000
        );
        Movement calculatedMovement = game.getGame().cue(params, game.playPhy);
        CueRecord cueRecord = makeCueRecord(player, params);  // 必须在randomCueError之后
        TargetRecord thisTarget = makeTargetRecord(player);
        game.getGame().getRecorder().recordCue(cueRecord, thisTarget);
        game.getGame().getRecorder().recordMovement(calculatedMovement);

        if (currentAttempt != null) {
            boolean success = currentAttempt.getTargetBall().isPotted() && !game.getGame().isThisCueFoul();
            if (curDefAttempt != null && curDefAttempt.defensePlayer != player) {
                // 如进攻成功，则上一杆防守失败了
                curDefAttempt.setSuccess(!success);
                if (success) {
                    System.out.println(curDefAttempt.defensePlayer.getPlayerPerson().getName() +
                            " defense failed!");
                }
                game.getGame().recordAttemptForAchievement(curDefAttempt, player);
            }
            if (lastPotAttempt != null && lastPotAttempt.getPlayerPerson() == player.getPlayerPerson()) {
                // 如上一杆也是进攻，则这一杆进不进就是上一杆走位成不成功
                lastPotAttempt.setPositionSuccess(success);
            }
            currentAttempt.setHandSkill(usedHand);
            currentAttempt.setSuccess(success);
            player.addAttempt(currentAttempt);
            if (success) {
                System.out.println("Pot success!");
            } else {
                System.out.println("Pot failed!");
            }
            lastPotAttempt = currentAttempt;
            curDefAttempt = null;
            game.getGame().recordAttemptForAchievement(lastPotAttempt, player);
        } else {
            // 防守
            if (lastPotAttempt != null && lastPotAttempt.getPlayerPerson() == player.getPlayerPerson()) {
                // 如上一杆是本人进攻，则走位失败
                lastPotAttempt.setPositionSuccess(false);
            }

            curDefAttempt = new DefenseAttempt(player, snookered);
            player.addAttempt(curDefAttempt);
            System.out.println("Defense!" + (snookered ? " Solving" : ""));
            lastPotAttempt = null;
            game.getGame().recordAttemptForAchievement(null, player);
        }

        // 放到这里来更新是为了避免上面这一堆运算的时间导致潜在bug
        // 说白了，为了线程安全
        movement = calculatedMovement;
    }

    private void aiCueCalculations(CuePlayParams realParams, 
                                   Player player,
                                   AiCueResult cueResult,
                                   double whiteStartingX,
                                   double whiteStartingY) {
        Movement calculatedMovement = game.getGame().cue(realParams, game.playPhy);

        CueRecord cueRecord = makeCueRecord(player, realParams);  // 必须在randomCueError之后
        TargetRecord thisTarget = makeTargetRecord(player);
        game.getGame().getRecorder().recordCue(cueRecord, thisTarget);
        game.getGame().getRecorder().recordMovement(calculatedMovement);

        if (cueResult.isAttack()) {
            PotAttempt currentAttempt = new PotAttempt(
                    gameValues,
                    realParams,
                    game.getGame().getCuingPlayer().getPlayerPerson(),
                    cueResult.getTargetBall(),
                    new double[]{whiteStartingX, whiteStartingY},
                    cueResult.getTargetOrigPos(),
                    cueResult.getTargetDirHole()
            );
            boolean success = currentAttempt.getTargetBall().isPotted();
//                     && !game.getGame().isLastCueFoul() todo: 想办法
            if (curDefAttempt != null && curDefAttempt.defensePlayer != player) {
                // 如进攻成功，则上一杆防守失败了
                curDefAttempt.setSuccess(!success);
                if (success) {
                    System.out.println(curDefAttempt.defensePlayer.getPlayerPerson().getName() +
                            " player defense failed!");
                }
            }
            if (lastPotAttempt != null && lastPotAttempt.getPlayerPerson() == player.getPlayerPerson()) {
                // 如上一杆也是进攻，则这一杆进不进就是上一杆走位成不成功
                lastPotAttempt.setPositionSuccess(success);
            }
            currentAttempt.setHandSkill(cueResult.getHandSkill());
            currentAttempt.setSuccess(success);
            player.addAttempt(currentAttempt);
            if (success) {
                System.out.println("AI Pot success!");
            } else {
                System.out.println("AI Pot failed!");
            }
            lastPotAttempt = currentAttempt;
            curDefAttempt = null;
        } else {
            curDefAttempt = new DefenseAttempt(player,
                    cueResult.getCueType() == AiCueResult.CueType.SOLVE);

            player.addAttempt(curDefAttempt);
            System.out.println("AI Defense!" + (curDefAttempt.isSolvingSnooker() ? " Solving" : ""));

            if (lastPotAttempt != null && lastPotAttempt.getPlayerPerson() == player.getPlayerPerson()) {
                // 如上一杆是本人进攻，则走位失败
                lastPotAttempt.setPositionSuccess(false);
            }
            lastPotAttempt = null;
        }

        movement = calculatedMovement;
    }

    private void updateBeforeCue() {
        Toggle sel1 = player1SpeedToggle.getSelectedToggle();
        if (sel1 != null) {
            double newSpeed = Double.parseDouble(sel1.getUserData().toString());

            if (newSpeed != p1PlaySpeed) {
                p1PlaySpeed = newSpeed;
                replayGap = (long) (DEFAULT_REPLAY_GAP / p1PlaySpeed);
                System.out.println("New speed 1 " + p1PlaySpeed);
            }
        }
        Toggle sel2 = player2SpeedToggle.getSelectedToggle();
        if (sel2 != null) {
            double newSpeed = Double.parseDouble(sel2.getUserData().toString());
            if (newSpeed != p2PlaySpeed) {
                p2PlaySpeed = newSpeed;
                replayGap = (long) (DEFAULT_REPLAY_GAP / p2PlaySpeed);
                System.out.println("New speed 2 " + p2PlaySpeed);
            }
        }
    }

    private void aiCue(Player player) {
        aiCue(player, true);
    }

    private void aiCue(Player player, boolean aiHasRightToReposition) {
        boolean aiHelpPlayerPlaying = player.getInGamePlayer().isHuman() && aiHelpPlay;
        if (aiHelpPlayerPlaying) {
            if (careerMatch != null) {
                AiCueResult.setAiPrecisionFactor(CareerManager.getInstance().getPlayerGoodness() * 
                        Career.AI_HELPER_PRECISION_FACTOR);  // 暗削自动击球
            } else {
                AiCueResult.setAiPrecisionFactor(ConfigLoader.getInstance().getDouble("fastGameAiming", 1.0));
            }
        } else {
            if (careerMatch != null) {
                AiCueResult.setAiPrecisionFactor(CareerManager.getInstance().getAiGoodness());
            } else {
                AiCueResult.setAiPrecisionFactor(ConfigLoader.getInstance().getDouble("fastGameAiStrength", 1.0));
            }
        }
        
        updateBeforeCue();
        disableUiWhenCuing();
        Ball.disableGearOffset();  // AI真不会这个，禁用了。在finishCueNextStep里重新启用
        cueButton.setText(strings.getString("aiThinking"));
//        cueButton.setDisable(true);
        aiCalculating = true;
        Thread aiCalculation = new Thread(() -> {
            System.out.println("ai cue");
            long st = System.currentTimeMillis();
            if (game.getGame().isBallInHand()) {
                System.out.println("AI is trying to place ball");
                double[] pos = AiCueBallPlacer.createAiCueBallPlacer(game.getGame(), player)
                        .getPositionToPlaceCueBall();
                if (pos == null) {
                    // Ai不知道摆哪了，认输
                    aiCalculating = false;
                    withdraw(player);
                    return;
                }
                System.out.println("AI placed cue ball at " + Arrays.toString(pos));

                game.getGame().placeWhiteBall(pos[0], pos[1]);
                game.getGame().getRecorder().writeBallInHandPlacement();
                Platform.runLater(this::draw);
            }
            if (gameValues.rule.snookerLike()) {
                AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
                if (aiHasRightToReposition && asg.canReposition()) {
                    if (asg.aiConsiderReposition(game.predictPhy, lastPotAttempt)) {
                        Platform.runLater(() -> {
                            AlertShower.showInfo(
                                    stage,
                                    strings.getString("aiAskReposition"),
                                    strings.getString("reposition"),
                                    3000
                            );
                            cueButton.setText(strings.getString("cueText"));
                            aiCalculating = false;
                            asg.reposition();
                            drawScoreBoard(game.getGame().getCuingPlayer(), true);
                            drawTargetBoard(true);
                            draw();
                            endCueAnimation();
                            finishCueNextStep(game.getGame().getCuingPlayer());

                            if (asg.isNoHitThreeWarning()) {
                                showThreeNoHitWarning();
                            }
                        });
                        return;
                    } else {
                        asg.notReposition();
                    }
                }
            }

            AiCueResult cueResult = game.getGame().aiCue(player, game.predictPhy);
            System.out.println("Ai calculation ends in " + (System.currentTimeMillis() - st) + " ms");
            if (cueResult == null) {
                aiCalculating = false;
                withdraw(player);
                return;
            }
            aiWhitePath = cueResult.getWhitePath();  // todo
            tableGraphicsChanged = true;
            if (game.gameValues.rule.snookerLike()) {
                AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
                if (cueResult.getTargetBall() != null) {
                    asg.setIndicatedTarget(cueResult.getTargetBall().getValue());
                } else {
                    // ai在乱打
                    System.out.println("AI angry cues");
                    asg.setIndicatedTarget(2);
                }
            }

            Platform.runLater(() -> {
                cueButton.setText(strings.getString("isCuing"));
                cursorDirectionUnitX = cueResult.getUnitX();
                cursorDirectionUnitY = cueResult.getUnitY();
                System.out.printf("Ai direction: %f, %f\n", cursorDirectionUnitX, cursorDirectionUnitY);
                currentHand = cueResult.getHandSkill();
                updateHandSelectionToggleByData(currentHand.hand);
                powerSlider.setValue(cueResult.getCueParams().selectedPower());
                cuePointX = cueCanvasWH / 2 + cueResult.getCueParams().selectedSideSpin() * cueAreaRadius;
                cuePointY = cueCanvasWH / 2 - cueResult.getCueParams().selectedFrontBackSpin() * cueAreaRadius;
                cueAngleDeg = 0.0;

                CuePlayParams realParams = applyRandomCueError(player);

                double whiteStartingX = game.getGame().getCueBall().getX();
                double whiteStartingY = game.getGame().getCueBall().getY();

                aiCalculating = false;

                beginCueAnimation(game.getGame().getCuingPlayer().getInGamePlayer(),
                        whiteStartingX, whiteStartingY, cueResult.getCueParams().selectedPower(),
                        cueResult.getUnitX(), cueResult.getUnitY());

                Thread thread = new Thread(() -> aiCueCalculations(
                        realParams,
                        player,
                        cueResult,
                        whiteStartingX,
                        whiteStartingY
                ));
                thread.start();
            });
        });
        aiCalculation.setDaemon(true);
        aiCalculation.start();
    }

    private void replayCue() {
        updateBeforeCue();
        if (replay.getCurrentFlag() == ActualRecorder.FLAG_CUE) {
            CueRecord cueRecord = replay.getCueRecord();
            if (cueRecord == null) return;

            replayNextCueButton.setText(strings.getString("replayFastForward"));
            replayNextCueButton.setOnAction(this::replayFastForwardAction);
            replayLastCueButton.setDisable(true);

            movement = replay.getMovement();
            System.out.println(movement.getMovementMap().get(replay.getCueBall()).size());

            cursorDirectionUnitX = cueRecord.aimUnitX;
            cursorDirectionUnitY = cueRecord.aimUnitY;

            intentCuePointX = getCuePointCanvasX(cueRecord.intendedHorPoint);
            intentCuePointY = getCuePointCanvasY(cueRecord.intendedVerPoint);
            cuePointX = getCuePointCanvasX(cueRecord.actualHorPoint);
            cuePointY = getCuePointCanvasY(cueRecord.actualVerPoint);
            cueAngleDeg = cueRecord.cueAngle;
            currentHand = cueRecord.cuePlayer.getPlayerPerson().handBody.getHandSkillByHand(cueRecord.hand);

            miscued = isMiscue();

            powerSlider.setValue(cueRecord.actualPower);
            Platform.runLater(() -> updatePowerSlider(cueRecord.cuePlayer.getPlayerPerson()));

            Ball cueBall = replay.getCueBall();
            MovementFrame cueBallPos = movement.getStartingPositions().get(cueBall);
            beginCueAnimation(cueRecord.cuePlayer, cueBallPos.x, cueBallPos.y,
                    cueRecord.selectedPower, cueRecord.aimUnitX, cueRecord.aimUnitY);
        } else if (replay.getCurrentFlag() == ActualRecorder.FLAG_HANDBALL) {
            System.out.println("Ball in hand!");
//            drawScoreBoard(null);
            drawTargetBoard(true);
            restoreCuePoint();
            restoreCueAngle();
            updateScoreDiffLabels();
        }
    }

    private void showThreeNoHitWarning() {
        AlertShower.showInfo(
                stage,
                strings.getString("snookerThreeWarning"),
                strings.getString("warning")
        );
    }

    private CuePlayParams generateCueParams() {
        return generateCueParams(getSelectedPower());
    }

    private CuePlayParams[] generateCueParamsSd1(int nPoints) {
        Player player = game.getGame().getCuingPlayer();
        double sd = 1;
        double corner = Math.sqrt(2) / 2 * sd;
        CuePlayParams[] res = new CuePlayParams[nPoints + 1];
        res[0] = generateCueParams();
        if (nPoints == 0) return res;

        if (nPoints == 8) {
            res[1] = applyCueError(player, -sd, sd, 0, false, currentHand);  // 又小又低
            res[2] = applyCueError(player, -corner, corner, -corner, false, currentHand);  // 偏小，左下
            res[3] = applyCueError(player, 0, 0, -sd, false, currentHand);  // 最左
            res[4] = applyCueError(player, corner, -corner, -corner, false, currentHand);  // 偏大，左上
            res[5] = applyCueError(player, sd, -sd, 0, false, currentHand);  // 又大又高
            res[6] = applyCueError(player, corner, -corner, corner, false, currentHand);  // 偏大，右上
            res[7] = applyCueError(player, 0, 0, sd, false, currentHand);  // 最右
            res[8] = applyCueError(player, -corner, corner, corner, false, currentHand);  // 偏小，右下
        } else if (nPoints == 4) {
            // 这里就没用corner了，因为我们宁愿画大点去吓玩家
            res[1] = applyCueError(player, -sd, sd, -sd, false, currentHand);  // 小，左下
            res[2] = applyCueError(player, sd, -sd, -sd, false, currentHand);  // 大，左上
            res[3] = applyCueError(player, sd, -sd, sd, false, currentHand);  // 大，右上
            res[4] = applyCueError(player, -sd, sd, sd, false, currentHand);  // 小，右下
        } else if (nPoints == 2) {
            // 这里就没用corner了，因为我们宁愿画大点去吓玩家
            res[1] = applyCueError(player, sd, -sd, -sd, false, currentHand);  // 大，左上
            res[2] = applyCueError(player, -sd, sd, sd, false, currentHand);  // 小，右下
        } else {
            throw new RuntimeException(nPoints + " points white prediction not supported");
        }
        return res;
    }

    private CuePlayParams generateCueParams(double selectedPower) {
        return generateCueParams(selectedPower, getSelectedSideSpin(), cueAngleDeg);
    }

    private CuePlayParams generateCueParams(double selectedPower, double selectedSideSpin,
                                            double cueAngleDeg) {
        return generateCueParams(selectedPower, getSelectedFrontBackSpin(), selectedSideSpin, cueAngleDeg, false);
    }

    private CuePlayParams generateCueParams(double selectedPower,
                                            double selectedFrontBackSpin,
                                            double selectedSideSpin,
                                            double cueAngleDeg,
                                            boolean slideCue) {
        CueParams cueParams = CueParams.createBySelected(
                selectedPower,
                selectedFrontBackSpin,
                selectedSideSpin,
                game.getGame(),
                game.getGame().getCuingIgp(),
                currentHand
        );
        return CuePlayParams.makeIdealParams(
                cursorDirectionUnitX, 
                cursorDirectionUnitY,
                cueParams,
                cueAngleDeg, 
                slideCue);
    }

    private void setButtonsCueStart() {
        withdrawMenu.setDisable(true);
        cueButton.setDisable(true);
    }

    private void setButtonsCueEnd(Player nextCuePlayer) {
        cueButton.setDisable(false);

        if (nextCuePlayer.getInGamePlayer().getPlayerType() == PlayerType.PLAYER) {
            cueButton.setText(strings.getString("cueText"));
            withdrawMenu.setDisable(false);
        } else {
            cueButton.setText(strings.getString("aiCueText"));
            withdrawMenu.setDisable(true);
        }
    }

//    private double getActualFrontBackSpin() {
//        return getActualFrontBackSpin(cuePointY);
//    }

//    private double getActualFrontBackSpin(double cpy) {
//        Cue cue;
//        if (replay != null) {
//            cue = replay.getCurrentCue();
//        } else {
//            cue = game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame());
//        }
//        return CuePlayParams.unitFrontBackSpin((cueCanvasWH / 2 - cpy) / cueAreaRadius,
//                game.getGame().getCuingPlayer().getPlayerPerson(),
//                cue
//        );
//    }

//    private double getUnitSideSpin() {
//        return getUnitSideSpin(cuePointX);
//    }
//
//    private double getUnitSideSpin(double cpx) {
//        Cue cue;
//        if (replay != null) {
//            cue = replay.getCurrentCue();
//        } else {
//            cue = game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame());
//        }
//        return CuePlayParams.unitSideSpin((cpx - cueCanvasWH / 2) / cueAreaRadius,
//                cue);
//    }

    /**
     * 返回受到侧塞影响的白球单位向量
     */
    private double[] getUnitXYWithSpins(double unitSideSpin, double actualPower) {
        return CuePlayParams.unitXYWithSpins(unitSideSpin, actualPower,
                cursorDirectionUnitX, cursorDirectionUnitY);
    }

    /**
     * @return 力量槽选的力量
     */
    private double getSelectedPower() {
        return Math.max(powerSlider.getValue(), 0.01);
    }
    
    private double getSelectedSideSpin() {
        return getSelectedSideSpin(cuePointX);
    }

    private double getSelectedFrontBackSpin() {
        return getSelectedFrontBackSpin(cuePointY);
    }
    
    private double getSelectedSideSpin(double cpx) {
        return (cpx - cueCanvasWH / 2) / cueAreaRadius;
    }
    
    private double getSelectedFrontBackSpin(double cpy) {
        return (cueCanvasWH / 2 - cpy) / cueAreaRadius;
    }

    @Deprecated
    private double getActualPowerPercentage() {
        return getActualPowerPercentage(getSelectedPower(), getSelectedSideSpin(), getSelectedFrontBackSpin());
    }

    @Deprecated
    private double getActualPowerPercentage(double selectedPower,
                                            double selectedSideSpin,
                                            double selectedFrontBackSpin) {
        CueParams cueParams = CueParams.createBySelected(
                selectedPower,
                selectedFrontBackSpin,
                selectedSideSpin,
                game.getGame(),
                game.getGame().getCuingIgp(),
                currentHand
        );
        return cueParams.actualPower();
    }

    private void restoreCuePoint() {
        cuePointX = cueCanvasWH / 2;
        cuePointY = cueCanvasWH / 2;
        intentCuePointX = -1;
        intentCuePointY = -1;

        if (game != null && game.getGame() != null) recalculateUiRestrictions();
    }

    private void startAnimation() {
//        frameAnimation = new AnimationFrame(this::oneFrame, uiFrameTimeMs);
        gameLoop = new GameLoop(this::oneFrame, fpsLabel);
        gameLoop.start();
    }

    private void oneFrame() {
        if (aiCalculating) return;

//        long t0 = System.nanoTime();
        draw();
//        long t1 = System.nanoTime();
        drawCueBallCanvas();
//        long t2 = System.nanoTime();
        drawCueAngleCanvas();
//        long t3 = System.nanoTime();
        drawCue();
//        long t4 = System.nanoTime();
//        fpsLabel.setText(String.valueOf(frameAnimation.getCurrentFps()));
//        fpsLabel.setText(gameLoop.getCurrentFps() + " -- " + gameLoop.getFpsSpike());
//        System.out.printf("%d %d %d %d\n", t1 - t0, t2 - t1, t3 - t2, t4 - t3);
    }

    private void setupPowerSlider() {
        powerSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (game != null) {
                double playerMaxPower = game.getGame().getCuingPlayer().getPlayerPerson().getMaxPowerPercentage();
                if (newValue.doubleValue() > playerMaxPower) {
                    powerSlider.setValue(playerMaxPower);
                    return;
                }
                createPathPrediction();
            }
            powerLabel.setText(String.format("%.1f", newValue.doubleValue()));
        }));
        powerSlider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double aDouble) {
                return String.format("%.1f", aDouble);
            }

            @Override
            public Double fromString(String s) {
                return Double.parseDouble(s);
            }
        });

        powerSlider.setValue(DEFAULT_POWER);
    }

    private void addListeners() {
        gamePane.getGameCanvas().setOnMouseClicked(this::onCanvasClicked);
        gamePane.getGameCanvas().setOnDragDetected(this::onDragStarted);
        gamePane.getGameCanvas().setOnMouseDragged(this::onDragging);
        gamePane.getGameCanvas().setOnMouseMoved(this::onMouseMoved);

        cuePointCanvas.setOnMouseClicked(this::onCuePointCanvasClicked);
        cuePointCanvas.setOnMouseDragged(this::onCuePointCanvasDragged);

        cueAngleCanvas.setOnMouseClicked(this::onCueAngleCanvasClicked);
        cueAngleCanvas.setOnMouseDragged(this::onCueAngleCanvasDragged);
    }

    private void drawBallInHand() {
        if (replay != null) return;
        if (game.getGame().isCalculating() || movement != null) return;

        if (debugMode && debuggingBall != null) {
            drawBallInHandEssential(debuggingBall);
        }

        if (game.getGame().isBallInHand()
                && game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.PLAYER) {
            drawBallInHandEssential(game.getGame().getCueBall());
        }
    }

    private void drawBallInHandEssential(Ball ball) {
        gamePane.drawBallInHandEssential(ball, game.getGame().getTable(), mouseX, mouseY);
    }

    private void drawBalls() {
//        System.out.println(playingMovement + " " + movement);
        if (playingMovement) {
            // 处理倍速
            long msSinceAnimationBegun = gameLoop.msSinceAnimationBegun();

            double appliedSpeed = getCurPlaySpeedMultiplier();
            int index = (int) (msSinceAnimationBegun * appliedSpeed / frameTimeMs);
//            if (replay == null) {
//                Player player = game.getGame().getCuingPlayer();
//                if (player.getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) {
//                    appliedSpeed = p1PlaySpeed;
//                    index = (int) (msSinceAnimationBegun * p1PlaySpeed / frameTimeMs);  // 天生一个floor
//                } else {
//                    appliedSpeed = 1.0;
//                    index = (int) (msSinceAnimationBegun / frameTimeMs);
//                }
//            } else {
//                appliedSpeed = p1PlaySpeed;
//                index = (int) (msSinceAnimationBegun * p1PlaySpeed / frameTimeMs);
//            }

            boolean isLast = false;
            int movementSize = movement.getNFrames();
            if (index >= movementSize) {
                index = movementSize - 1;
                isLast = true;
            }
            if (movement.isCongested()) {
                index = movementSize - 1;
                isLast = true;
                Platform.runLater(() -> AlertShower.showInfo(
                        stage,
                        strings.getString("physicalCongestion"),
                        strings.getString("bugged")
                ));
            }

            double uiFrameSinceThisAniFrame = (msSinceAnimationBegun - (index * frameTimeMs / appliedSpeed));
            double rate = uiFrameSinceThisAniFrame / frameTimeMs * appliedSpeed;
            double frameRateRatio = gameLoop.lastAnimationFrameMs() / frameTimeMs * appliedSpeed;
//            System.out.printf("%d %f %f %f\n", index, uiFrameSinceThisAniFrame, rate, frameRateRatio);

            GameHolder holder = getActiveHolder();
            for (Map.Entry<Ball, List<MovementFrame>> entry :
                    movement.getMovementMap().entrySet()) {
                List<MovementFrame> list = entry.getValue();
                MovementFrame frame = list.get(index);

                if (!frame.potted) {
                    MovementFrame nextFrame = null;
                    if (index + 1 < list.size()) {
                        nextFrame = list.get(index + 1);
                    }
                    double x, y;
                    if (nextFrame == null || nextFrame.potted) {
                        x = frame.x;
                        y = frame.y;
                    } else {
                        x = Algebra.rateBetween(frame.x, nextFrame.x, rate);
                        y = Algebra.rateBetween(frame.y, nextFrame.y, rate);
                    }
                    double frameDeg = frame.frameDegChange * frameRateRatio;

                    entry.getKey().model.sphere.setVisible(true);
                    holder.getTable().forceDrawBall(
                            gamePane,
                            entry.getKey(),
                            x, y,
                            frame.xAxis, frame.yAxis, frame.zAxis,
                            frameDeg);
                } else {
                    entry.getKey().model.sphere.setVisible(false);
                }

                switch (frame.movementType) {
                    case MovementFrame.COLLISION:
                        break;
                    case MovementFrame.CUSHION:
                        GameAudio.hitCushion(gameValues.table, frame.movementValue);
                        break;
                    case MovementFrame.POT:
                        GameAudio.pot(gameValues.table, frame.movementValue);
                        break;
                }
            }
            if (isLast) {
                playingMovement = false;
                movement = null;
                if (replay != null) finishCueReplay();
                else {
                    game.getGame().finishMove(this);  // 这本不应该在UI线程，但是懒得改
                }
            }
        } else {
            if (movement == null) {
//                if (tableGraphicsChanged) {  // 有些bug比较难修，算了
                if (replay != null) {
                    gamePane.drawStoppedBalls(replay.getTable(), replay.getAllBalls(), replay.getCurrentPositions());
                } else {
                    if (cueAnimationPlayer != null) {
                        // 正在进行物理运算，运杆的动画在放
                    } else {
                        gamePane.drawStoppedBalls(game.getGame().getTable(), game.getGame().getAllBalls(), null);
                    }
                }
//                }
                if (replay != null && !replay.finished() &&
                        System.currentTimeMillis() - replayStopTime > replayGap &&
                        replayAutoPlayBox.isSelected()) {
                    System.out.println("replay auto next cue");
                    replayNextCueAction(null);
                }
            } else {
                // 已经算出，但还在放运杆动画
                for (Map.Entry<Ball, MovementFrame> entry : movement.getStartingPositions().entrySet()) {
                    MovementFrame frame = entry.getValue();
                    if (!frame.potted) {
                        entry.getKey().model.sphere.setVisible(true);
                        getActiveHolder().getTable().forceDrawBall(
                                gamePane,
                                entry.getKey(),
                                frame.x, frame.y,
                                frame.xAxis, frame.yAxis, frame.zAxis,
                                frame.frameDegChange * getCurPlaySpeedMultiplier());
                    } else {
                        entry.getKey().model.sphere.setVisible(false);
                    }
                }
            }
        }
    }

    private void drawScoreBoard(Player cuePlayer, boolean showNextCue) {
        // TODO
        if (replay != null) {
            Platform.runLater(() -> {
                player1FramesLabel.setText(String.valueOf(replay.getItem().p1Wins));
                player2FramesLabel.setText(String.valueOf(replay.getItem().p2Wins));

                wipeCanvas(singlePoleCanvas);

                if (gameValues.rule.snookerLike()) {
                    SnookerScoreResult ssr = (SnookerScoreResult) replay.getScoreResult();
                    player1ScoreLabel.setText(String.valueOf(ssr.getP1TotalScore()));
                    player2ScoreLabel.setText(String.valueOf(ssr.getP2TotalScore()));
                    drawSnookerSinglePoles(ssr.getSinglePoleMap());
                    singlePoleLabel.setText(String.valueOf(ssr.getSinglePoleScore()));
                } else if (gameValues.rule == GameRule.CHINESE_EIGHT || gameValues.rule == GameRule.LIS_EIGHT) {
                    ChineseEightScoreResult csr = (ChineseEightScoreResult) replay.getScoreResult();
//                    List<PoolBall> rems = ChineseEightTable.filterRemainingTargetOfPlayer(replay.getCueRecord().targetRep, replay);
                    List<PoolBall> rems = replay.getCueRecord().cuePlayer.getPlayerNumber() == 1 ?
                            csr.getP1Rems() : csr.getP2Rems();
                    System.out.println(rems.size());
                    drawChineseEightAllTargets(rems);
                } else if (gameValues.rule == GameRule.AMERICAN_NINE) {
                    NineBallScoreResult nsr = (NineBallScoreResult) replay.getScoreResult();
                    Map<PoolBall, Boolean> rems = nsr.getRemBalls();
                    drawPoolBallAllTargets(rems);
                }
            });
        } else {
            Platform.runLater(() -> {
                player1ScoreLabel.setText(String.valueOf(game.getGame().getPlayer1().getScore()));
                player2ScoreLabel.setText(String.valueOf(game.getGame().getPlayer2().getScore()));
                player1FramesLabel.setText(String.valueOf(game.getP1Wins()));
                player2FramesLabel.setText(String.valueOf(game.getP2Wins()));

                wipeCanvas(singlePoleCanvas);

                if (gameValues.rule.snookerLike()) {
                    AbstractSnookerGame asg = (AbstractSnookerGame) game.getGame();
                    drawSnookerSinglePoles(cuePlayer.getSinglePole());

                    int singlePoleScore = cuePlayer.getSinglePoleScore();

                    String singlePoleText;
                    if (singlePoleScore < 3) {  // 至少一红一彩再显吧？
                        singlePoleText = String.valueOf(singlePoleScore);
                    } else {
                        singlePoleText = singlePoleScore +
                                String.format(" (%s)",
                                        String.format(strings.getString("possibleBreak"),
                                                asg.getPossibleBreak(singlePoleScore)));
                    }

                    singlePoleLabel.setText(singlePoleText);
                } else if (gameValues.rule.poolLike()) {
                    if (cuePlayer == game.getGame().getCuingPlayer()) {
                        // 进攻成功了
                        drawNumberedAllTargets((NumberedBallGame<?>) game.getGame(),
                                (NumberedBallPlayer) cuePlayer);
                    } else {
                        // 进攻失败了
                        drawNumberedAllTargets((NumberedBallGame<?>) game.getGame(),
                                (NumberedBallPlayer) game.getGame().getCuingPlayer());
                    }
                }
            });
        }
    }

    private void drawTargetBoard(boolean showNextTarget) {
        Platform.runLater(() -> {
            if (gameValues.rule.snookerLike())
                drawSnookerTargetBoard(showNextTarget);
            else if (gameValues.rule.poolLike())
                drawPoolTargetBoard(showNextTarget);
        });
    }

    private void drawSnookerTargetBoard(boolean showNextCue) {
        int tar;
        boolean p1;
        boolean snookerFreeBall;
        if (replay != null) {
            CueRecord cueRecord = replay.getCueRecord();
            TargetRecord target = showNextCue ? replay.getNextTarget() : replay.getThisTarget();
            if (cueRecord == null || target == null) return;
            p1 = target.playerNum == 1;
            tar = target.targetRep;
            snookerFreeBall = target.isSnookerFreeBall;
            System.out.println("Target: " + tar + ", player: " + target.playerNum);
        } else {
            AbstractSnookerGame game1 = (AbstractSnookerGame) game.getGame();
            p1 = game1.getCuingPlayer().getInGamePlayer().getPlayerNumber() == 1;
            tar = game1.getCurrentTarget();
            snookerFreeBall = game1.isDoingFreeBall();
        }
        if (p1) {
            drawSnookerTargetBall(player1TarCanvas, tar, snookerFreeBall);
            wipeCanvas(player2TarCanvas);
        } else {
            drawSnookerTargetBall(player2TarCanvas, tar, snookerFreeBall);
            wipeCanvas(player1TarCanvas);
        }
    }

    private void drawPoolTargetBoard(boolean showNextCue) {
        int tar;
        boolean p1;
        if (replay != null) {
            CueRecord cueRecord = replay.getCueRecord();
            TargetRecord target = showNextCue ? replay.getNextTarget() : replay.getThisTarget();
            if (cueRecord == null || target == null) return;
            p1 = target.playerNum == 1;
            tar = target.targetRep;
        } else {
            NumberedBallGame<?> game1 = (NumberedBallGame<?>) game.getGame();
            p1 = game1.getCuingPlayer().getInGamePlayer().getPlayerNumber() == 1;
            tar = game1.getCurrentTarget();
        }

        if (p1) {
            drawPoolTargetBall(player1TarCanvas, tar);
            wipeCanvas(player2TarCanvas);
        } else {
            drawPoolTargetBall(player2TarCanvas, tar);
            wipeCanvas(player1TarCanvas);
        }
    }

    private void drawChineseEightAllTargets(List<PoolBall> targets) {
        double x = ballDiameter * 0.6;
        double y = ballDiameter * 0.6;

        for (PoolBall ball : targets) {
            NumberedBallTable.drawPoolBallEssential(
                    x, y, ballDiameter, ball.getColor(), ball.getValue(),
                    singlePoleCanvas.getGraphicsContext2D());
            x += ballDiameter * 1.2;
        }
    }

    private void drawPoolBallAllTargets(Map<PoolBall, Boolean> balls) {
        double x = ballDiameter * 0.6;
        double y = ballDiameter * 0.6;

        for (Map.Entry<PoolBall, Boolean> ballPot : balls.entrySet()) {
            PoolBall ball = ballPot.getKey();
            boolean greyOut = ballPot.getValue();
            NumberedBallTable.drawPoolBallEssential(
                    x, y, ballDiameter, ball.getColor(), ball.getValue(),
                    singlePoleCanvas.getGraphicsContext2D(),
                    greyOut);
            x += ballDiameter * 1.2;
        }
    }

    private void drawNumberedAllTargets(NumberedBallGame<?> frame, NumberedBallPlayer player) {
        // 别想了，不会每一帧都画一遍，只有
        if (frame instanceof ChineseEightBallGame) {  // 李式八球也instanceof中八
            int ballRange = ((ChineseEightBallPlayer) player).getBallRange();
            List<PoolBall> targets = ChineseEightTable.filterRemainingTargetOfPlayer(
                    ballRange, frame
            );
            drawChineseEightAllTargets(targets);
        } else if (frame instanceof AmericanNineBallGame) {
            Map<PoolBall, Boolean> balls = ((AmericanNineBallGame) frame).getBalls();
            drawPoolBallAllTargets(balls);
        }
    }

    private void drawSnookerSinglePoles(SortedMap<Ball, Integer> singlePoleBalls) {
        GraphicsContext gc = singlePoleCanvas.getGraphicsContext2D();
        double x = 0;
        double y = ballDiameter * 0.1;
        double textY = ballDiameter * 0.8;
        for (Map.Entry<Ball, Integer> ballCount : singlePoleBalls.entrySet()) {
            // 这里是利用了TreeMap和comparable的特性
            Ball ball = ballCount.getKey();
            gc.setFill(ball.getColor());
            gc.fillOval(x + ballDiameter * 0.1, y, ballDiameter, ballDiameter);
            gc.strokeText(String.valueOf(ballCount.getValue()), x + ballDiameter * 0.6, textY);
            x += ballDiameter * 1.2;
        }
    }

    private void wipeCanvas(Canvas canvas) {
        canvas.getGraphicsContext2D().setFill(GLOBAL_BACKGROUND);
        canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawSnookerTargetBall(Canvas canvas, int value, boolean isFreeBall) {
        if (value == 0) {
            if (isFreeBall) throw new RuntimeException("自由球打彩球？你他妈懂不懂规则？");
            drawTargetColoredBall(canvas);
        } else {
            Color color = Values.getColorOfTarget(value);
            canvas.getGraphicsContext2D().setFill(color);
            canvas.getGraphicsContext2D().fillOval(ballDiameter * 0.1, ballDiameter * 0.1, ballDiameter, ballDiameter);
            if (isFreeBall)
                canvas.getGraphicsContext2D().strokeText("F", ballDiameter * 0.6, ballDiameter * 0.8);
        }
    }

    /**
     * @param value see {@link Game#getCurrentTarget()}
     */
    private void drawPoolTargetBall(Canvas canvas, int value) {
        System.out.println(value);
        if (value == 0) {
            drawTargetColoredBall(canvas);
        } else {
            NumberedBallTable.drawPoolBallEssential(
                    ballDiameter * 0.6,
                    ballDiameter * 0.6,
                    ballDiameter,
                    Ball.poolBallBaseColor(value),
                    value,
                    canvas.getGraphicsContext2D()
            );
        }
    }

    private void drawTargetColoredBall(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double x = ballDiameter * 0.1;
        double y = ballDiameter * 0.1;

        double deg = 0.0;
        for (Color color : Values.COLORED_LOW_TO_HIGH) {
            gc.setFill(color);
            gc.fillArc(x, y, ballDiameter, ballDiameter, deg, 60.0, ArcType.ROUND);
            deg += 60.0;
        }
    }

    private double getCurPlaySpeedMultiplier() {
//        return (replay != null || game.getGame().getCuingPlayer().getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) ?
//                p1PlaySpeed : 1;
        InGamePlayer player = getActiveHolder().getCuingIgp();
        return player.getPlayerNumber() == 1 ? p1PlaySpeed : p2PlaySpeed;
    }

    private double getPredictionLineTotalLength(
            WhitePrediction prediction,
            double potDt, 
            PlayerPerson playerPerson) {
        Cue cue = game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame());

        // 最大的预测长度
        double origMaxLength = playerPerson.getPrecisionPercentage() / 100 *
                cue.accuracyMultiplier * maxRealPredictLength;
        // 只计算距离的最小长度
        double minLength = origMaxLength / 2.2 * playerPerson.getLongPrecision();

        double potDt2 = Math.max(potDt, maxPredictLengthPotDt);
        double dtRange = minPredictLengthPotDt - maxPredictLengthPotDt;
        double lengthRange = origMaxLength - minLength;
        double potDtInRange = (potDt2 - maxPredictLengthPotDt) / dtRange;
        double predictLength = origMaxLength - potDtInRange * lengthRange;

        double side = Math.abs(cuePointX - cueCanvasWH / 2) / cueCanvasWH;  // 0和0.5之间
        double afterSide = predictLength * (1 - side);  // 加塞影响瞄准
        double mul = 1 - Math.sin(Math.toRadians(cueAngleDeg)); // 抬高杆尾影响瞄准
        double res = afterSide * mul;

        // 这是对靠近库边球瞄准线的惩罚
        // 因为游戏里贴库球瞄准线有库边作参照物，比现实中简单得多，所以要惩罚回来
        double dtToClosetCushion =
                gameValues.table.dtToClosetCushion(
                        prediction.getFirstBallX(),
                        prediction.getFirstBallY()) - gameValues.ball.ballRadius;
        double threshold = gameValues.table.closeCushionPenaltyThreshold();

        // 越接近1说明打底袋的角度越差，但是中袋角度越好
        // 但是又因为离库近的球必定不适合打中袋，所以二次补偿回来的也还将就
        // 唯一的问题就是中袋袋口附近90度的球，预测线会很短；但是那种球拿脚都打得进，所以也无所谓了
        double directionBadness =
                Math.abs(Math.abs(prediction.getBallDirectionX()) -
                        Math.abs(prediction.getBallDirectionY()));
        if (dtToClosetCushion < threshold) {
            double cushionBadness = 1 - dtToClosetCushion / threshold;
            double badness = cushionBadness + directionBadness - 1.0;
            badness = Math.max(badness, 0.0);  // 必须要都很差才算差
            double minimum = 0.25;
            double mul2 = (1 - badness) * (1 - minimum) + minimum;
//            System.out.println("Close to wall, " + directionBadness + ", " + cushionBadness + ", " + mul2);
            res *= mul2;
        }

        if (enablePsy) {
            res *= getPsyAccuracyMultiplier(playerPerson);
        }
        return res;
    }

    private void updatePlayStage() {
        if (game != null)
            currentPlayStage = game.getGame().getGamePlayStage(predictedTargetBall, printPlayStage);
    }

    private GamePlayStage gamePlayStage() {
        if (replay != null)
            return replay.getCueRecord().playStage;
        else
            return currentPlayStage;
    }

    private double getPsyAccuracyMultiplier(PlayerPerson playerPerson) {
        GamePlayStage stage = gamePlayStage();
        switch (stage) {
            case THIS_BALL_WIN:
            case ENHANCE_WIN:
                return playerPerson.psy / 100;
            default:
                return 1.0;
        }
    }

    private double getPsyControlMultiplier(PlayerPerson playerPerson) {
        GamePlayStage stage = gamePlayStage();
        switch (stage) {
            case THIS_BALL_WIN:
            case NEXT_BALL_WIN:
                return playerPerson.psy / 100;
            default:
                return 1.0;
        }
    }

    private void drawStandingPos() {
        Ball cueBall = game.getGame().getCueBall();
        PlayerPerson playerPerson = game.getGame().getCuingPlayer().getPlayerPerson();
        double[][] standingPos = CuePlayParams.personStandingPosition(
                cueBall.getX(), cueBall.getY(),
                cursorDirectionUnitX, cursorDirectionUnitY,
                playerPerson,
                playerPerson.handBody.getPrimary().hand
        );

        double canvasX1 = gamePane.canvasX(standingPos[0][0]);
        double canvasY1 = gamePane.canvasY(standingPos[0][1]);
        double canvasX2 = gamePane.canvasX(standingPos[1][0]);
        double canvasY2 = gamePane.canvasY(standingPos[1][1]);

        gamePane.getGraphicsContext().setStroke(WHITE);
        gamePane.getGraphicsContext().strokeOval(canvasX1 - 5, canvasY1 - 5, 10, 10);
        gamePane.getGraphicsContext().strokeOval(canvasX2 - 5, canvasY2 - 5, 10, 10);
    }

    private void drawWhitePathSingle(WhitePrediction prediction) {
        gamePane.drawWhitePathSingle(game.getGame().getCueBall(), prediction);
    }

    private void drawCursor() {
        if (replay != null) return;
        if (game.getGame().isEnded()) return;
        if (isPlayingMovement()) return;
        if (movement != null) return;
        if (cueAnimationPlayer != null) return;
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;
        if (game.getGame().getCueBall().isPotted()) return;

        if (drawStandingPos) drawStandingPos();
        if (cursorDrawer == null || cursorDrawer.center == null) return;

//        PlayerPerson playerPerson = game.getGame().getCuingPlayer().getPlayerPerson();

        WhitePrediction center = cursorDrawer.center;
        List<double[]> outPoints = cursorDrawer.outPoints;

        if (outPoints.size() >= 3) {
            // 画白球的落位范围
            gamePane.drawWhiteStopArea(outPoints);
        }

        // 画白球路线
        drawWhitePathSingle(center);
        if (center.getFirstCollide() != null) {
            gamePane.getGraphicsContext().strokeOval(
                    gamePane.canvasX(center.getWhiteCollisionX()) - ballRadius,
                    gamePane.canvasY(center.getWhiteCollisionY()) - ballRadius,
                    ballDiameter,
                    ballDiameter);  // 绘制预测撞击点的白球

            if (!center.isHitWallBeforeHitBall() && predictedTargetBall != null) {
                gamePane.getGraphicsContext().setFill(cursorDrawer.fill);
                gamePane.getGraphicsContext().fillPolygon(
                        new double[]{cursorDrawer.leftStartX,
                                cursorDrawer.leftEndX,
                                cursorDrawer.rightEndX,
                                cursorDrawer.rightStartX},
                        new double[]{cursorDrawer.leftStartY,
                                cursorDrawer.leftEndY,
                                cursorDrawer.rightEndY,
                                cursorDrawer.rightStartY},
                        4
                );

                if (drawTargetRefLine) {
                    double tarCanvasX = gamePane.canvasX(cursorDrawer.tarX);
                    double tarCanvasY = gamePane.canvasY(cursorDrawer.tarY);
                    gamePane.getGraphicsContext().setStroke(center.getFirstCollide().getColor().brighter().brighter());
                    gamePane.getGraphicsContext().strokeLine(tarCanvasX, tarCanvasY,
                            tarCanvasX + cursorDrawer.lineX * gamePane.getScale(),
                            tarCanvasY + cursorDrawer.lineY * gamePane.getScale());
                }
            }
        }
    }

    private void createPathPrediction() {
        if (replay != null) return;
        if (game.getGame().isEnded()) return;
        if (isPlayingMovement()) return;
        if (movement != null) return;
        if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) return;
        if (game.getGame().getCueBall().isPotted()) return;
        if (aiCalculating) return;
        if (isGameCalculating()) return;
        if (cursorDrawer != null && cursorDrawer.running) {
            return;
        }

        PlayerPerson playerPerson = game.getGame().getCuingPlayer().getPlayerPerson();
        cursorDrawer.predict(playerPerson);
//        cursorDrawer = new PredictionDrawing();
//        cursorDrawer.predict(playerPerson);

        tableGraphicsChanged = true;
    }

    private void draw() {
        if (tableGraphicsChanged) {
            gamePane.drawTable(getActiveHolder());
            if (drawAiPathItem.isSelected()) gamePane.drawPredictedWhitePath(aiWhitePath);
            if (predictPlayerPathItem.isSelected())
                gamePane.drawPredictedWhitePath(suggestedPlayerWhitePath);
        }
        drawBalls();
        if (tableGraphicsChanged) {  // drawBalls在movement最后一帧会触发一系列反应，让该值改变
            drawCursor();
        }
        drawBallInHand();
        tableGraphicsChanged = false;  // 一定在drawBalls之后
    }

    private void playMovement() {
        playingMovement = true;
        gameLoop.beginNewAnimation();
    }

    private void beginCueAnimationOfHumanPlayer(double whiteStartingX, double whiteStartingY) {
        beginCueAnimation(game.getGame().getCuingPlayer().getInGamePlayer(),
                whiteStartingX, whiteStartingY, getSelectedPower(),
                cursorDirectionUnitX, cursorDirectionUnitY);
    }

    private void beginCueAnimation(InGamePlayer cuingPlayer, double whiteStartingX, double whiteStartingY,
                                   double selectedPower, double directionX, double directionY) {
        PlayerPerson playerPerson = cuingPlayer.getPlayerPerson();
        double personPower = getPersonPower(playerPerson);  // 球手的用力程度
        double errMulWithPower = playerPerson.getErrorMultiplierOfPower(selectedPower);
        double maxPullDt = pullDtOf(playerPerson, personPower);
        double handDt = maxPullDt + HAND_DT_TO_MAX_PULL;
        double[] handXY = handPosition(handDt, cueAngleDeg, whiteStartingX, whiteStartingY, directionX, directionY);

        Cue cue;
        if (replay != null) {
            cue = replay.getCurrentCue();
        } else {
            cue = cuingPlayer.getCurrentCue(game.getGame());
        }

        double[] restCuePointing = null;
        if (currentHand != null && currentHand.hand == PlayerPerson.Hand.REST) {
            double trueAimingAngle = Algebra.thetaOf(cursorDirectionUnitX, cursorDirectionUnitY);
            double restCueAngleOffset = playerPerson.handBody.isLeftHandRest() ? -0.1 : 0.1;
            double restAngleWithOffset = trueAimingAngle - restCueAngleOffset;
            restCuePointing = Algebra.unitVectorOfAngle(restAngleWithOffset);
        }

        // 出杆速度与白球球速算法相同
        try {
            cueAnimationPlayer = new CueAnimationPlayer(
                    MIN_CUE_BALL_DT,
                    maxPullDt,
                    selectedPower,
                    errMulWithPower,
                    handXY[0],
                    handXY[1],
                    directionX,
                    directionY,
                    cue,
                    cuingPlayer,
                    currentHand,
                    restCuePointing
            );
        } catch (RuntimeException re) {
            EventLogger.error(re);
            endCueAnimation();
            playMovement();
        }
    }

    private void endCueAnimation() {
//        System.out.println("End!");
//        for (CueModel cueModel : CueModel.getAllCueModels()) {
//            cueModel.hide();
//        }
        hideCue();
        cursorDirectionUnitX = 0.0;
        cursorDirectionUnitY = 0.0;
        cueAnimationPlayer = null;
    }

    private void drawCue() {
        boolean notPlay = false;
        Ball cueBall;
        if (replay != null) {
            cueBall = replay.getCueBall();

        } else {
            if (game.getGame().isEnded()) notPlay = true;
            cueBall = game.getGame().getCueBall();
        }

        if (notPlay) return;
        if (cueAnimationPlayer == null) {
            // 绘制人类玩家瞄球时的杆

            if (replay != null) return;
            if (movement != null) return;
            if (cursorDirectionUnitX == 0.0 && cursorDirectionUnitY == 0.0) {
                return;
            }

            if (cueBall.isPotted()) return;

            double aimingOffset = aimingOffsetOfPlayer(
                    game.getGame().getCuingPlayer().getPlayerPerson(),
                    getSelectedPower());
            double trueAimingAngle = Algebra.thetaOf(cursorDirectionUnitX, cursorDirectionUnitY);
            double angleWithOffset = trueAimingAngle - aimingOffset;
            double[] cuePointing = Algebra.unitVectorOfAngle(angleWithOffset);

            PlayerPerson person = game.getGame().getCuingPlayer().getPlayerPerson();
            double personPower = getPersonPower(person);  // 球手的用力程度
            double maxPullDt = pullDtOf(person, personPower);
            double handDt = maxPullDt + HAND_DT_TO_MAX_PULL;
//            System.out.println(handDt);
            double[] handXY = handPosition(handDt,
                    cueAngleDeg,
                    cueBall.getX(), cueBall.getY(),
                    cursorDirectionUnitX, cursorDirectionUnitY);

            if (currentHand != null && currentHand.hand == PlayerPerson.Hand.REST) {
                // 画架杆，要在画杆之前，让杆覆盖在架杆之上
                double restCueAngleOffset = person.handBody.isLeftHandRest() ? -0.1 : 0.1;
                double restAngleWithOffset = trueAimingAngle - restCueAngleOffset;
                double[] restCuePointing = Algebra.unitVectorOfAngle(restAngleWithOffset);

                Cue restCue = DataLoader.getInstance().getRestCue();
                drawCueWithDtToHand(handXY[0], handXY[1],
                        restCuePointing[0],
                        restCuePointing[1],
                        0.0,
                        restCue,
                        true);
            } else {
                getCueModel(DataLoader.getInstance().getRestCue()).hide();
            }

            drawCueWithDtToHand(handXY[0], handXY[1],
                    cuePointing[0],
                    cuePointing[1],
                    MIN_CUE_BALL_DT -
                            maxPullDt - HAND_DT_TO_MAX_PULL +
                            gameValues.ball.ballRadius,
                    game.getGame().getCuingPlayer().getInGamePlayer().getCurrentCue(game.getGame()),
                    false);
        } else {
//            System.out.println("Drawing!");
            if (currentHand != null && currentHand.hand == PlayerPerson.Hand.REST) {
//                if (cueAnimationPlayer.restCuePointing == null) {
//                    System.err.println("RPNull");
//                    return;
//                }
                // 画架杆，要在画杆之前，让杆覆盖在架杆之上
                Cue restCue = DataLoader.getInstance().getRestCue();
                drawCueWithDtToHand(
                        cueAnimationPlayer.handX,
                        cueAnimationPlayer.handY,
                        cueAnimationPlayer.restCuePointing[0],
                        cueAnimationPlayer.restCuePointing[1],
                        0.0,
                        restCue,
                        true);
            } else {
//                DataLoader.getInstance().getRestCue().getCueModel(basePane).hide();
                getCueModel(DataLoader.getInstance().getRestCue()).hide();
            }

            double[] pointingVec = Algebra.unitVectorOfAngle(cueAnimationPlayer.pointingAngle);
            drawCueWithDtToHand(
                    cueAnimationPlayer.handX,
                    cueAnimationPlayer.handY,
                    pointingVec[0],
                    pointingVec[1],
                    cueAnimationPlayer.cueDtToWhite -
                            cueAnimationPlayer.maxPullDistance - HAND_DT_TO_MAX_PULL +
                            gameValues.ball.ballRadius,
                    cueAnimationPlayer.cue,
                    false);
            cueAnimationPlayer.nextFrame();
        }
    }

    /**
     * 将杆画在加了塞的位置
     */
    private double[] getCueHitPoint(double cueBallRealX, double cueBallRealY,
                                    double pointingUnitX, double pointingUnitY) {
        double originalTouchX = gamePane.canvasX(cueBallRealX);
        double originalTouchY = gamePane.canvasY(cueBallRealY);
        double sideRatio = getSelectedSideSpin() * 0.7;
        double sideXOffset = -pointingUnitY *
                sideRatio * gameValues.ball.ballRadius * gamePane.getScale();
        double sideYOffset = pointingUnitX *
                sideRatio * gameValues.ball.ballRadius * gamePane.getScale();
        return new double[]{
                originalTouchX + sideXOffset,
                originalTouchY + sideYOffset
        };
    }

    private void hideCue() {
        GameHolder gameHolder = getActiveHolder();
        List<Cue> toHide = List.of(
                gameHolder.getP1().getPlayCue(),
                gameHolder.getP1().getBreakCue(),
                gameHolder.getP2().getPlayCue(),
                gameHolder.getP2().getBreakCue(),
                DataLoader.getInstance().getRestCue()
        );

        for (Cue cue : toHide) {
            CueModel cm = cueModelMap.get(cue);
            if (cm != null) cm.hide();
        }
    }

    private void drawCueWithDtToHand(double handX,
                                     double handY,
                                     double pointingUnitX,
                                     double pointingUnitY,
                                     double realDistance,
                                     Cue cue,
                                     boolean isRest) {
//        System.out.println(distance);
//        Ball cb = getActiveHolder().getCueBall();
        double[] touchXY = getCueHitPoint(handX, handY, pointingUnitX, pointingUnitY);

        double cueAngleCos = Math.cos(Math.toRadians(cueAngleDeg));
        double cosDistance = cueAngleCos * realDistance;

        double correctedTipX = touchXY[0] - pointingUnitX * cosDistance * gamePane.getScale();
        double correctedTipY = touchXY[1] - pointingUnitY * cosDistance * gamePane.getScale();

        Bounds ballPanePos = gamePane.localToScene(gamePane.getBoundsInLocal());
//        Bounds ballPanePos = gamePane.getBoundsInParent();
//        System.out.println(correctedTipX + " " + correctedTipY + " " + realDistance + Arrays.toString(touchXY));
//        System.out.println(ballPanePos.getMinX() + " " + ballPanePos.getMinY());
        double anchorX = ballPanePos.getMinX();
        double anchorY = ballPanePos.getMinY();
        if (!basePane.getTransforms().isEmpty()) {
            for (Transform transform : basePane.getTransforms()) {
                if (transform instanceof Scale scale) {
                    anchorX /= scale.getX();
                    anchorY /= scale.getY();
                    break;
                }
            }
        }

        getCueModel(cue).show(
                anchorX + correctedTipX,
                anchorY + correctedTipY,
                pointingUnitX,
                pointingUnitY,
                cueAngleDeg,
                gamePane.getScale());
    }

    private CueModel getCueModel(Cue cue) {
        CueModel cueModel = cueModelMap.get(cue);
        if (cueModel == null) {
            cueModel = new CueModel(cue);
            cueModel.setDisable(true);
            basePane.getChildren().add(cueModel);
            cueModelMap.put(cue, cueModel);
        }

        return cueModel;
    }

    private void recalculateUiRestrictions() {
        recalculateUiRestrictions(false);
    }

    private void recalculateUiRestrictions(boolean forceChangeHand) {
        if (game == null || game.getGame() == null) return;

        Cue currentCue = game.getGame().getCuingPlayer().getInGamePlayer()
                .getCurrentCue(game.getGame());
        CueBackPredictor.Result backPre =
                game.getGame().getObstacleDtHeight(cursorDirectionUnitX, cursorDirectionUnitY,
                        currentCue.getCueTipWidth());
        if (backPre != null) {
            if (backPre instanceof CueBackPredictor.CushionObstacle cushionObstacle) {
                // 影响来自裤边
                obstacleProjection = new CushionProjection(
                        gameValues,
                        game.getGame().getCueBall(),
                        cushionObstacle.distance,
                        cushionObstacle.relativeAngle,
                        cueAngleDeg,
                        currentCue.getCueTipWidth());
            } else if (backPre instanceof CueBackPredictor.BallObstacle ballObstacle) {
                // 后斯诺
                obstacleProjection = new BallProjection(
                        ballObstacle.obstacle, game.getGame().getCueBall(),
                        cursorDirectionUnitX, cursorDirectionUnitY,
                        cueAngleDeg);
            }
        } else {
            obstacleProjection = null;
        }

        // 启用/禁用手
        updateHandSelection(forceChangeHand);

        // 如果打点不可能，把出杆键禁用了
        // 自动调整打点太麻烦了
        setCueButtonForPoint();

        createPathPrediction();
    }

    private void setCueButtonForPoint() {
        cueButton.setDisable(obstacleProjection != null &&
                !obstacleProjection.cueAble(
                        getCuePointRelX(cuePointX), getCuePointRelY(cuePointY),
                        getRatioOfCueAndBall()));
    }

    private void drawCueAngleCanvas() {
        double angleCanvasWh = cueAngleCanvas.getWidth();
        double arcRadius = angleCanvasWh * 0.75;
        cueAngleCanvasGc.setFill(GLOBAL_BACKGROUND);
        cueAngleCanvasGc.fillRect(0, 0,
                angleCanvasWh, angleCanvasWh);
        cueAngleCanvasGc.setStroke(Color.GRAY);
        cueAngleCanvasGc.setLineWidth(1.0);
        cueAngleCanvasGc.strokeArc(
                -arcRadius + cueAngleBaseHor,
                cueAngleBaseVer,
                arcRadius * 2,
                arcRadius * 2,
                0,
                90,
                ArcType.OPEN
        );

        cueAngleCanvasGc.setStroke(BLACK);
        cueAngleCanvasGc.setLineWidth(3.0);
        double lineWidth = angleCanvasWh - cueAngleBaseHor;

        cueAngleCanvasGc.strokeLine(cueAngleBaseHor, angleCanvasWh - cueAngleBaseVer,
                angleCanvasWh, angleCanvasWh - cueAngleBaseVer -
                        Math.tan(Math.toRadians(cueAngleDeg)) * lineWidth);

    }

    private void drawCueBallCanvas() {
        // Wipe
        double cueAreaDia = cueAreaRadius * 2;
        double padding = (cueCanvasWH - cueAreaDia) / 2;
        cuePointCanvasGc.setFill(GLOBAL_BACKGROUND);
        cuePointCanvasGc.fillRect(0, 0, cuePointCanvas.getWidth(), cuePointCanvas.getHeight());

        cuePointCanvasGc.setFill(Values.WHITE);
        cuePointCanvasGc.fillOval(padding, padding, cueAreaDia, cueAreaDia);

        if (obstacleProjection instanceof CushionProjection projection) {
            // 影响来自裤边
            double lineYLeft = padding + (projection.getLineYLeft() + 1) * cueAreaRadius;
            double lineYRight = padding + (projection.getLineYRight() + 1) * cueAreaRadius;

            double[] xs = new double[]{
                    padding,
                    padding,
                    cueCanvasWH - padding,
                    cueCanvasWH - padding
            };
            double[] ys = new double[]{
                    cueCanvasWH,
                    lineYLeft,
                    lineYRight,
                    cueCanvasWH
            };
            cuePointCanvasGc.setFill(Color.GRAY);
            cuePointCanvasGc.fillPolygon(xs, ys, 4);

//            if (lineY < cueCanvasWH - padding) {
//                cuePointCanvasGc.setFill(Color.GRAY);
//                cuePointCanvasGc.fillRect(0, lineY, cueCanvasWH, cueCanvasWH - lineY);
//            }
        } else if (obstacleProjection instanceof BallProjection) {
            // 后斯诺
            BallProjection projection = (BallProjection) obstacleProjection;
            cuePointCanvasGc.setFill(Color.GRAY);
            cuePointCanvasGc.fillOval(padding + cueAreaRadius * projection.getCenterHor(),
                    padding + cueAreaRadius * projection.getCenterVer(),
                    cueAreaDia,
                    cueAreaDia);
        }

        // 画个十字
        cuePointCanvasGc.setStroke(CUE_AIMING_CROSS);
        cuePointCanvasGc.strokeLine(padding, cueCanvasWH / 2, padding + cueAreaDia, cueCanvasWH / 2);
        cuePointCanvasGc.strokeLine(cueCanvasWH / 2, padding, cueCanvasWH / 2, padding + cueAreaDia);

        // 球的边界
        cuePointCanvasGc.setStroke(BLACK);
        cuePointCanvasGc.strokeOval(padding, padding, cueAreaDia, cueAreaDia);

        if (intentCuePointX >= 0 && intentCuePointY >= 0) {
            cuePointCanvasGc.setFill(INTENT_CUE_POINT);
            cuePointCanvasGc.fillOval(intentCuePointX - cueRadius, intentCuePointY - cueRadius,
                    cueRadius * 2, cueRadius * 2);
        }

        cuePointCanvasGc.setFill(CUE_POINT);
        cuePointCanvasGc.fillOval(cuePointX - cueRadius, cuePointY - cueRadius, cueRadius * 2, cueRadius * 2);

        if (miscued) {
            cuePointCanvasGc.setFill(BLACK);
            cuePointCanvasGc.fillText(strings.getString("miscued"), cueCanvasWH / 2, cueCanvasWH / 2);
        }
    }

    private double getPersonPower(PlayerPerson person) {
        return getPersonPower(getSelectedPower(), person);
    }

    private boolean isGameCalculating() {
        return game.getGame().isCalculating();
    }

    private boolean isPlayingMovement() {
        return movement != null && playingMovement;
    }

    private boolean isPlayingCueAnimation() {
        return cueAnimationPlayer != null;
    }

    class CueAnimationPlayer {
        final double[] restCuePointing;
        private final long holdMs;  // 拉至满弓的停顿时间
        private final long endHoldMs;  // 出杆完成后的停顿时间
        private final double initDistance, maxPullDistance;
        private final double cueBeforeSpeed;  // 出杆前半段的速度，毫米/ms
        private final double cueMaxSpeed;  // 杆速最快时每毫秒运动的距离，毫米/ms
        //        private final double
        private final double maxExtension;  // 杆的最大延伸距离，始终为负
        //        private final double cueBallX, cueBallY;
        private final double handX, handY;  // 手架的位置，作为杆的摇摆中心点
        private final double errMulWithPower;
        private final double aimingOffset;  // 针对瞄偏打正的球手，杆头向右拐的正
        private final Cue cue;
        private final InGamePlayer igp;
        private final PlayerPerson playerPerson;
        private final double playSpeedMultiplier;
        private long heldMs = 0;
        private long endHeldMs = 0;
        private double cueDtToWhite;  // 杆的动画离白球的真实距离，未接触前为正
        private boolean touched;  // 是否已经接触白球
        private boolean reachedMaxPull;
        private double pointingAngle;
        private boolean ended = false;
        private CuePlayType.DoubleAction doubleAction;
        private double doubleStopDt;  // 如果二段出杆，在哪里停（离白球的距离）
        private double doubleHoldMs;  // 二段出杆停的计时器

        private double framesPlayed = 0;

        CueAnimationPlayer(double initDistance,
                           double maxPullDt,
                           double selectedPower,
                           double errMulWithPower,
                           double handX, double handY,
                           double pointingUnitX, double pointingUnitY,
                           Cue cue,
                           InGamePlayer igp,
                           PlayerPerson.HandSkill handSkill,
                           double[] restCuePointing) {

            playerPerson = igp.getPlayerPerson();
            double personPower = getPersonPower(selectedPower, playerPerson);

            if (selectedPower < Values.MIN_SELECTED_POWER)
                selectedPower = Values.MIN_SELECTED_POWER;

            CuePlayType cuePlayType = playerPerson.getCuePlayType();
            if (cuePlayType.willApplySpecial(selectedPower, handSkill.hand)) {
                CuePlayType.SpecialAction sa = cuePlayType.getSpecialAction();
                if (sa instanceof CuePlayType.DoubleAction) {
                    doubleAction = (CuePlayType.DoubleAction) sa;
                    doubleStopDt = doubleAction.stoppingDtToWhite(selectedPower);
                }
            }

            this.initDistance = Math.min(initDistance, maxPullDt);
            this.maxPullDistance = maxPullDt;
            this.cueDtToWhite = this.initDistance;
            this.maxExtension = -extensionDtOf(playerPerson, personPower);

            this.cueMaxSpeed = selectedPower *
                    PlayerPerson.HandBody.getPowerMulOfHand(handSkill) *
                    Values.MAX_POWER_SPEED / 100_000.0;
            this.cueBeforeSpeed = cueMaxSpeed *
                    Algebra.shiftRange(0, 100, 1, 0.5,
                            playerPerson.getMaxSpinPercentage());  // 杆法差的人白用功比较多（无用的杆速快）

            System.out.println("Animation max speed: " + cueMaxSpeed + ", before speed: " + cueBeforeSpeed);

            this.errMulWithPower = errMulWithPower;

            this.aimingOffset = aimingOffsetOfPlayer(playerPerson, selectedPower);
            double correctPointingAngle = Algebra.thetaOf(pointingUnitX, pointingUnitY);
            pointingAngle = correctPointingAngle - aimingOffset;

            this.handX = handX;
            this.handY = handY;
            this.restCuePointing = restCuePointing;

            System.out.println(cueDtToWhite + ", " + this.cueMaxSpeed + ", " + maxExtension);

            this.cue = cue;
            this.igp = igp;
//            this.playSpeedMultiplier = (igp.getPlayerType() == PlayerType.COMPUTER || replay != null) ?
//                    p1PlaySpeed : 1;
            this.playSpeedMultiplier = igp.getPlayerNumber() == 1 ? p1PlaySpeed : p2PlaySpeed;

            this.holdMs = cuePlayType.getPullHoldMs();
            this.endHoldMs = cuePlayType.getEndHoldMs();
        }

        void nextFrame() {
            try {
                for (int i = 0; i < playSpeedMultiplier; i++) {
                    if (framesPlayed % 1.0 == 0.0) {
                        if (ended) return;
                        calculateOneFrame();
                    }
                }
                framesPlayed += playSpeedMultiplier;
            } catch (RuntimeException re) {
                // 防止动画的问题导致游戏卡死
                EventLogger.error(re);
                endCueAnimation();
                playMovement();
            }
        }

        private void calculateOneFrame() {
            if (reachedMaxPull && (heldMs < holdMs || movement == null)) {
                // 后停，至少要停指定的时间。或者是物理运算还没算好时，也用后停来拖时间
                heldMs += gameLoop.lastAnimationFrameMs();
            } else if (endHeldMs > 0) {
                endHeldMs += gameLoop.lastAnimationFrameMs();
                if (endHeldMs >= endHoldMs) {
                    ended = true;
                    endCueAnimation();
                }
            } else if (reachedMaxPull) {
                double lastCueDtToWhite = cueDtToWhite;

                if (doubleAction != null) {
                    double deltaD = gameLoop.lastAnimationFrameMs() * doubleAction.speedMul / 2.5;
                    double nextTickDt = cueDtToWhite - deltaD;
                    if (cueDtToWhite > doubleStopDt) {
                        if (nextTickDt <= doubleStopDt) {
                            // 就是这一帧停
                            doubleHoldMs += gameLoop.lastAnimationFrameMs();
                            if (doubleHoldMs >= doubleAction.holdMs) {
                                // 中停结束了，但是为了代码简单我们还是让它多停一帧
                                doubleAction = null;
                            }
                        } else {
                            // 还没到
                            cueDtToWhite = nextTickDt;
                        }
                        return;
                    } else {
                        System.err.println("Wired. Why there is double cue action but never reached");
                    }
                }
                // 正常出杆
                if (cueDtToWhite > maxPullDistance * 0.4) {
                    cueDtToWhite -= cueBeforeSpeed * gameLoop.lastAnimationFrameMs();
                } else if (!touched) {
                    cueDtToWhite -= cueMaxSpeed * gameLoop.lastAnimationFrameMs();
                } else {
                    cueDtToWhite -= cueBeforeSpeed * gameLoop.lastAnimationFrameMs();
                }
                double wholeDtPercentage = 1 - (cueDtToWhite - maxExtension) /
                        (maxPullDistance - maxExtension);  // 出杆完成的百分比
                wholeDtPercentage = Math.max(0, Math.min(wholeDtPercentage, 0.9999));
//                System.out.println(wholeDtPercentage);

                List<Double> stages = playerPerson.getCuePlayType().getSequence();
                double stage = stages.isEmpty() ?
                        0 :
                        stages.get((int) (wholeDtPercentage * stages.size()));
                double baseSwingMag = playerPerson.getCueSwingMag();
                if (enablePsy) {
                    double psyFactor = 1.0 - getPsyAccuracyMultiplier(playerPerson);
                    baseSwingMag *= (1.0 + psyFactor * 5);
                }
                double frameRateRatio = gameLoop.lastAnimationFrameMs() / frameTimeMs;
                if (!touched) {
                    double changeRatio = (lastCueDtToWhite - cueDtToWhite) / maxPullDistance;
                    pointingAngle += aimingOffset * changeRatio;
                }
                if (stage < 0) {  // 向左扭
                    pointingAngle = pointingAngle + baseSwingMag *
                            errMulWithPower / 2000 * frameRateRatio;
                } else if (stage > 0) {  // 向右扭
                    pointingAngle = pointingAngle - baseSwingMag *
                            errMulWithPower / 2000 * frameRateRatio;
                }

                if (!touched) {
                    if (cueDtToWhite < 0 && lastCueDtToWhite >= 0) {
                        touched = true;
                        playMovement();
                    }
                } else if (cueDtToWhite <= maxExtension) {
                    endHeldMs += gameLoop.lastAnimationFrameMs();  // 出杆结束了
                }
            } else {
                cueDtToWhite += gameLoop.lastAnimationFrameMs() / 3.0 *
                        playerPerson.getCuePlayType().getPullSpeedMul();  // 往后拉
                if (cueDtToWhite >= maxPullDistance) {
                    reachedMaxPull = true;
                }
            }
        }
    }

    private class PredictionDrawing {
        final int nPoints = predictionQuality.nPoints;

        final int nThreads = Math.min(nPoints, ConfigLoader.getInstance().getInt("nThreads", nPoints));
        Game[] gamePool = new Game[nPoints];
        ExecutorService threadPool;
        ExecutorCompletionService<PredictionResult> ecs;

        WhitePrediction[] clockwise = new WhitePrediction[nPoints];
        WhitePrediction center;
        List<double[]> outPoints;
        LinearGradient fill;

        double lineX, lineY;
        double tarX, tarY;
        double leftStartX, rightStartX, leftStartY, rightStartY;
        double leftEndX, rightEndX, leftEndY, rightEndY;

        boolean running;

        PredictionDrawing() {
            if (predictionQuality.nPoints > 0) {
                threadPool = Executors.newFixedThreadPool(nThreads,
                        r -> {
                            Thread t = Executors.defaultThreadFactory().newThread(r);
                            t.setDaemon(true);
                            return t;
                        });
                ecs = new ExecutorCompletionService<>(threadPool);
            }

            synchronizeGame();
        }

        private void synchronizeGame() {
//            predictionPool[0] = game.getGame();
            for (int i = 0; i < gamePool.length; i++) {
                gamePool[i] = game.getGame().clone();
            }
            center = null;
            outPoints = null;
            fill = null;
            Arrays.fill(clockwise, null);
        }

        private void predict(PlayerPerson playerPerson) {
            running = true;
//            long t0 = System.currentTimeMillis();

            CuePlayParams[] possibles = generateCueParamsSd1(nPoints);
            center = game.getGame().predictWhite(
                    possibles[0],
                    game.whitePhy,
                    WHITE_PREDICT_LEN_AFTER_WALL * playerPerson.getSolving() / 100,
                    predictionQuality.secondCollision,
                    false,
                    true,
                    false);
            if (center == null) {
                predictedTargetBall = null;
                running = false;
                return;
            }

            if (nPoints > 0) {
                for (int i = 0; i < clockwise.length; i++) {
                    final int ii = i;
                    ecs.submit(() -> new PredictionResult(ii, gamePool[ii].predictWhite(
                            possibles[ii + 1],
                            game.whitePhy,
                            WHITE_PREDICT_LEN_AFTER_WALL * playerPerson.getSolving() / 100,
                            predictionQuality.secondCollision,
                            false,
                            true,
                            false
                    )));
                }
                try {
                    for (int i = 0; i < clockwise.length; i++) {
                        Future<PredictionResult> res = ecs.take();
                        PredictionResult pr = res.get(1, TimeUnit.SECONDS);
                        if (pr.wp == null) {
                            throw new InterruptedException();
                        }
                        clockwise[pr.index] = pr.wp;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    synchronizeGame();
                    running = false;
                    return;
                } catch (TimeoutException e) {
                    System.err.println("Cannot finish white prediction in time.");
                    synchronizeGame();
                    running = false;
                    return;
                }
            }

//            long t1 = System.currentTimeMillis();
//            System.out.println("Prediction time: " + (t1 - t0));

            if (nPoints >= 4) {
                double[][] predictionStops = new double[clockwise.length + 1][];
                predictionStops[0] = center.stopPoint();
                for (int i = 0; i < clockwise.length; i++) {
                    predictionStops[i + 1] = clockwise[i].stopPoint();
                }
                if (nPoints == 8) {
                    outPoints = GraphicsUtil.processPoints(gameValues.table, predictionStops);
                } else if (nPoints == 4) {
                    double[][] corners = new double[4][];
                    for (int i = 0; i < corners.length; i++) {
                        corners[i] = clockwise[i].stopPoint();
                    }
                    outPoints = GraphicsUtil.populatePoints(gameValues.table,
                            center.getWhitePath(),
                            corners);
                }
            } else if (nPoints == 2) {
                outPoints = GraphicsUtil.populatePoints(gameValues.table,
                        center.getWhitePath(),
                        clockwise[0].stopPoint(),
                        clockwise[1].stopPoint());
            }

            if (center.getFirstCollide() == null) {
                predictedTargetBall = null;
            } else {
                // 弹库的球就不给预测线了
                if (center.isHitWallBeforeHitBall()) {
                    predictedTargetBall = null;
                } else {
                    predictedTargetBall = center.getFirstCollide();
                    double potDt = Algebra.distanceToPoint(
                            center.getWhiteCollisionX(), center.getWhiteCollisionY(),
                            center.whiteX, center.whiteY);
                    // 白球行进距离越长，预测线越短
                    double predictLineTotalLen = getPredictionLineTotalLength(
                            center,
                            potDt,
                            game.getGame().getCuingPlayer().getPlayerPerson());

                    targetPredictionUnitY = center.getBallDirectionY();
                    targetPredictionUnitX = center.getBallDirectionX();
                    double whiteUnitXBefore = center.getWhiteDirectionXBeforeCollision();
                    double whiteUnitYBefore = center.getWhiteDirectionYBeforeCollision();

                    double theta = Algebra.thetaBetweenVectors(
                            center.getBallDirectionXRaw(), 
                            center.getBallDirectionYRaw(),  // 防止齿轮/投掷效应的影响
                            whiteUnitXBefore, 
                            whiteUnitYBefore
                    );

                    // 画预测线
                    // 角度越大，目标球预测线越短
                    double pureMultiplier = Algebra.powerTransferOfAngle(theta);
                    double multiplier = Math.pow(pureMultiplier, 1 / playerPerson.getAnglePrecision());

                    // 击球的手的multiplier
                    double handMul = PlayerPerson.HandBody.getPrecisionOfHand(currentHand);

                    double totalLen = predictLineTotalLen * multiplier * handMul;
                    totalLen = Math.max(totalLen, 1.0);  // 至少也得一毫米吧哈哈哈哈哈哈

                    lineX = targetPredictionUnitX * totalLen;
                    lineY = targetPredictionUnitY * totalLen;

                    Ball targetBall = center.getFirstCollide();
                    tarX = targetBall.getX();
                    tarY = targetBall.getY();

                    // 画宽线
                    double xShift = targetPredictionUnitY * gameValues.ball.ballRadius;
                    double yShift = -targetPredictionUnitX * gameValues.ball.ballRadius;

                    leftStartX = gamePane.canvasX(tarX - xShift);
                    rightStartX = gamePane.canvasX(tarX + xShift);
                    leftStartY = gamePane.canvasY(tarY - yShift);
                    rightStartY = gamePane.canvasY(tarY + yShift);

                    leftEndX = gamePane.canvasX(tarX - xShift + lineX);
                    rightEndX = gamePane.canvasX(tarX + xShift + lineX);
                    leftEndY = gamePane.canvasY(tarY - yShift + lineY);
                    rightEndY = gamePane.canvasY(tarY + yShift + lineY);

                    Stop[] stops = new Stop[]{
                            new Stop(0, targetBall.getColorWithOpa()),
                            new Stop(1, targetBall.getColorTransparent())
                    };

                    double sx, sy, ex, ey;
                    if (targetPredictionUnitX < 0) {
                        sx = -targetPredictionUnitX;
                        ex = 0;
                    } else {
                        sx = 0;
                        ex = targetPredictionUnitX;
                    }
                    if (targetPredictionUnitY < 0) {
                        sy = -targetPredictionUnitY;
                        ey = 0;
                    } else {
                        sy = 0;
                        ey = targetPredictionUnitY;
                    }

                    fill = new LinearGradient(
                            sx, sy, ex, ey,
                            true,
                            CycleMethod.NO_CYCLE,
                            stops
                    );
                }
            }

            running = false;
        }

        class PredictionResult {
            final int index;
            final WhitePrediction wp;

            PredictionResult(int index, WhitePrediction wp) {
                this.index = index;
                this.wp = wp;
            }
        }
    }
}
