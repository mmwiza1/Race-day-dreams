package org.racedaydreams.domain.presentation;
import org.racedaydreams.domain.activity.*;
/** Displays the existing qualified-sample analysis; aggregate average pace is never a substitute. */
public record DurabilityData(String status,Double firstHalfSecondsPerKm,Double secondHalfSecondsPerKm,LongRunDurability evidence) {
    public static DurabilityData of(ActivityRecord run){var d=LongRunDurability.analyse(run);if(!d.sufficient())return new DurabilityData("Insufficient detailed run data",null,null,d);
        var first=run.samples().get(0);var last=run.samples().get(run.samples().size()-1);
        double seconds=last.elapsed().minus(first.elapsed()).toMillis()/1000.0,metres=last.distanceMetres()-first.distanceMetres(),ratio=1+d.paceFadePercent()/100;
        double pace=seconds/2*(1+1/ratio)/metres*1000;
        return new DurabilityData(d.paceFadePercent()>8?"Late fade":d.paceFadePercent() < -3?"Faster second half":"Stable pacing",pace,pace*ratio,d);
    }
}
