package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.table.AbstractSnookerTable;
import trashsoftware.trashSnooker.core.table.MiniSnookerTable;
import trashsoftware.trashSnooker.fxml.GameView;

public class MiniSnookerGame extends AbstractSnookerGame {

    public MiniSnookerGame(EntireGame entireGame,
                           GameSettings gameSettings,
                           GameValues gameValues,
                           int frameIndex) {
        super(entireGame, gameSettings, gameValues, new MiniSnookerTable(gameValues.table), frameIndex);
    }

    @Override
    public AbstractSnookerTable getTable() {
        return (AbstractSnookerTable) super.getTable();
    }

    @Override
    public GameRule getGameType() {
        return GameRule.MINI_SNOOKER;
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        if (isBreaking) {
            return super.canPlaceWhiteInTable(x, y);
        } else {
            return !isOccupied(x, y);
        }
    }

    @Override
    protected int numRedBalls() {
        return 6;
    }
}
