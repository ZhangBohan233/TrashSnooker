package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public class ChineseEightBallGame extends NumberedBallGame
        implements NeedBigBreak {
    
    public static final int FULL_BALL_REP = 16;
    public static final int HALF_BALL_REP = 17;

    private static final int[] FULL_BALL_SLOTS = {0, 2, 3, 7, 9, 10, 12};
    private static final int[] HALF_BALL_SLOTS = {1, 5, 6, 7, 11, 13, 14};

    private ChineseEightBallPlayer winingPlayer;

    private final PoolBall eightBall;
    private final PoolBall[] allBalls = new PoolBall[16];
    private boolean isBreaking = true;
    private String foulReason;

    public ChineseEightBallGame(GameView parent, GameSettings gameSettings, int frameIndex) {
        super(parent, gameSettings, GameValues.CHINESE_EIGHT_VALUES, frameIndex);

        eightBall = new PoolBall(8, false, gameValues);
        initBalls();
    }

    private void initBalls() {
        List<PoolBall> fullBalls = new ArrayList<>();
        List<PoolBall> halfBalls = new ArrayList<>();
        for (int i = 0; i < 7; ++i) {
            fullBalls.add(new PoolBall(i + 1, false, gameValues));
        }
        for (int i = 0; i < 7; ++i) {
            halfBalls.add(new PoolBall(i + 9, false, gameValues));
        }

        allBalls[0] = (PoolBall) cueBall;
        for (int i = 0; i < 7; ++i) {
            allBalls[i + 1] = fullBalls.get(i);
        }
        allBalls[8] = eightBall;
        for (int i = 0; i < 7; ++i) {
            allBalls[i + 9] = halfBalls.get(i);
        }

        Collections.shuffle(fullBalls);
        Collections.shuffle(halfBalls);

        double curX = breakPointX();
        double rowStartY = gameValues.midY;
        double rowOccupyX = gameValues.ballDiameter * Math.sin(Math.toRadians(60.0))
                + Game.MIN_PLACE_DISTANCE * 0.6;
        int ballCountInRow = 1;
        int index = 0;
        for (int row = 0; row < 5; ++row) {
            double y = rowStartY;
            for (int col = 0; col < ballCountInRow; ++col) {
                if (index == 4) {
                    eightBall.setX(curX);
                    eightBall.setY(y);
                } else if (Util.arrayContains(FULL_BALL_SLOTS, index)) {
                    PoolBall ball = fullBalls.remove(fullBalls.size() - 1);
                    ball.setX(curX);
                    ball.setY(y);
                } else {
                    PoolBall ball = halfBalls.remove(halfBalls.size() - 1);
                    ball.setX(curX);
                    ball.setY(y);
                }
                index++;
                y += gameValues.ballDiameter + Game.MIN_PLACE_DISTANCE;
            }
            ballCountInRow++;
            rowStartY -= gameValues.ballRadius + Game.MIN_PLACE_DISTANCE;
            curX += rowOccupyX;
        }
    }

    @Override
    public boolean isBreaking() {
        return isBreaking;
    }

    @Override
    protected double breakPointX() {
        return gameValues.leftX + (gameValues.innerWidth * 0.75);
    }

    @Override
    protected void initPlayers() {
        player1 = new ChineseEightBallPlayer(1, gameSettings.getPlayer1());
        player2 = new ChineseEightBallPlayer(2, gameSettings.getPlayer2());
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        if (isJustAfterBreak()) {
            return x < breakLineX() && !isOccupied(x, y);
        } else {
            return !isOccupied(x, y);
        }
    }

    @Override
    public PoolBall[] getAllBalls() {
        return allBalls;
    }

    private boolean isTargetSelected() {
        return ((ChineseEightBallPlayer) player1).getBallRange() != 0;
    }

    @Override
    protected void endMoveAndUpdate() {
        updateScore(newPotted);
        isBreaking = false;
    }

    @Override
    public Player getWiningPlayer() {
        return winingPlayer;
    }

    private boolean isFullBall(Ball ball) {
        return ball.getValue() >= 1 && ball.getValue() <= 7;
    }

    private boolean isHalfBall(Ball ball) {
        return ball.getValue() >= 9 && ball.getValue() <= 15;
    }

    private boolean hasFullBallOnTable() {
        for (PoolBall ball : allBalls) {
            if (!ball.isPotted() && isFullBall(ball)) return true;
        }
        return false;
    }

    private boolean hasHalfBallOnTable() {
        for (PoolBall ball : allBalls) {
            if (!ball.isPotted() && isHalfBall(ball)) return true;
        }
        return false;
    }

    private boolean allFullBalls(Set<Ball> balls) {
        for (Ball ball : balls) {
            if (isHalfBall(ball)) return false;
        }
        return true;
    }

    private boolean allHalfBalls(Set<Ball> balls) {
        for (Ball ball : balls) {
            if (isFullBall(ball)) return false;
        }
        return true;
    }
    
    private Collection<Ball> fullBallsOf(Set<Ball> balls) {
        return balls.stream().filter(this::isFullBall).collect(Collectors.toSet());
    }

    private Collection<Ball> halfBallsOf(Set<Ball> balls) {
        return balls.stream().filter(this::isHalfBall).collect(Collectors.toSet());
    }

    private boolean hasFullBalls(Set<Ball> balls) {
        for (Ball ball : balls) {
            if (isFullBall(ball)) return true;
        }
        return false;
    }

    private boolean hasHalfBalls(Set<Ball> balls) {
        for (Ball ball : balls) {
            if (isHalfBall(ball)) return true;
        }
        return false;
    }

    private int getTargetOfPlayer(Player playerX) {
        ChineseEightBallPlayer player = (ChineseEightBallPlayer) playerX;
        if (player.getBallRange() == 0) return 0;
        if (player.getBallRange() == FULL_BALL_REP) {
            if (hasFullBallOnTable()) return FULL_BALL_REP;
            else return 8;
        }
        if (player.getBallRange() == HALF_BALL_REP) {
            if (hasHalfBallOnTable()) return HALF_BALL_REP;
            else return 8;
        }
        throw new RuntimeException("?????????");
    }

    private boolean isJustAfterBreak() {
        return finishedCuesCount == 1;
    }

    private void updateScore(Set<Ball> pottedBalls) {
        boolean foul = false;
        if (!collidesWall && pottedBalls.isEmpty()) {
            foul = true;
            foulReason = "????????????????????????????????????";
        }
        if (lastCueFoul && isJustAfterBreak()) {
            // ?????????????????????????????????
            if (lastCueVx < 0) {
                foul = true;
                foulReason = "????????????????????????????????????????????????";
            }
        }

        if (cueBall.isPotted()) {
            if (eightBall.isPotted()) {  // ?????????????????????
                ended = true;
                winingPlayer = (ChineseEightBallPlayer) getAnotherPlayer();
                return;
            }
            foul = true;
            foulReason = "????????????";
        }

        if (whiteFirstCollide == null) {
            foul = true;
            foulReason = "??????";
        } else {
            if (currentTarget == FULL_BALL_REP) {
                if (!isFullBall(whiteFirstCollide)) {
                    foul = true;
                    foulReason = "?????????????????????????????????????????????";
                }
            } else if (currentTarget == HALF_BALL_REP) {
                if (!isHalfBall(whiteFirstCollide)) {
                    foul = true;
                    foulReason = "?????????????????????????????????????????????";
                }
            } else if (currentTarget == 8) {
                if (whiteFirstCollide.getValue() != 8) {
                    foul = true;
                    foulReason = "???????????????????????????????????????";
                }
            }
        }

        if (foul) {
            lastCueFoul = true;
            cueBall.pot();
            ballInHand = true;
            switchPlayer();
            currentTarget = getTargetOfPlayer(currentPlayer);  // ???switchPlayer??????
            System.out.println(foulReason);
            return;
        }

        if (!pottedBalls.isEmpty()) {
            if (pottedBalls.contains(eightBall)) {
                ended = true;
                if (currentTarget == 8) {
                    winingPlayer = (ChineseEightBallPlayer) currentPlayer;
                } else {  // ???????????? todo: ????????????
                    winingPlayer = (ChineseEightBallPlayer) getAnotherPlayer();
                }
                return;
            }
            if (currentTarget == 0) {  // ?????????
                if (isBreaking) {  // ????????????????????????
                    return;
                }
                if (allFullBalls(pottedBalls)) {
                    ((ChineseEightBallPlayer) currentPlayer).setBallRange(FULL_BALL_REP);
                    ((ChineseEightBallPlayer) getAnotherPlayer()).setBallRange(HALF_BALL_REP);
                    currentTarget = getTargetOfPlayer(currentPlayer);
                    lastPotSuccess = true;
                    currentPlayer.correctPotBalls(fullBallsOf(pottedBalls));
                }
                if (allHalfBalls(pottedBalls)) {
                    ((ChineseEightBallPlayer) currentPlayer).setBallRange(HALF_BALL_REP);
                    ((ChineseEightBallPlayer) getAnotherPlayer()).setBallRange(FULL_BALL_REP);
                    currentTarget = getTargetOfPlayer(currentPlayer);
                    lastPotSuccess = true;
                    currentPlayer.correctPotBalls(fullBallsOf(pottedBalls));
                }
            } else {
                if (currentTarget == FULL_BALL_REP) {
                    if (hasFullBalls(pottedBalls)) {
                        lastPotSuccess = true;
                    }
                    currentPlayer.correctPotBalls(fullBallsOf(pottedBalls));
                } else if (currentTarget == HALF_BALL_REP) {
                    if (hasHalfBalls(pottedBalls)) {
                        lastPotSuccess = true;
                    }
                    currentPlayer.correctPotBalls(halfBallsOf(pottedBalls));
                }
            }
        }
        if (lastPotSuccess) {
            potSuccess();
        } else {
            switchPlayer();
        }
        currentTarget = getTargetOfPlayer(currentPlayer);  // ???switchPlayer??????
    }

    @Override
    protected void updateTargetPotSuccess(boolean isSnookerFreeBall) {

    }

    @Override
    protected void updateTargetPotFailed() {

    }
}
