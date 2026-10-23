package org.racedaydreams.data

import android.test.AndroidTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.strength.*
import java.time.*

class Release081ReliabilityTest:AndroidTestCase() {
    override fun setUp(){super.setUp();SqliteUserPreferencesRepository(context).clear()}
    override fun tearDown(){SqliteUserPreferencesRepository(context).clear();super.tearDown()}
    private val wall=Instant.parse("2026-10-01T00:00:00Z")
    private fun trace():RunMetricsEngine {val e=RunMetricsEngine(1000,wall);for(i in 0..20)e.accept(RunMetricsEngine.Fix(wall.plusSeconds(i.toLong()),1000+i*1000L,0.0,i*.00003,3.0,null,null,3.0));return e}
    fun testAtomicStartRollsBackSessionWhenPrescriptionInvalid() {
        val store=RecordingStore(context)
        try{store.start("bad",wall,1000,JSONObject().put("version",99));fail("Invalid snapshot accepted")}catch(_:IllegalArgumentException){}
        assertNull(store.unfinished())
        val origin=JSONObject().put("version",1).put("mode","free").put("at",wall.toString())
        store.start("good",wall,1000,null,origin)
        assertEquals("good",store.unfinished()!!.id)
        assertEquals("free",Release08Store(context).get("origin","good")!!.getString("mode"))
    }
    fun testCommittedBatchRetryDoesNotDuplicateSamplesEventsOrDistance() {
        val store=RecordingStore(context);val e=trace();store.start("retry",wall,1000)
        repeat(2){store.flush("retry",e.points(),e.events(),0,0,21000,"ACTIVE")}
        assertEquals(21,store.points("retry").size)
        val session=store.unfinished()!!;val id=store.finish(session,null)
        assertEquals(id,store.finish(session,null))
        assertEquals(1,FeatureStore(context).activitySummaries().size)
        assertEquals(e.status(21000).metres(),FeatureStore(context).activityDetail(id).distanceMetres()!!,.00001)
        try{store.flush("retry",e.points(),e.events(),0,0,22000,"ACTIVE");fail("Finished stage revived")}catch(_:IllegalStateException){}
        assertNull(store.unfinished())
    }
    fun testConflictingRetryAndSkippedSequencesRetainOriginalStage() {
        val store=RecordingStore(context);val e=trace();store.start("conflict",wall,1000);store.flush("conflict",e.points(),e.events(),0,0,21000,"PAUSED")
        val p=e.points()[0];val f=RunMetricsEngine.Fix(p.fix().timestamp(),1000,0.0,.00001,3.0,null,null,3.0)
        try{store.appendBatch("conflict",listOf(RunMetricsEngine.Point(f,0.0,0,1,true,null,null)),emptyList(),0,0,22000,"ACTIVE");fail("Conflicting sample replaced")}catch(_:IllegalStateException){}
        try{store.appendBatch("conflict",emptyList(),emptyList(),99,0,22000,"ACTIVE");fail("Sequence hole accepted")}catch(_:IllegalArgumentException){}
        assertEquals(e.points(),store.points("conflict"));assertEquals("PAUSED",store.unfinished()!!.state)
    }
    fun testPausedRecoveryKeepsDurableClockAndNeverCountsPostRestartDowntime() {
        val store=RecordingStore(context);val e=trace();e.pause(22000,wall.plusSeconds(21))
        store.start("pause",wall,1000);store.flush("pause",e.points(),e.events(),0,0,182000,"PAUSED");store.recoverable()
        // A new repository represents process recreation; no current boot clock is used.
        val after=RecordingStore(context);val s=after.unfinished()!!;assertEquals(182000L,s.last);val retained=after.recoverySummary(s);assertTrue(retained.paused);assertEquals(21,retained.points);assertEquals(21000L,retained.activeMillis)
        val id=after.finish(s,null);val a=FeatureStore(context).activityDetail(id)
        assertEquals(181000L,a.elapsedTime().toMillis());assertEquals(21000L,a.movingTime()!!.toMillis())
    }
    fun testTwoThousandHistoryRowsPageWithoutHydratingRetainedCoordinates() {
        val db=ProfileDatabase.get(context).writableDatabase
        db.beginTransaction();try {
            repeat(2000) { i -> val start=wall.minusSeconds(i*86400L);val id="history-$i"
                val a=ActivityRecord(id,SourceIdentity("manual",id,null),start,start.plusSeconds(1800),ZoneOffset.UTC,ZoneOffset.UTC,5000.0,null,null,null,null,null,null,null,emptyList(),emptyList(),null,null)
                db.execSQL("INSERT INTO activities VALUES(?,?,?,?,?,?,?)",arrayOf(id,"manual",id,null,start.toEpochMilli(),start.plusSeconds(1800).toEpochMilli(),FeatureJson.activity(a).toString()))
            }
            db.execSQL("INSERT INTO recording_sessions VALUES('large','FINISHED',?,0,100000,'history-0')",arrayOf(wall.toString()))
            // Deliberately unreadable raw points prove list paging does not parse/hydrate them.
            val insert=db.compileStatement("INSERT INTO recording_points VALUES('large',?,?)")
            repeat(100000){i->insert.bindLong(1,i.toLong());insert.bindString(2,"not a point; history must not read me");insert.executeInsert()};insert.close()
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        val store=WorkoutHistoryStore(context);val ids=linkedSetOf<String>();var cursor:WorkoutHistoryStore.Cursor?=null
        do {val p=store.page(WorkoutHistoryQuery.Kind.RUNNING,null,"",null,null,emptyList(),cursor,100);assertTrue(p.rows.size<=100);p.rows.forEach{assertTrue("Duplicate page row",ids.add(it.id));assertEquals(5000.0,it.distanceMetres!!,.001)};cursor=p.next}while(cursor!=null)
        assertEquals(2000,ids.size);assertEquals("history-0",ids.first());assertEquals("history-1999",ids.last())
        val metadata=FeatureStore(context).activitySummaryPage(null,null,30)
        assertEquals(30,metadata.size);assertTrue(metadata.all{it.samples().isEmpty()})
        val later=FeatureStore(context).activitySummaryPage(metadata.last().start(),metadata.last().id(),30)
        assertEquals(30,later.size);assertTrue(metadata.map{it.id()}.toSet().intersect(later.map{it.id()}.toSet()).isEmpty())
        val range=store.page(WorkoutHistoryQuery.Kind.ALL,null,"",LocalDate.of(2026,9,29),LocalDate.of(2026,10,1),emptyList())
        assertEquals(3,range.rows.size)
    }
    fun testMixedSameTimeSameIdPagesNeverSkipStrengthOrDuplicateRunning() {
        val a=ActivityRecord("same",SourceIdentity("manual","same",null),wall,wall.plusSeconds(60),ZoneOffset.UTC,ZoneOffset.UTC,100.0,null,null,null,null,null,null,null,emptyList(),emptyList(),null,null)
        FeatureStore(context).upsertManualActivity(a)
        val e=ExerciseCatalog.definition("back-squat").exercise()
        CoachingStore(context).saveStrength(StrengthSession("same",wall,ZoneOffset.UTC,Duration.ofMinutes(30),7.0,"",listOf(StrengthSession.ExerciseSets(e,listOf(StrengthSession.SetEntry("set",5,40.0,7.0,false))))))
        val store=WorkoutHistoryStore(context)
        val first=store.page(WorkoutHistoryQuery.Kind.ALL,null,"",null,null,emptyList(),null,1)
        assertEquals(1,first.rows.size);assertEquals(WorkoutHistoryQuery.Kind.RUNNING,first.rows.first().kind);assertNotNull(first.next)
        val second=store.page(WorkoutHistoryQuery.Kind.ALL,null,"",null,null,emptyList(),first.next,1)
        assertEquals(1,second.rows.size);assertEquals(WorkoutHistoryQuery.Kind.STRENGTH,second.rows.first().kind);assertNull(second.next)
    }
    fun testRunTypeFilterUsesLinkedPreservedVersionRatherThanCurrentCategory() {
        val date=LocalDate.of(2026,10,1)
        val profile=org.racedaydreams.domain.UserProfile(org.racedaydreams.domain.RaceGoal(org.racedaydreams.domain.RaceDistance.TEN_K,"",date.plusWeeks(8),org.racedaydreams.domain.GoalMode.FINISH,null),org.racedaydreams.domain.TrainingPreferences(org.racedaydreams.domain.TrainingStrategy.BALANCED,java.util.EnumSet.allOf(DayOfWeek::class.java),3,DayOfWeek.SUNDAY,emptySet(),org.racedaydreams.domain.ScheduleType.FLEXIBLE,org.racedaydreams.domain.UnitSystem.METRIC),false)
        fun version(n:Int,category:org.racedaydreams.domain.WorkoutCategory):org.racedaydreams.domain.coaching.PlanVersion {
            val workout=org.racedaydreams.domain.PlannedWorkout("preserved",date,category,5.0,null,emptyList(),"Retained")
            return org.racedaydreams.domain.coaching.PlanVersion("version-$n","plan",n,wall,"Reason",profile,"Evidence",org.racedaydreams.domain.TrainingPlan("plan",date,date.plusWeeks(8),emptyList(),listOf(workout)))
        }
        val first=version(1,org.racedaydreams.domain.WorkoutCategory.EASY_RUN);val current=version(2,org.racedaydreams.domain.WorkoutCategory.INTERVALS)
        val coaching=CoachingStore(context);coaching.saveCurrent(first);coaching.saveCurrent(current)
        val a=ActivityRecord("linked",SourceIdentity("manual","linked",null),wall,wall.plusSeconds(1800),ZoneOffset.UTC,ZoneOffset.UTC,5000.0,null,null,null,null,null,null,null,emptyList(),emptyList(),null,null)
        FeatureStore(context).upsertManualActivity(a);coaching.saveResponse(org.racedaydreams.domain.coaching.RunExecution("linked","preserved",first.id(),null,null,"Explicit link"))
        val store=WorkoutHistoryStore(context)
        val easy=store.page(WorkoutHistoryQuery.Kind.RUNNING,org.racedaydreams.domain.WorkoutCategory.EASY_RUN,"",null,null,current.plan().workouts(),versions=coaching.versions())
        assertEquals(1,easy.rows.size)
        assertTrue(store.page(WorkoutHistoryQuery.Kind.RUNNING,org.racedaydreams.domain.WorkoutCategory.INTERVALS,"",null,null,current.plan().workouts(),versions=coaching.versions()).rows.isEmpty())
        assertEquals(first,coaching.versions().first{it.id()==first.id()})
    }
    fun testStrengthAliasAndDateFiltersRemainCorrectAcrossPages() {
        val e=ExerciseCatalog.definition("romanian-deadlift").exercise()
        repeat(65){i->CoachingStore(context).saveStrength(StrengthSession("lift-$i",wall.minusSeconds(i*86400L),ZoneOffset.ofHours(2),Duration.ofMinutes(30),7.0,"",listOf(StrengthSession.ExerciseSets(e,listOf(StrengthSession.SetEntry("set",8,40.0,7.0,false))))))}
        val store=WorkoutHistoryStore(context)
        val first=store.page(WorkoutHistoryQuery.Kind.ALL,null,"RDL",null,null,emptyList(),null,30)
        assertEquals(30,first.rows.size);assertNotNull(first.next);assertTrue(first.rows.all{it.kind==WorkoutHistoryQuery.Kind.STRENGTH})
        val second=store.page(WorkoutHistoryQuery.Kind.ALL,null,"RDL",null,null,emptyList(),first.next,30)
        assertEquals(30,second.rows.size);assertTrue(first.rows.map{it.id}.toSet().intersect(second.rows.map{it.id}.toSet()).isEmpty())
        assertTrue(store.page(WorkoutHistoryQuery.Kind.RUNNING,null,"RDL",null,null,emptyList()).rows.isEmpty())
        assertTrue(store.page(WorkoutHistoryQuery.Kind.ALL,null,"no such exercise",null,null,emptyList()).rows.isEmpty())
    }
}
