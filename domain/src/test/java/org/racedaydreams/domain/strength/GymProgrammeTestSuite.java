package org.racedaydreams.domain.strength;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.coaching.Immutable;
public final class GymProgrammeTestSuite {
    static int checks;static void ok(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static final LocalDate START=LocalDate.of(2026,10,26);static final Instant NOW=Instant.parse("2026-10-22T12:00:00Z");
    public static void main(String[] args){
        var generator=new GymProgrammeGenerator();
        for(int frequency=1;frequency<=6;frequency++)for(var goal:GymProgramme.Goal.values())for(var priority:GymProgramme.Priority.values()){
            int minutes=new int[]{30,45,60,75}[(frequency+goal.ordinal())%4];var experience=GymProgramme.Experience.values()[(frequency+priority.ordinal())%3];Set<String> equipment=frequency%2==0?Set.of("full gym"):Set.of("bodyweight");
            var settings=new GymProgramme.Settings(goal,priority,experience,EnumSet.allOf(DayOfWeek.class),frequency,equipment,minutes,GymProgramme.Effort.RPE);
            var runs=List.of(new PlannedWorkout("key",START.plusDays(3),WorkoutCategory.INTERVALS,7.0,null,List.of(),"Quality"),new PlannedWorkout("long",START.plusDays(6),WorkoutCategory.LONG_RUN,14.0,null,List.of(),"Long"));
            var plan=generator.generate("p",1,NOW,"Runner created a block",settings,START,runs,List.of());
            ok(plan.equals(generator.generate("p",1,NOW,"Runner created a block",settings,START,runs,List.of())),"Deterministic complete programme");ok(plan.sessions().size()==frequency*6,"Exact requested frequency");var dates=new HashSet<LocalDate>();
            for(var session:plan.sessions()){ok(dates.add(session.date()),"No duplicated dates");ok(settings.days().contains(session.date().getDayOfWeek()),"Available dates only");int sets=0;for(var p:session.exercises()){ok(ExerciseCatalog.definition(p.exerciseId())!=null,"Canonical exercise exists");ok(GymProgrammeGenerator.available(p.exerciseId(),equipment),"No missing equipment");sets+=p.sets();ok(p.minimumRpe()<=p.maximumRpe(),"Valid effort range");}ok(sets<=18,"Reasonable session volume");if(minutes==30)ok(session.exercises().size()<=4,"Short session budget");}
        }
        var settings=new GymProgramme.Settings(GymProgramme.Goal.SUPPORT_RUNNING,GymProgramme.Priority.RUNNING_FIRST,GymProgramme.Experience.BEGINNER,Set.of(DayOfWeek.TUESDAY,DayOfWeek.FRIDAY),2,Set.of("bodyweight"),30,GymProgramme.Effort.RIR);
        var plan=generator.generate("support",1,NOW,"Runner chose running support",settings,START,List.of(),List.of());ok(plan.sessions().stream().allMatch(s->Set.of(DayOfWeek.TUESDAY,DayOfWeek.FRIDAY).contains(s.date().getDayOfWeek())),"Required weekdays respected");
        var p=new GymProgramme.Prescription("back-squat",2,5,8,null,null,6,8,120,"Controlled reps");
        ok(StrengthProgressionAdvisor.review(p,List.of()).action()==StrengthProgressionAdvisor.Action.GATHER_EVIDENCE,"No fabricated baseline");
        var first=session("a",NOW.minusSeconds(3600),8,7.0);var second=session("b",NOW.minusSeconds(86400),8,7.0);var advice=StrengthProgressionAdvisor.review(p,List.of(first,second));ok(advice.action()==StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,"Repeated controlled top reps");ok(advice.options().contains("Keep current load"),"No forced increase");ok(first.exercises().get(0).sets().get(0).kilograms()==80,"Evidence immutable");
        ok(StrengthProgressionAdvisor.review(p,List.of(session("hard",NOW,5,9.0),second)).action()==StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY,"High effort -> reduce option");ok(StrengthProgressionAdvisor.review(p,List.of(session("unknown",NOW,8,null),second)).action()==StrengthProgressionAdvisor.Action.HOLD,"Missing effort -> hold");
        ok(!GymProgrammeGenerator.available("back-squat",Set.of("barbell")),"Squat needs rack");ok(!GymProgrammeGenerator.available("bench-press",Set.of("barbell","squat rack")),"Bench needs bench");
        System.out.println("GymProgrammeTestSuite: "+checks+" checks passed.");
    }
    static StrengthSession session(String id,Instant at,int reps,Double rpe){var e=ExerciseCatalog.definition("back-squat").exercise();return new StrengthSession(id,at,ZoneOffset.UTC,Duration.ofMinutes(40),rpe,"",List.of(new StrengthSession.ExerciseSets(e,List.of(new StrengthSession.SetEntry(id+"1",reps,80,rpe,false),new StrengthSession.SetEntry(id+"2",reps,80,rpe,false)))));}
}
