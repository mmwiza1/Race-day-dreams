package org.racedaydreams.domain.strength;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.coaching.Immutable;
/** Original deterministic six-week blocks; scheduling penalties are inspectable product heuristics. */
public final class GymProgrammeGenerator {
    public GymProgramme generate(String id,int version,Instant createdAt,String reason,GymProgramme.Settings settings,LocalDate start,
            List<PlannedWorkout> runs,List<StrengthSession> actual) {
        var sessions=new ArrayList<GymProgramme.Session>();var tradeoffs=new LinkedHashSet<String>();LocalDate end=start.plusWeeks(6).minusDays(1);
        var hardActual=StrengthAnalytics.hardLowerBodyDates(actual);
        for(LocalDate week=start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));!week.isAfter(end);week=week.plusWeeks(1)){
            var candidates=new ArrayList<LocalDate>();for(int i=0;i<7;i++){var date=week.plusDays(i);if(!date.isBefore(start)&&!date.isAfter(end)&&settings.days().contains(date.getDayOfWeek()))candidates.add(date);}
            int n=Math.min(settings.frequency(),candidates.size());if(n==0)continue;
            var templates=new ArrayList<String>();for(int i=0;i<n;i++)templates.add(template(settings.frequency(),i));
            var best=new Search(settings,runs,hardActual,templates);best.permute(candidates,new ArrayList<>(),new HashSet<>());
            for(int i=0;i<best.chosen.size();i++){
                var date=best.chosen.get(i);var template=templates.get(i);boolean lower=!template.equals("Upper")&&!template.equals("Push")&&!template.equals("Pull");
                var p=prescriptions(settings,template,i);
                var warnings=conflicts(date,lower,runs,hardActual);tradeoffs.addAll(warnings);
                sessions.add(new GymProgramme.Session(id+"-v"+version+"-"+date,date,template+" "+(i+1),p,
                    "Stable six-week block. "+(settings.goal()==GymProgramme.Goal.SUPPORT_RUNNING?"Hinge, unilateral, calf and trunk work complement the running plan.":"Exercise and rep ranges reflect your selected strength goal.")+" Scheduling uses your available days and "+settings.priority().name().toLowerCase(Locale.ROOT).replace('_',' ')+" priority."));
            }
        }
        sessions.sort(Comparator.comparing(GymProgramme.Session::date));
        if(settings.frequency()>3&&settings.priority()==GymProgramme.Priority.RUNNING_FIRST)tradeoffs.add("You selected frequent gym training with running first. Working sets are deliberately limited; more days are not inherently better.");
        if(!settings.equipment().contains("full gym")&&java.util.Collections.disjoint(settings.equipment(),Immutable.list("dumbbells","barbell","cables","pull-up bar")))tradeoffs.add("No pulling equipment was selected. Trunk work fills a practical slot; it is not equivalent to rows or pull-ups.");
        if(settings.goal()==GymProgramme.Goal.POWER&&settings.experience()==GymProgramme.Experience.BEGINNER)tradeoffs.add("Beginner power preparation uses controlled fundamentals; technical Olympic lifts are not automatically prescribed.");
        return new GymProgramme(id,version,createdAt,reason,settings,start,end,sessions,Immutable.copyList(tradeoffs));
    }
    static String template(int frequency,int index){if(frequency<=3)return "Full body";if(frequency==4)return index%2==0?"Upper":"Lower";if(frequency==5)return new String[]{"Upper","Lower","Push","Pull","Lower"}[index];return new String[]{"Push","Pull","Lower"}[index%3];}
    private static final class Search {
        final GymProgramme.Settings settings;final List<PlannedWorkout> runs;final Set<LocalDate> actual;final List<String> templates;List<LocalDate> chosen=new ArrayList<>();long best=Long.MAX_VALUE;
        Search(GymProgramme.Settings s,List<PlannedWorkout> r,Set<LocalDate>a,List<String>t){settings=s;runs=r;actual=a;templates=t;}
        void permute(List<LocalDate> days,List<LocalDate> current,Set<LocalDate> used){if(current.size()==templates.size()){long score=0;for(int i=0;i<current.size();i++){boolean lower=!(templates.get(i).equals("Upper")||templates.get(i).equals("Push")||templates.get(i).equals("Pull"));int w=settings.priority()==GymProgramme.Priority.RUNNING_FIRST?100:settings.priority()==GymProgramme.Priority.BALANCED?60:30;score+=conflicts(current.get(i),lower,runs,actual).size()*w;for(int j=0;j<i;j++)if(Math.abs(java.time.temporal.ChronoUnit.DAYS.between(current.get(j),current.get(i)))==1&&lower&&templates.get(j).equals("Lower"))score+=40;score+=Math.abs(current.get(i).getDayOfWeek().getValue()-(1+i*7/templates.size()));}if(score<best){best=score;chosen=new ArrayList<>(current);}return;}
            for(var day:days)if(used.add(day)){current.add(day);permute(days,current,used);current.remove(current.size()-1);used.remove(day);}
        }
    }
    public static List<String> conflicts(LocalDate day,boolean lower,List<PlannedWorkout> runs,Set<LocalDate> actual){var out=new ArrayList<String>();if(!lower)return out;
        for(var r:runs)if(demanding(r.category())&&(r.date().equals(day.plusDays(1))||r.date().equals(day)))out.add("Lower-body strength on "+date(day)+" neighbours "+r.category().name().toLowerCase(Locale.ROOT).replace('_',' ')+" on "+date(r.date())+". Consider separating sessions or reducing gym effort; nothing has been removed.");
        if(actual.contains(day.minusDays(1)))out.add("Hard lower-body work was actually recorded before "+date(day)+". Review the proposed session before training.");return Immutable.copyList(out);}
    private static String date(LocalDate d){return d.format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu",Locale.UK));}
    public static boolean demanding(WorkoutCategory c){return c==WorkoutCategory.LONG_RUN||c==WorkoutCategory.INTERVALS||c==WorkoutCategory.TEMPO_THRESHOLD_RUN||c==WorkoutCategory.RACE_PACE_RUN||c==WorkoutCategory.PROGRESSION_RUN||c==WorkoutCategory.RACE||c==WorkoutCategory.TIME_TRIAL;}
    public static boolean available(String id,Set<String> equipment){if(equipment.contains("full gym"))return true;var d=ExerciseCatalog.definition(id);if(d==null)return false;var required=d.equipment();if(required.equals("bodyweight"))return !id.contains("pull-up")&&!id.contains("chin-up")&&!id.equals("inverted-row")&&!id.startsWith("hanging-")||equipment.contains("pull-up bar");if(!equipment.contains(required))return false;if(id.equals("back-squat")||id.equals("front-squat"))return equipment.contains("squat rack");if(id.contains("bench-press"))return equipment.contains("bench")&&(required.equals("dumbbells")||equipment.contains("squat rack"));return true;}
    private static String choose(Set<String> equipment,String...ids){for(String id:ids)if(available(id,equipment))return id;throw new IllegalArgumentException("No suitable exercise for selected equipment.");}
    private static List<GymProgramme.Prescription> prescriptions(GymProgramme.Settings s,String template,int variation){var eq=s.equipment();var ids=new ArrayList<String>();boolean lower=template.equals("Lower")||template.equals("Full body");boolean upper=!template.equals("Lower");
        if(lower){ids.add(choose(eq,variation%2==0?"back-squat":"romanian-deadlift","goblet-squat","bodyweight-squat"));ids.add(choose(eq,s.goal()==GymProgramme.Goal.SUPPORT_RUNNING?"split-squat":"hip-thrust","reverse-lunge-bodyweight","glute-bridge"));}
        if(upper){if(!template.equals("Pull"))ids.add(choose(eq,"bench-press","dumbbell-bench-press","push-up"));if(!template.equals("Push"))ids.add(choose(eq,"seated-cable-row","dumbbell-row","barbell-row","bird-dog"));}
        if(s.goal()==GymProgramme.Goal.SUPPORT_RUNNING&&lower)ids.add(choose(eq,"calf-raise","single-leg-calf-raise-bodyweight"));
        if(s.minutes()>=45&&ids.size()<5)ids.add("plank");
        if(s.minutes()>=60&&upper)ids.add(choose(eq,template.equals("Pull")?"barbell-curl":"overhead-press","dumbbell-shoulder-press","dead-bug"));
        int sets=s.experience()==GymProgramme.Experience.BEGINNER||s.priority()==GymProgramme.Priority.RUNNING_FIRST?2:s.goal()==GymProgramme.Goal.MUSCLE?3:3;
        if(s.frequency()>=4&&s.priority()==GymProgramme.Priority.RUNNING_FIRST)sets=2;
        int minimum=s.goal()==GymProgramme.Goal.STRENGTH?4:s.goal()==GymProgramme.Goal.POWER?3:s.goal()==GymProgramme.Goal.MUSCLE?8:6;
        int maximum=s.goal()==GymProgramme.Goal.STRENGTH?6:s.goal()==GymProgramme.Goal.POWER?5:s.goal()==GymProgramme.Goal.MUSCLE?12:10;
        int minRpe=s.experience()==GymProgramme.Experience.BEGINNER?5:6,maxRpe=s.priority()==GymProgramme.Priority.RUNNING_FIRST?7:8;
        int rest=s.goal()==GymProgramme.Goal.STRENGTH||s.goal()==GymProgramme.Goal.POWER?150:90;
        var out=new ArrayList<GymProgramme.Prescription>();int budget=s.minutes()-6;
        for(String id:new LinkedHashSet<>(ids)){int cost=sets*(rest/60+1)+1;if(budget<cost&&!out.isEmpty())break;budget-=cost;var profile=ExerciseCatalog.definition(id).exercise().measurement();out.add(new GymProgramme.Prescription(id,sets,profile.usesReps()?minimum:0,profile.usesReps()?maximum:0,profile.usesDuration()?30:null,profile.usesDistance()?30.0:null,minRpe,maxRpe,rest,s.goal()==GymProgramme.Goal.POWER?"Move deliberately; stop before speed or control deteriorates. No maximal testing.":"Choose a manageable load or variation within the effort range; progression is optional."));}
        return Immutable.copyList(out);
    }
}
