package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.core.BreakRule;
import trashsoftware.trashSnooker.core.Cue;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

/**
 * 一个比赛类型。
 * <p>
 * 一文厘清这几个的关系:
 * GameType指这个游系，如斯诺克，与GameValues其实是完全对应的，只是因为初期设计原因分开了。
 *
 * @see TableMetrics 已经说了
 * @see trashsoftware.trashSnooker.core.table.Table 与GameType还是一一对应的，只不过主要功能是绘图
 * @see EntireGame 一场比赛的实例
 * @see Game 一局游戏的实例
 */
public enum GameRule {
    SNOOKER(22, "Snooker",
            new Cue.Size[]{Cue.Size.VERY_SMALL, Cue.Size.SMALL},
            new TrainType[]{TrainType.SNAKE_FULL, TrainType.SNAKE_FULL_DENSE,
                    TrainType.SNAKE_HALF, TrainType.SNAKE_CROSS, TrainType.SNAKE_X},
            BreakRule.ALTERNATE) {
        @Override
        public boolean snookerLike() {
            return true;
        }
    },
    MINI_SNOOKER(13, "MiniSnooker",
            new Cue.Size[]{Cue.Size.VERY_SMALL, Cue.Size.SMALL},
            new TrainType[]{TrainType.SNAKE_FULL, TrainType.SNAKE_HALF, TrainType.SNAKE_CROSS},
            BreakRule.ALTERNATE) {
        @Override
        public boolean snookerLike() {
            return true;
        }
    },
    CHINESE_EIGHT(16, "ChineseEight",
            new Cue.Size[]{Cue.Size.MEDIUM, Cue.Size.SMALL, Cue.Size.BIG},
            new TrainType[]{TrainType.SNAKE_FULL, TrainType.SNAKE_HALF,
                    TrainType.SNAKE_FULL_ORDERED, TrainType.SNAKE_HALF_ORDERED},
            BreakRule.WINNER) {
        @Override
        public boolean poolLike() {
            return true;
        }

        @Override
        public boolean eightBallLike() {
            return true;
        }
    },
    LIS_EIGHT(16, "LisEight",
            new Cue.Size[]{Cue.Size.MEDIUM, Cue.Size.SMALL, Cue.Size.BIG},
            new TrainType[]{TrainType.SNAKE_FULL, TrainType.SNAKE_HALF,
                    TrainType.SNAKE_FULL_ORDERED, TrainType.SNAKE_HALF_ORDERED},
            BreakRule.WINNER) {
        @Override
        public boolean poolLike() {
            return true;
        }

        @Override
        public boolean eightBallLike() {
            return true;
        }
    },
    SIDE_POCKET(10, "SidePocket",
            new Cue.Size[]{Cue.Size.BIG, Cue.Size.MEDIUM},
            new TrainType[]{TrainType.SNAKE_FULL,
                    TrainType.SNAKE_FULL_ORDERED, TrainType.SNAKE_HALF_ORDERED},
            BreakRule.WINNER) {
        @Override
        public boolean poolLike() {
            return true;
        }
    };

    public final String sqlKey;
    public final int nBalls;
    public final Cue.Size[] suggestedCues;
    public final TrainType[] supportedTrainings;
    public final BreakRule breakRule;

    GameRule(int nBalls, String sqlKey, Cue.Size[] suggestedCues,
             TrainType[] supportedTrainings,
             BreakRule breakRule) {
        this.nBalls = nBalls;
        this.sqlKey = sqlKey;
        this.suggestedCues = suggestedCues;
        this.supportedTrainings = supportedTrainings;
        this.breakRule = breakRule;
    }

    public static GameRule fromSqlKey(String sqlKey) {
        for (GameRule gameRule : values()) {
            if (gameRule.sqlKey.equalsIgnoreCase(sqlKey)) return gameRule;
        }
        throw new EnumConstantNotPresentException(GameRule.class, sqlKey);
    }

    public static String toReadable(GameRule gameRule) {
        String key = Util.toLowerCamelCase(gameRule.sqlKey);
        return App.getStrings().getString(key);
    }
    
    public static BallMetrics getDefaultBall(GameRule rule) {
        switch (rule) {
            case SNOOKER:
            case MINI_SNOOKER:
                return BallMetrics.SNOOKER_BALL;
            default:
                return BallMetrics.POOL_BALL;
        }
    }

    public boolean snookerLike() {
        return false;
    }

    public boolean poolLike() {
        return false;
    }

    public boolean eightBallLike() {
        return false;
    }

    @Override
    public String toString() {
        return toReadable(this);
    }

    public String toSqlKey() {
        return sqlKey;
    }
}
