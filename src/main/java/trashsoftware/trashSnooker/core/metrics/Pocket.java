package trashsoftware.trashSnooker.core.metrics;

public class Pocket {
    
    public final boolean isMid;
    public final double[] fallCenter;
    public final double fallRadius;
    public final double[] graphicalCenter;
    public final double graphicalRadius;
    public final double extraSlopeWidth;
    
    Pocket(boolean isMid, 
           double[] fallCenter, 
           double fallRadius, 
           double[] graphicalCenter, 
           double graphicalRadius,
           double extraSlopeWidth) {
        this.isMid = isMid;
        this.fallCenter = fallCenter;
        this.fallRadius = fallRadius;
        this.graphicalCenter = graphicalCenter;
        this.graphicalRadius = graphicalRadius;
        this.extraSlopeWidth = extraSlopeWidth;
    }

    public boolean isMid() {
        return isMid;
    }

    public double[] getFallCenter() {
        return fallCenter;
    }

    public double getFallRadius() {
        return fallRadius;
    }

    public double[] getGraphicalCenter() {
        return graphicalCenter;
    }

    public double getGraphicalRadius() {
        return graphicalRadius;
    }

    public double getExtraSlopeWidth() {
        return extraSlopeWidth;
    }
}
