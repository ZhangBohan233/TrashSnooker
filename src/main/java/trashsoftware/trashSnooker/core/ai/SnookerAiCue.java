package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;

public class SnookerAiCue extends AiCue<AbstractSnookerGame, SnookerPlayer> {

    protected static final double ALIVE_THRESHOLD = 10.0;
    private final Map<Ball, Double> selfBallAlivePrices = new HashMap<>();
    private int allRedCount;
    private int aliveRedCount;

    public SnookerAiCue(AbstractSnookerGame game, SnookerPlayer aiPlayer) {
        super(game, aiPlayer);
        
        makeAliveMap();
    }

    @Override
    protected boolean supportAttackWithDefense(int targetRep) {
        return targetRep != AbstractSnookerGame.RAW_COLORED_REP;
    }

    private void makeAliveMap() {
        for (Ball ball : game.getAllBalls()) {
            if (ball.isRed() && !ball.isPotted()) {
                allRedCount++;
                double aliveScore = ballAlivePrice(ball);
                if (aliveScore > ALIVE_THRESHOLD) aliveRedCount++;
                selfBallAlivePrices.put(ball, aliveScore);
            }
        }
    }

    @Override
    protected double priceOfKick(Ball kickedBall, double kickSpeed) {
        if (aliveRedCount >= 2) return KICK_USELESS_BALL_MUL;  // 剩的多，不急着k
        
        Double alivePrice = selfBallAlivePrices.get(kickedBall);
        if (alivePrice == null) return KICK_USELESS_BALL_MUL;

        double speedThreshold = Values.MAX_POWER_SPEED / 8.0;
        double speedMul;
        if (kickSpeed > speedThreshold * 2) speedMul = 1.5;
        else if (kickSpeed > speedThreshold) speedMul = 1.0;
        else speedMul = 0.5;

        if (alivePrice == 0) return 2.0 * speedMul;
        double kickPriority = 20.0 / alivePrice;

        return Math.max(0.5, speedMul * Math.min(2.0, kickPriority));
    }

    @Override
    protected DefenseChoice breakCue(Phy phy) {
        AiPlayStyle.SnookerBreakMethod method =
                aiPlayer.getPlayerPerson().getAiPlayStyle().snookerBreakMethod;

        if (method == AiPlayStyle.SnookerBreakMethod.BACK) return backBreak(phy);
        boolean leftBreak = method == AiPlayStyle.SnookerBreakMethod.LEFT;

        double aimingPosX = game.firstRedX() +
                game.getGameValues().ball.ballDiameter * game.redRowOccupyX * 4.0;
        double yOffset = (game.getGameValues().ball.ballDiameter + game.redGapDt * 0.6) * 6.90;
        if (leftBreak) yOffset = -yOffset;
        double aimingPosY = game.getGameValues().table.midY + yOffset;

        double dirX = aimingPosX - game.getCueBall().getX();
        double dirY = aimingPosY - game.getCueBall().getY();
        double[] unitXY = Algebra.unitVector(dirX, dirY);
        double actualPower = 27.0 * phy.cloth.smoothness.speedReduceFactor / TableCloth.Smoothness.NORMAL.speedReduceFactor;
        double selectedSideSpin = leftBreak ? -0.6 : 0.6;
        double actualSideSpin = CuePlayParams.unitSideSpin(selectedSideSpin,
                aiPlayer.getInGamePlayer().getCurrentCue(game));
        System.out.println(actualSideSpin + " " + Util.powerMultiplierOfCuePoint(0.6, 0));
        double selectedPower = actualPowerToSelectedPower(
                actualPower / 100 * phy.cloth.smoothness.speedReduceFactor,
                actualSideSpin,  // fixme
                0,
                aiPlayer.getPlayerPerson().handBody.getPrimary()
        );

        double[] correctedXY = CuePlayParams.aimingUnitXYIfSpin(
                actualSideSpin,
                actualPower,
                unitXY[0],
                unitXY[1]
        );
        CuePlayParams cpp = CuePlayParams.makeIdealParams(
                correctedXY[0],
                correctedXY[1],
                0.0,
                actualSideSpin,
                0.0,
                actualPower
        );
//        System.out.println(Arrays.toString(unitXY) + Arrays.toString(correctedXY));
        return new DefenseChoice(correctedXY, selectedPower, selectedSideSpin, cpp,
                aiPlayer.getPlayerPerson().handBody.getPrimary());
    }

    private DefenseChoice backBreak(Phy phy) {
        return null;
    }

    public boolean considerReposition(Phy phy, 
                                      Map<SnookerBall, double[]> lastPositions, 
                                      PotAttempt opponentAttempt) {
        if (opponentAttempt != null) return false;  // 对手都在进攻，你还敢复位？
        
        IntegratedAttackChoice attackChoice = standardAttack(phy, ATTACK_DIFFICULTY_THRESHOLD / 2);
        return attackChoice == null;  // 先就这样吧，暂时不考虑更好的防一杆
    }

    @Override
    protected boolean requireHitCushion() {
        return false;
    }

    @Override
    protected DefenseChoice standardDefense() {
        return null;
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        int behind = -game.getScoreDiff(aiPlayer);
        int rem = game.getRemainingScore();
        int currentTarget = game.getCurrentTarget();
        if (behind > rem) {  // 被超分
            int defaultFoul = game.getDefaultFoulValue();
            int withdrawLimit = aiPlayer.getPlayerPerson().getAiPlayStyle().snookerWithdrawLimit;
            if (behind > rem + defaultFoul * (withdrawLimit + 1)) {
                // 超太多了，认输
                return null;
            } else {
                if (rem == 7) return null;  // 只剩一颗球还防个屁
                else if (rem <= 27 && behind > rem + defaultFoul * withdrawLimit) return null; // 清彩阶段，落后多了就认输
            }
            // 其他情况还可以挣扎
            if (currentTarget == 1) {
                if (game.remainingRedCount() == 1) {
                    System.out.println("Last red, make snooker");
                    DefenseChoice def = getBestDefenseChoice(phy);
                    if (def != null) return makeDefenseCue(def, AiCueResult.CueType.DEFENSE);
                }
            } else if (game.getCurrentTarget() != AbstractSnookerGame.RAW_COLORED_REP) {
                System.out.println("Ordered colors, make snooker");
                DefenseChoice def = getBestDefenseChoice(phy);
                if (def != null) return makeDefenseCue(def, AiCueResult.CueType.DEFENSE);
            }
        }
        if (-behind > rem + 7 && currentTarget == 7) {
            IntegratedAttackChoice exhibition = lastExhibitionShot(phy);
            if (exhibition != null) {
                return makeAttackCue(exhibition);
            }
        }
        return regularCueDecision(phy);
    }

    private IntegratedAttackChoice lastExhibitionShot(Phy phy) {
        List<AttackChoice> choiceList = getCurrentAttackChoices(99999999);
        if (choiceList.isEmpty()) {
            System.out.println("Cannot exhibit!");
            return null;
        }
        System.out.println("Exhibition shot!");

        AttackChoice choice = choiceList.get(0);
        for (AttackChoice ac : choiceList.subList(1, choiceList.size())) {
            if (ac.defaultRef.price > choice.defaultRef.price) {
                choice = ac;
            }
        }

        GameValues values = game.getGameValues();
        
        // 刚好推进的白球球速
        double minWhiteSpeed = values.estimateSpeedNeeded(phy,
                choice.targetHoleDistance + 
                        (choice.whiteCollisionDistance / (1 - Ball.MAX_GEAR_EFFECT)) + values.ball.ballDiameter * 1.5);
        double minActualPower = minWhiteSpeed / Values.MAX_POWER_SPEED * 100;

        Random random = new Random();
        boolean isSmallPower = random.nextDouble() < 0.25;
        PlayerPerson pp = aiPlayer.getPlayerPerson();
        double[] spin;
        double power;
        if (isSmallPower) {  // 是否是小力轻推
            System.out.println("Small power!");
            Ball cueBall = game.getCueBall();
            spin = SPIN_POINTS[0];
            power = actualPowerToSelectedPower(
                    minActualPower,
                    spin[0],
                    spin[1],
                    CuePlayParams.getPlayableHand(
                            cueBall.getX(),
                            cueBall.getY(),
                            choice.cueDirectionUnitVector[0],
                            choice.cueDirectionUnitVector[1],
                            game.getGameValues().table,
                            pp
                    )
            );
        } else {
            System.out.println("Big power!");
            int index = random.nextInt(SPIN_POINTS.length);
            spin = SPIN_POINTS[index];
            double powerLow = pp.getControllablePowerPercentage() * 0.7;
            double interval = pp.getControllablePowerPercentage() - powerLow;
            power = random.nextDouble() * interval + powerLow;
        }

        AttackParam attackParam = new AttackParam(
                choice,
                game,
                aiPlayer,
                power,
                spin[0],
                spin[1]
        );
        AttackThread at = new AttackThread(
                attackParam,
                game.getGameValues(),
                AbstractSnookerGame.END_REP,
                new ArrayList<>(),
                phy,
                GamePlayStage.NO_PRESSURE,
                ATTACK_DIFFICULTY_THRESHOLD
        );
        at.run();
        return at.result;
    }

}
