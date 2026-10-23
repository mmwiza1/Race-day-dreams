package org.racedaydreams.domain.support;
import java.util.*;

/** Original short supportive routines. Never calendar workouts, load or adherence targets. */
public final class RunningRoutines {
    public record Movement(String id,String title,String dose,String instructions,String cue,String mistake,String pose) {}
    public record Routine(String id,String title,String purpose,int minutes,List<Movement> movements) {}
    private static Movement move(String id,String title,String dose,String text,String cue,String mistake,String pose){return new Movement(id,title,dose,text,cue,mistake,pose);}
    public static final List<Movement> MOVEMENTS=Collections.unmodifiableList(Arrays.asList(
        move("jog","Easy walk or jog","2–5 min","Start comfortably and gradually settle into conversational movement.","Relax your shoulders.","Starting hard before you feel prepared.","walk"),
        move("ankle","Ankle rocks","8 each side","With one foot ahead, gently move your knee forward over the foot; keep the heel down.","Use a comfortable range.","Forcing the heel up or pushing into pain.","lunge"),
        move("circles","Ankle circles","6 each direction","With support if needed, lift one foot slightly and circle the ankle slowly.","Move the ankle rather than your whole leg.","Rushing or forcing range.","stand"),
        move("calf","Calf raises","8–10","Hold support, rise onto the forefoot and lower under control.","Keep pressure comfortable across the forefoot.","Bouncing or losing balance.","calf"),
        move("swing","Supported leg swings","8 each side","Hold steady support and swing one leg gently forward and back.","Keep the trunk steady.","Swinging forcefully or twisting.","swing"),
        move("lunge","Walking lunges","6 each side","Step forward, lower the back knee comfortably, then stand and change sides.","Keep your torso upright.","Taking an unstable step or forcing depth.","lunge"),
        move("march","Controlled marching","30 sec","March with gentle alternating knee lifts and natural arm movement.","Build rhythm, not height.","Leaning backwards.","march"),
        move("stride","Controlled strides","3 × 15 sec, easy walk between","On a clear flat space, gradually accelerate to relaxed brisk running, then ease down. Recover fully.","Smooth rather than maximal.","Sprinting or doing strides when uncomfortable.","walk"),
        move("bridge","Glute bridges","8","Lie on your back with knees bent and feet planted. Gently lift hips, pause, then lower.","Use a comfortable hip extension.","Overarching the lower back.","bridge"),
        move("hip","Hip-flexor stretch","20 sec each side","From a supported split stance or half-kneeling position, gently shift forward.","Keep the pelvis and trunk steady.","Forcing into discomfort or arching.","lunge"),
        move("glute","Seated glute stretch","20 sec each side","Sit securely, place an ankle across the opposite thigh if comfortable and lean forward slightly.","Use a gentle stretch.","Pressing the knee down forcefully.","sit"),
        move("calf-stretch","Calf stretch","20 sec each side","Place one foot behind, heel down, and lean gently toward a wall or support.","Keep the back foot comfortably forward.","Bouncing or forcing range.","lunge"),
        move("quad","Supported quad stretch","20 sec each side","Hold support. Bend one knee and hold the ankle only if comfortably reachable.","Stay balanced; use less bend if needed.","Pulling hard or twisting the knee.","quad"),
        move("walk","Easy walk","2–3 min","Gradually ease down to comfortable walking.","Let breathing settle naturally.","Treating this as compulsory recovery treatment.","walk")
    ));
    private static Routine routine(String id,String title,String purpose,int minutes,String... ids){var list=new ArrayList<Movement>();for(String key:ids)list.add(MOVEMENTS.stream().filter(m->m.id().equals(key)).findFirst().orElseThrow());return new Routine(id,title,purpose,minutes,Collections.unmodifiableList(list));}
    public static List<Routine> all(){return Arrays.asList(
        routine("warm-easy","Easy-run preparation","Short, optional comfortable preparation.",3,"jog","ankle"),
        routine("warm-quality","Quality-session warm-up","Dynamic preparation before demanding running. Count the easy jog within any prescribed warm-up, rather than adding mileage.",8,"jog","ankle","swing","march","stride"),
        routine("warm-long","Long-run preparation","Short preparation without unnecessary expenditure.",5,"jog","ankle","calf"),
        routine("warm-race-short","5K / 10K race warm-up","Comfortable dynamic preparation; strides only if familiar and comfortable.",10,"jog","swing","march","stride"),
        routine("warm-race-half","Half-marathon warm-up","Moderate preparation, conserving energy.",6,"jog","ankle","march"),
        routine("warm-race-marathon","Marathon preparation","Brief preparation; conserve energy for the distance.",3,"walk","ankle"),
        routine("cool-easy","Optional easy-run cool-down","A short comfortable finish.",3,"walk"),
        routine("cool-demanding","After demanding running","Optional short walk and gentle stretches; not a guarantee of recovery.",6,"walk","calf-stretch","hip","glute"),
        routine("mobility-five","5-minute runner mobility","A flexible way to support comfortable movement.",5,"ankle","swing","bridge","hip"),
        routine("mobility-ten","10-minute runner mobility","Short general movement practice.",10,"march","circles","swing","lunge","bridge","calf-stretch","hip"),
        routine("hips","Hips & glutes","May help with ordinary stiffness; not treatment.",8,"march","lunge","bridge","hip","glute"),
        routine("ankles","Calves & ankles","Gentle ankle and calf movement.",6,"circles","ankle","calf","calf-stretch"),
        routine("post-long","Post-long-run mobility","Optional gentle movement after a long run.",8,"walk","calf-stretch","quad","hip","glute")
    );}
    public static boolean isMobility(String id){return Arrays.asList("mobility-five","mobility-ten","hips","ankles","post-long").contains(id);}
    public static Routine get(String id){return all().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown routine"));}
    public static String warmup(org.racedaydreams.domain.WorkoutCategory category,double raceKm){if(category==org.racedaydreams.domain.WorkoutCategory.RACE)return raceKm>=40?"warm-race-marathon":raceKm>=20?"warm-race-half":"warm-race-short";if(category==org.racedaydreams.domain.WorkoutCategory.LONG_RUN)return "warm-long";return org.racedaydreams.domain.coaching.AdaptiveCoachingEngine.isQuality(category)?"warm-quality":"warm-easy";}
}
