package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.storage.CoachingArchiveJson.Table

class CoachingArchiveJsonTest {
    private fun row(vararg values: Pair<String, String?>) = buildJsonObject {
        values.forEach { (key, value) -> put(key, StoredJson.nullable(value)) }
    }

    private fun empty() = buildJsonObject {
        put("version", 1)
        Table.entries.forEach { put(it.sqlName, JsonArray(emptyList())) }
    }

    private fun change(root: JsonObject, table: Table, rows: List<JsonObject>) =
        JsonObject(root + (table.sqlName to JsonArray(rows)))

    private fun rows(root: JsonObject, table: Table) =
        root.getValue(table.sqlName).jsonArray.map { it.jsonObject }

    private fun field(row: JsonObject, key: String, value: String?) =
        JsonObject(row + (key to StoredJson.nullable(value)))

    private fun populated(): JsonObject {
        var root = empty()
        root =
            change(
                root,
                Table.VERSIONS,
                (1..2).map {
                    val v = CoachingArchiveFixtures.version(it)
                    row(
                        "id" to v.id(),
                        "plan_id" to v.planId(),
                        "version" to "$it",
                        "payload" to RunningCoachingJson.encodeVersion(v),
                    )
                },
            )
        root = change(root, Table.CURRENT, listOf(row("id" to "1", "version_id" to "v2")))
        val actual = LegacyFingerprintFixtures.session()
        root =
            change(
                root,
                Table.SESSIONS,
                listOf(
                    row(
                        "id" to actual.id(),
                        "started_at" to actual.startedAt().toString(),
                        "payload" to (" \n" + StrengthEvidenceJson.encodeSession(actual)),
                    )
                ),
            )
        val exercise = actual.exercises().first().exercise()
        root =
            change(
                root,
                Table.EXERCISES,
                listOf(
                    row(
                        "id" to exercise.id(),
                        "payload" to StrengthEvidenceJson.encodeExercise(exercise),
                    )
                ),
            )
        val response = RunExecution("run", "done", "v1", null, null, "Recorded evidence")
        root =
            change(
                root,
                Table.RESPONSES,
                listOf(
                    row(
                        "activity_id" to "run",
                        "planned_workout_id" to "done",
                        "plan_version_id" to "v1",
                        "payload" to RunningCoachingJson.encodeResponse(response),
                    )
                ),
            )
        val proposal = CoachingArchiveFixtures.proposal()
        root =
            change(
                root,
                Table.PROPOSALS,
                listOf(
                    row(
                        "id" to proposal.id(),
                        "base_version_id" to "v1",
                        "payload" to RunningCoachingJson.encodeProposal(proposal),
                        "decision" to "keep",
                        "result_version_id" to "v2",
                    )
                ),
            )
        return change(
            root,
            Table.FEEDBACK,
            listOf(
                row(
                    "activity_id" to "run",
                    "plan_version_id" to "v1",
                    "payload" to
                        """ {"ruleVersion":1,"score":7,"components":[{"legacy":"retained"}],"unknown":"keep"} """,
                )
            ),
        )
    }

    @Test
    fun completeGraphRoundTripsExactOriginalStringsAndKeepsHistorySeparate() {
        val root = populated()
        val snapshot = CoachingArchiveJson.decode(root)
        assertEquals("v2", snapshot.currentVersionId)
        assertEquals(setOf("v1", "v2"), snapshot.versions.keys)
        assertEquals(LegacyFingerprintFixtures.session(), snapshot.sessions.getValue("actual"))
        assertEquals(1, snapshot.exercises.size)
        assertEquals("keep", snapshot.proposals.single().decision)
        assertEquals("v1", snapshot.responses.getValue("run").planVersionId())
        assertEquals(root, CoachingArchiveJson.element(snapshot))
    }

    @Test
    fun emptyArchiveAndUndecidedOrUnlinkedEvidenceRemainValid() {
        assertNull(CoachingArchiveJson.decode(empty()).currentVersionId)
        var root = populated()
        val p = rows(root, Table.PROPOSALS).single()
        root =
            change(
                root,
                Table.PROPOSALS,
                listOf(field(field(p, "decision", null), "result_version_id", null)),
            )
        val actual = RunExecution("free-run", null, null, null, null, "Not associated")
        root =
            change(
                root,
                Table.RESPONSES,
                rows(root, Table.RESPONSES) +
                    row(
                        "activity_id" to actual.activityId(),
                        "planned_workout_id" to null,
                        "plan_version_id" to null,
                        "payload" to RunningCoachingJson.encodeResponse(actual),
                    ),
            )
        assertEquals(2, CoachingArchiveJson.decode(root).responses.size)
        assertNull(CoachingArchiveJson.decode(root).proposals.single().decision)
    }

    @Test
    fun duplicateRowsForEveryTableAndDuplicatePlanNumbersAreRejected() {
        val root = populated()
        for (table in Table.entries) assertFails {
            CoachingArchiveJson.decode(
                change(root, table, rows(root, table) + rows(root, table).first())
            )
        }
        val v = CoachingArchiveFixtures.version(1, id = "another")
        val extra =
            row(
                "id" to v.id(),
                "plan_id" to v.planId(),
                "version" to "1",
                "payload" to RunningCoachingJson.encodeVersion(v),
            )
        assertFails {
            CoachingArchiveJson.decode(
                change(root, Table.VERSIONS, rows(root, Table.VERSIONS) + extra)
            )
        }
    }

    @Test
    fun mismatchedColumnsMissingTablesAndUnsupportedVersionFailClosed() {
        val root = populated()
        for (table in Table.entries) {
            assertFails { CoachingArchiveJson.decode(JsonObject(root - table.sqlName)) }
            val first = rows(root, table).first()
            assertFails {
                CoachingArchiveJson.decode(
                    change(root, table, listOf(JsonObject(first + ("extra" to JsonNull))))
                )
            }
            val missing = JsonObject(first - table.columns.first())
            assertFails { CoachingArchiveJson.decode(change(root, table, listOf(missing))) }
        }
        assertFails {
            CoachingArchiveJson.decode(JsonObject(root + ("version" to JsonPrimitive(2))))
        }
        assertFails {
            CoachingArchiveJson.decode(
                change(
                    root,
                    Table.SESSIONS,
                    listOf(
                        field(
                            rows(root, Table.SESSIONS).single(),
                            "started_at",
                            "2020-01-01T00:00:00Z",
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun currentResponsesProposalsAndFeedbackMustRetainTheirVersionReferences() {
        val root = populated()
        assertFails {
            CoachingArchiveJson.decode(
                change(root, Table.CURRENT, listOf(row("id" to "1", "version_id" to "missing")))
            )
        }
        val feedback = rows(root, Table.FEEDBACK).single()
        assertFails {
            CoachingArchiveJson.decode(
                change(root, Table.FEEDBACK, listOf(field(feedback, "plan_version_id", "missing")))
            )
        }
        val proposal = CoachingArchiveFixtures.proposal(versionId = "missing")
        assertFails {
            CoachingArchiveJson.decode(
                change(
                    root,
                    Table.PROPOSALS,
                    listOf(
                        row(
                            "id" to proposal.id(),
                            "base_version_id" to "missing",
                            "payload" to RunningCoachingJson.encodeProposal(proposal),
                            "decision" to null,
                            "result_version_id" to null,
                        )
                    ),
                )
            )
        }
        // v1 is referenced by actual response, proposal and feedback, even though v2 is current.
        assertFails {
            CoachingArchiveJson.decode(
                change(root, Table.VERSIONS, rows(root, Table.VERSIONS).drop(1))
            )
        }
        val r = RunExecution("run", "missing", "v1", null, null, "Recorded")
        assertFails {
            CoachingArchiveJson.decode(
                change(
                    root,
                    Table.RESPONSES,
                    listOf(
                        row(
                            "activity_id" to "run",
                            "planned_workout_id" to "missing",
                            "plan_version_id" to "v1",
                            "payload" to RunningCoachingJson.encodeResponse(r),
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun decisionsRequireAnExistingOptionAndResultTogetherWithoutActivatingAnything() {
        val root = populated()
        val p = rows(root, Table.PROPOSALS).single()
        for (bad in
            listOf(
                field(p, "decision", null),
                field(p, "result_version_id", null),
                field(p, "decision", "unknown"),
                field(p, "result_version_id", "missing"),
            )) assertFails {
            CoachingArchiveJson.decode(change(root, Table.PROPOSALS, listOf(bad)))
        }
        assertEquals("v2", CoachingArchiveJson.decode(root).currentVersionId)
    }

    @Test
    fun historicFeedbackIsPreservedWithoutInventingCurrentComponentsOrScores() {
        val root = populated()
        val f = rows(root, Table.FEEDBACK).single()
        for (payload in
            listOf(
                """{"ruleVersion":999,"score":7,"components":[]}""",
                """{"ruleVersion":1,"score":11,"components":[]}""",
                """{"ruleVersion":1,"score":null,"components":null}""",
            )) assertFails {
            CoachingArchiveJson.decode(
                change(root, Table.FEEDBACK, listOf(field(f, "payload", payload)))
            )
        }
        assertEquals(root, CoachingArchiveJson.element(CoachingArchiveJson.decode(root)))
    }

    @Test
    fun archiveViewsDoNotExposeMutableSqlNullEntries() {
        val root = populated()
        val snapshot = CoachingArchiveJson.decode(root)
        assertFails { (snapshot.rows as MutableMap).clear() }
        val row = snapshot.rows.getValue(Table.RESPONSES).single()
        assertEquals(
            rows(root, Table.RESPONSES).single().mapValues { it.value.jsonPrimitive.contentOrNull },
            row,
        )
        assertEquals(
            rows(root, Table.RESPONSES)
                .single()
                .mapValues { it.value.jsonPrimitive.contentOrNull }
                .hashCode(),
            row.hashCode(),
        )
        assertFails { (row as MutableMap).clear() }
    }
}
