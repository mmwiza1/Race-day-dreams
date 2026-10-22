package org.racedaydreams.domain.strength;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.coaching.Immutable;
/** Immutable prescribed strength. Actual sets remain exclusively in StrengthSession. */
public record GymProgramme(String id,int version,Instant createdAt,String reason,Settings settings,
        LocalDate start,LocalDate end,List<Session> sessions,List<String> tradeoffs) {
    public enum Goal { SUPPORT_RUNNING, STRENGTH, MUSCLE, GENERAL, POWER }
    public enum Priority { RUNNING_FIRST, BALANCED, STRENGTH_FIRST }
    public enum Experience { BEGINNER, INTERMEDIATE, EXPERIENCED }
    public enum Effort { RPE, RIR }
    public record Settings(Goal goal,Priority priority,Experience experience,Set<DayOfWeek> days,int frequency,
            Set<String> equipment,int minutes,Effort effort) {
        public Settings { Objects.requireNonNull(goal);Objects.requireNonNull(priority);Objects.requireNonNull(experience);Objects.requireNonNull(effort);days=Immutable.copySet(days);equipment=Immutable.copySet(equipment);
            if(frequency<1||frequency>6||frequency>days.size()||!(minutes==30||minutes==45||minutes==60||minutes==75))throw new IllegalArgumentException("Choose 1–6 available days and a supported session length.");
            var supported=Immutable.list("bodyweight","dumbbells","barbell","squat rack","bench","machines","cables","kettlebells","pull-up bar","trap bar","sled","full gym");
            if(equipment.stream().anyMatch(e->!supported.contains(e)))throw new IllegalArgumentException("Unsupported equipment selection.");
        }
    }
    public record Prescription(String exerciseId,int sets,int minimumReps,int maximumReps,Integer seconds,Double metres,
            int minimumRpe,int maximumRpe,int restSeconds,String intent) {
        public Prescription {
            if(exerciseId==null||exerciseId.isEmpty()||sets<1||sets>6||minimumReps<0||maximumReps<minimumReps||maximumReps>100||minimumRpe<1||maximumRpe>10||minimumRpe>maximumRpe||restSeconds<30||restSeconds>300||intent==null)throw new IllegalArgumentException("Invalid strength prescription.");
            if((minimumReps>0?1:0)+(seconds!=null?1:0)+(metres!=null?1:0)!=1||seconds!=null&&(seconds<5||seconds>600)||metres!=null&&(!Double.isFinite(metres)||metres<=0||metres>1000))throw new IllegalArgumentException("Choose one valid measurement for this prescription.");
        }
    }
    public record Session(String id,LocalDate date,String title,List<Prescription> exercises,String explanation) {
        public Session { if(id==null||id.isEmpty()||date==null||title==null||title.isEmpty()||explanation==null)throw new IllegalArgumentException("Invalid strength session.");exercises=Immutable.copyList(exercises);if(exercises.isEmpty()||exercises.size()>10)throw new IllegalArgumentException("A session requires 1–10 exercises."); }
    }
    public GymProgramme {if(id==null||id.isEmpty()||version<1||createdAt==null||reason==null||reason.isEmpty()||start==null||end==null||end.isBefore(start)||Duration.between(start.atStartOfDay(),end.atStartOfDay()).toDays()>56)throw new IllegalArgumentException("Invalid programme version or block.");Objects.requireNonNull(settings);sessions=Immutable.copyList(sessions);tradeoffs=Immutable.copyList(tradeoffs);Set<String> ids=new HashSet<>();Set<LocalDate> dates=new HashSet<>();for(var s:sessions){if(!ids.add(s.id())||!dates.add(s.date())||s.date().isBefore(start)||s.date().isAfter(end))throw new IllegalArgumentException("Invalid or duplicated session placement.");}}
}
