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
        check(new org.racedaydreams.domain.recording.RunMetricsEngine.Status(100,10000,10000,300.0,"GPS available",false).hasReliablePace());
        check(!new org.racedaydreams.domain.recording.RunMetricsEngine.Status(100,10000,10000,300.0,"GPS jump rejected",false).hasReliablePace());
        check(!new org.racedaydreams.domain.recording.RunMetricsEngine.Status(100,10000,10000,300.0,"GPS interrupted · distance incomplete",false).hasReliablePace());
        check(!new org.racedaydreams.domain.recording.RunMetricsEngine.Status(100,10000,10000,300.0,"GPS available",true).hasReliablePace());
        check(!new org.racedaydreams.domain.recording.RunMetricsEngine.Status(100,10000,10000,null,"GPS available",false).hasReliablePace());
        var geographic=org.racedaydreams.domain.support.DreamRoutes.assign(5020,new java.util.Random(5),java.util.Collections.singleton("berlin-v2"));
        check(geographic.routeId().equals("berlin-2014-geo-v1"));
        var berlin=org.racedaydreams.domain.support.DreamRoutes.forId(geographic.routeId());
        check(berlin.vertices().size()==191);
        check(berlin.vertices().stream().allMatch(v->v.x()>=0&&v.x()<=10&&v.y()>=0&&v.y()<=10));
        check(org.racedaydreams.domain.support.DreamRoutes.forId("berlin-v2").vertices().size()==29);
        var legacyAssignment=new org.racedaydreams.domain.support.DreamRoutes.Assignment("berlin-v2",1000,5000);
        check(legacyAssignment.routeId().equals("berlin-v2"));
        check(org.racedaydreams.domain.support.DreamRoutes.provenance(berlin).contains("2014"));
        check(!org.racedaydreams.domain.support.DreamRoutes.provenance(berlin).contains("schematic"));
        check(org.racedaydreams.domain.support.DreamRoutes.at(berlin,0).equals(berlin.vertices().get(0)));
        var endpoint=org.racedaydreams.domain.support.DreamRoutes.at(berlin,42195);var last=berlin.vertices().get(190);
        check(Math.hypot(endpoint.x()-last.x(),endpoint.y()-last.y())<.0000001);
        check(org.racedaydreams.domain.support.DreamRoutes.at(berlin,50000).equals(endpoint));
        var fullSection=org.racedaydreams.domain.support.DreamRoutes.section(berlin,0,42195);
        check(fullSection.size()==191);check(fullSection.get(0).equals(berlin.vertices().get(0)));
        check(org.racedaydreams.domain.support.DreamRoutes.section(berlin,10000,15000).size()>2);
        var duplicates=new org.racedaydreams.domain.support.DreamRoutes.Route("test","Test",java.util.Arrays.asList(new org.racedaydreams.domain.support.DreamRoutes.Vertex(1,1),new org.racedaydreams.domain.support.DreamRoutes.Vertex(1,1),new org.racedaydreams.domain.support.DreamRoutes.Vertex(2,2)));
        check(org.racedaydreams.domain.support.DreamRoutes.at(duplicates,0).equals(duplicates.vertices().get(0)));
        check(org.racedaydreams.domain.support.DreamRoutes.section(duplicates,0,0).size()==1);
        try{org.racedaydreams.domain.support.DreamRoutes.section(berlin,10,5);throw new AssertionError("Reversed section");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("GuidancePresentationTestSuite: "+checks+" checks passed.");
    }
}
