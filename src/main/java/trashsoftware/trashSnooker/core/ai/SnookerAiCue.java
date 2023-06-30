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
    protected double priceOfKick(Ball kickedBall, double kickSpeed, double dtFromFirst) {
        if (aliveRedCount >= 2) return kickUselessBallPrice(dtFromFirst);  // 剩的多，不急着k
        
        Double alivePrice = selfBallAlivePrices.get(kickedBall);
        if (alivePrice == null) return kickUselessBallPrice(dtFromFirst);

        double speedThreshold = Values.BEST_KICK_SPEED;
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
        return predictiveRegularBreak(phy, leftBreak);
    }
    
    private DefenseChoice predictiveRegularBreak(Phy phy, boolean leftBreak) {
        double sign = leftBreak ? -1 : 1;
        
        double whiteX = game.getCueBall().getX();
        double whiteY = game.getCueBall().getY();
        
        double[] cornerBallPos = leftBreak ? game.getCornerRedBallPosGreenSide() : game.getCornerRedBallPosYellowSide();
        double thinY = cornerBallPos[1] + sign * game.getGameValues().ball.ballDiameter * 2.0;  // 主要有个加塞瞄点偏移的事情
        double[] thinVec = Algebra.unitVector(cornerBallPos[0] - whiteX, thinY - whiteY);
        double thickY = cornerBallPos[1];
        double[] thickVec = Algebra.unitVector(cornerBallPos[0] - whiteX, thickY - whiteY);
        
        double beginDeg = Math.toDegrees(Algebra.thetaOf(thinVec));
        int nTicks = 20;
        double totalAng = Math.toDegrees(Algebra.thetaBetweenVectors(thickVec, thinVec));
        double tickDeg = totalAng / nTicks * -sign;
        
        double selectedSideSpin = 0.6 * sign;
//        double actualSideSpin = CuePlayParams.unitSideSpin(selectedSideSpin,
//                aiPlayer.getInGamePlayer().getCurrentCue(game));
        
        List<Ball> legalList = game.getAllLegalBalls(1, false, false);
        Set<Ball> legalSet = new HashSet<>(legalList);
        
        double clothSlowFactor = phy.cloth.smoothness.speedReduceFactor / TableCloth.Smoothness.FAST.speedReduceFactor;
        double selPowerLow = 20.0 * clothSlowFactor;
        double selPowerHigh = 38.0 * clothSlowFactor;
        double selPowerTick = 2.0;
        
        double allowedYLow = game.getTable().greenBallPos()[1];
        double allowedYHigh = game.getTable().yellowBallPos()[1];
        double allowedX = game.getTable().breakLineX();
        
        Set<Ball> suggestedTarget = game.getSuggestedRegularBreakBalls();
        PlayerPerson.HandSkill handSkill = aiPlayer.getPlayerPerson().handBody.getPrimary();
        List<DefenseChoice> legalChoices = new ArrayList<>();
        for (double deg = beginDeg, i = 0; i < nTicks; deg += tickDeg, i++) {
            double rad = Math.toRadians(deg);
            double[] unitXy = Algebra.unitVectorOfAngle(rad);
            for (double selectedPower = selPowerLow; selectedPower <= selPowerHigh; selectedPower += selPowerTick) {
                CueParams cueParams = CueParams.createBySelected(
                        selectedPower,
                        0.0,
                        selectedSideSpin,
                        game,
                        aiPlayer.getInGamePlayer(),
                        handSkill
                );

                CuePlayParams cpp = CuePlayParams.makeIdealParams(
                        unitXy[0],
                        unitXy[1],
                        cueParams,
                        0.0
                );
                DefenseChoice dc = analyseDefense(
                        this,
                        cpp,
                        cueParams,
                        phy,
                        game,
                        legalSet,
                        aiPlayer,
                        unitXy,
                        false,
                        0.0,
                        false
                );
                if (dc != null) {
                    double[] whiteStopPos = dc.wp.stopPoint();
                    if (whiteStopPos == null) continue;
                    if (whiteStopPos[1] < allowedYLow || 
                            whiteStopPos[1] > allowedYHigh || 
                            whiteStopPos[0] > allowedX) continue;
                    
                    if (dc.wp.getWhiteCushionCountAfter() == 4
                            && !dc.wp.isWhiteHitsHoleArcs()
                            && !dc.wp.isHitWallBeforeHitBall()
                            && dc.wp.getSecondCollide() == null 
                            && suggestedTarget.contains(dc.wp.getFirstCollide())) {
                        legalChoices.add(dc);
                    }
                }
            }
        }
        if (legalChoices.isEmpty()) {
            System.out.println("Cannot find break");
            return null;
        }
        Collections.sort(legalChoices);
        Collections.reverse(legalChoices);
        System.out.println("Break sel power " + legalChoices.get(0).cueParams.selectedPower());
        return legalChoices.get(0);
    }

    private DefenseChoice backBreak(Phy phy) {
        return null;
    }

    public boolean considerReposition(Phy phy, 
                                      Map<SnookerBall, double[]> lastPositions, 
                                      PotAttempt opponentAttempt,
                                      boolean isFreeBall) {
        if (opponentAttempt != null && !isFreeBall) {
            // 对手都在进攻，你还敢复位？
            // 例外: 如果AI要打自由球，那就不直接放弃复位，因为我们无法避免AI打自由球直接贴球防守这种犯规打法
            return false;
        }
        
        IntegratedAttackChoice attackChoice = standardAttack(phy, false);
        return attackChoice == null || !attackChoice.isPureAttack;  // 先就这样吧，暂时不考虑更好的防一杆
    }

    @Override
    protected DefenseChoice standardDefense() {
        return null;
    }

    @Override
    public AiCueResult makeCue(Phy phy) {
        int behind = -game.getScoreDiff(aiPlayer);
        int rem = game.getRemainingScore(game.isDoingSnookerFreeBll());
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
            } else if (currentTarget != AbstractSnookerGame.RAW_COLORED_REP) {
                System.out.println("Ordered colors, make snooker");
                DefenseChoice def = getBestDefenseChoice(phy);
                if (def != null) return makeDefenseCue(def, AiCueResult.CueType.DEFENSE);
            } else {
                System.out.println("Being over score and target is colored, must attack");
                IntegratedAttackChoice iac = standardAttack(phy, true);
                if (iac != null) {
                    return makeAttackCue(iac);
                }
            }
        } else if (currentTarget == AbstractSnookerGame.RAW_COLORED_REP && rem - 7 <= behind) {
            System.out.println("Near being over score and target is colored, must attack");
            IntegratedAttackChoice iac = standardAttack(phy, true);
            if (iac != null) {
                return makeAttackCue(iac);
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
        List<AttackChoice> choiceList = getCurrentAttackChoices();
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
        Ball cueBall = game.getCueBall();
        CueParams cueParams;
//        double[] spin;
//        double power;
        if (isSmallPower) {  // 是否是小力轻推
            System.out.println("Small power!");
            cueParams = CueParams.createByActual(
                    minActualPower,
                    0.0,
                    0.0,
                    game,
                    aiPlayer.getInGamePlayer(),
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
            double[] spin = SPIN_POINTS[index];
            double powerLow = pp.getControllablePowerPercentage() * 0.7;
            double interval = pp.getControllablePowerPercentage() - powerLow;
            cueParams = CueParams.createBySelected(
                    random.nextDouble() * interval + powerLow,
                    spin[0],
                    spin[1],
                    game,
                    aiPlayer.getInGamePlayer(),
                    CuePlayParams.getPlayableHand(
                            cueBall.getX(),
                            cueBall.getY(),
                            choice.cueDirectionUnitVector[0],
                            choice.cueDirectionUnitVector[1],
                            game.getGameValues().table,
                            pp
                    )
            );
        }

        AttackParam attackParam = new AttackParam(
                choice,
                game,
                phy,
                cueParams
        );
        AttackThread at = new AttackThread(
                attackParam,
                game.getGameValues(),
                AbstractSnookerGame.END_REP,
                new ArrayList<>(),
                phy,
                GamePlayStage.NO_PRESSURE
        );
        at.run();
        return at.result;
    }

}
