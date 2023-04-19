package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.phy.Phy;

public abstract class ObjectOnTable {
    protected static final double GENERAL_BOUNCE_ACC = 0.2;
    protected final GameValues values;
    protected final TableMetrics table;
    protected final double radius;
    protected double distance;
    protected double x, y;
    protected double nextX, nextY;
    protected double vx, vy;  // unit: mm/(sec/frameRate)

    protected Bounce currentBounce;

    public ObjectOnTable(GameValues values, double radius) {
        this.values = values;
        this.table = values.table;
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setXY(double x, double y) {
        setX(x);
        setY(y);
    }

    public void setVx(double vx) {
        this.vx = vx;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }

    protected double getSpeed() {
        return Math.hypot(vx, vy);
    }

    protected double getDistanceMoved() {
        return distance;
    }

    protected void prepareMove(Phy phy) {
        nextX = x + vx;
        nextY = y + vy;
    }

    protected abstract void normalMove(Phy phy);

    protected double currentDtTo(Ball ball) {
        return Algebra.distanceToPoint(x, y, ball.x, ball.y);
    }

    protected double predictedDtTo(Ball ball) {
        return Algebra.distanceToPoint(nextX, nextY, ball.nextX, ball.nextY);
    }

    protected double currentDtToLine(double[][] line) {
        return Algebra.distanceToLine(x, y, line[0], line[1]);
    }

    protected double predictedDtToLine(double[][] line) {
        return Algebra.distanceToLine(nextX, nextY, line[0], line[1]);
    }

    protected double currentDtToPoint(double[] point) {
        return Algebra.distanceToPoint(x, y, point[0], point[1]);
    }

    protected double predictedDtToPoint(double[] point) {
        return predictedDtToPoint(point[0], point[1]);
    }

    protected double predictedDtToPoint(double px, double py) {
        return Algebra.distanceToPoint(nextX, nextY, px, py);
    }

    protected double midHolePowerFactor(Phy phy) {
        return 1;
    }

    protected void clearBounceDesiredLeavePos() {
        if (currentBounce != null) {
            System.out.println("Cleared!");
            currentBounce.clearDesireLeavePos();
        }
    }

    protected void processBounce(boolean print) {
        currentBounce.oneFrame();

        if (currentBounce.isLeaving(x, y)) {
            if (print)
                System.out.println("Bounce lasts for " + currentBounce.framesCount + " frames");
            currentBounce.leave();
            currentBounce = null;
        }
    }

    protected boolean willPot(Phy phy) {
        if (x < table.leftClothX || 
                x >= table.rightClothX || 
                y < table.topClothY || 
                y >= table.botClothY) {
            // 出台了
            return true;
        }
        
        double cornerRoom = table.cornerHoleRadius - values.ball.ballRadius;
        double midRoom = table.midHoleRadius - values.ball.ballRadius;

        return predictedDtToPoint(table.topLeftHoleXY) < cornerRoom ||
                predictedDtToPoint(table.botLeftHoleXY) < cornerRoom ||
                predictedDtToPoint(table.topRightHoleXY) < cornerRoom ||
                predictedDtToPoint(table.botRightHoleXY) < cornerRoom ||
                predictedDtToPoint(table.topMidHoleXY) < midRoom ||
                predictedDtToPoint(table.botMidHoleXY) < midRoom;
    }

    protected double[] hitHoleArcArea(double[] arcXY, Phy phy, double arcRadius) {
        double axisX = arcXY[0] - x;  // 切线的法向量
        double axisY = arcXY[1] - y;
//        double[] reflect = Algebra.symmetricVector(vx, vy, axisX, axisY);
//        vx = -reflect[0];
//        vy = -reflect[1];
        if (currentBounce != null) {
            System.err.println("Current is bouncing!");
        }

//        double[] normal = new double[]{axisX, axisY};
//        double[] unitNormal = Algebra.unitVector(normal);
//        double verticalSpeed = Algebra.projectionLengthOn(normal, new double[]{vx, vy});

//        double nFrames = 2.0 / GENERAL_BOUNCE_ACC / phy.cloth.smoothness.cushionBounceFactor;

        double speed = Math.hypot(vx, vy);
        currentBounce = new ArcBounce(
                arcXY, 
                speed * GENERAL_BOUNCE_ACC * phy.cloth.smoothness.cushionBounceFactor,
                speed * table.wallBounceRatio
        );
//        currentBounce = new Bounce(
//                -unitNormal[0] * verticalSpeed * phy.cloth.smoothness.cushionBounceFactor * GENERAL_BOUNCE_ACC,
//                -unitNormal[1] * verticalSpeed * phy.cloth.smoothness.cushionBounceFactor * GENERAL_BOUNCE_ACC);
//        if (!phy.isPrediction) {
//            System.out.println("Acc: " + currentBounce.accX + " " + currentBounce.accY);
//        }

        return new double[]{axisX, axisY};  // 返回切线的法向量
    }

    protected void hitHoleLineArea(double[] lineNormalVec, Phy phy) {
//        double[] reflect = Algebra.symmetricVector(vx, vy, lineNormalVec[0], lineNormalVec[1]);
//        vx = -reflect[0];
//        vy = -reflect[1];

        double[] vv = new double[]{vx, vy};
        
        double[] unitNormal = Algebra.unitVector(lineNormalVec);
        double verticalSpeed = Algebra.projectionLengthOn(unitNormal, vv);
//        double horizontalSpeed = Algebra.projectionLengthOn(new double[]{unitNormal[1], -unitNormal[0]}, vv);
//        double nFrames = 2.0 / GENERAL_BOUNCE_ACC / phy.cloth.smoothness.cushionBounceFactor;

        currentBounce = new LineBounce(
                -unitNormal[0] * verticalSpeed * phy.cloth.smoothness.cushionBounceFactor * GENERAL_BOUNCE_ACC,
                -unitNormal[1] * verticalSpeed * phy.cloth.smoothness.cushionBounceFactor * GENERAL_BOUNCE_ACC,
                Math.hypot(vx, vy) * table.wallBounceRatio
        );
//        if (!phy.isPrediction) {
//            System.out.println("Acc: " + currentBounce.accX + " " + currentBounce.accY);
//        }
    }

    protected void tryEnterGravityArea(Phy phy, double[] holeXy, boolean isMidHole) {
        double xDiff = holeXy[0] - nextX;
        double yDiff = holeXy[1] - nextY;
        double dt = Math.hypot(xDiff, yDiff);

        double holeRadius = isMidHole ? table.midHoleRadius : table.cornerHoleRadius;
        double holeAndSlopeRadius = holeRadius + table.holeGravityAreaWidth;

        if (dt < holeAndSlopeRadius) {
            // dt应该不会小于 holeRadius - ballRadius 太多
            double accMag;
            if (dt < holeRadius) {
                accMag = 1;
            } else {
                accMag = (table.holeGravityAreaWidth - dt + holeRadius) / table.holeGravityAreaWidth;
            }

            accMag *= 4800;
            accMag /= phy.calculationsPerSecSqr;

            double[] accVec = Algebra.unitVector(xDiff, yDiff);
            accVec[0] *= accMag;
            accVec[1] *= accMag;

            vx += accVec[0];
            vy += accVec[1];
        }
    }

    /**
     * 检测是否撞击袋角或进入袋角区域。
     * 如果处于弹性中，返回{@code 3}。
     * 如果撞击袋角，返回{@code 2}且处理撞击。
     * 如果进入袋角区域但未发生撞击，返回{@code 1}。
     * 如未进入，返回{@code 0}
     */
    protected int tryHitHoleArea(Phy phy) {
//        if (currentBounce != null && currentBounce.isHoleArea()) {
//            processBounce();
//            normalMove(phy);
//            prepareMove(phy);
//            return 3;
//        }

        if (nextY < radius + table.topY) {
            if (nextX < table.midHoleAreaRightX && nextX >= table.midHoleAreaLeftX) {
                // 上方中袋在袋角范围内
                if (predictedDtToPoint(table.topMidHoleLeftArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.topMidHoleLeftArcXy) >= table.midArcRadius + radius) {
                    // 击中上方中袋左侧
                    hitHoleArcArea(table.topMidHoleLeftArcXy, phy, table.midArcRadius);
                } else if (predictedDtToPoint(table.topMidHoleRightArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.topMidHoleRightArcXy) >= table.midArcRadius + radius) {
                    // 击中上方中袋右侧
                    hitHoleArcArea(table.topMidHoleRightArcXy, phy, table.midArcRadius);
                } else if (table.isStraightHole() &&
                        nextX >= table.midHoleLineLeftX && nextX < table.midHoleLineRightX) {
                    // 疑似上方中袋直线
                    double[][] line = table.topMidHoleLeftLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                                phy);
                        return 2;
                    }
                    line = table.topMidHoleRightLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                                phy);
                        return 2;
                    }

                    tryEnterGravityArea(phy, table.topMidHoleXY, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return 1;
                } else {

                    tryEnterGravityArea(phy, table.topMidHoleXY, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return 1;
                }
                return 2;
            }
        } else if (nextY >= table.botY - radius) {
            if (nextX < table.midHoleAreaRightX && nextX >= table.midHoleAreaLeftX) {
                // 下方中袋袋角范围内
                if (predictedDtToPoint(table.botMidHoleLeftArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.botMidHoleLeftArcXy) >= table.midArcRadius + radius) {
                    // 击中下方中袋左侧
                    hitHoleArcArea(table.botMidHoleLeftArcXy, phy, table.midArcRadius);
                } else if (predictedDtToPoint(table.botMidHoleRightArcXy) < table.midArcRadius + radius &&
                        currentDtToPoint(table.botMidHoleRightArcXy) >= table.midArcRadius + radius) {
                    // 击中下方中袋右侧
                    hitHoleArcArea(table.botMidHoleRightArcXy, phy, table.midArcRadius);
                } else if (table.isStraightHole() &&
                        nextX >= table.midHoleLineLeftX && nextX < table.midHoleLineRightX) {
                    // 疑似下方中袋直线
                    double[][] line = table.botMidHoleLeftLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                                phy);
                        return 2;
                    }
                    line = table.botMidHoleRightLine;
                    if (predictedDtToLine(line) < radius &&
                            currentDtToLine(line) >= radius) {
                        hitHoleLineArea(
                                Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                                phy);
                        return 2;
                    }

                    tryEnterGravityArea(phy, table.botMidHoleXY, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return 1;
                } else {

                    tryEnterGravityArea(phy, table.botMidHoleXY, true);
                    normalMove(phy);
                    prepareMove(phy);
                    return 1;
                }
                return 2;
            }
        }

        // 底袋
        double[] probHole = null;
        if (nextY < table.topCornerHoleAreaDownY) {
            if (nextX < table.leftCornerHoleAreaRightX) probHole = table.topLeftHoleXY;  // 左上底袋
            else if (nextX >= table.rightCornerHoleAreaLeftX)
                probHole = table.topRightHoleXY;  // 右上底袋
        } else if (nextY >= table.botCornerHoleAreaUpY) {
            if (nextX < table.leftCornerHoleAreaRightX) probHole = table.botLeftHoleXY;  // 左下底袋
            else if (nextX >= table.rightCornerHoleAreaLeftX)
                probHole = table.botRightHoleXY;  // 右下底袋
        }

        if (probHole != null) {
            for (int i = 0; i < table.allCornerLines.length; ++i) {
                double[][] line = table.allCornerLines[i];

                if (predictedDtToLine(line) < radius && currentDtToLine(line) >= radius) {
                    hitHoleLineArea(
                            Algebra.normalVector(new double[]{line[0][0] - line[1][0], line[0][1] - line[1][1]}),
                            phy);
                    return 2;
                }
            }
            if (!table.isStraightHole()) {
                for (double[] cornerArc : table.allCornerArcs) {
                    if (predictedDtToPoint(cornerArc) < table.cornerArcRadius + radius &&
                            currentDtToPoint(cornerArc) >= table.cornerArcRadius + radius) {
                        hitHoleArcArea(cornerArc, phy, table.cornerArcRadius);
                        return 2;
                    }
                }
            }

            tryEnterGravityArea(phy, probHole, false);
            normalMove(phy);
            prepareMove(phy);
            return 1;
        }
        return 0;
    }

    abstract class Bounce {

//        /*
//        1: 上边库 
//        2: 下边库 
//        3: 左底库 
//        4: 右底库 
//        5: 袋角弧线 
//        6: 袋角直线
//        */
//        int scenario;
//
//        // 仅有情况5时需要

//        double holeArcRadius;

        int framesCount = 0;

        abstract void oneFrame();

//        boolean isHoleArea() {
//            return scenario == 5 || scenario == 6;
//        }

        boolean isLeaving(double curX, double curY) {
            return framesCount > 30 || !values.isInTable(curX, curY, values.ball.ballRadius) &&
                    values.isInTable(curX + vx, curY + vy, values.ball.ballRadius);
//            double ballRadius = values.ball.ballRadius;
//            switch (scenario) {
//                case 1:
//                    return nextY >= values.table.topY + ballRadius;
//                case 2:
//                    return nextY < values.table.botY - ballRadius;
//                case 3:
//                    return nextX >= values.table.leftX + ballRadius;
//                case 4:
//                    return nextX < values.table.rightX - ballRadius;
//                case 5:
//                    return Algebra.distanceToPoint(
//                            nextX, 
//                            nextY, 
//                            holeArcCenter[0], 
//                            holeArcCenter[1]) >= holeArcRadius + ballRadius;
//                case 6:
//                default:
//                    throw new RuntimeException();
//            }
        }

        abstract void leave();

        void clearDesireLeavePos() {
        }
    }

    class CushionBounce extends Bounce {
        double accX, accY;  // 反弹力的加速度
        // 如果一切顺利，会在什么地方离开库
        double desiredX;
        double desiredY;
        double desiredVx;
        double desiredVy;

        CushionBounce(double accX, double accY) {
            this.accX = accX;
            this.accY = accY;
        }

        @Override
        void oneFrame() {
            vx += accX;
            vy += accY;

            currentBounce.framesCount++;
        }

        @Override
        void leave() {
            if (desiredX != 0) {
                x = desiredX;
                y = desiredY;
                vx = desiredVx;
                vy = desiredVy;
            }
        }

        @Override
        void clearDesireLeavePos() {
            setDesiredLeavePos(0, 0, 0, 0);
        }

        void setDesiredLeavePos(double desiredX, double desiredY,
                                double desiredVx, double desiredVy) {
            this.desiredX = desiredX;
            this.desiredY = desiredY;
            this.desiredVx = desiredVx;
            this.desiredVy = desiredVy;
        }
    }
    
    class LineBounce extends CushionBounce {
        double desiredLeaveSpeed;
        
        LineBounce(double accX, double accY, double desiredLeaveSpeed) {
            super(accX, accY);
            
            this.desiredLeaveSpeed = desiredLeaveSpeed;
        }

        @Override
        void leave() {
            if (desiredLeaveSpeed != 0) {
//                System.out.println("speed ratio: " + Math.hypot(vx, vy) / desiredLeaveSpeed / table.wallBounceRatio);
                double speed = Math.hypot(vx, vy);
                double ratio = speed / desiredLeaveSpeed;
                vx /= ratio;
                vy /= ratio;
            }
        }

        @Override
        void clearDesireLeavePos() {
            desiredLeaveSpeed = 0.0;
        }
    }

    class ArcBounce extends Bounce {
        double[] holeArcCenter;
        double verticalSpeed;
        double desiredLeaveSpeed;

        ArcBounce(double[] arcCenter, double verticalSpeed, double desiredLeaveSpeed) {
            this.holeArcCenter = arcCenter;
            this.verticalSpeed = verticalSpeed;
            this.desiredLeaveSpeed = desiredLeaveSpeed;
        }

        @Override
        void oneFrame() {
            // 每一帧都得更新加速方向
            // 加速方向是球当前位置与圆心的连线
            // 此处假设球永远砸不到圆的半径那么深
            double[] unitAcc = Algebra.unitVector(x - holeArcCenter[0], y - holeArcCenter[1]);
            
            double accX = unitAcc[0] * verticalSpeed;
            double accY = unitAcc[1] * verticalSpeed;
            vx += accX;
            vy += accY;
            
            framesCount++;
        }

        @Override
        void leave() {
            if (desiredLeaveSpeed != 0) {
//                System.out.println("speed ratio: " + Math.hypot(vx, vy) / desiredLeaveSpeed / table.wallBounceRatio);
                double speed = Math.hypot(vx, vy);
                double ratio = speed / desiredLeaveSpeed;
                vx /= ratio;
                vy /= ratio;
            }
        }

        @Override
        void clearDesireLeavePos() {
            this.desiredLeaveSpeed = 0.0;
        }
    }
}
