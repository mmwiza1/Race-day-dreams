#!/usr/bin/env python3
"""Exercise the actual Java schema with SQLite, without Android or downloaded packages.

This is a schema smoke test, not a substitute for Android repository instrumentation tests.
Run from any directory: python3 scripts/test-schema.py
"""

import ast
from pathlib import Path
import re
import sqlite3
import unittest


ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "app/src/main/java/org/racedaydreams/data/ProfileDatabase.java"


# Frozen Phase 1 fixture: migrations must accept existing installations, not only
# databases generated from the current implementation.
FROZEN_V1 = [
    "CREATE TABLE user_profile (id INTEGER PRIMARY KEY CHECK(id = 1), performance_eligible INTEGER NOT NULL CHECK(performance_eligible IN (0, 1)))",
    "CREATE TABLE race_goal (user_id INTEGER PRIMARY KEY REFERENCES user_profile(id) ON DELETE CASCADE, distance TEXT NOT NULL, race_name TEXT NOT NULL, race_date TEXT, goal_mode TEXT NOT NULL, target_seconds INTEGER CHECK(target_seconds > 0))",
    "CREATE TABLE training_preferences (user_id INTEGER PRIMARY KEY REFERENCES user_profile(id) ON DELETE CASCADE, strategy TEXT NOT NULL, runs_per_week INTEGER NOT NULL CHECK(runs_per_week BETWEEN 1 AND 7), long_run_day INTEGER CHECK(long_run_day BETWEEN 1 AND 7), schedule_type TEXT NOT NULL, units TEXT NOT NULL)",
    "CREATE TABLE availability_day (user_id INTEGER NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE, kind TEXT NOT NULL CHECK(kind IN ('RUN', 'GYM')), day INTEGER NOT NULL CHECK(day BETWEEN 1 AND 7), PRIMARY KEY(user_id, kind, day))",
    "CREATE TABLE onboarding_draft (id INTEGER PRIMARY KEY CHECK(id = 1), payload TEXT NOT NULL)",
]

# Frozen public draft schema from commit 60272f4; do not read current DDL for
# upgrade fixtures, since that would hide incompatible edits to a released version.
FROZEN_V2_ADDITIONS = [
    'CREATE TABLE activities (id TEXT PRIMARY KEY NOT NULL, provider TEXT NOT NULL, record_id TEXT NOT NULL, origin_package TEXT, start_millis INTEGER NOT NULL, end_millis INTEGER NOT NULL, payload TEXT NOT NULL, UNIQUE(provider, record_id), CHECK(end_millis >= start_millis))',
    'CREATE INDEX activities_start ON activities(start_millis DESC)',
    'CREATE TABLE shoes (id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, is_default INTEGER NOT NULL DEFAULT 0 CHECK(is_default IN (0, 1)))',
    'CREATE UNIQUE INDEX shoes_one_default ON shoes(is_default) WHERE is_default = 1',
    "CREATE TABLE activity_annotations (provider TEXT NOT NULL, record_id TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '', rpe REAL CHECK(rpe BETWEEN 0 AND 10), shoe_id TEXT REFERENCES shoes(id) ON DELETE SET NULL, PRIMARY KEY(provider, record_id))",
    'CREATE TABLE manual_results (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT, distance_code TEXT NOT NULL, distance_metres REAL NOT NULL CHECK(distance_metres > 0), result_date TEXT NOT NULL, payload TEXT NOT NULL)',
    'CREATE TABLE best_efforts (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT NOT NULL REFERENCES activities(id) ON DELETE CASCADE, distance_code TEXT NOT NULL, payload TEXT NOT NULL, UNIQUE(activity_id, distance_code))',
    'CREATE TABLE preserved_best_efforts (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT NOT NULL, distance_code TEXT NOT NULL, provider TEXT NOT NULL, record_id TEXT NOT NULL, origin_package TEXT, payload TEXT NOT NULL, UNIQUE(activity_id, distance_code))',
    'CREATE INDEX preserved_efforts_source ON preserved_best_efforts(provider, record_id)',
    'CREATE TABLE import_checkpoints (provider TEXT PRIMARY KEY NOT NULL, token TEXT, updated_at TEXT NOT NULL)',
    'CREATE TABLE baseline_state (id INTEGER PRIMARY KEY CHECK(id = 1), manual_json TEXT NOT NULL, manual_recorded_at TEXT, reviewed INTEGER NOT NULL DEFAULT 0 CHECK(reviewed IN (0, 1)))',
    'CREATE TABLE settings (setting_key TEXT PRIMARY KEY NOT NULL, setting_value TEXT NOT NULL)',
    'CREATE TABLE assessment_results (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT, payload TEXT NOT NULL)',
    'CREATE TABLE planned_workouts (id TEXT PRIMARY KEY NOT NULL, scheduled_date TEXT NOT NULL, revision INTEGER NOT NULL CHECK(revision > 0), payload TEXT NOT NULL)',
    'CREATE TABLE training_plans (id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL)',
    'CREATE TABLE plan_adjustments (id TEXT PRIMARY KEY NOT NULL, plan_id TEXT, user_decision TEXT, payload TEXT NOT NULL)',
]


def schema_statements(version):
    """Read the marked Java literals, testing the exact DDL used on Android."""
    source = SCHEMA.read_text(encoding="utf-8")
    match = re.search(rf"// V{version}_SCHEMA_BEGIN.*?\n(.*?)// V{version}_SCHEMA_END",
                      source, flags=re.DOTALL)
    if not match:
        raise AssertionError(f"Missing schema version {version} markers")
    statements = re.findall(r"db\.execSQL\((.*?)\);", match.group(1), flags=re.DOTALL)
    if not statements:
        raise AssertionError(f"No statements for schema version {version}")
    return ["".join(ast.literal_eval(literal) for literal in
                    re.findall(r'"(?:\\.|[^"\\])*"', statement))
            for statement in statements]


def create_schema(connection, version=10, frozen=False):
    connection.execute("PRAGMA foreign_keys = ON")
    with connection:
        connection.execute("BEGIN")
        for statement in FROZEN_V1 if frozen else schema_statements(1):
            connection.execute(statement)
        for schema_version in range(2, version + 1):
            for statement in schema_statements(schema_version):
                connection.execute(statement)
        connection.execute(f"PRAGMA user_version = {version}")


def migrate_to_latest(connection, target=10):
    old = connection.execute("PRAGMA user_version").fetchone()[0]
    # Match the Java preflight guard before running any intermediate DDL.
    if old not in (1, 2, 3, 4, 5, 6, 7, 8, 9) or target != 10:
        raise ValueError("Only explicit schemas 1–9 to schema 10 paths are supported")
    # SQLiteOpenHelper invokes the whole onUpgrade in one transaction. A failure
    # in the second migration must roll back the first migration as well.
    with connection:
        connection.execute("BEGIN")
        for version in range(old + 1, target + 1):
            for statement in schema_statements(version):
                connection.execute(statement)
        connection.execute(f"PRAGMA user_version = {target}")


def create_frozen_v2(connection, nullable_notes=False):
    connection.execute("PRAGMA foreign_keys = ON")
    with connection:
        connection.execute("BEGIN")
        for statement in FROZEN_V1 + FROZEN_V2_ADDITIONS:
            if nullable_notes:
                statement = statement.replace("notes TEXT NOT NULL DEFAULT ''", "notes TEXT")
            connection.execute(statement)
        connection.execute("PRAGMA user_version = 2")


class ProfileSchemaTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        create_schema(self.db)

    def tearDown(self):
        self.db.close()

    def add_profile(self):
        self.db.execute("INSERT INTO user_profile VALUES (1, 0)")
        self.db.execute(
            "INSERT INTO race_goal VALUES (1, 'HALF_MARATHON', '', NULL, 'FINISH', NULL)"
        )
        self.db.execute(
            "INSERT INTO training_preferences "
            "VALUES (1, 'BALANCED', 3, 7, 'FLEXIBLE', 'METRIC')"
        )
        self.db.commit()

    def test_schema_initializes_without_rows(self):
        tables = {row[0] for row in self.db.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        )}
        self.assertTrue({"user_profile", "race_goal", "training_preferences",
                         "availability_day", "onboarding_draft"}.issubset(tables))
        for table in tables:
            self.assertEqual(0, self.db.execute("SELECT COUNT(*) FROM " + table).fetchone()[0])

    def test_running_and_gym_days_can_overlap(self):
        self.add_profile()
        self.db.execute("INSERT INTO availability_day VALUES (1, 'RUN', 1)")
        self.db.execute("INSERT INTO availability_day VALUES (1, 'GYM', 1)")
        self.assertEqual(2, self.db.execute(
            "SELECT COUNT(*) FROM availability_day WHERE day = 1"
        ).fetchone()[0])

    def test_schema_rejects_invalid_values_and_orphans(self):
        self.add_profile()
        invalid = [
            "INSERT INTO user_profile VALUES (2, 0)",
            "INSERT INTO availability_day VALUES (1, 'RUN', 0)",
            "INSERT INTO availability_day VALUES (1, 'RUN', 8)",
            "INSERT INTO availability_day VALUES (1, 'SWIM', 2)",
            "INSERT INTO race_goal VALUES (2, 'FIVE_K', '', NULL, 'FINISH', NULL)",
            "UPDATE training_preferences SET runs_per_week = 0",
            "UPDATE training_preferences SET runs_per_week = 8",
            "UPDATE training_preferences SET long_run_day = 8",
            "UPDATE race_goal SET target_seconds = 0",
            "UPDATE user_profile SET performance_eligible = 2",
        ]
        for statement in invalid:
            with self.subTest(statement=statement):
                with self.assertRaises(sqlite3.IntegrityError):
                    self.db.execute(statement)
                self.db.rollback()

    def test_deleting_profile_cascades_but_keeps_independent_draft(self):
        self.add_profile()
        self.db.execute("INSERT INTO availability_day VALUES (1, 'RUN', 1)")
        self.db.execute("INSERT INTO onboarding_draft VALUES (1, '{\"step\":2}')")
        self.db.commit()
        with self.db:
            self.db.execute("DELETE FROM user_profile")
        for table in ("race_goal", "training_preferences", "availability_day"):
            self.assertEqual(0, self.db.execute("SELECT COUNT(*) FROM " + table).fetchone()[0])
        self.assertEqual(1, self.db.execute(
            "SELECT COUNT(*) FROM onboarding_draft"
        ).fetchone()[0])

    def test_failed_transaction_retains_previous_profile_and_draft(self):
        self.add_profile()
        self.db.execute("INSERT INTO onboarding_draft VALUES (1, '{\"step\":2}')")
        self.db.commit()
        with self.assertRaises(sqlite3.IntegrityError):
            with self.db:
                self.db.execute("DELETE FROM user_profile")
                self.db.execute("INSERT INTO user_profile VALUES (1, 2)")
        self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM user_profile").fetchone()[0])
        self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM race_goal").fetchone()[0])
        self.assertEqual(1, self.db.execute(
            "SELECT COUNT(*) FROM onboarding_draft"
        ).fetchone()[0])


class FeatureSchemaTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        create_schema(self.db)

    def tearDown(self):
        self.db.close()

    def add_activity(self, identifier="run", record="source-run", provider="health-connect"):
        self.db.execute(
            "INSERT INTO activities VALUES (?, ?, ?, 'writer.app', 1000, 2000, '{}')",
            (identifier, provider, record),
        )

    def test_all_feature_tables_and_indexes_are_created(self):
        tables = {row[0] for row in self.db.execute(
            "SELECT name FROM sqlite_master WHERE type='table'"
        )}
        self.assertTrue({"activities", "shoes", "activity_annotations", "manual_results",
                         "best_efforts", "preserved_best_efforts", "import_checkpoints",
                         "baseline_state", "settings", "assessment_results", "planned_workouts",
                         "training_plans", "plan_adjustments"}.issubset(tables))
        indexes = {row[0] for row in self.db.execute(
            "SELECT name FROM sqlite_master WHERE type='index'"
        )}
        self.assertTrue({"activities_start", "shoes_one_default", "preserved_efforts_source"}.issubset(indexes))
        self.assertEqual(10, self.db.execute("PRAGMA user_version").fetchone()[0])

    def test_source_record_identity_is_unique_but_other_provider_can_share_id(self):
        self.add_activity()
        self.add_activity("other-provider-run", "source-run", "another-provider")
        with self.assertRaises(sqlite3.IntegrityError):
            self.add_activity("duplicate-record", "source-run")
        self.assertEqual(2, self.db.execute("SELECT COUNT(*) FROM activities").fetchone()[0])

    def test_activity_time_and_required_identity_constraints(self):
        for statement in [
            "INSERT INTO activities VALUES ('run','provider','record',NULL,2000,1000,'{}')",
            "INSERT INTO activities VALUES (NULL,'provider','record',NULL,1000,2000,'{}')",
            "INSERT INTO activities VALUES ('run',NULL,'record',NULL,1000,2000,'{}')",
            "INSERT INTO activities VALUES ('run','provider',NULL,NULL,1000,2000,'{}')",
        ]:
            with self.subTest(statement=statement), self.assertRaises(sqlite3.IntegrityError):
                self.db.execute(statement)

    def test_activity_delete_invalidates_rolling_efforts_not_manual_results(self):
        self.add_activity()
        self.db.execute("INSERT INTO best_efforts VALUES ('effort','run','5k','{}')")
        self.db.execute("INSERT INTO manual_results VALUES ('chip','run','5k',5000,'2030-01-01','{}')")
        self.db.execute("INSERT INTO activity_annotations VALUES ('health-connect','source-run','My note',7,NULL)")
        self.db.execute("DELETE FROM activities WHERE id='run'")
        self.assertEqual(0, self.db.execute("SELECT COUNT(*) FROM best_efforts").fetchone()[0])
        self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM manual_results").fetchone()[0])
        self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM activity_annotations").fetchone()[0])

    def test_rolling_effort_requires_existing_activity_and_unique_distance(self):
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO best_efforts VALUES ('orphan','missing','5k','{}')")
        self.add_activity()
        self.db.execute("INSERT INTO best_efforts VALUES ('one','run','5k','{}')")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO best_efforts VALUES ('two','run','5k','{}')")

    def test_preserved_pb_history_can_await_reimported_activity(self):
        self.db.execute("INSERT INTO preserved_best_efforts VALUES "
                        "('pb','not-on-device','5k','health-connect','source-run','writer.app','{}')")
        self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM preserved_best_efforts").fetchone()[0])
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO preserved_best_efforts VALUES "
                            "('duplicate','not-on-device','5k','health-connect','source-run','writer.app','{}')")

    def test_only_one_default_shoe_is_allowed(self):
        self.db.execute("INSERT INTO shoes VALUES ('one','{}',1)")
        self.db.execute("INSERT INTO shoes VALUES ('two','{}',0)")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE shoes SET is_default=1 WHERE id='two'")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO shoes VALUES ('bad','{}',2)")
        self.assertEqual("one", self.db.execute("SELECT id FROM shoes WHERE is_default=1").fetchone()[0])

    def test_shoe_delete_clears_assignment_but_keeps_annotation(self):
        self.db.execute("INSERT INTO shoes VALUES ('shoe','{}',0)")
        self.db.execute("INSERT INTO activity_annotations VALUES ('provider','record','Remember',5,'shoe')")
        self.db.execute("DELETE FROM shoes WHERE id='shoe'")
        self.assertEqual(("Remember", 5.0, None), self.db.execute(
            "SELECT notes,rpe,shoe_id FROM activity_annotations"
        ).fetchone())
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO activity_annotations VALUES ('provider','other','',NULL,'missing')")

    def test_absent_and_explicitly_cleared_note_overrides_are_distinct(self):
        self.db.execute("INSERT INTO activity_annotations (provider,record_id) VALUES ('provider','no-override')")
        self.db.execute("INSERT INTO activity_annotations VALUES ('provider','cleared','',NULL,NULL)")
        self.assertIsNone(self.db.execute(
            "SELECT notes FROM activity_annotations WHERE record_id='no-override'"
        ).fetchone()[0])
        self.assertEqual("", self.db.execute(
            "SELECT notes FROM activity_annotations WHERE record_id='cleared'"
        ).fetchone()[0])

    def test_baseline_and_workout_revision_constraints(self):
        for statement in [
            "INSERT INTO baseline_state VALUES (2,'{}',NULL,0)",
            "INSERT INTO baseline_state VALUES (1,'{}',NULL,2)",
            "INSERT INTO planned_workouts VALUES ('plan','2030-01-01',0,'{}')",
            "INSERT INTO planned_workouts VALUES ('plan','2030-01-01',-1,'{}')",
            "INSERT INTO activity_annotations VALUES ('provider','record','',11,NULL)",
            "INSERT INTO activity_annotations VALUES ('provider','record','',-1,NULL)",
        ]:
            with self.subTest(statement=statement), self.assertRaises(sqlite3.IntegrityError):
                self.db.execute(statement)

    def test_failed_import_transaction_keeps_previous_checkpoint_and_activity(self):
        self.add_activity()
        self.db.execute("INSERT INTO import_checkpoints VALUES ('health-connect','before','2030-01-01T00:00:00Z')")
        self.db.commit()
        with self.assertRaises(sqlite3.IntegrityError):
            with self.db:
                self.db.execute("DELETE FROM activities")
                self.db.execute("UPDATE import_checkpoints SET token='after'")
                self.db.execute("INSERT INTO best_efforts VALUES ('orphan','missing','5k','{}')")
        self.assertEqual("run", self.db.execute("SELECT id FROM activities").fetchone()[0])
        self.assertEqual("before", self.db.execute("SELECT token FROM import_checkpoints").fetchone()[0])


class MigrationSchemaTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        create_schema(self.db, version=1, frozen=True)
        self.db.execute("INSERT INTO user_profile VALUES (1,1)")
        self.db.execute("INSERT INTO race_goal VALUES (1,'TEN_K','Original race','2031-04-20','TARGET_TIME',3300)")
        self.db.execute("INSERT INTO training_preferences VALUES (1,'PERFORMANCE',3,7,'FLEXIBLE','IMPERIAL')")
        self.db.execute("INSERT INTO availability_day VALUES (1,'RUN',2),(1,'RUN',4),(1,'RUN',7),(1,'GYM',2)")
        self.db.execute("INSERT INTO onboarding_draft VALUES (1,'{\"step\":2}')")
        self.db.commit()

    def tearDown(self):
        self.db.close()

    def snapshot(self):
        return {table: self.db.execute(f"SELECT * FROM {table}").fetchall()
                for table in ("user_profile", "race_goal", "training_preferences", "availability_day", "onboarding_draft")}

    def test_current_version_one_schema_matches_frozen_baseline(self):
        current = sqlite3.connect(":memory:")
        try:
            create_schema(current, version=1)
            for table in self.snapshot():
                self.assertEqual(self.db.execute(f"PRAGMA table_info({table})").fetchall(),
                                 current.execute(f"PRAGMA table_info({table})").fetchall())
                self.assertEqual(self.db.execute(f"PRAGMA foreign_key_list({table})").fetchall(),
                                 current.execute(f"PRAGMA foreign_key_list({table})").fetchall())
        finally:
            current.close()

    def test_additive_migration_preserves_all_existing_rows(self):
        before = self.snapshot()
        migrate_to_latest(self.db)
        self.assertEqual(before, self.snapshot())
        self.assertEqual(10, self.db.execute("PRAGMA user_version").fetchone()[0])
        self.db.execute("INSERT INTO shoes VALUES ('new-shoe','{}',1)")
        self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM shoes").fetchone()[0])

    def test_failed_migration_rolls_back_every_new_table_and_keeps_old_rows(self):
        before = self.snapshot()
        # A collision occurs after activities/index creation, proving earlier DDL rolls back.
        self.db.execute("CREATE TABLE shoes (collision TEXT)")
        with self.assertRaises(sqlite3.OperationalError):
            migrate_to_latest(self.db)
        self.assertEqual(1, self.db.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual(before, self.snapshot())
        self.assertIsNone(self.db.execute("SELECT name FROM sqlite_master WHERE name='activities'").fetchone())
        self.assertIsNone(self.db.execute("SELECT name FROM sqlite_master WHERE name='activities_start'").fetchone())
        self.assertEqual("collision", self.db.execute("PRAGMA table_info(shoes)").fetchone()[1])

    def test_unknown_migration_version_is_rejected_without_changes(self):
        before = self.snapshot()
        self.db.execute("PRAGMA user_version=99")
        with self.assertRaises(ValueError):
            migrate_to_latest(self.db)
        self.assertEqual(99, self.db.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual(before, self.snapshot())


class AnnotationMigrationSchemaTest(unittest.TestCase):
    def make_v2(self, nullable=False):
        connection = sqlite3.connect(":memory:")
        create_frozen_v2(connection, nullable)
        # Seed every persisted data family. The migration may only change the
        # annotation table's nullability, not user-owned or imported rows.
        rows = [
            "INSERT INTO user_profile VALUES (1,0)",
            "INSERT INTO race_goal VALUES (1,'TEN_K','Retain race','2031-04-20','TARGET_TIME',3300)",
            "INSERT INTO training_preferences VALUES (1,'BALANCED',3,7,'FIXED','METRIC')",
            "INSERT INTO availability_day VALUES (1,'RUN',7)",
            "INSERT INTO onboarding_draft VALUES (1,'{\"step\":1}')",
            "INSERT INTO activities VALUES ('run','provider','record','writer',1000,2000,'{\"distance\":5}')",
            "INSERT INTO shoes VALUES ('shoe','{\"model\":\"retain\"}',1)",
            "INSERT INTO activity_annotations VALUES ('provider','record','Kept note',6.5,'shoe')",
            "INSERT INTO activity_annotations VALUES ('provider','cleared','',NULL,'shoe')",
            "INSERT INTO manual_results VALUES ('result','run','5k',5000,'2030-01-01','{}')",
            "INSERT INTO best_efforts VALUES ('effort','run','1k','{}')",
            "INSERT INTO preserved_best_efforts VALUES ('archived','absent','5k','provider','old','writer','{}')",
            "INSERT INTO import_checkpoints VALUES ('provider','token','2030-01-01T00:00:00Z')",
            "INSERT INTO baseline_state VALUES (1,'{}','2030-01-01T00:00:00Z',1)",
            "INSERT INTO settings VALUES ('units','metric')",
            "INSERT INTO assessment_results VALUES ('assessment','run','{}')",
            "INSERT INTO planned_workouts VALUES ('planned','2030-01-02',3,'{}')",
            "INSERT INTO training_plans VALUES ('plan','{}')",
            "INSERT INTO plan_adjustments VALUES ('adjustment','plan','accepted','{}')",
        ]
        for row in rows:
            connection.execute(row)
        if nullable:
            connection.execute("INSERT INTO activity_annotations VALUES ('provider','no-override',NULL,4,'shoe')")
        connection.commit()
        return connection

    @staticmethod
    def snapshot(connection):
        return {name: connection.execute(f"SELECT * FROM {name} ORDER BY rowid").fetchall()
                for (name,) in connection.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")}

    def test_both_published_v2_variants_upgrade_without_changing_any_rows(self):
        for nullable in (False, True):
            with self.subTest(nullable=nullable):
                db = self.make_v2(nullable)
                try:
                    before = self.snapshot(db)
                    migrate_to_latest(db)
                    self.assertEqual(10, db.execute("PRAGMA user_version").fetchone()[0])
                    self.assertEqual(before, {key: value for key, value in self.snapshot(db).items() if key in before})
                    columns = {row[1]: row for row in db.execute("PRAGMA table_info(activity_annotations)")}
                    self.assertEqual(0, columns['notes'][3])
                    self.assertIsNone(columns['notes'][4])
                    db.execute("INSERT INTO activity_annotations (provider,record_id) VALUES ('provider','new-no-override')")
                    self.assertIsNone(db.execute("SELECT notes FROM activity_annotations WHERE record_id='new-no-override'").fetchone()[0])
                    db.execute("DELETE FROM shoes WHERE id='shoe'")
                    self.assertEqual(0, db.execute("SELECT COUNT(*) FROM activity_annotations WHERE shoe_id IS NOT NULL").fetchone()[0])
                    self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())
                finally:
                    db.close()

    def test_v2_annotation_rebuild_failure_rolls_back_data_and_version(self):
        db = self.make_v2()
        try:
            db.execute("CREATE TABLE activity_annotations_v3 (collision TEXT)")
            before = self.snapshot(db)
            with self.assertRaises(sqlite3.OperationalError):
                migrate_to_latest(db)
            self.assertEqual(2, db.execute("PRAGMA user_version").fetchone()[0])
            self.assertEqual(before, self.snapshot(db))
            columns = {row[1]: row for row in db.execute("PRAGMA table_info(activity_annotations)")}
            self.assertEqual(1, columns['notes'][3])
        finally:
            db.close()

    def test_v1_failure_during_v3_rolls_back_all_intermediate_v2_tables(self):
        db = sqlite3.connect(":memory:")
        try:
            create_schema(db, version=1, frozen=True)
            db.execute("INSERT INTO onboarding_draft VALUES (1,'{\"step\":2}')")
            db.execute("CREATE TABLE activity_annotations_v3 (collision TEXT)")
            db.commit()
            before = self.snapshot(db)
            with self.assertRaises(sqlite3.OperationalError):
                migrate_to_latest(db)
            self.assertEqual(1, db.execute("PRAGMA user_version").fetchone()[0])
            self.assertEqual(before, self.snapshot(db))
            self.assertIsNone(db.execute("SELECT name FROM sqlite_master WHERE name='activities'").fetchone())
        finally:
            db.close()

    def test_unsupported_source_and_target_paths_are_rejected_before_writes(self):
        for source, target in ((0,6), (6,6), (99,6), (1,2), (2,2), (1,11), (2,11)):
            with self.subTest(source=source, target=target):
                db = self.make_v2()
                try:
                    db.execute(f"PRAGMA user_version={source}")
                    before = self.snapshot(db)
                    schema_before = db.execute("SELECT type,name,sql FROM sqlite_master ORDER BY type,name").fetchall()
                    with self.assertRaises(ValueError):
                        migrate_to_latest(db, target)
                    self.assertEqual(source, db.execute("PRAGMA user_version").fetchone()[0])
                    self.assertEqual(before, self.snapshot(db))
                    self.assertEqual(schema_before, db.execute("SELECT type,name,sql FROM sqlite_master ORDER BY type,name").fetchall())
                finally:
                    db.close()


class CoachingSchemaTest(unittest.TestCase):
    def test_version_three_upgrade_preserves_every_existing_table(self):
        db = sqlite3.connect(":memory:")
        try:
            create_schema(db, version=3)
            db.execute("INSERT INTO settings VALUES ('existing','retained')")
            db.commit()
            migrate_to_latest(db)
            self.assertEqual([('existing','retained')], db.execute("SELECT * FROM settings").fetchall())
            self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())
            self.assertEqual(10, db.execute("PRAGMA user_version").fetchone()[0])
        finally:
            db.close()

    def test_one_current_version_foreign_keys_and_unique_versions(self):
        db = sqlite3.connect(":memory:")
        try:
            create_schema(db)
            db.execute("INSERT INTO coach_plan_versions VALUES ('v1','p',1,'{}')")
            db.execute("INSERT INTO coach_current_plan VALUES (1,'v1')")
            with self.assertRaises(sqlite3.IntegrityError):
                db.execute("INSERT INTO coach_current_plan VALUES (2,'v1')")
            with self.assertRaises(sqlite3.IntegrityError):
                db.execute("INSERT INTO coach_plan_versions VALUES ('duplicate','p',1,'{}')")
            with self.assertRaises(sqlite3.IntegrityError):
                db.execute("DELETE FROM coach_plan_versions WHERE id='v1'")
        finally:
            db.close()

    def test_failed_version_four_upgrade_rolls_back_and_retains_version_three(self):
        db = sqlite3.connect(":memory:")
        try:
            create_schema(db, version=3)
            db.execute("INSERT INTO settings VALUES ('existing','retained')")
            db.execute("CREATE TABLE strength_sessions (collision TEXT)")
            db.commit()
            with self.assertRaises(sqlite3.OperationalError):
                migrate_to_latest(db)
            self.assertEqual(3, db.execute("PRAGMA user_version").fetchone()[0])
            self.assertEqual([('existing','retained')], db.execute("SELECT * FROM settings").fetchall())
            self.assertIsNone(db.execute("SELECT name FROM sqlite_master WHERE name='coach_plan_versions'").fetchone())
        finally:
            db.close()


class EvidenceSchemaTest(unittest.TestCase):
    def test_all_prior_versions_upgrade_and_preserve_profile(self):
        for version in (1,2,3,4):
            db=sqlite3.connect(":memory:")
            try:
                create_schema(db,version=version)
                db.execute("INSERT INTO user_profile VALUES (1,0)")
                db.commit()
                migrate_to_latest(db)
                self.assertEqual([(1,0)],db.execute("SELECT * FROM user_profile").fetchall())
                self.assertEqual(10,db.execute("PRAGMA user_version").fetchone()[0])
                self.assertEqual([],db.execute("PRAGMA foreign_key_check").fetchall())
            finally: db.close()

    def test_kind_and_identity_constraints(self):
        db=sqlite3.connect(":memory:")
        try:
            create_schema(db)
            db.execute("INSERT INTO coach_evidence VALUES ('WEIGHT','w','{}')")
            with self.assertRaises(sqlite3.IntegrityError): db.execute("INSERT INTO coach_evidence VALUES ('WEIGHT','w','{}')")
            with self.assertRaises(sqlite3.IntegrityError): db.execute("INSERT INTO coach_evidence VALUES ('UNKNOWN','x','{}')")
            db.execute("INSERT INTO coach_evidence VALUES ('HISTORICAL_PB','w','{}')")
        finally: db.close()

    def test_five_migration_rolls_back_without_touching_four(self):
        db=sqlite3.connect(":memory:")
        try:
            create_schema(db,version=4)
            db.execute("INSERT INTO settings VALUES ('retained','yes')")
            db.execute("CREATE TABLE coach_evidence (collision TEXT)")
            db.commit()
            with self.assertRaises(sqlite3.OperationalError): migrate_to_latest(db)
            self.assertEqual(4,db.execute("PRAGMA user_version").fetchone()[0])
            self.assertEqual([('retained','yes')],db.execute("SELECT * FROM settings").fetchall())
        finally: db.close()


class StrengthChoicesSchemaTest(unittest.TestCase):
    def test_v5_upgrade_normalizes_only_provider_preference(self):
        db=sqlite3.connect(":memory:")
        create_schema(db,version=5)
        db.execute("INSERT INTO settings VALUES ('provider.preference','SAMSUNG_EXPERIMENTAL')")
        db.execute("INSERT INTO settings VALUES ('calendar.view','Month')")
        db.execute("INSERT INTO strength_exercises VALUES ('old','old payload')")
        db.commit()
        migrate_to_latest(db)
        self.assertEqual('HEALTH_CONNECT',db.execute("SELECT setting_value FROM settings WHERE setting_key='provider.preference'").fetchone()[0])
        self.assertEqual('Month',db.execute("SELECT setting_value FROM settings WHERE setting_key='calendar.view'").fetchone()[0])
        self.assertEqual([('old','old payload')],db.execute("SELECT * FROM strength_exercises").fetchall())
        db.close()

    def test_failed_v6_rolls_back_setting_normalization(self):
        db=sqlite3.connect(":memory:");create_schema(db,version=5)
        db.execute("INSERT INTO settings VALUES ('provider.preference','SAMSUNG_EXPERIMENTAL')")
        db.execute("CREATE TABLE strength_user_state (collision TEXT)");db.commit()
        with self.assertRaises(sqlite3.OperationalError):migrate_to_latest(db)
        self.assertEqual(5,db.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual('SAMSUNG_EXPERIMENTAL',db.execute("SELECT setting_value FROM settings").fetchone()[0]);db.close()

    def test_user_choice_constraints(self):
        db=sqlite3.connect(":memory:");create_schema(db)
        with self.assertRaises(sqlite3.IntegrityError):db.execute("INSERT INTO strength_user_state VALUES ('x',2,0)")
        db.close()

class RecordingMigrationSchemaTest(unittest.TestCase):
    def test_every_supported_version_retains_old_rows_and_adds_recording_tables(self):
        for old in range(1,7):
            db=sqlite3.connect(":memory:");create_schema(db,version=old)
            db.execute("INSERT INTO user_profile VALUES (1,0)");db.commit()
            before={name:db.execute("SELECT * FROM "+name).fetchall() for (name,) in db.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()}
            migrate_to_latest(db)
            for name,rows in before.items():self.assertEqual(rows,db.execute("SELECT * FROM "+name).fetchall())
            self.assertEqual(10,db.execute("PRAGMA user_version").fetchone()[0]);db.close()

    def test_only_one_unfinished_run_and_orphan_points_rejected(self):
        db=sqlite3.connect(":memory:");create_schema(db)
        db.execute("INSERT INTO recording_sessions VALUES ('one','ACTIVE','2026-10-22T00:00:00Z',1,1,NULL)")
        with self.assertRaises(sqlite3.IntegrityError):db.execute("INSERT INTO recording_sessions VALUES ('two','PAUSED','2026-10-22T00:00:00Z',1,1,NULL)")
        with self.assertRaises(sqlite3.IntegrityError):db.execute("INSERT INTO recording_points VALUES ('missing',0,'{}')")
        db.close()

    def test_failed_v7_upgrade_preserves_all_old_tables_and_version(self):
        db=sqlite3.connect(":memory:");create_schema(db,version=6)
        db.execute("INSERT INTO settings VALUES ('keep','yes')");db.execute("CREATE TABLE recording_points (collision TEXT)");db.commit()
        with self.assertRaises(sqlite3.OperationalError):migrate_to_latest(db)
        self.assertEqual(6,db.execute("PRAGMA user_version").fetchone()[0]);self.assertEqual('yes',db.execute("SELECT setting_value FROM settings WHERE setting_key='keep'").fetchone()[0])
        self.assertIsNone(db.execute("SELECT name FROM sqlite_master WHERE name='health_observations'").fetchone());db.close()


class SupportMigrationSchemaTest(unittest.TestCase):
    def test_all_supported_upgrades_preserve_old_rows(self):
        for old in range(1,8):
            db=sqlite3.connect(":memory:");create_schema(db,version=old)
            db.execute("INSERT INTO user_profile VALUES (1,0)");db.commit()
            migrate_to_latest(db)
            self.assertEqual(10,db.execute("PRAGMA user_version").fetchone()[0])
            self.assertEqual(1,db.execute("SELECT COUNT(*) FROM user_profile").fetchone()[0])
            db.execute("INSERT INTO coach_support VALUES ('routine','one','{}')")
            with self.assertRaises(sqlite3.IntegrityError):db.execute("INSERT INTO coach_support VALUES ('calendar','one','{}')")
            db.close()
    def test_failure_rolls_back_v8_and_retains_v7(self):
        db=sqlite3.connect(":memory:");create_schema(db,version=7)
        db.execute("INSERT INTO settings VALUES ('keep','yes')");db.execute("CREATE TABLE coach_support (collision TEXT)");db.commit()
        with self.assertRaises(sqlite3.OperationalError):migrate_to_latest(db)
        self.assertEqual(7,db.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual('yes',db.execute("SELECT setting_value FROM settings WHERE setting_key='keep'").fetchone()[0]);db.close()

class Release08MigrationTest(unittest.TestCase):
    def test_every_supported_schema_retains_user_rows_and_adds_release08(self):
        for old in range(1,9):
            db=sqlite3.connect(":memory:")
            create_schema(db,old)
            db.execute("INSERT INTO onboarding_draft VALUES (1, '{}')")
            db.commit()
            migrate_to_latest(db)
            self.assertEqual(10,db.execute("PRAGMA user_version").fetchone()[0])
            self.assertEqual('{}',db.execute("SELECT payload FROM onboarding_draft").fetchone()[0])
            db.execute("INSERT INTO release08 VALUES ('custom','saved','{}')")
            with self.assertRaises(sqlite3.IntegrityError):db.execute("INSERT INTO release08 VALUES ('unknown','bad','{}')")
            db.close()
    def test_v9_failure_preserves_v8_support_and_schema(self):
        db=sqlite3.connect(":memory:")
        create_schema(db,8)
        db.execute("INSERT INTO coach_support VALUES ('routine','retained','{}')")
        db.execute("CREATE TABLE release08 (collision TEXT)")
        db.commit()
        with self.assertRaises(sqlite3.OperationalError):migrate_to_latest(db)
        self.assertEqual(8,db.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual('retained',db.execute("SELECT id FROM coach_support").fetchone()[0])
        db.close()

class Release081MigrationTest(unittest.TestCase):
    def test_every_supported_schema_preserves_all_rows_and_adds_history_indexes(self):
        for old in range(1,10):
            db=sqlite3.connect(":memory:");create_schema(db,old)
            db.execute("INSERT INTO onboarding_draft VALUES(1,'kept')");db.commit()
            before={name:db.execute('SELECT * FROM '+name).fetchall() for (name,) in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            migrate_to_latest(db)
            for name,rows in before.items():self.assertEqual(rows,db.execute('SELECT * FROM '+name).fetchall())
            indexes={r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='index'")}
            self.assertTrue({'activities_history_order','strength_history_order'}<=indexes)
            self.assertEqual(10,db.execute('PRAGMA user_version').fetchone()[0]);db.close()

    def test_history_indexes_are_used_without_raw_sample_queries(self):
        db=sqlite3.connect(":memory:");create_schema(db)
        runs=' '.join(str(r) for r in db.execute('EXPLAIN QUERY PLAN SELECT id,start_millis,payload FROM activities ORDER BY start_millis DESC,id LIMIT 30'))
        strength=' '.join(str(r) for r in db.execute("EXPLAIN QUERY PLAN SELECT id,payload FROM strength_sessions ORDER BY CAST(ROUND((julianday(started_at)-2440587.5)*86400000) AS INTEGER) DESC,id LIMIT 30"))
        self.assertIn('activities_history_order',runs);self.assertIn('strength_history_order',strength)
        self.assertNotIn('recording_points',runs+strength);db.close()

    def test_failed_tenth_schema_rolls_back_indexes_and_keeps_all_records(self):
        db=sqlite3.connect(":memory:");create_schema(db,9)
        db.execute("INSERT INTO settings VALUES('kept','value')")
        # An incompatible object prevents the second index; first index must roll back.
        db.execute('CREATE TABLE strength_history_order(collision TEXT)');db.commit()
        with self.assertRaises(sqlite3.OperationalError):migrate_to_latest(db)
        self.assertEqual(9,db.execute('PRAGMA user_version').fetchone()[0])
        self.assertIsNone(db.execute("SELECT name FROM sqlite_master WHERE name='activities_history_order'").fetchone())
        self.assertEqual('value',db.execute("SELECT setting_value FROM settings WHERE setting_key='kept'").fetchone()[0]);db.close()

if __name__ == "__main__":
    unittest.main(verbosity=2)
