package trashsoftware.trashSnooker.core;

import java.util.*;

public abstract class Player {

//    protected final int number;
    protected final InGamePlayer inGamePlayer;
    protected final TreeMap<Ball, Integer> singlePole = new TreeMap<>();
    protected final List<PotAttempt> attempts = new ArrayList<>();
    protected final List<DefenseAttempt> defenseAttempts = new ArrayList<>();
    protected int score;
    protected int lastAddedScore;
    private boolean withdrawn = false;

    public Player(InGamePlayer inGamePlayer) {
//        this.number = number;
        this.inGamePlayer = inGamePlayer;
    }
    
    protected abstract void addScoreOfPotted(Collection<? extends Ball> pottedBalls);

    public PlayerPerson getPlayerPerson() {
        return inGamePlayer.getPlayerPerson();
    }

    public InGamePlayer getInGamePlayer() {
        return inGamePlayer;
    }
    
    public void addAttempt(PotAttempt potAttempt) {
        attempts.add(potAttempt);
    }

    public List<PotAttempt> getAttempts() {
        return attempts;
    }
    
    public void addDefenseAttempt(DefenseAttempt defenseAttempt) {
        defenseAttempts.add(defenseAttempt);
    }

    public List<DefenseAttempt> getDefenseAttempts() {
        return defenseAttempts;
    }

//    public int getNumber() {
//        return number;
//    }

    public int getScore() {
        return score;
    }

    public final void correctPotBalls(Collection<? extends Ball> pottedBalls) {
        for (Ball ball : pottedBalls) {
            if (singlePole.containsKey(ball)) {
                singlePole.put(ball, singlePole.get(ball) + 1);
            } else {
                singlePole.put(ball, 1);
            }
        }
        int curScore = score;
        addScoreOfPotted(pottedBalls);
        lastAddedScore = score - curScore;
    }

    public TreeMap<Ball, Integer> getSinglePole() {
        return singlePole;
    }

    public int getSinglePoleScore() {
        int singlePoleScore = 0;
        for (Map.Entry<Ball, Integer> entry : singlePole.entrySet()) {
            singlePoleScore += entry.getKey().getValue() * entry.getValue();
        }
        return singlePoleScore;
    }

    public int getLastAddedScore() {
        return lastAddedScore;
    }

    public void clearSinglePole() {
        singlePole.clear();
    }

    public void addScore(int score) {
        this.score += score;
        this.lastAddedScore = score;
    }

    public boolean isWithdrawn() {
        return withdrawn;
    }

    public void withdraw() {
        withdrawn = true;
    }
}
