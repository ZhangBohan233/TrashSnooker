package trashsoftware.trashSnooker.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import trashsoftware.trashSnooker.fxml.GameView;

public abstract class NumberedBallGame extends Game {

    NumberedBallGame(GameView parent, GameSettings gameSettings, GameValues gameValues) {
        super(parent, gameSettings, gameValues);
    }

    protected abstract double breakPointX();

    protected double breakLineX() {
        return gameValues.leftX + 635.0;
    }

    @Override
    protected Ball createWhiteBall() {
        return new PoolBall(0, true, gameValues);
    }

    @Override
    public void drawTableMarks(GraphicsContext graphicsContext, double scale) {
        // 开球线
        double breakLineX = parent.canvasX(breakLineX());
        graphicsContext.setStroke(GameView.WHITE);
        graphicsContext.strokeLine(
                breakLineX,
                parent.canvasY(gameValues.topY),
                breakLineX,
                parent.canvasY(gameValues.topY + gameValues.innerHeight));
    }

    @Override
    protected void drawBall(Ball ball, GraphicsContext graphicsContext, double scale) {
        if (ball.isPotted()) return;
        drawPoolBallEssential(
                parent.canvasX(ball.getX()),
                parent.canvasY(ball.getY()),
                gameValues.ballDiameter * scale,
                ball.getColor(),
                ball.getValue(),
                graphicsContext);
    }

    public static void drawPoolBallEssential(
            double canvasX,
            double canvasY,
            double canvasBallDiameter,
            Color baseColor,
            int ballNumber,
            GraphicsContext graphicsContext) {

        drawBallBase(canvasX, canvasY, canvasBallDiameter, baseColor, graphicsContext,
                ballNumber >= 9 && ballNumber != 16);

        if (ballNumber == 0) return;  // 母球
        if (ballNumber > 15) return;  // 仅用于表示目标球

        // 号码区域
        double textAreaRadius = canvasBallDiameter * 0.25;
        graphicsContext.setFill(Values.WHITE);
        graphicsContext.fillOval(
                canvasX - textAreaRadius,
                canvasY - textAreaRadius,
                textAreaRadius * 2,
                textAreaRadius * 2);

        // 号码
        graphicsContext.setFont(GameView.POOL_NUMBER_FONT);
        double textDown = GameView.POOL_NUMBER_FONT.getSize() * 0.36;
        graphicsContext.setFill(GameView.BLACK);
        graphicsContext.fillText(
                String.valueOf(ballNumber),
                canvasX,
                canvasY + textDown);
    }
}
