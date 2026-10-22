package org.racedaydreams.data;
import android.test.AndroidTestCase;
import android.database.sqlite.SQLiteDatabase;
import java.time.*;
import java.util.*;
import org.json.*;
import org.racedaydreams.domain.body.*;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.domain.coaching.*;

@SuppressWarnings("deprecation")
public final class DevelopmentStoreTest extends AndroidTestCase {
    private DevelopmentStore development;
    private FeatureStore features;
    @Override protected void setUp()throws Exception{super.setUp();new SqliteUserPreferencesRepository(getContext()).clear();development=new DevelopmentStore(getContext());features=new FeatureStore(getContext());}
    @Override protected void tearDown()throws Exception{new SqliteUserPreferencesRepository(getContext()).clear();super.tearDown();}
    private BodyWeight weight(String id,String provider,double kg){return new BodyWeight(id,kg,Instant.parse("2026-10-01T12:00:00Z"),ZoneOffset.UTC,provider,id,"source");}
    public void testManualWeightEditDeleteAndUnitRoundTrip(){development.saveWeight(weight("w","manual",MassUnit.LB.toKilograms(180)));assertEquals(180,MassUnit.LB.fromKilograms(development.weights().get(0).kilograms()),.00001);development.saveWeight(weight("w","manual",81));assertEquals(1,development.weights().size());assertEquals(81,development.weights().get(0).kilograms(),.00001);development.deleteWeight("w");assertTrue(development.weights().isEmpty());}
    public void testHistoricalUnknownApproximateDatesAndCustomExerciseRoundTrip(){var e=new StrengthExercise("custom","My lift",StrengthExercise.Region.LOWER,StrengthExercise.Mode.WEIGHTED,true);development.saveHistorical(new HistoricalStrengthRecord("unknown",e,100,5,null,HistoricalStrengthRecord.DatePrecision.UNKNOWN,"Before installation"));development.saveHistorical(new HistoricalStrengthRecord("approx",e,90,10,LocalDate.of(2024,1,1),HistoricalStrengthRecord.DatePrecision.APPROXIMATE,"Approximate year"));assertEquals(2,development.historical().size());assertNull(development.historical().stream().filter(r->r.id().equals("unknown")).findFirst().get().date());JSONObject backup=features.exportUserData();features.restoreUserData(backup,false);assertEquals(2,development.historical().size());assertEquals("custom",development.historical().get(0).exercise().id());}
    public void testExportIncludesManualWeightButNotRawProviderMeasurements()throws Exception{development.saveWeight(weight("manual","manual",80));development.saveWeight(weight("provider","health-connect",81));JSONObject backup=features.exportUserData();JSONArray rows=backup.getJSONObject("development").getJSONArray("records");assertEquals(1,rows.length());features.restoreUserData(backup,false);assertEquals(1,development.weights().size());assertEquals("manual",development.weights().get(0).provider());}
    public void testInvalidEvidenceBackupDoesNotChangeExistingRecords()throws Exception{development.saveWeight(weight("w","manual",80));JSONObject backup=features.exportUserData();backup.getJSONObject("development").getJSONArray("records").getJSONObject(0).getJSONObject("payload").put("kg",-10);try{features.restoreUserData(backup,false);fail("Invalid weight accepted");}catch(IllegalArgumentException expected){}assertEquals(80,development.weights().get(0).kilograms(),.0001);}
    public void testProviderWeightWindowUpdatesAndDeletesWithoutTouchingManual(){development.saveWeight(weight("m","manual",80));development.saveWeight(weight("p","health-connect",81));development.replaceWeightWindow("health-connect",Instant.parse("2026-09-01T00:00:00Z"),Instant.parse("2026-11-01T00:00:00Z"),List.of());assertEquals(1,development.weights().size());assertEquals("m",development.weights().get(0).id());}
    public void testAllPriorSchemasUpgradeWithoutLosingSettings()throws Exception{for(int old=1;old<=4;old++){
        new SqliteUserPreferencesRepository(getContext()).clear();SQLiteDatabase db=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null);
        String[] methods={"createVersionOne","migrateOneToTwo","migrateTwoToThree","migrateThreeToFour"};for(int i=0;i<old;i++){var method=ProfileDatabase.class.getDeclaredMethod(methods[i],SQLiteDatabase.class);method.setAccessible(true);method.invoke(null,db);}
        db.execSQL("INSERT INTO user_profile VALUES (1,0)");if(old>=2)db.execSQL("INSERT INTO settings VALUES ('retained','yes')");db.setVersion(old);db.close();
        var store=new FeatureStore(getContext());store.activities();if(old>=2)assertEquals("yes",store.getSetting("retained"));assertTrue(new DevelopmentStore(getContext()).weights().isEmpty());
        try(var read=getContext().openOrCreateDatabase(ProfileDatabase.NAME,0,null)){assertEquals(5,read.getVersion());try(var c=read.rawQuery("SELECT COUNT(*) FROM user_profile",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));}}
    }}
    public void testSelectedCalendarViewAndMassPreferenceSurviveBackup(){features.putSetting("calendar.view","Month");features.putSetting("mass.unit","LB");JSONObject b=features.exportUserData();features.restoreUserData(b,false);assertEquals("Month",features.getSetting("calendar.view"));assertEquals("LB",features.getSetting("mass.unit"));}
    public void testBreakStateAndPlannedStrengthLinkSurviveBackup(){development.saveInterruption(new WeeklyCoaching.Interruption(LocalDate.of(2026,9,1),LocalDate.of(2026,10,1),"Travel"));development.linkStrength("session",LocalDate.of(2026,10,1));JSONObject b=features.exportUserData();features.restoreUserData(b,false);assertEquals("Travel",development.interruption().reason());}
}
