package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.core.BreakRule;
import trashsoftware.trashSnooker.core.Cue;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.Game;
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
    SNOOKER(true, 22, "Snooker", 
            new Cue.Size[]{Cue.Size.VERY_SMALL, Cue.Size.SMALL}, BreakRule.ALTERNATE),
    MINI_SNOOKER(true, 13, "MiniSnooker",
            new Cue.Size[]{Cue.Size.VERY_SMALL, Cue.Size.SMALL}, BreakRule.ALTERNATE),
    CHINESE_EIGHT(false, 16, "ChineseEight",
            new Cue.Size[]{Cue.Size.MEDIUM, Cue.Size.SMALL, Cue.Size.BIG}, BreakRule.WINNER),
    LIS_EIGHT(false, 16, "LisEight",
            new Cue.Size[]{Cue.Size.MEDIUM, Cue.Size.SMALL, Cue.Size.BIG}, BreakRule.WINNER),
    SIDE_POCKET(false, 10, "SidePocket",
            new Cue.Size[]{Cue.Size.BIG, Cue.Size.MEDIUM}, BreakRule.WINNER);

    public final boolean snookerLike;
    public final String sqlKey;
    public final int nBalls;
    public final Cue.Size[] suggestedCues;
    public final BreakRule breakRule;

    GameRule(boolean snookerLike, int nBalls, String sqlKey, Cue.Size[] suggestedCues, 
             BreakRule breakRule) {
        this.snookerLike = snookerLike;
        this.nBalls = nBalls;
        this.sqlKey = sqlKey;
        this.suggestedCues = suggestedCues;
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

    @Override
    public String toString() {
        return toReadable(this);
    }

    public String toSqlKey() {
        return sqlKey;
    }
}
