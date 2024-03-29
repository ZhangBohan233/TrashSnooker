package trashsoftware.trashSnooker.core;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.json.JSONObject;
import trashsoftware.trashSnooker.fxml.drawing.CueModel;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.Objects;

public class Cue {

    public final String cueId;
    public final String name;
    public final double frontLength;
    public final double midLength;
    public final double backLength;
    public final double cueTipThickness;
    public final double tipRingThickness;
    private final double endWidth;
    private final double cueTipWidth;

    public final double powerMultiplier;
    public final double spinMultiplier;
    public final double accuracyMultiplier;

    public final Color tipRingColor;
    public final Color frontColor;
    public final Color midColor;
    public final Color backColor;

    public final boolean privacy;
    
    public Arrow arrow;
    public final Size tipSize;

    public Cue(String cueId,
               String name,
               double frontLength,
               double midLength,
               double backLength,
               double tipRingThickness,
               double cueTipThickness,
               double endWidth,
               double cueTipWidth,
               Color tipRingColor,
               Color frontColor,
               Color midColor,
               Color backColor,
               double powerMultiplier,
               double spinMultiplier,
               double accuracyMultiplier,
               boolean privacy) {
        this.cueId = cueId;
        this.name = name;
        this.frontLength = frontLength;
        this.midLength = midLength;
        this.backLength = backLength;
        this.tipRingThickness = tipRingThickness;
        this.cueTipThickness = cueTipThickness;
        this.endWidth = endWidth;
        this.cueTipWidth = cueTipWidth;
        this.tipRingColor = tipRingColor;
        this.frontColor = frontColor;
        this.midColor = midColor;
        this.backColor = backColor;
        this.powerMultiplier = powerMultiplier;
        this.spinMultiplier = spinMultiplier;
        this.accuracyMultiplier = accuracyMultiplier;
        this.privacy = privacy;
        
        if (cueTipWidth <= 9.8) tipSize = Size.VERY_SMALL;
        else if (cueTipWidth < 11.0) tipSize = Size.SMALL;
        else if (cueTipWidth < 12.5) tipSize = Size.MEDIUM;
        else tipSize = Size.BIG;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cue cue = (Cue) o;
        return Objects.equals(cueId, cue.cueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cueId);
    }

    public String getCueId() {
        return cueId;
    }

    public String getName() {
        return name;
    }

    public double getFrontMaxWidth() {
        return cueTipWidth + (endWidth - cueTipWidth) *
                ((tipRingThickness + frontLength) / getWoodPartLength());
    }

    public double getMidMaxWidth() {
        return cueTipWidth + (endWidth - cueTipWidth) *
                ((tipRingThickness + frontLength + midLength) / getWoodPartLength());
    }

    public double getRingMaxWidth() {
        return cueTipWidth + (endWidth - cueTipWidth) *
                (tipRingThickness / getWoodPartLength());
    }

    public double getTotalLength() {
        return getWoodPartLength() + cueTipThickness;
    }

    public double getWoodPartLength() {
        return tipRingThickness + frontLength + midLength + backLength;
    }

    public double getEndWidth() {
        return endWidth;
    }

    public double getCueTipWidth() {
        return cueTipWidth;
    }
    
    public void createArrow(JSONObject arrowObj) {
        this.arrow = new Arrow(
                DataLoader.parseColor(arrowObj.getString("color")),
                arrowObj.getDouble("firstGap"),
                arrowObj.getDouble("lastGap"),
                arrowObj.getDouble("firstScale"),
                arrowObj.getDouble("lastScale"),
                arrowObj.getDouble("frontSpace"),
                arrowObj.getDouble("depth")
        );
    }
    
    public class Arrow {
        public final Color arrowColor;
        public final double firstGap;  // 第一道纹尖与第二道纹尖的距离。从杆头往杆尾数
        public final double lastGap;  // 最后一道纹尖与倒数第二道纹尖的距离
        public final double firstScale;  // 第一道纹的长度，也就是第一道纹尖与第一道纹尾的距离
        public final double lastScale;
        public final double frontSpace;  // 前肢最前与第一道纹尖的距离
        public final double depth;  // 纹路的粗细
        public String name;
        
        public final double[][] arrowScales;  // 每一道箭纹的[尖，尾]离前肢头部的距离，加上纹尾的[宽度]
        
        public Arrow(Color arrowColor, 
                     double firstGap, double lastGap, 
                     double firstScale, double lastScale,
                     double frontSpace,
                     double depth) {
            this.arrowColor = arrowColor;
            this.firstGap = firstGap;
            this.lastGap = lastGap;
            this.firstScale = firstScale;
            this.lastScale = lastScale;
            this.frontSpace = frontSpace;
            this.depth = depth;
            
            arrowScales = new double[computeNArrows()][3];
            
            calculate();
        }
        
        private void calculate() {
            double gapIncrementer = (lastGap - firstGap) / arrowScales.length;
            double sizeIncrementer = (lastScale - firstScale) / arrowScales.length;
            
            double minWidth = getRingMaxWidth();
            double widthDiff = getFrontMaxWidth() - minWidth;
            
            double currentGap = firstGap;
            double currentSize = firstScale;
            
            double arrowTop = frontSpace;
            for (int i = 0; i < arrowScales.length; i++) {
                double arrowEnd = arrowTop + currentSize;
                arrowScales[i][0] = arrowTop;  // 纹尖位置
                arrowScales[i][1] = arrowEnd;  // 纹尾位置
                arrowScales[i][2] = arrowEnd / frontLength * widthDiff + minWidth;  // 纹尾宽度
                
                arrowTop += currentGap;
                currentGap += gapIncrementer;
                currentSize += sizeIncrementer;
            }
        }
        
        private int computeNArrows() {
            // 等差数列
            double space = Cue.this.frontLength - frontSpace;
            double avg = (firstGap + lastGap) / 2;
            return (int) (space / avg);
        }
        
        public int getNArrows() {
            return arrowScales.length;
        }
    }
    
    public enum Size {
        VERY_SMALL,
        SMALL,
        MEDIUM,
        BIG
    }
}
