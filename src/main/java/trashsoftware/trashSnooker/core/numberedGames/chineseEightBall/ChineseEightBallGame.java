package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.ChineseEightAiCue;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.scoreResult.ChineseEightScoreResult;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.table.ChineseEightTable;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.core.table.Tables;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;
import java.util.stream.Collectors;

public class ChineseEightBallGame extends NumberedBallGame<ChineseEightBallPlayer>
        implements NeedBigBreak {
    
    public static final int NOT_SELECTED_REP = 0;
    public static final int FULL_BALL_REP = 16;
    public static final int HALF_BALL_REP = 17;

    private static final int[] FULL_BALL_SLOTS = {0, 2, 3, 7, 9, 10, 12};
    private static final int[] HALF_BALL_SLOTS = {1, 5, 6, 7, 11, 13, 14};

    private ChineseEightBallPlayer winingPlayer;
    private ChineseEightScoreResult curResult;

    private final PoolBall eightBall;
    private final PoolBall[] allBalls = new PoolBall[16];

    public ChineseEightBallGame(GameView parent, GameSettings gameSettings, int frameIndex) {
        super(parent, gameSettings, GameValues.CHINESE_EIGHT_VALUES, frameIndex);

        eightBall = new PoolBall(8, false, gameValues);
        initBalls();
    }

    @Override
    public GameType getGameType() {
        return GameType.CHINESE_EIGHT;
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

        allBalls[0] = cueBall;
        for (int i = 0; i < 7; ++i) {
            allBalls[i + 1] = fullBalls.get(i);
        }
        allBalls[8] = eightBall;
        for (int i = 0; i < 7; ++i) {
            allBalls[i + 9] = halfBalls.get(i);
        }

        Collections.shuffle(fullBalls);
        Collections.shuffle(halfBalls);

        double curX = getTable().breakPointX();
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
    public ScoreResult makeScoreResult(Player justCuedPlayer) {
        return curResult;
    }

    public Movement cue(CuePlayParams params) {
        createScoreResult();
        return super.cue(params);
    }
    
    private void createScoreResult() {
        curResult = new ChineseEightScoreResult(
                thinkTime,
                getCuingPlayer().getInGamePlayer().getPlayerNumber(),
                ChineseEightTable.filterRemainingTargetOfPlayer(player1.getBallRange(), this),
                ChineseEightTable.filterRemainingTargetOfPlayer(player2.getBallRange(), this));
    }

    @Override
    protected AiCue<?, ?> createAiCue(ChineseEightBallPlayer aiPlayer) {
        return new ChineseEightAiCue(this, aiPlayer);
    }

    @Override
    public boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall) {
        if (!ball.isPotted() && !ball.isWhite()) {
            if (targetRep == NOT_SELECTED_REP) {
                return ball.getValue() != 8;
            } else if (targetRep == FULL_BALL_REP) {
                return ball.getValue() < 8;
            } else if (targetRep == HALF_BALL_REP) {
                return ball.getValue() > 8;
            } else if (targetRep == 8) {
                return ball.getValue() == 8;
            }
        }
        return false;
    }

    @Override
    public int getTargetAfterPotSuccess(Ball pottingBall, boolean isSnookerFreeBall) {
        ChineseEightBallPlayer player = getCuingPlayer();
        if (player.getBallRange() == NOT_SELECTED_REP) {
            if (isFullBall(pottingBall)) return FULL_BALL_REP;
            else if (isHalfBall(pottingBall)) return HALF_BALL_REP;
            else return 0;
        }
        if (player.getBallRange() == FULL_BALL_REP) {
            if (getRemFullBallOnTable() > 1) return FULL_BALL_REP;
            else return 8;
        }
        if (player.getBallRange() == HALF_BALL_REP) {
            if (getRemHalfBallOnTable() > 1) return HALF_BALL_REP;
            else return 8;
        }
        throw new RuntimeException("不可能");
    }

    @Override
    public int getTargetAfterPotFailed() {
        return getTargetOfPlayer(getAnotherPlayer());
    }

    @Override
    public double priceOfTarget(int targetRep, Ball ball, Player attackingPlayer,
                                Ball lastPotting) {
        return 1.0;
    }

    /**
     * 返回给定的球员还剩几颗球没打，含黑八
     */
    public int getRemainingBallsOfPlayer(Player player) {
        int target = getTargetOfPlayer(player);
        if (target == 8) return 1;
        else if (target == FULL_BALL_REP) {
            int rem = 0;
            for (int i = 1; i <= 8; i++) {
                Ball ball = getAllBalls()[i];
                if (!ball.isPotted()) rem++;
            }
            return rem;
        } else if (target == HALF_BALL_REP) {
            int rem = 0;
            for (int i = 8; i <= 15; i++) {
                Ball ball = getAllBalls()[i];
                if (!ball.isPotted()) rem++;
            }
            return rem;
        } else {
            int rem = 0;
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted()) rem++;
            }
            return rem;
        }
    }

    @Override
    public ChineseEightTable getTable() {
        return Tables.CHINESE_EIGHT_TABLE;
    }

    @Override
    protected void initPlayers() {
        player1 = new ChineseEightBallPlayer(gameSettings.getPlayer1());
        player2 = new ChineseEightBallPlayer(gameSettings.getPlayer2());
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        if (isJustAfterBreak()) {
            return x < getTable().breakLineX() && !isOccupied(x, y);
        } else {
            return !isOccupied(x, y);
        }
    }

    @Override
    public PoolBall[] getAllBalls() {
        return allBalls;
    }

    private boolean isTargetSelected() {
        return player1.getBallRange() != 0;
    }

    @Override
    protected void endMoveAndUpdate() {
        updateScore(newPotted);
    }

    @Override
    public Player getWiningPlayer() {
        return winingPlayer;
    }

    @Override
    public GamePlayStage getGamePlayStage(Ball predictedTargetBall, boolean printPlayStage) {
        int rems = getRemainingBallsOfPlayer(getCuingPlayer());
        if (rems == 1) {
            if (printPlayStage) System.out.println("打进就赢！");
            return GamePlayStage.THIS_BALL_WIN;
        } else if (rems == 2) {
            if (printPlayStage) System.out.println("下一颗赢！");
            return GamePlayStage.NEXT_BALL_WIN;
        }
        return GamePlayStage.NORMAL;
    }

    private boolean isFullBall(Ball ball) {
        return ball.getValue() >= 1 && ball.getValue() <= 7;
    }

    private boolean isHalfBall(Ball ball) {
        return ball.getValue() >= 9 && ball.getValue() <= 15;
    }
    
    private int getRemFullBallOnTable() {
        int count = 0;
        for (PoolBall ball : allBalls) {
            if (!ball.isPotted() && isFullBall(ball)) count++;
        }
        return count;
    }

    private int getRemHalfBallOnTable() {
        int count = 0;
        for (PoolBall ball : allBalls) {
            if (!ball.isPotted() && isHalfBall(ball)) count++;
        }
        return count;
    }

    private boolean hasFullBallOnTable() {
        return getRemFullBallOnTable() > 0;
    }

    private boolean hasHalfBallOnTable() {
        return getRemHalfBallOnTable() > 0;
    }

    private boolean allFullBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isHalfBall(ball)) return false;
        }
        return true;
    }

    private boolean allHalfBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isFullBall(ball)) return false;
        }
        return true;
    }
    
    private Collection<PoolBall> fullBallsOf(Set<PoolBall> balls) {
        return balls.stream().filter(this::isFullBall).collect(Collectors.toSet());
    }

    private Collection<PoolBall> halfBallsOf(Set<PoolBall> balls) {
        return balls.stream().filter(this::isHalfBall).collect(Collectors.toSet());
    }

    private boolean hasFullBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isFullBall(ball)) return true;
        }
        return false;
    }

    private boolean hasHalfBalls(Set<PoolBall> balls) {
        for (PoolBall ball : balls) {
            if (isHalfBall(ball)) return true;
        }
        return false;
    }

    private int getTargetOfPlayer(Player playerX) {
        ChineseEightBallPlayer player = (ChineseEightBallPlayer) playerX;
        if (player.getBallRange() == NOT_SELECTED_REP) return NOT_SELECTED_REP;
        if (player.getBallRange() == FULL_BALL_REP) {
            if (hasFullBallOnTable()) return FULL_BALL_REP;
            else return 8;
        }
        if (player.getBallRange() == HALF_BALL_REP) {
            if (hasHalfBallOnTable()) return HALF_BALL_REP;
            else return 8;
        }
        throw new RuntimeException("不可能");
    }

    public boolean isJustAfterBreak() {
        return finishedCuesCount == 1;
    }

    private void updateScore(Set<PoolBall> pottedBalls) {
        boolean foul = false;
        if (!collidesWall && pottedBalls.isEmpty()) {
            foul = true;
            foulReason = "没有任何球接触库边或落袋";
        }
        if (lastCueFoul && isJustAfterBreak()) {
            // 开球后直接造成的自由球
            if (lastCueVx < 0) {
                foul = true;
                foulReason = "开球直接造成的自由球必须向前击打";
            }
        }

        if (cueBall.isPotted()) {
            if (eightBall.isPotted()) {  // 白球黑八一起进
                end();
                winingPlayer = getAnotherPlayer();
                return;
            }
            foul = true;
            foulReason = "白球落袋";
        }

        if (whiteFirstCollide == null) {
            foul = true;
            foulReason = "空杆";
        } else {
            if (currentTarget == FULL_BALL_REP) {
                if (!isFullBall(whiteFirstCollide)) {
                    foul = true;
                    foulReason = "目标球为全色球，但击打了半色球";
                }
            } else if (currentTarget == HALF_BALL_REP) {
                if (!isHalfBall(whiteFirstCollide)) {
                    foul = true;
                    foulReason = "目标球为半色球，但击打了全色球";
                }
            } else if (currentTarget == 8) {
                if (whiteFirstCollide.getValue() != 8) {
                    foul = true;
                    foulReason = "目标球为黑球，但击打了其他球";
                }
            }
        }

        if (foul) {
            lastCueFoul = true;
            cueBall.pot();
            ballInHand = true;
            switchPlayer();
            currentTarget = getTargetOfPlayer(currentPlayer);  // 在switchPlayer之后
            System.out.println(foulReason);
            return;
        }

        if (!pottedBalls.isEmpty()) {
            if (pottedBalls.contains(eightBall)) {
                end();
                if (currentTarget == 8) {
                    winingPlayer = currentPlayer;
                } else {  // 误进黑八 todo: 检查开球
                    winingPlayer = getAnotherPlayer();
                }
                return;
            }
            if (currentTarget == NOT_SELECTED_REP) {  // 未选球
                if (isBreaking) {  // 开球进袋不算选球
                    System.out.println("开球进球不选球");
                    return;
                }
                if (allFullBalls(pottedBalls)) {
                    currentPlayer.setBallRange(FULL_BALL_REP);
                    getAnotherPlayer().setBallRange(HALF_BALL_REP);
                    currentTarget = getTargetOfPlayer(currentPlayer);
                    lastPotSuccess = true;
                    currentPlayer.correctPotBalls(fullBallsOf(pottedBalls));
                }
                if (allHalfBalls(pottedBalls)) {
                    currentPlayer.setBallRange(HALF_BALL_REP);
                    getAnotherPlayer().setBallRange(FULL_BALL_REP);
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
        currentTarget = getTargetOfPlayer(currentPlayer);  // 在switchPlayer之后
    }

    @Override
    protected void updateTargetPotSuccess(boolean isSnookerFreeBall) {

    }

    @Override
    protected void updateTargetPotFailed() {

    }
}
