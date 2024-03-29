package trashsoftware.trashSnooker.core.scoreResult;

import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.util.Util;

import java.util.Map;

public class NineBallScoreResult extends ScoreResult {
    public static final int BYTE_LENGTH = 20;  // 空了一些，实际只有1, 4-8, 8-17用了
    private final Map<PoolBall, Boolean> remBalls;

    public NineBallScoreResult(int thinkTime, int justCuedPlayerNum, Map<PoolBall, Boolean> remBalls) {
        super(thinkTime, justCuedPlayerNum);

        this.remBalls = remBalls;
    }

    @Override
    public byte[] toBytes() {
        byte[] res = new byte[BYTE_LENGTH];
        res[0] = (byte) justCuedPlayerNum;

        Util.int32ToBytes(thinkTime, res, 4);

        putBallToArray(remBalls, res, 8);
        return res;
    }

    private void putBallToArray(Map<PoolBall, Boolean> remBalls, byte[] arr, int resBeginIndex) {
        for (Map.Entry<PoolBall, Boolean> entry : remBalls.entrySet()) {
            int index = entry.getKey().getValue() - 1 + resBeginIndex;
            arr[index] = entry.getValue() ? (byte) 1 : (byte) 0;
        }
    }

    public Map<PoolBall, Boolean> getRemBalls() {
        return remBalls;
    }
}
