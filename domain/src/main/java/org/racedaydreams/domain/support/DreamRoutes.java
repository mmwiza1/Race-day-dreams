package org.racedaydreams.domain.support;
import java.util.*;

/** Original city-inspired abstract marathon schematics. NOT surveyed/official course geometry,
 * maps or navigation. Coordinates are drawing units, scaled to a marathon equivalent. */
public final class DreamRoutes {
    public record Vertex(double x,double y) {}
    public record Route(String id,String name,List<Vertex> vertices) {}
    public record Assignment(String routeId,double startMetres,double coveredMetres) {
        public Assignment {if(routes().stream().noneMatch(r->r.id().equals(routeId))||!Double.isFinite(startMetres)||!Double.isFinite(coveredMetres)||coveredMetres<=0||startMetres<0||startMetres+Math.min(42195,coveredMetres)>42195.001)throw new IllegalArgumentException("Invalid Dream Route segment");}
    }
    public static List<Route> routes(){return Arrays.asList(route("london","London",0,8,2,7,3,9,5,8,4,6,7,6,8,4,6,3,4,4,2,2,5,1,8,2,9,1),route("berlin","Berlin",1,5,2,2,5,1,8,2,9,5,7,7,5,6,3,8,1,6,2,4,5,4,7,5),route("paris","Paris",2,2,5,1,8,2,9,4,7,5,8,8,5,9,3,7,1,6,2,4,4,4,6,5),route("tokyo","Tokyo",2,1,3,3,6,3,8,1,9,3,7,5,5,5,5,8,3,9,2,7,4,6,1,4),route("new-york","New York",5,9,4,7,5,5,4,3,5,1,7,2,6,4,7,6,6,8,4,6,2,7,1,5,2,3,4,4));}
    private static Route route(String id,String name,double... p){var v=new ArrayList<Vertex>();for(int i=0;i<p.length;i+=2)v.add(new Vertex(p[i],p[i+1]));return new Route(id,name,Collections.unmodifiableList(v));}
    public static Assignment assign(double metres,Random random){return assign(metres,random,new HashSet<>(Arrays.asList("london","berlin","paris","tokyo","new-york")));}
    public static Assignment assign(double metres,Random random,Set<String> eligible){if(!Double.isFinite(metres)||metres<=0)throw new IllegalArgumentException("No recorded distance");var routes=new ArrayList<Route>();for(var r:routes())if(eligible.contains(r.id()))routes.add(r);if(routes.isEmpty())throw new IllegalArgumentException("No eligible city");var route=routes.get(random.nextInt(routes.size()));return new Assignment(route.id(),random.nextDouble()*Math.max(0,42195-metres),metres);}
    /** Interpolated drawing position at a marathon-equivalent offset. No GPS is implied. */
    public static Vertex at(Route r,double metres){double length=0;for(int i=1;i<r.vertices().size();i++)length+=distance(r.vertices().get(i-1),r.vertices().get(i));double left=Math.max(0,Math.min(1,metres/42195))*length;for(int i=1;i<r.vertices().size();i++){var a=r.vertices().get(i-1);var b=r.vertices().get(i);double d=distance(a,b);if(left<=d)return new Vertex(a.x()+(b.x()-a.x())*left/d,a.y()+(b.y()-a.y())*left/d);left-=d;}return r.vertices().get(r.vertices().size()-1);}
    private static double distance(Vertex a,Vertex b){return Math.hypot(a.x()-b.x(),a.y()-b.y());}
}
