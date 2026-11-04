package org.racedaydreams.domain.presentation;

import java.time.*;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.coaching.*;
import org.racedaydreams.domain.strength.StrengthSession;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.StrengthProgramme;

/** Labelled calendar entries. A blank date is never sufficient evidence of rest. */
public final class CalendarData {
    private CalendarData() { }
    public enum Category { EASY, QUALITY, LONG, STRENGTH, RACE, REST, RUN }
    public enum State { PLANNED, COMPLETED, NOT_COMPLETED, MOVED, SKIPPED }
    public record Entry(Category category, State state, String label, String id, String activityId) {
        public String description() { return label + ": " + state.name().toLowerCase(Locale.UK).replace('_',' '); }
    }
    /** Decorate the existing calendar without changing its category/state/mask semantics. */
    public static List<Entry> withGym(LocalDate date,LocalDate today,List<Entry> original,GymProgramme gym,Map<String,String> actualByPrescription){
        if(gym==null||date.isBefore(gym.start())||date.isAfter(gym.end()))return original;
        var out=new ArrayList<Entry>();
        for(var e:original)if(!(e.category()==Category.STRENGTH&&e.state()==State.PLANNED&&e.label().equals("Strength routine")))out.add(e);
        for(var s:gym.sessions())if(s.date().equals(date)){
            out.removeIf(e->e.category()==Category.REST);
            // Actual completion requires an explicit preserved link, not merely a gym visit that day.
            if(!actualByPrescription.containsKey(s.id()))out.add(new Entry(Category.STRENGTH,date.isBefore(today)?State.NOT_COMPLETED:State.PLANNED,s.title()+" · programmed strength",s.id(),null));
        }
        return Immutable.copyList(out);
    }
    /** Authored slots remain frozen; actual completion still requires an explicit valid link. */
    public static List<Entry> withAuthoredGym(LocalDate date, LocalDate today, List<Entry> original,
            StrengthProgramme programme, Map<String,String> actualByPrescription) {
        if (programme == null || date.isBefore(programme.start()) || date.isAfter(programme.end())) return original;
        var out = new ArrayList<Entry>();
        for (var entry : original)
            if (!(entry.category() == Category.STRENGTH && entry.state() == State.PLANNED && entry.label().equals("Strength routine"))) out.add(entry);
        for (var workout : programme.workouts()) if (workout.date().equals(date)) {
            out.removeIf(entry -> entry.category() == Category.REST);
            if (!actualByPrescription.containsKey(workout.id())) out.add(new Entry(Category.STRENGTH,
                    date.isBefore(today) ? State.NOT_COMPLETED : State.PLANNED,
                    workout.definition().title() + " · programmed strength", workout.id(), null));
        }
        return Immutable.copyList(out);
    }
    public static Category category(WorkoutCategory c) {
        return switch(c) {
            case RECOVERY_RUN, EASY_RUN, ZONE_2_RUN -> Category.EASY;
            case LONG_RUN -> Category.LONG;
            case RACE -> Category.RACE;
            case CUSTOM_RUN -> Category.RUN;
            default -> Category.QUALITY;
        };
    }
    public static boolean outsideMonth(LocalDate date, LocalDate month) { return !YearMonth.from(date).equals(YearMonth.from(month)); }
    public static List<Entry> day(LocalDate date, LocalDate today, PlanVersion plan, List<PlannedWorkout> workouts,
            List<ActivityRecord> activities, List<StrengthSession> strength, Map<String,LocalDate> strengthLinks, List<WorkoutDecision> decisions) {
        var out = new ArrayList<Entry>(); var represented = new HashSet<String>();
        var latest = new HashMap<String,WorkoutDecision>();
        for(var d:decisions) { var old=latest.get(d.workoutId()); if(old==null||old.at().isBefore(d.at()))latest.put(d.workoutId(),d); }
        for(var w:workouts) if(w.date().equals(date)) {
            var a=activities.stream().filter(r->w.id().equals(r.plannedWorkoutId())).findFirst().orElse(null);
            var decision=latest.get(w.id());
            if(a!=null) {
                if(runDate(a).equals(date)){out.add(new Entry(category(w.category()),State.COMPLETED,label(w.category()),w.id(),a.id()));represented.add(a.id());}
                else out.add(new Entry(category(w.category()),State.MOVED,label(w.category())+" · completed on another day",w.id(),a.id()));
            } else if(decision==null||decision.kind()!=WorkoutDecision.Kind.SKIPPED)
                out.add(new Entry(category(w.category()),date.isBefore(today)?State.NOT_COMPLETED:State.PLANNED,label(w.category()),w.id(),null));
        }
        for(var a:activities)if(runDate(a).equals(date)&&represented.add(a.id())) {
            var w=workouts.stream().filter(p->p.id().equals(a.plannedWorkoutId())).findFirst().orElse(null);
            out.add(new Entry(w==null?Category.RUN:category(w.category()),State.COMPLETED,w==null?"Recorded run":label(w.category()),a.plannedWorkoutId(),a.id()));
        }
        for(var s:strength)if(s.startedAt().atOffset(s.offset()).toLocalDate().equals(date))out.add(new Entry(Category.STRENGTH,State.COMPLETED,"Strength · "+s.duration().toMinutes()+" min",s.id(),null));
        if(plan!=null&&date.compareTo(plan.plan().startDate())>=0&&date.compareTo(plan.plan().endDate())<=0&&plan.goalSnapshot().preferences().gymDays().contains(date.getDayOfWeek())&&strength.stream().noneMatch(s->s.startedAt().atOffset(s.offset()).toLocalDate().equals(date))&&strengthLinks.values().stream().noneMatch(date::equals))out.add(new Entry(Category.STRENGTH,State.PLANNED,"Strength routine",null,null));
        for(var link:strengthLinks.entrySet())if(link.getValue().equals(date))strength.stream().filter(s->s.id().equals(link.getKey())&&!s.startedAt().atOffset(s.offset()).toLocalDate().equals(date)).findFirst().ifPresent(s->out.add(new Entry(Category.STRENGTH,State.MOVED,"Strength routine · recorded on "+s.startedAt().atOffset(s.offset()).toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu",Locale.UK)),s.id(),null)));
        for(var d:latest.values())if(d.originalDate().equals(date)) {
            var w=workouts.stream().filter(p->p.id().equals(d.workoutId())).findFirst().orElse(null);
            if(d.kind()==WorkoutDecision.Kind.SKIPPED||!date.equals(d.movedTo()))out.add(new Entry(w==null?Category.RUN:category(w.category()),d.kind()==WorkoutDecision.Kind.SKIPPED?State.SKIPPED:State.MOVED,d.kind()==WorkoutDecision.Kind.SKIPPED?"Skipped by choice":"Moved to "+d.movedTo().format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu",Locale.UK)),d.workoutId(),null));
        }
        // An available slot deliberately left free by an active generated plan is planned rest.
        // Unavailable dates, dates outside the plan, and moved/skipped dates stay distinct.
        if(out.isEmpty()&&plan!=null&&!date.isBefore(plan.plan().startDate())&&!date.isAfter(plan.plan().endDate())&&plan.goalSnapshot().preferences().runningDays().contains(date.getDayOfWeek()))out.add(new Entry(Category.REST,State.PLANNED,"Rest day",null,null));
        return Collections.unmodifiableList(out);
    }
    private static LocalDate runDate(ActivityRecord a) { return a.start().atOffset(a.startOffset()==null?ZoneOffset.UTC:a.startOffset()).toLocalDate(); }
    public static String label(WorkoutCategory c) { return switch(c) { case TEMPO_THRESHOLD_RUN -> "Tempo run";case ZONE_2_RUN -> "Zone 2 run"; default -> c.name().toLowerCase(Locale.UK).replace('_',' ').replaceFirst("^.",c.name().substring(0,1)); }; }
}
