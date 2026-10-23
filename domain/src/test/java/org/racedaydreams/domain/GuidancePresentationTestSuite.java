package org.racedaydreams.domain;
import org.racedaydreams.domain.health.HealthIntelligence.*;
import org.racedaydreams.domain.presentation.HealthContextSummary;

/** Presentation must not bypass personal coverage or turn absent values into zero. */
public final class GuidancePresentationTestSuite {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("Guidance presentation check "+checks);}
    public static void main(String[] args){
        var sleep=new Trend(Metric.SLEEP_MINUTES,"source",450.0,448.7,7,28,"Within recent personal range","Context");
        var s=HealthContextSummary.of(sleep);check(s.title().equals("Sleep"));check(s.value().equals("7h 30m"));check(s.context().equals("Around your usual range"));
        var missing=HealthContextSummary.of(new Trend(Metric.HRV_RMSSD,"Unavailable",null,null,0,0,"Insufficient evidence","Missing"));check(missing.value().equals("Not available"));check(missing.context().equals("Insufficient comparable data"));
        var weak=HealthContextSummary.of(new Trend(Metric.RESTING_HR,"source",62.0,55.0,3,13,"Above recent personal baseline","Sparse"));check(weak.value().equals("62 bpm"));check(weak.context().equals("Insufficient comparable data"));
        var elevated=HealthContextSummary.of(new Trend(Metric.RESTING_HR,"source",62.0,55.0,4,14,"Above recent personal baseline","Context"));check(elevated.context().equals("Above your recent baseline"));
        var steps=HealthContextSummary.of(new Trend(Metric.STEPS,"aggregate",12345.0,9000.0,7,28,"Above recent personal baseline","Context"));check(steps.value().equals("12,345 steps/day"));
        check(HealthContextSummary.of(new Trend(Metric.SLEEP_MINUTES,"source",390.0,450.0,4,14,"Below recent personal baseline","Context")).context().equals("Below your usual range"));
        System.out.println("GuidancePresentationTestSuite: "+checks+" checks passed.");
    }
}
