package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.fxml.GameView;

public class MiniSnookerGame extends AbstractSnookerGame {

    MiniSnookerGame(GameView parent, GameSettings gameSettings) {
        super(parent, gameSettings, GameValues.MINI_SNOOKER_VALUES);
    }

    @Override
    protected double breakLineX() {
        return gameValues.leftX + 635.0;
    }

    @Override
    protected double breakArcRadius() {
        return 219.0;  // todo: 存疑
    }

    @Override
    protected double[] blackBallPos() {
        return new double[]{gameValues.rightX - 243.0, gameValues.midY};  // todo: 存疑
    }
}
