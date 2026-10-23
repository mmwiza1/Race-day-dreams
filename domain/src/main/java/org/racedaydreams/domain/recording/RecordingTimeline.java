package org.racedaydreams.domain.recording;
import java.util.*;
/** Active recorded time includes GPS outages but excludes explicit manual/automatic pause intervals. */
public final class RecordingTimeline {
 private RecordingTimeline(){}
 public static long activeMillis(long start,long end,List<RunMetricsEngine.Event> input){
  if(start<0||end<start)throw new IllegalArgumentException("Invalid recording bounds");
  var events=new ArrayList<>(input);events.sort(Comparator.comparingLong(RunMetricsEngine.Event::monotonicMillis));
  long paused=-1,total=0;
  for(var e:events){long t=e.monotonicMillis();if(t<start||t>end)continue;
   if((e.type().equals("PAUSE")||e.type().equals("AUTO_PAUSE"))&&paused<0)paused=t;
   else if((e.type().equals("RESUME")||e.type().equals("AUTO_RESUME"))&&paused>=0){total+=t-paused;paused=-1;}
  }
  if(paused>=0)total+=end-paused;
  return Math.max(0,end-start-total);
 }
}
