package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public class PocketSize {
    public static final PocketSize[] SNOOKER_HOLES = {
            new PocketSize("pocketLarge", 89, 93),
            new PocketSize("pocketStd", 
                    84.15, 
                    88.0),
            new PocketSize("pocketSmall", 81, 84.15),
            new PocketSize("pocketLittle", 78, 80),
            new PocketSize("pocketTiny", 72, 72)
    };
    public static final PocketSize[] CHINESE_EIGHT_HOLES = {
            new PocketSize("pocketHuge", 93, 98),
            new PocketSize("pocketLarge", 89, 93),
            new PocketSize("pocketStd", 85, 89),
            new PocketSize("pocketSmall", 82, 85),
            new PocketSize("pocketLittle", 78, 81)
    };
    public static final PocketSize[] SIDE_POCKET_HOLES = {
            new PocketSize("pocketLarge", 123.825, 133),
            new PocketSize("pocketStd", 117.475, 127),  // 中袋是mouth的直径
            new PocketSize("pocketSmall", 111.125, 120)
    };
    
    public final String key;
    public final double cornerHoleDiameter;
    public final double cornerMouthWidth;
    public final double midHoleDiameter;
    public final double midThroatWidth;
    public final double midArcRadius;
    public final boolean midHoleThroatSpecified;

    public PocketSize(String name, 
                      double cornerHoleDiameter, 
                      double cornerMouthWidth,
                      double midHoleDiameter, 
                      double midThroatWidth,
                      double midArcRadius,
                      boolean midHoleThroatSpecified) {
        this.key = name;
        this.cornerHoleDiameter = cornerHoleDiameter;
        this.cornerMouthWidth = cornerMouthWidth;
        this.midHoleDiameter = midHoleDiameter;
        this.midThroatWidth = midThroatWidth;
        this.midArcRadius = midArcRadius;
        this.midHoleThroatSpecified = midHoleThroatSpecified;
    }

    public PocketSize(String name, 
                      double cornerHoleDiameter, 
                      double midHoleDiameter) {
        this(name, cornerHoleDiameter, cornerHoleDiameter, midHoleDiameter, midHoleDiameter, midHoleDiameter / 2, false);
    }

    public static PocketSize valueOf(TableMetrics.TableBuilderFactory factory, String jsonString) {
        String camelString = Util.toLowerCamelCase("POCKET_" + jsonString);
        for (PocketSize pd : factory.supportedHoles) {
            if (pd.key.equals(camelString) || pd.key.equals(jsonString)) return pd;
        }
        throw new RuntimeException("No match pocket size: " + jsonString);
    }

    @Override
    public String toString() {
        return String.format("%s (%d mm)", App.getStrings().getString(key), (int) cornerHoleDiameter);
    }
}
