# Synthetic whole-plan regression snapshots

These five snapshots are **test data only**, not production training plans,
not an NHS Couch to 5K programme and not instructions for a runner to follow.

`SyntheticPlanFixtures` constructs stable examples for exercising whole-plan
serialization and configured regression invariants. The production
`TrainingPlanGenerator` still has no implementation.

Normal test runs compare every phase/workout/segment against these files and do
not rewrite them. If a fixture intentionally changes, use the test runner's
explicit `--write-snapshots <directory>` mode and review the full Git diff.

The fixture acceptance budgets are engineering test parameters, not universal
training progression rules or safety guarantees.
