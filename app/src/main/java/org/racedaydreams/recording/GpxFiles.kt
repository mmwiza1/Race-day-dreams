package org.racedaydreams.recording

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.domain.recording.RunMetricsEngine
import android.content.Context
import java.io.*
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

interface ActivityFileImporter {fun importFrom(input:InputStream):String}
/** FIT intentionally has no implementation or SDK dependency in this release. */
interface FutureFitImporter:ActivityFileImporter

/** Original bounded parser over Android's maintained XML parser. No DTD/entities/network access. */
class GpxFiles(private val context:Context):ActivityFileImporter {
    data class RoutePoint(val time:Instant,val lat:Double,val lon:Double,val altitude:Double?,val segment:Int)
    companion object {
        const val MAX_BYTES=24*1024*1024;const val MAX_POINTS=100000
        fun parse(input:InputStream):List<RoutePoint>{
            val bytes=ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(bytes.size()+n<=MAX_BYTES){"GPX exceeds 24 MiB limit"};bytes.write(buffer,0,n)}
            val parser=Xml.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);parser.setInput(ByteArrayInputStream(bytes.toByteArray()),null)
            val out=mutableListOf<RoutePoint>();var segment=0;var tracks=0;var root=false;var lat:Double?=null;var lon:Double?=null;var altitude:Double?=null;var time:Instant?=null;var inPoint=false
            var event=parser.eventType
            while(event!=XmlPullParser.END_DOCUMENT){
                require(parser.depth<=16){"GPX nesting too deep"}
                require(event!=XmlPullParser.DOCDECL&&(event!=XmlPullParser.ENTITY_REF||parser.name in setOf("amp","lt","gt","apos","quot"))){"GPX entities/DOCTYPE are not supported"}
                if(event==XmlPullParser.START_TAG){when(parser.name){
                    "gpx"->{require(parser.depth==1);root=true}
                    "trk"->{require(++tracks<=1){"Import one recorded track per file"}}
                    "trkseg"->segment++
                    "trkpt"->{require(root&&!inPoint);inPoint=true;lat=parser.getAttributeValue(null,"lat")?.toDouble();lon=parser.getAttributeValue(null,"lon")?.toDouble();altitude=null;time=null}
                    "ele"->if(inPoint)altitude=parser.nextText().toDouble()
                    "time"->if(inPoint)time=Instant.parse(parser.nextText())
                }}
                if(event==XmlPullParser.END_TAG&&parser.name=="trkpt"){
                    require(inPoint&&lat!=null&&lon!=null&&time!=null){"Every imported track point needs coordinates and a timestamp"};val p=RoutePoint(time!!,lat!!,lon!!,altitude,segment);RunMetricsEngine.Fix(p.time,0,p.lat,p.lon,null,p.altitude,null,null)
                    require(out.isEmpty()||p.time>out.last().time){"GPX times must increase"};require(out.size<MAX_POINTS){"Too many GPX points"};out.add(p);inPoint=false
                }
                event=parser.nextToken()
            }
            require(root&&out.size>=2){"No timestamped GPX track found"};require(java.time.Duration.between(out.first().time,out.last().time)<=java.time.Duration.ofHours(24)){"GPX tracks longer than 24 hours are not supported"};return out
        }
    }
    override fun importFrom(input:InputStream):String {
        val route=parse(input);val start=route.first().time;val engine=RunMetricsEngine(0,start);var previous=-1
        for(p in route){val t=java.time.Duration.between(start,p.time).toMillis();if(previous!=-1&&previous!=p.segment)engine.breakSegment(t,p.time);engine.accept(RunMetricsEngine.Fix(p.time,t,p.lat,p.lon,null,p.altitude,null,null));previous=p.segment}
        require(engine.points().size>=2&&engine.status(java.time.Duration.between(start,route.last().time).toMillis()).metres()>0){"Insufficient reliable continuous GPX evidence"}
        val digest=MessageDigest.getInstance("SHA-256");for(p in route)digest.update("${p.time}|${p.lat}|${p.lon}|${p.segment}\n".toByteArray());val id="gpx-"+digest.digest().joinToString(""){"%02x".format(it)}
        val store=RecordingStore(context);store.existingActivity(id)?.let{return it}
        // Export/re-import of exactly retained coordinates/timestamps is the same evidence.
        for((existing,wall) in store.finished())if(kotlin.math.abs(java.time.Duration.between(Instant.parse(wall),start).seconds)<=600){val points=store.points(existing);if(points.size==route.size&&points.indices.all{i->val a=points[i].fix();val b=route[i];a.timestamp()==b.time&&a.latitude()==b.lat&&a.longitude()==b.lon})store.existingActivity(existing)?.let{return it}}
        require(store.unfinished()==null){"Finish or recover the active recording before importing GPX"}
        val last=java.time.Duration.between(start,route.last().time).toMillis()
        store.importRecording(id,start,last,engine.points(),engine.events())
        return store.finish(RecordingStore.Session(id,"ACTIVE",start,0,last),last,"gpx")
    }
    fun exportTo(id:String,output:OutputStream){
        val points=RecordingStore(context).points(id);require(points.size>=2){"No route samples retained for this recording"}
        val writer=output.bufferedWriter(Charsets.UTF_8);writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"Race Day Dreams\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>Recorded run</name>")
        var segment:Int?=null
        for(p in points){if(segment!=p.segment()){if(segment!=null)writer.write("</trkseg>");writer.write("<trkseg>");segment=p.segment()};val f=p.fix();writer.write("<trkpt lat=\"${f.latitude()}\" lon=\"${f.longitude()}\">");f.altitude()?.let{writer.write("<ele>$it</ele>")};writer.write("<time>${f.timestamp()}</time></trkpt>")};writer.write("</trkseg></trk></gpx>");writer.flush()
    }
}
