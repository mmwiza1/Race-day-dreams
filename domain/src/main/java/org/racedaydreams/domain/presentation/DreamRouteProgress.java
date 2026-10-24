package org.racedaydreams.domain.presentation;

import java.util.Locale;
import org.racedaydreams.domain.support.DreamRoutes;

/** Presentation of the retained equivalent assignment; never changes geometry or GPS. */
public record DreamRouteProgress(double highlightedMetres,double fraction,int percentage,double extraMetres,String distanceLabel) {
    public static DreamRouteProgress of(DreamRoutes.Assignment assignment) {
        double distance=assignment.coveredMetres(),highlighted=Math.min(42195,distance);
        double fraction=highlighted/42195;
        return new DreamRouteProgress(highlighted,fraction,(int)Math.round(fraction*100),Math.max(0,distance-42195),String.format(Locale.UK,"%.2f km / 42.20 km",highlighted/1000));
    }
}
