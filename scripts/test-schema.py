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


def create_schema(connection, version=2, frozen=False):
    connection.execute("PRAGMA foreign_keys = ON")
    with connection:
        connection.execute("BEGIN")
        for statement in FROZEN_V1 if frozen else schema_statements(1):
            connection.execute(statement)
        if version >= 2:
            for statement in schema_statements(2):
                connection.execute(statement)
        connection.execute(f"PRAGMA user_version = {version}")


def migrate_one_to_two(connection):
    if connection.execute("PRAGMA user_version").fetchone()[0] != 1:
        raise ValueError("Only the explicit version 1 to 2 path is supported")
    # SQLiteOpenHelper invokes onUpgrade inside a transaction. Keep that same
    # boundary here so a failure cannot leave partially created tables.
    with connection:
        connection.execute("BEGIN")
        for statement in schema_statements(2):
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
        self.assertEqual(2, self.db.execute("PRAGMA user_version").fetchone()[0])

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
        migrate_one_to_two(self.db)
        self.assertEqual(before, self.snapshot())
        self.assertEqual(2, self.db.execute("PRAGMA user_version").fetchone()[0])
        self.db.execute("INSERT INTO shoes VALUES ('new-shoe','{}',1)")
        self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM shoes").fetchone()[0])

    def test_failed_migration_rolls_back_every_new_table_and_keeps_old_rows(self):
        before = self.snapshot()
        # A collision occurs after activities/index creation, proving earlier DDL rolls back.
        self.db.execute("CREATE TABLE shoes (collision TEXT)")
        with self.assertRaises(sqlite3.OperationalError):
            migrate_one_to_two(self.db)
        self.assertEqual(1, self.db.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual(before, self.snapshot())
        self.assertIsNone(self.db.execute("SELECT name FROM sqlite_master WHERE name='activities'").fetchone())
        self.assertIsNone(self.db.execute("SELECT name FROM sqlite_master WHERE name='activities_start'").fetchone())
        self.assertEqual("collision", self.db.execute("PRAGMA table_info(shoes)").fetchone()[1])

    def test_unknown_migration_version_is_rejected_without_changes(self):
        before = self.snapshot()
        self.db.execute("PRAGMA user_version=99")
        with self.assertRaises(ValueError):
            migrate_one_to_two(self.db)
        self.assertEqual(99, self.db.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual(before, self.snapshot())


if __name__ == "__main__":
    unittest.main(verbosity=2)
