package org.racedaydreams.domain.coaching;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.strength.*;
/** Local deterministic briefing and one-off movement previews. Never changes recurring availability. */
public final class WorkoutContext {
    private WorkoutContext(){}
    public record Briefing(String title,List<String> observations){}
    public static Briefing briefing(PlannedWorkout workout,List<PlannedWorkout> plan,List<ActivityRecord> actual,List<StrengthSession> strength,WeeklyCoaching.Review review){
        var reasons=new ArrayList<String>();boolean hardGym=StrengthAnalytics.hardLowerBodyDates(strength).contains(workout.date().minusDays(1));
        if(hardGym)reasons.add("Hard lower-body strength was recorded yesterday. Start conservatively and adjust if your legs feel unusually fatigued.");
        Set<String> similar=new HashSet<>();for(var w:plan)if(w.category()==workout.category()&&!w.date().isAfter(workout.date()))similar.add(w.id());
        var recent=new ArrayList<ActivityRecord>();for(var a:actual)if(similar.contains(a.plannedWorkoutId())&&a.start().atOffset(a.startOffset()==null?ZoneOffset.UTC:a.startOffset()).toLocalDate().isBefore(workout.date()))recent.add(a);
        recent.sort(Comparator.comparing(ActivityRecord::start).reversed());long high=recent.stream().limit(3).filter(a->a.rpe()!=null&&a.rpe()>=8).count();
        if(high>=2)reasons.add("Recent similar sessions had high reported effort. Keep the opening work controlled.");
        if(review!=null&&review.channels().stream().anyMatch(c->c.state()==WeeklyCoaching.State.NEEDS_ATTENTION))reasons.add("Your weekly review has evidence worth checking before this session. Recommendations remain optional.");
        if(reasons.isEmpty())reasons.add(recent.isEmpty()?"We are still gathering similar-session evidence. Begin at the intended effort and adjust to conditions.":"Recent comparable sessions are available. Keep the opening work controlled; completion alone does not prove fitness improved.");
        return new Briefing(hardGym||high>=2?"Take care with today's intensity":recent.isEmpty()?"Start with the planned effort":"Keep today's purpose in focus",Immutable.copyList(reasons));
    }
    public static List<String> moveWarnings(PlannedWorkout workout,LocalDate proposed,List<PlannedWorkout> plan,List<StrengthSession> strength){
        var out=new ArrayList<String>();if(proposed==null)return Immutable.list("Choose a valid date.");
        if(StrengthAnalytics.hardLowerBodyDates(strength).contains(proposed.minusDays(1))&&GymProgrammeGenerator.demanding(workout.category()))out.add("This places a demanding run immediately after recorded hard lower-body strength.");
        for(var other:plan)if(!other.id().equals(workout.id())&&GymProgrammeGenerator.demanding(other.category())&&GymProgrammeGenerator.demanding(workout.category())&&Math.abs(java.time.temporal.ChronoUnit.DAYS.between(proposed,other.date()))==1)out.add("A demanding running session is on the neighbouring day.");
        if(out.isEmpty())out.add("No neighbouring conflict was found in the available evidence. This is not medical readiness advice.");
        out.add("Only this workout will move. Your normal training days and other workouts stay unchanged.");return Immutable.copyList(out);
    }
}
