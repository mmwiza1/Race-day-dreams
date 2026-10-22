package org.racedaydreams.domain.presentation;
import java.util.*;
import org.racedaydreams.domain.coaching.*;
/** Reuses the weekly coach's evidence; no competing readiness score or fabricated missing metrics. */
public record HomeCoachingSummary(String title,List<String> observations,String recommendation) {
    public static HomeCoachingSummary from(WeeklyCoaching.Review review) {
        if(review==null)return new HomeCoachingSummary("Still learning your training",Immutable.list("Record training and review your running baseline."),"Continue gathering evidence");
        boolean attention=review.channels().stream().anyMatch(c->c.state()==WeeklyCoaching.State.NEEDS_ATTENTION);
        boolean known=review.channels().stream().anyMatch(c->c.state()!=WeeklyCoaching.State.INSUFFICIENT_EVIDENCE);
        var notes=new ArrayList<String>();notes.add(review.completed()+" / "+review.planned()+" planned runs completed");
        for(var c:review.channels())if(c.state()!=WeeklyCoaching.State.INSUFFICIENT_EVIDENCE&&notes.size()<4)notes.add(c.name()+": "+c.state().name().toLowerCase(Locale.ROOT).replace('_',' '));
        return new HomeCoachingSummary(attention?"Something may need adjusting":known?"Training is on track":"Still learning your training",Immutable.copyList(notes),review.recommendation());
    }
}
