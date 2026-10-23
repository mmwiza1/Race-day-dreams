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
        var wall=java.time.Instant.parse("2026-10-01T00:00:00Z");var engine=new org.racedaydreams.domain.recording.RunMetricsEngine(0,wall);
        for(int i=0;i<=14400;i++)engine.accept(new org.racedaydreams.domain.recording.RunMetricsEngine.Fix(wall.plusSeconds(i),i*1000L,0,i*.00003,3.0,null,null,3.0));
        check(engine.pointCount()==14401);check(engine.pointsFrom(14400).size()==1);check(engine.pointsFrom(engine.pointCount()).isEmpty());
        check(engine.eventsFrom(engine.eventCount()).isEmpty());
        var retained=engine.pointsFrom(14400);try{retained.clear();throw new AssertionError("Mutable persistence snapshot");}catch(UnsupportedOperationException expected){checks++;}
        var pause=new org.racedaydreams.domain.recording.RunMetricsEngine.Event("PAUSE",1000,wall);
        var resume=new org.racedaydreams.domain.recording.RunMetricsEngine.Event("RESUME",3000,wall);
        check(org.racedaydreams.domain.recording.RecordingTimeline.pausedAt(0,2000,java.util.Arrays.asList(resume,pause)));
        check(!org.racedaydreams.domain.recording.RecordingTimeline.pausedAt(0,4000,java.util.Arrays.asList(resume,pause)));
        check(!org.racedaydreams.domain.recording.RecordingTimeline.pausedAt(0,500,java.util.Arrays.asList(pause)));
        var small=org.racedaydreams.domain.presentation.DreamRouteProgress.of(new org.racedaydreams.domain.support.DreamRoutes.Assignment("london-v2",10000,5020));
        check(small.percentage()==12);check(small.distanceLabel().equals("5.02 km / 42.20 km"));check(small.extraMetres()==0);
        var full=org.racedaydreams.domain.presentation.DreamRouteProgress.of(new org.racedaydreams.domain.support.DreamRoutes.Assignment("london-v2",0,42195));check(full.percentage()==100);check(full.fraction()==1);check(full.distanceLabel().equals("42.20 km / 42.20 km"));
        var over=org.racedaydreams.domain.presentation.DreamRouteProgress.of(new org.racedaydreams.domain.support.DreamRoutes.Assignment("london-v2",0,50000));check(over.percentage()==100);check(over.extraMetres()==7805);check(over.highlightedMetres()==42195);
        System.out.println("GuidancePresentationTestSuite: "+checks+" checks passed.");
    }
}
