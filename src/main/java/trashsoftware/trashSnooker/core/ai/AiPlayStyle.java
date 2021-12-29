package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.PlayerPerson;

public class AiPlayStyle {
    public static final AiPlayStyle DEFAULT = new AiPlayStyle(100.0, 100.0, 100.0);
    
    public final double precision;
    public final double position;  // 走位能力
    public final double defense;
    
    public AiPlayStyle(double precision, double position, double defense) {
        this.precision = precision;
        this.position = position;
        this.defense = defense;
    }
}
