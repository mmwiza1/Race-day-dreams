package org.racedaydreams.domain.support;
import java.util.*;

/** Versioned offline marathon-equivalent geometry. Legacy schematics and approximate participant traces, NOT official course geometry,
 * maps or navigation. Coordinates are drawing units, scaled to a marathon equivalent. */
public final class DreamRoutes {
    public record Vertex(double x,double y) {}
    public record Route(String id,String name,List<Vertex> vertices) {
        public Route {if(id==null||id.trim().isEmpty()||name==null||name.trim().isEmpty()||vertices==null||vertices.size()<2||vertices.size()>10000)throw new IllegalArgumentException("Invalid Dream geometry");vertices=Collections.unmodifiableList(new ArrayList<>(vertices));boolean length=false;for(int i=0;i<vertices.size();i++){var v=vertices.get(i);if(!Double.isFinite(v.x())||!Double.isFinite(v.y())||v.x()<0||v.x()>10||v.y()<0||v.y()>10)throw new IllegalArgumentException("Invalid drawing bounds");if(i>0&&distance(vertices.get(i-1),v)>0)length=true;}if(!length)throw new IllegalArgumentException("Empty route geometry");}
    }
    public record Assignment(String routeId,double startMetres,double coveredMetres) {
        public Assignment {if(!GeometryRegistry.BY_ID.containsKey(routeId)||!Double.isFinite(startMetres)||!Double.isFinite(coveredMetres)||coveredMetres<=0||startMetres<0||startMetres+Math.min(42195,coveredMetres)>42195.001)throw new IllegalArgumentException("Invalid Dream Route segment");}
    }
    private static List<Route> legacyRoutes(){return Arrays.asList(route("london","London",0,8,2,7,3,9,5,8,4,6,7,6,8,4,6,3,4,4,2,2,5,1,8,2,9,1),route("berlin","Berlin",1,5,2,2,5,1,8,2,9,5,7,7,5,6,3,8,1,6,2,4,5,4,7,5),route("paris","Paris",2,2,5,1,8,2,9,4,7,5,8,8,5,9,3,7,1,6,2,4,4,4,6,5),route("tokyo","Tokyo",2,1,3,3,6,3,8,1,9,3,7,5,5,5,5,8,3,9,2,7,4,6,1,4),route("new-york","New York",5,9,4,7,5,5,4,3,5,1,7,2,6,4,7,6,6,8,4,6,2,7,1,5,2,3,4,4));}
    /** V2 independently drawn course-inspired anchors, drawing units rather than geographic data. */
    private static List<Route> cityRoutes(){return Arrays.asList(
        route("london-v2","London",7,8,8,8,9,7.5,9.3,7,8,6.9,7,7.1,6,7.2,5.5,6.5,4.7,6.5,4.3,5.8,5,5.5,5.5,5.7,6,5.3,6,4.5,7,4.4,8,4.8,8.7,4.4,8.5,3.6,7.8,3.2,7.1,3.7,6.7,4.3,5.7,4.1,4.8,4.4,4,4.8,3.1,5.1,2.2,5.7,1.4,5.4,1.1,4.9,1.7,4.7),
        route("berlin-v2","Berlin",4.5,3,3.5,3,2.7,2.4,3,1.8,4.2,1.7,5.3,1.9,6.4,2.2,7.4,2,8.1,2.8,8.6,3.7,7.5,4,6.7,4.1,6.2,5.1,6.7,6,7.4,6.6,6.9,7.5,5.7,7.8,4.4,7.5,3.3,8.2,2.3,8,1.4,7.2,1.3,6.1,2.1,5.4,3,5.8,3.8,5.3,3.3,4.7,3.5,4,4.1,3.4,4.7,3.3),
        route("paris-v2","Paris",3.1,3,4.3,2.9,5.2,3.2,6.2,3.7,7.3,3.8,8.2,4.3,9.1,4.8,9.2,5.9,8.5,6.6,7.6,6.8,7,6,7.5,5.3,6.7,4.9,5.9,4.9,5.1,5.1,4.2,5.4,3.3,5.8,2.5,6.2,1.6,6.8,.9,6.5,.8,5.5,1.2,4.8,1.7,4,2,3.4,1.7,2.8,1.8,2,2.4,1.5,3.2,1.7,3.3,2.4,2.8,2.8),
        route("tokyo-v2","Tokyo",1,3,1.9,3.2,2.7,3,3.4,2.8,4.2,2.7,4.8,3.3,5.4,3.7,5.8,3.3,6.3,2.7,7.2,2.6,8,2.2,8.6,1.5,9,1.8,8.8,2.7,8,3,7.1,3.3,6.6,4,7.4,4.7,8.2,4.5,8.8,4.1,8.3,4.7,7.6,5.1,6.7,4.8,6.2,5.5,6.1,6.5,5.8,7.5,5.6,8.7,5.2,8.8,5.5,7.4,5.8,6.2,5.5,5.2,5,4.6,4.8,4),
        route("new-york-v2","New York",4.7,9.2,6,8.5,6.6,8,6.3,7.1,6.4,6.2,6.7,5.5,7.2,4.8,7,4,6.7,3.6,6.1,3.8,5.7,4.3,5.2,3.8,5.6,3.1,6,2.4,6.1,1.6,5.8,1.1,5,1.5,4.5,2.2,4,2.7,3.5,3.4,3.3,4.1,3,4.8,2.7,5.1,2.1,4.6,2.3,3.8,2.8,3.1,3,2.6));}
    /** Immutable geometry is built once, never rehydrated for each card/recomposition. */
    private static final class GeometryRegistry {
        static final List<Route> CITIES=List.copyOf(cityRoutes());
        static final Map<String,Route> BY_ID=build();
        private static Map<String,Route> build() {
            var routes=new ArrayList<>(legacyRoutes());routes.addAll(CITIES);
            routes.add(Berlin2014Geometry.route());routes.add(Tokyo2026Geometry.route());
            routes.add(LondonHistoricalGeometry.route());routes.add(NewYork2026Geometry.route());routes.add(Paris2024Geometry.route());
            var index=new LinkedHashMap<String,Route>();
            for(var route:routes)if(index.put(route.id(),route)!=null)throw new IllegalStateException("Duplicate geometry revision");
            return Collections.unmodifiableMap(index);
        }
    }
    public static List<Route> routes(){return GeometryRegistry.CITIES;}
    public static Route forId(String id){var route=GeometryRegistry.BY_ID.get(id);if(route==null)throw new IllegalArgumentException("Unknown route geometry");return route;}
    /** City preference IDs stay stable; only new assignments use a new geometry revision. */
    public static Route currentGeometry(Route city) {return city.id().equals("paris-v2")?forId("paris-2024-olympic-geo-v1"):city.id().equals("new-york-v2")?forId("new-york-2026-geo-v1"):city.id().equals("london-v2")?forId("london-historical-geo-v1"):city.id().equals("berlin-v2")?forId("berlin-2014-geo-v1"):city.id().equals("tokyo-v2")?forId("tokyo-2026-geo-v1"):city;}
    public static String provenance(Route route) {return route.id().equals("paris-2024-olympic-geo-v1")?
        "Paris2024 Olympic marathon community geometry by Thomas Krakow (Running-tomy), via Wikimedia Commons, ODbL-1.0. This is not the annual Paris Marathon, official/certified geometry or navigation. The marathon-equivalent axis is separate from the source line.":route.id().equals("new-york-2026-geo-v1")?
        "Approximate New York2026 road centreline, © OpenStreetMap contributors, prepared by MARATHON TRIP; ODbL-1.0 data. Not official/certified geometry or navigation. The marathon-equivalent axis is separate from the approximate42.67 km retained line.":route.id().equals("london-historical-geo-v1")?
        "Approximate historical London course line published2023 by Georgios Karamanis under MIT. Course edition and survey accuracy unspecified. Not an official current course or navigation. The marathon-equivalent axis is separate from the approximate42.78 km source line.":route.id().equals("tokyo-2026-geo-v1")?
        "Approximate Tokyo2026 participant GPS trace, simplified from CC0 data published by kekomat/marathon-gpx-dataset. Not an official race map or navigation. The marathon-equivalent axis is separate from the observed42.75 km trace.":route.id().equals("berlin-2014-geo-v1")?
        "Approximate Berlin2014 participant GPS trace, simplified from an MIT-licensed source by Alexandre Donciu-Julin. Not a current official course or navigation. The marathon-equivalent axis is separate from the observed43.01 km trace.":
        "Original v2 course-inspired schematic; geographic replacement is still in progress. Not official course artwork or navigation.";}
    public record Landmark(String label,double x,double y,String shape){}
    public static List<Landmark> landmarks(Route r){var geographic=DreamCartography.landmarks(r.id());if(!geographic.isEmpty())return geographic;String id=r.id().replace("-v2","");return switch(id){
        case "london"->Arrays.asList(new Landmark("Tower Bridge",5,6,"bridge"),new Landmark("Thames",3,6.8,"river"),new Landmark("Palace",1.4,4,"gate"));
        case "berlin"->Arrays.asList(new Landmark("Brandenburg Gate",4.7,2.6,"gate"),new Landmark("Tiergarten",2.2,3.2,"park"),new Landmark("City skyline",7.4,1,"tower"));
        case "paris"->Arrays.asList(new Landmark("Eiffel Tower",3.7,6.7,"tower"),new Landmark("Seine",5.8,5.7,"river"),new Landmark("Arc",2.2,2.2,"gate"));
        case "tokyo"->Arrays.asList(new Landmark("City skyline",1,2,"tower"),new Landmark("Asakusa cue",8.6,1,"gate"),new Landmark("Tower cue",4.5,8,"tower"));
        default->Arrays.asList(new Landmark("Bridge",5.2,8.6,"bridge"),new Landmark("Central Park",2.6,3.8,"park"),new Landmark("Skyline",3.6,6,"tower"));};}
    private static Route route(String id,String name,double... p){var v=new ArrayList<Vertex>();for(int i=0;i<p.length;i+=2)v.add(new Vertex(p[i],p[i+1]));return new Route(id,name,Collections.unmodifiableList(v));}
    public static Assignment assign(double metres,Random random){return assign(metres,random,new HashSet<>(Arrays.asList("london-v2","berlin-v2","paris-v2","tokyo-v2","new-york-v2")));}
    public static Assignment assign(double metres,Random random,Set<String> eligible){if(!Double.isFinite(metres)||metres<=0)throw new IllegalArgumentException("No recorded distance");var routes=new ArrayList<Route>();for(var r:routes())if(eligible.contains(r.id()))routes.add(r);if(routes.isEmpty())throw new IllegalArgumentException("No eligible city");var route=currentGeometry(routes.get(random.nextInt(routes.size())));return new Assignment(route.id(),random.nextDouble()*Math.max(0,42195-metres),metres);}
    /** Interpolated drawing position at a marathon-equivalent offset. No GPS is implied. */
    public static Vertex at(Route r,double metres){if(!Double.isFinite(metres))throw new IllegalArgumentException("Invalid route progress");if(metres<=0)return r.vertices().get(0);if(metres>=42195)return r.vertices().get(r.vertices().size()-1);double length=0;for(int i=1;i<r.vertices().size();i++)length+=distance(r.vertices().get(i-1),r.vertices().get(i));double left=Math.max(0,Math.min(1,metres/42195))*length;for(int i=1;i<r.vertices().size();i++){var a=r.vertices().get(i-1);var b=r.vertices().get(i);double d=distance(a,b);if(d==0)continue;if(left<=d)return new Vertex(a.x()+(b.x()-a.x())*left/d,a.y()+(b.y()-a.y())*left/d);left-=d;}return r.vertices().get(r.vertices().size()-1);}
    /** Exact clipped polyline: retain all intervening original vertices, not sampled chords. */
    public static List<Vertex> section(Route route,double from,double to) {
        if(!Double.isFinite(from)||!Double.isFinite(to)||to<from)throw new IllegalArgumentException("Invalid route section");
        double low=Math.max(0,Math.min(42195,from)),high=Math.max(0,Math.min(42195,to));
        var points=new ArrayList<Vertex>();points.add(at(route,low));
        if(high==low)return Collections.unmodifiableList(points);
        double total=0;for(int i=1;i<route.vertices().size();i++)total+=distance(route.vertices().get(i-1),route.vertices().get(i));
        double cumulative=0;
        for(int i=1;i<route.vertices().size();i++) {cumulative+=distance(route.vertices().get(i-1),route.vertices().get(i));double equivalent=cumulative/total*42195;
            if(equivalent>low&&equivalent<high&&!route.vertices().get(i).equals(points.get(points.size()-1)))points.add(route.vertices().get(i));}
        var end=at(route,high);if(!end.equals(points.get(points.size()-1)))points.add(end);
        return Collections.unmodifiableList(points);
    }
    private static double distance(Vertex a,Vertex b){return Math.hypot(a.x()-b.x(),a.y()-b.y());}
}
