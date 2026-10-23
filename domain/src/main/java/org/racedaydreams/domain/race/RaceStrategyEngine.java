package org.racedaydreams.domain.race;
import java.util.*;
/** Target-based strategy, independent of predictions and never a goal mutation. */
public final class RaceStrategyEngine {
    public record Stage(double from,double to,Double fastPace,Double slowPace,String guidance) {}
    public static List<Stage> create(double metres,Long targetSeconds,boolean hilly){
        if(!Double.isFinite(metres)||metres<=0||targetSeconds!=null&&targetSeconds<=0)throw new IllegalArgumentException("Invalid race target");
        if(hilly||targetSeconds==null)return Collections.singletonList(new Stage(0,metres,null,null,"Run by effort. Keep the opening controlled; progress late only if comfortable. No grade-adjusted pace is inferred."));
        double pace=targetSeconds/(metres/1000);return Arrays.asList(new Stage(0,metres*.15,pace*1.01,pace*1.035,"Controlled opening; avoid chasing seconds."),new Stage(metres*.15,metres*.85,pace*.995,pace*1.01,"Settle into your chosen target. Conditions and effort take precedence."),new Stage(metres*.85,metres,null,null,"Hold rhythm. Progress only if comfortable; no sprint is required."));
    }
}
