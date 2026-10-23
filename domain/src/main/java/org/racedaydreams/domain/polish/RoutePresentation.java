package org.racedaydreams.domain.polish;
import java.util.*;
import org.racedaydreams.domain.recording.RunMetricsEngine;
/** Pure drawing projection; never modifies stored observations or joins discontinuities. */
public final class RoutePresentation {
    public record XY(double x,double y) {}
    public static List<XY> fit(List<RunMetricsEngine.Point> points){
        if(points.isEmpty())return Collections.emptyList();double lat=points.get(0).fix().latitude();double cos=Math.max(.01,Math.cos(Math.toRadians(lat)));
        double minX=Double.MAX_VALUE,maxX=-Double.MAX_VALUE,minY=Double.MAX_VALUE,maxY=-Double.MAX_VALUE;
        for(var p:points){double x=p.fix().longitude()*cos,y=-p.fix().latitude();minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
        double span=Math.max(1e-8,Math.max(maxX-minX,maxY-minY));var out=new ArrayList<XY>();for(var p:points)out.add(new XY(.5+(p.fix().longitude()*cos-(minX+maxX)/2)/span*.84,.5+(-p.fix().latitude()-(minY+maxY)/2)/span*.84));return Collections.unmodifiableList(out);
    }
    public record Section(int kilometre,double secondsPerKm) {}
    public static List<Section> kilometres(List<RunMetricsEngine.Point> points){
        var out=new ArrayList<Section>();double boundary=1000;Long last=null;boolean valid=true;
        if(points.isEmpty())return out;last=points.get(0).activeMillis();
        for(int i=1;i<points.size();i++){var a=points.get(i-1);var b=points.get(i);if(b.discontinuity()||b.segment()!=a.segment())valid=false;
            while(b.cumulativeMetres()>=boundary){double delta=b.cumulativeMetres()-a.cumulativeMetres();if(delta<=0)break;long time=a.activeMillis()+Math.round((b.activeMillis()-a.activeMillis())*(boundary-a.cumulativeMetres())/delta);if(valid&&last!=null&&time>last)out.add(new Section((int)(boundary/1000),(time-last)/1000.0));last=time;valid=true;boundary+=1000;}}
        return Collections.unmodifiableList(out);
    }
    public static List<String> story(List<RunMetricsEngine.Point> points){var out=new ArrayList<String>();var km=kilometres(points);if(!km.isEmpty()){var fast=Collections.min(km,Comparator.comparingDouble(Section::secondsPerKm));out.add("Fastest complete kilometre: km "+fast.kilometre()+" at "+pace(fast.secondsPerKm())+"/km.");}if(km.size()>=4){double first=0,last=0;int n=km.size()/2;for(int i=0;i<n;i++){first+=km.get(i).secondsPerKm();last+=km.get(km.size()-n+i).secondsPerKm();}long diff=Math.round(last-first);out.add("The final "+n+" complete kilometres took "+Math.abs(diff)+" sec "+(diff<=0?"less":"more")+" than the opening "+n+". This describes pacing, not fitness.");}return out;}
    private static String pace(double v){long s=Math.round(v);return String.format(Locale.UK,"%d:%02d",s/60,s%60);}
    public static boolean reliableAltitude(List<RunMetricsEngine.Point> p){return p.size()>=5&&p.stream().filter(x->x.fix().altitude()!=null&&x.fix().verticalAccuracy()!=null&&x.fix().verticalAccuracy()<=10).count()>=p.size()*.8;}
}
