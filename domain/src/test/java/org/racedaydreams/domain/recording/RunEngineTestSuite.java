package org.racedaydreams.domain.recording;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.health.HealthIntelligence;
public final class RunEngineTestSuite {
 static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("Check "+n);}
 static RunMetricsEngine.Fix fix(long ms,double metres){return new RunMetricsEngine.Fix(Instant.EPOCH.plusMillis(ms),ms,0,metres/111194.9266,3.0,null,null,3.0);}
 public static void main(String[] args){
  var e=new RunMetricsEngine(0,Instant.EPOCH);for(int i=0;i<=5000;i+=5)check(e.accept(fix(i*300L,i)));
  check(Math.abs(e.status(1500000).metres()-5000)<0.1);check(e.status(1500000).secondsPerKm()!=null);check(Math.abs(e.status(1500000).secondsPerKm()-300)<0.1);
  var a=new ActivityRecord("a",new SourceIdentity("rdd","a",null),Instant.EPOCH,Instant.EPOCH.plusSeconds(1500),null,null,e.status(1500000).metres(),Duration.ofSeconds(1500),null,null,null,null,null,null,e.samples(),List.of(),null,null);
  check(SplitCalculator.calculate(a,1000).splits().size()==5);
  var jump=new RunMetricsEngine(0,Instant.EPOCH);jump.accept(fix(0,0));jump.accept(fix(1000,3));check(!jump.accept(fix(2000,303)));jump.accept(fix(3000,9));check(jump.status(3000).metres()<10);
  jump.accept(fix(48000,100));check(jump.status(48000).metres()<10);check(jump.events().stream().anyMatch(x->x.type().equals("GPS_LOST")));
  var paused=new RunMetricsEngine(0,Instant.EPOCH);paused.accept(fix(0,0));paused.accept(fix(1000,3));paused.pause(1000,Instant.EPOCH);paused.resume(181000,Instant.EPOCH.minusSeconds(3600));paused.accept(fix(182000,1000));paused.accept(fix(183000,1003));check(paused.status(183000).elapsedMillis()==183000);check(paused.status(183000).movingMillis()==3000);check(paused.status(183000).metres()<7);
  paused.heartRate(155.0,183000);paused.accept(fix(184000,1006));check(paused.points().get(paused.points().size()-1).heartRate()==155);paused.heartRate(null,184000);paused.accept(fix(185000,1009));check(paused.points().get(paused.points().size()-1).heartRate()==null);
  var today=LocalDate.of(2026,10,22);var obs=new ArrayList<HealthIntelligence.Observation>();for(int i=0;i<35;i++){var day=today.minusDays(i);obs.add(new HealthIntelligence.Observation("h"+i,HealthIntelligence.Metric.RESTING_HR,day,day.atStartOfDay(ZoneOffset.UTC).toInstant(),day.atStartOfDay(ZoneOffset.UTC).toInstant(),i<7?65:55,"one-device",Instant.EPOCH));}
  var trend=HealthIntelligence.evaluate(obs,today).get(1);check(trend.recentDays()==7);check(trend.referenceDays()==28);check(trend.interpretation().startsWith("Above"));check(HealthIntelligence.evaluate(List.of(),today).stream().allMatch(t->t.recent()==null));
  obs.add(new HealthIntelligence.Observation("new-source",HealthIntelligence.Metric.RESTING_HR,today,Instant.parse("2026-10-22T01:00:00Z"),Instant.parse("2026-10-22T01:00:00Z"),50,"different-device",Instant.EPOCH));check(HealthIntelligence.evaluate(obs,today).get(1).interpretation().equals("Insufficient evidence"));
  var policy=new AutoPausePolicy();for(int i=0;i<30;i++)check(policy.observe(new RunMetricsEngine.Fix(Instant.EPOCH,i*1000,0,0,3.0,null,null,0.0))==AutoPausePolicy.Decision.NONE);
  check(policy.observe(new RunMetricsEngine.Fix(Instant.EPOCH,30000,0,0,3.0,null,null,0.0))==AutoPausePolicy.Decision.PAUSE);
  check(policy.observe(new RunMetricsEngine.Fix(Instant.EPOCH,31000,0,0,60.0,null,null,3.0))==AutoPausePolicy.Decision.NONE);
  policy.observe(new RunMetricsEngine.Fix(Instant.EPOCH,32000,0,0,3.0,null,null,3.0));check(policy.observe(new RunMetricsEngine.Fix(Instant.EPOCH,37000,0,0,3.0,null,null,3.0))==AutoPausePolicy.Decision.RESUME);
  var delayed=new RunMetricsEngine(0,Instant.EPOCH);delayed.accept(fix(1000,0));delayed.missingFix(2500,Instant.EPOCH);check(delayed.accept(fix(2000,3)));
  delayed.heartRate(150.0,5000);delayed.accept(fix(3000,6));check(delayed.points().get(delayed.points().size()-1).heartRate()==null);delayed.accept(fix(6000,15));check(delayed.points().get(delayed.points().size()-1).heartRate()==150.0);
  System.out.println("RunEngineTestSuite: "+n+" checks passed (traces, split interpolation, gaps, pauses, clock change, HR, personal baselines).");
 }
}
