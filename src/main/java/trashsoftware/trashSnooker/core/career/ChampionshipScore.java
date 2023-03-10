package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.util.Calendar;

/**
 * 一个人一次赛事的成绩
 */
public class ChampionshipScore {

    public final ChampionshipData data;
    public final Calendar timestamp;
    public final Rank[] ranks;  // 一般也就一个，两个的时候是额外的奖，比如单杆最高

    public ChampionshipScore(String championshipId,
                             int year,
                             Rank[] ranks) {
        this.data = ChampDataManager.getInstance().findDataById(championshipId);
        this.ranks = ranks;

        this.timestamp = data.toCalendar(year);
    }

    public static ChampionshipScore fromJson(JSONObject jsonObject) {
        JSONArray ranksArr = jsonObject.getJSONArray("ranks");
        Rank[] ranks = new Rank[ranksArr.length()];
        for (int i = 0; i < ranks.length; i++) {
            ranks[i] = Rank.valueOf(ranksArr.getString(i));
        }
        return new ChampionshipScore(
                jsonObject.getString("id"),
                jsonObject.getInt("year"),
                ranks
        );
    }

    public JSONObject toJsonObject() {
        JSONObject out = new JSONObject();
        out.put("id", data.id);
        out.put("year", getYear());

        JSONArray ranksArr = new JSONArray();
        for (Rank rank : ranks) {
            ranksArr.put(rank.name());
        }
        out.put("ranks", ranksArr);

        return out;
    }

    public int getYear() {
        return timestamp.get(Calendar.YEAR);
    }

    public enum Rank implements Comparable<Rank> {
        CHAMPION(true),
        SECOND_PLACE(true),
        TOP_4(true),
        TOP_8(true),
        TOP_16(true),
        TOP_32(true),
        TOP_64(true),
        PRE_GAMES_4(false),
        PRE_GAMES_3(false),
        PRE_GAMES_2(false),
        PRE_GAMES_1(false),
        BEST_SINGLE(false),
        MAXIMUM(false);

        public final boolean isMain;

        Rank(boolean isMain) {
            this.isMain = isMain;
        }

        public static Rank[] getSequenceOfLosers(int mainRounds, int preRounds) {
            Rank[] res = new Rank[mainRounds + preRounds];
            int mainSkip = SECOND_PLACE.ordinal();
            int preSkip = PRE_GAMES_1.ordinal() - preRounds + 1;

            int rounds = mainRounds + preRounds;

            for (int i = 0; i < rounds; i++) {
                if (i < mainRounds) {
                    res[i] = values()[i + mainSkip];
                } else {
                    res[i] = values()[i - mainRounds + preSkip];
                }
            }
            return res;
        }

//        @Override
//        public String toString() {
//            return shown;
//        }

        public String getShown() {
            return App.getStrings().getString(Util.toLowerCamelCase(name()));
        }
    }
}
