package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.util.GameSaver;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;

import java.sql.Timestamp;

public class EntireGame {

    public final int totalFrames;
    public final GameType gameType;
    private final Timestamp startTime = new Timestamp(System.currentTimeMillis());
    private final GameView gameView;
    InGamePlayer p1;
    InGamePlayer p2;
    Game game;
    private int p1Wins;
    private int p2Wins;
    private boolean p1Breaks;

    public EntireGame(GameView gameView, InGamePlayer p1, InGamePlayer p2, GameType gameType,
                      int totalFrames) {
        this.p1 = p1;
        this.p2 = p2;
        if (totalFrames % 2 != 1) {
            throw new RuntimeException("Total frames must be odd.");
        }
        this.gameType = gameType;
        this.totalFrames = totalFrames;
        this.gameView = gameView;

        DBAccess.getInstance().recordAnEntireGameStarts(this);

        createNextFrame();
    }

    public static EntireGame load() {
        return GameSaver.load();
    }

    public void save() {
        GameSaver.save(this);
    }

    public Game getGame() {
        return game;
    }

    public int getP1Wins() {
        return p1Wins;
    }

    public int getP2Wins() {
        return p2Wins;
    }

    public InGamePlayer getPlayer1() {
        return p1;
    }

    public InGamePlayer getPlayer2() {
        return p2;
    }

    public boolean playerWinsAframe(InGamePlayer player) {
        DBAccess.getInstance().recordAFrameEnds(
                this, game, player.getPlayerPerson());
        updateFrameRecords(game.getPlayer1(), player);
        updateFrameRecords(game.getPlayer2(), player);
        if (player.getPlayerPerson().equals(p1.getPlayerPerson())) {
            return p1WinsAFrame();
        } else {
            return p2WinsAFrame();
        }
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public boolean isFinished() {
        return p1Wins > totalFrames / 2 || p2Wins > totalFrames / 2;
    }

    private boolean p1WinsAFrame() {
        p1Wins++;
        return p1Wins > totalFrames / 2;
    }

    private boolean p2WinsAFrame() {
        p2Wins++;
        return p2Wins > totalFrames / 2;
    }

    private void updateFrameRecords(Player framePlayer, InGamePlayer winingPlayer) {
        if (framePlayer instanceof SnookerPlayer) {
            SnookerPlayer snookerPlayer = (SnookerPlayer) framePlayer;
            snookerPlayer.flushSinglePoles();
            DBAccess.getInstance().recordSnookerBreaks(this,
                    getGame(),
                    snookerPlayer, 
                    snookerPlayer.getSinglePolesInThisGame());
        } else if (framePlayer instanceof NumberedBallPlayer) {
            // ???????????????
            NumberedBallPlayer numberedBallPlayer = (NumberedBallPlayer) framePlayer;
            numberedBallPlayer.flushSinglePoles();
            DBAccess.getInstance().recordNumberedBallResult(this,
                    getGame(),
                    numberedBallPlayer,
                    winingPlayer.getPlayerPerson().equals(
                            framePlayer.playerPerson.getPlayerPerson()),
                    numberedBallPlayer.getContinuousPots());
        }
    }

    public void quitGame() {
        if (game != null) {
            game.quitGame();
        }
        if (!isFinished()) {
            DBAccess.getInstance().abortEntireGame(this);
        }
    }

    public void startNextFrame() {
        createNextFrame();
    }

    public String getStartTimeSqlString() {
        return Util.timeStampFmt(startTime);
    }

    private void createNextFrame() {
        p1Breaks = !p1Breaks;
        GameSettings gameSettings = new GameSettings.Builder()
                .player1Breaks(p1Breaks)
                .players(p1, p2)
                .build();
        game = Game.createGame(gameView, gameSettings, gameType, p1Wins + p2Wins + 1);
        DBAccess.getInstance().recordAFrameStarts(
                this, game);
    }
}
