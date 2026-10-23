package org.racedaydreams.domain.recording;
/** Opt-in heuristic: 30 seconds of very accurate, almost stationary GPS, then five seconds moving.
 * Missing/poor fixes never request a transition. Manual pause is outside this policy.
 */
public final class AutoPausePolicy {
 public enum Decision { NONE, PAUSE, RESUME }
 private RunMetricsEngine.Fix first;private long movingSince=-1;private boolean paused;
 public void reset(){first=null;movingSince=-1;paused=false;}
 public Decision observe(RunMetricsEngine.Fix f){
  if(f.accuracy()==null||f.accuracy()>8||f.speed()==null){first=null;movingSince=-1;return Decision.NONE;}
  if(!paused){if(f.speed()<=0.25){if(first==null)first=f;else if(RunMetricsEngine.metres(first,f)>4)first=f;if(f.monotonicMillis()-first.monotonicMillis()>=30000){paused=true;first=null;return Decision.PAUSE;}}else first=null;}
  else if(f.speed()>=1.2){if(movingSince<0)movingSince=f.monotonicMillis();if(f.monotonicMillis()-movingSince>=5000){paused=false;movingSince=-1;return Decision.RESUME;}}else movingSince=-1;
  return Decision.NONE;
 }
}
