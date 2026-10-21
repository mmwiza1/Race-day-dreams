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


def create_schema(connection):
    """Read concatenated Java string literals directly, avoiding a second schema definition."""
    source = SCHEMA.read_text(encoding="utf-8")
    statements = re.findall(r"db\.execSQL\((.*?)\);", source, flags=re.DOTALL)
    if not statements:
        raise AssertionError("No SQLite schema statements found in ProfileDatabase.java")
    connection.execute("PRAGMA foreign_keys = ON")
    for statement in statements:
        literals = re.findall(r'"(?:\\.|[^"\\])*"', statement)
        connection.execute("".join(ast.literal_eval(value) for value in literals))


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


if __name__ == "__main__":
    unittest.main(verbosity=2)
