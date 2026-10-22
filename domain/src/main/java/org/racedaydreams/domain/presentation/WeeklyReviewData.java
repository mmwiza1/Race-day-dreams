package org.racedaydreams.domain.presentation;
import java.util.*;
import org.racedaydreams.domain.coaching.WeeklyCoaching;
/** Groups existing evidence for disclosure; never changes coaching outcomes. */
public record WeeklyReviewData(List<String> positives,List<String> attention,String recommendation,boolean noChange) {
    public static WeeklyReviewData of(WeeklyCoaching.Review review) {
        var positive=new ArrayList<String>();var attention=new ArrayList<String>();
        for(var c:review.channels())if(c.state()==WeeklyCoaching.State.IMPROVING||c.state()==WeeklyCoaching.State.STABLE)positive.add(c.name()+": "+c.state().name().toLowerCase(Locale.UK).replace('_',' '));
        for(var c:review.channels())if(c.state()==WeeklyCoaching.State.NEEDS_ATTENTION)attention.add(c.name()+": worth reviewing");
        return new WeeklyReviewData(Collections.unmodifiableList(positive),Collections.unmodifiableList(attention),review.recommendation(),review.recommendation().startsWith("No adjustment required"));
    }
}
