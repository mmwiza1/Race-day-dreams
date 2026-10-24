package org.racedaydreams.domain.presentation;

import java.util.Locale;
import org.racedaydreams.domain.support.DreamRoutes;

/** Presentation of the retained equivalent assignment; never changes geometry or GPS. */
public record DreamRouteProgress(double highlightedMetres,double fraction,int percentage,double extraMetres,String distanceLabel) {
    public static final double MARATHON_METRES = 42195.0;

    public static DreamRouteProgress of(DreamRoutes.Assignment assignment) {
        double distance=assignment.coveredMetres(),highlighted=Math.min(MARATHON_METRES,distance);
        double fraction=highlighted/MARATHON_METRES;
        return new DreamRouteProgress(highlighted,fraction,(int)Math.round(fraction*100),Math.max(0,distance-MARATHON_METRES),String.format(Locale.UK,"%.2f km / 42.20 km",highlighted/1000));
    }

    /** Subtract retained metres before formatting; never subtract rounded kilometre labels. */
    public double remainingMetres() { return Math.max(0, MARATHON_METRES-highlightedMetres); }
    public double totalMetres() { return MARATHON_METRES; }
    public String completedDistanceLabel() { return kilometres(highlightedMetres); }
    public String remainingDistanceLabel() { return kilometres(remainingMetres()); }
    public String totalDistanceLabel() { return kilometres(MARATHON_METRES); }
    public String completionLabel() { return percentage+"% complete"; }

    /** The caller retains the saved course's identity, including historical editions. */
    public String equivalenceStatement(String courseName) {
        String statement="You covered the equivalent of "+completedDistanceLabel()+" of the "+courseName(courseName)+" today.";
        return extraMetres>0 ? statement+" Your run also included "+kilometres(extraMetres)+" beyond one marathon equivalent." : statement;
    }

    /** Colour-independent progress text; decorative city art is not physical-location evidence. */
    public String accessibilitySummary(String courseName) {
        return courseName(courseName)+". "+completionLabel()+". "+completedDistanceLabel()+" completed; "+
            remainingDistanceLabel()+" remaining; "+totalDistanceLabel()+" total marathon-equivalent distance."+
            (extraMetres>0 ? " Plus "+kilometres(extraMetres)+" beyond one marathon equivalent." : "")+
            " This is an equivalent section, not your physical location.";
    }

    private static String kilometres(double metres) { return String.format(Locale.UK,"%.2f km",metres/1000); }
    private static String courseName(String value) {
        if(value==null||value.trim().isEmpty()) throw new IllegalArgumentException("Missing saved course name");
        return value.trim();
    }
}
