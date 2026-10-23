package org.racedaydreams.domain.recording;

import java.time.*;
import java.util.*;
import org.racedaydreams.domain.activity.*;

/** Original deterministic trace processor. No Android sensors, wall-clock timing or invented routes. */
public final class RunMetricsEngine {
    public record Fix(Instant timestamp,long monotonicMillis,double latitude,double longitude,
                      Double accuracy,Double altitude,Double verticalAccuracy,Double speed) {
        public Fix {
            Objects.requireNonNull(timestamp);
            if(monotonicMillis<0||!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180||(accuracy!=null&&(!Double.isFinite(accuracy)||accuracy<0)))throw new IllegalArgumentException("Invalid location");
            if(altitude!=null&&!Double.isFinite(altitude)||verticalAccuracy!=null&&(!Double.isFinite(verticalAccuracy)||verticalAccuracy<0)||speed!=null&&(!Double.isFinite(speed)||speed<0))throw new IllegalArgumentException("Invalid optional location evidence");
        }
    }
    public record Point(Fix fix,double cumulativeMetres,long activeMillis,int segment,boolean discontinuity,Double heartRate,Double cadence) {}
    public record Event(String type,long monotonicMillis,Instant timestamp,Double value) {public Event(String type,long monotonicMillis,Instant timestamp){this(type,monotonicMillis,timestamp,null);}}
    public record Status(double metres,long elapsedMillis,long movingMillis,Double secondsPerKm,String gps,boolean paused) {}
    private final long started;private long minimumFixTime;private long lastTime,pausedSince=-1,pausedTotal;private int segment;
    private double distance;private Fix anchor;private boolean gap=true;private String gps="Waiting for GPS";
    private Double hr,cadence;private long hrTime=-1,cadenceTime=-1;
    private final List<Point> points=new ArrayList<>();private final List<Event> events=new ArrayList<>();
    public RunMetricsEngine(long monotonicMillis,Instant wall){started=lastTime=monotonicMillis;events.add(new Event("START",monotonicMillis,wall));}
    public void heartRate(Double value,long time){if(value!=null&&(!Double.isFinite(value)||value<25||value>250))throw new IllegalArgumentException("Invalid HR");hr=value;hrTime=time;}
    public void cadence(Double value,long time){if(value!=null&&(!Double.isFinite(value)||value<0||value>300))throw new IllegalArgumentException("Invalid cadence");cadence=value;cadenceTime=time;}
    public void pause(long time,Instant wall){pause(time,wall,false);}
    public void pause(long time,Instant wall,boolean auto){tick(time);if(pausedSince<0){minimumFixTime=time;pausedSince=time;anchor=null;gap=true;events.add(new Event(auto?"AUTO_PAUSE":"PAUSE",time,wall));}}
    public void resume(long time,Instant wall){resume(time,wall,false);}
    public void resume(long time,Instant wall,boolean auto){tick(time);if(pausedSince>=0){minimumFixTime=time;pausedTotal+=time-pausedSince;pausedSince=-1;segment++;anchor=null;gap=true;events.add(new Event(auto?"AUTO_RESUME":"RESUME",time,wall));}}
    public void sensorEvent(String type,double value,long time,Instant wall){tick(time);events.add(new Event(type,time,wall,value));}
    public void event(String type,long time,Instant wall){tick(time);events.add(new Event(type,time,wall));}
    private void tick(long time){if(time<lastTime)throw new IllegalArgumentException("Monotonic time moved backwards");lastTime=time;}
    public boolean accept(Fix f){
        if(f.monotonicMillis()<Math.max(started,minimumFixTime)||!points.isEmpty()&&f.monotonicMillis()<=points.get(points.size()-1).fix().monotonicMillis())return false;
        lastTime=Math.max(lastTime,f.monotonicMillis());
        if(f.accuracy()!=null&&f.accuracy()>35){lost(f);return false;}
        if(pausedSince>=0){gps="GPS available · paused";return false;}
        if(anchor!=null){long dt=f.monotonicMillis()-anchor.monotonicMillis();double delta=metres(anchor,f);
            if(dt>15000){lost(f);anchor=null;}
            else if(dt<=0||delta/(dt/1000.0)>12||f.speed()!=null&&f.speed()>12){gps="GPS jump rejected";return false;}
            else distance+=delta;
        }
        if(gap){segment++;events.add(new Event("GPS_RETURNED",f.monotonicMillis(),f.timestamp()));}
        gps="GPS available";
        points.add(new Point(f,distance,active(f.monotonicMillis()),segment,gap,
            hr!=null&&f.monotonicMillis()-hrTime<=10000?hr:null,cadence!=null&&f.monotonicMillis()-cadenceTime<=10000?cadence:null));
        gap=false;anchor=f;return true;
    }
    public void breakSegment(long time,Instant wall){tick(time);anchor=null;gap=true;segment++;events.add(new Event("GPS_LOST",time,wall));}
    private void lost(Fix f){if(!gap){events.add(new Event("GPS_LOST",f.monotonicMillis(),f.timestamp()));segment++;}gap=true;anchor=null;gps="GPS interrupted · distance incomplete";}
    public void missingFix(long time,Instant wall){tick(time);if(anchor!=null&&time-anchor.monotonicMillis()>15000){lost(new Fix(wall,time,anchor.latitude(),anchor.longitude(),99.0,null,null,null));anchor=null;}}
    private long active(long time){return Math.max(0,time-started-pausedTotal-(pausedSince>=0?time-pausedSince:0));}
    public Status status(long time){
        long now=Math.max(time,lastTime);Double pace=null;
        if(!points.isEmpty()&&pausedSince<0&&now-points.get(points.size()-1).fix().monotonicMillis()<=10000){
            Point end=points.get(points.size()-1),begin=end;
            for(int i=points.size()-2;i>=0;i--){Point p=points.get(i);if(p.segment()!=end.segment()||end.activeMillis()-p.activeMillis()>30000)break;begin=p;}
            double d=end.cumulativeMetres()-begin.cumulativeMetres();long dt=end.activeMillis()-begin.activeMillis();if(d>=10&&dt>=5000)pace=dt/d;
        }
        return new Status(distance,now-started,active(now),pace,gps,pausedSince>=0);
    }
    public List<Point> points(){return Collections.unmodifiableList(new ArrayList<>(points));}public List<Event> events(){return Collections.unmodifiableList(new ArrayList<>(events));}
    /** Elapsed samples retain pauses/gaps; existing qualified calculators cannot bridge them. */
    public List<DistanceSample> samples(){return points.stream().map(p->new DistanceSample(Duration.ofMillis(p.fix().monotonicMillis()-started),p.cumulativeMetres(),p.heartRate(),p.cadence(),p.fix().altitude(),p.discontinuity())).collect(java.util.stream.Collectors.toList());}
    public static double metres(Fix a,Fix b){double lat=Math.toRadians(b.latitude()-a.latitude()),lon=Math.toRadians(b.longitude()-a.longitude());double h=Math.sin(lat/2)*Math.sin(lat/2)+Math.cos(Math.toRadians(a.latitude()))*Math.cos(Math.toRadians(b.latitude()))*Math.sin(lon/2)*Math.sin(lon/2);return 6371000*2*Math.atan2(Math.sqrt(h),Math.sqrt(Math.max(0,1-h)));}
}
