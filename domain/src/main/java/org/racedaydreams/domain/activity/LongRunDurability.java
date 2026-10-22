package org.racedaydreams.domain.activity;
import java.time.Duration;
/** Descriptive halves, never an injury prediction. Uses the same quality gate as real splits. */
public record LongRunDurability(boolean sufficient, Double paceFadePercent, Double heartRateDriftPercent,
        Double cadenceChangePercent, String explanation) {
    public static LongRunDurability analyse(ActivityRecord run) {
        SampleSeries series = new SampleSeries(run);
        if (series.quality != SampleQuality.QUALIFIED || run.samples().size() < 4 || series.totalsDisagree)
            return new LongRunDurability(false,null,null,null,"Insufficient reliable continuous samples for durability analysis.");
        var samples=run.samples(); var first=samples.get(0); var last=samples.get(samples.size()-1);
        double seconds=last.elapsed().minus(first.elapsed()).toMillis()/1000.0;
        if(seconds < 1800 || first.elapsed().toMillis()>30000 || last.elapsed().toMillis()<run.elapsedTime().toMillis()-30000)
            return new LongRunDurability(false,null,null,null,"At least 30 minutes with samples covering the run are needed.");
        double half=first.elapsed().toMillis()/1000.0+seconds/2, distance=nullDistance(samples,half);
        double d1=distance-first.distanceMetres(),d2=last.distanceMetres()-distance;
        if(d1<=0||d2<=0)return new LongRunDurability(false,null,null,null,"Insufficient distance in both halves.");
        double pace1=seconds/2/d1,pace2=seconds/2/d2;
        Double h1=average(samples,half,true,true),h2=average(samples,half,false,true);
        Double c1=average(samples,half,true,false),c2=average(samples,half,false,false);
        double fade=(pace2/pace1-1)*100;
        Double drift=h1==null||h2==null?null:(h2*pace2/(h1*pace1)-1)*100;
        return new LongRunDurability(true,fade,drift,c1==null||c2==null?null:(c2/c1-1)*100,
            fade>8?"The second half was slower. Terrain, conditions and intention can also explain this; review alongside effort.":"Late-run pace was relatively stable or faster. This alone does not prove improved fitness.");
    }
    private static double nullDistance(java.util.List<DistanceSample> s,double at){for(int i=1;i<s.size();i++){double t=s.get(i).elapsed().toMillis()/1000.0;if(t>=at){var a=s.get(i-1);double f=(at-a.elapsed().toMillis()/1000.0)/(t-a.elapsed().toMillis()/1000.0);return a.distanceMetres()+f*(s.get(i).distanceMetres()-a.distanceMetres());}}return s.get(s.size()-1).distanceMetres();}
    private static Double average(java.util.List<DistanceSample>s,double half,boolean early,boolean hr){double sum=0,coverage=0,total=0;for(int i=1;i<s.size();i++){var a=s.get(i-1);var b=s.get(i);double from=a.elapsed().toMillis()/1000.0,to=b.elapsed().toMillis()/1000.0;double w=early?Math.max(0,Math.min(to,half)-from):Math.max(0,to-Math.max(from,half));total+=w;Double v=hr?b.heartRate():b.cadence();if(v!=null&&v>0){sum+=v*w;coverage+=w;}}return coverage==0||coverage<total*.8?null:sum/coverage;}
}
