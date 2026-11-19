package org.racedaydreams.domain.strength

import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Incomplete authoring remains saveable; only a complete valid programme may be previewed. */
data class StrengthProgrammeDraft(
    private val id: String?,
    private val revision: Int,
    private val programmeId: String?,
    private val baseRevision: Int,
    private val name: String?,
    private val settings: StrengthProgramme.Settings?,
    private val start: LocalDate?,
    private var blocks: List<WeekBlock>?,
    private val origin: StrengthProgramme.Origin?,
    private val templateId: String?,
) {
    data class Day(
        private val id: String?,
        private val weekday: DayOfWeek?,
        private val title: String?,
        private var slots: List<GymWorkoutDefinition.Slot>?,
        private val notes: String?,
    ) {
        init {
            GymWorkoutDefinition.requireId(id, "draft workout")
            if (weekday == null) throw NullPointerException()
            StrengthProgramme.text(title, 120, false, "workout title")
            slots = Immutable.copyList(slots ?: throw NullPointerException())
            GymWorkoutDefinition.validateSlots(slots!!, true)
            require(slots!!.size <= 12) { "Use up to 12 programme exercises." }
            StrengthProgramme.text(notes, 2000, true, "workout notes")
        }

        fun id(): String = id!!

        fun weekday(): DayOfWeek = weekday!!

        fun title(): String = title!!

        fun slots(): List<GymWorkoutDefinition.Slot> = slots!!

        fun notes(): String? = notes

        override fun toString(): String =
            "Day[id=$id, weekday=$weekday, title=$title, slots=$slots, notes=$notes]"
    }

    data class WeekBlock(
        private val id: String?,
        private val phase: StrengthProgramme.Phase?,
        private val name: String?,
        private val weeks: Int,
        private var days: List<Day>?,
    ) {
        init {
            GymWorkoutDefinition.requireId(id, "draft block")
            if (phase == null) throw NullPointerException()
            StrengthProgramme.text(name, 120, false, "block title")
            require(weeks in 1..26) { "A block spans 1–26 weeks." }
            days = Immutable.copyList(days ?: throw NullPointerException())
            require(days!!.size <= 6) { "Use up to six weekly workouts." }
        }

        fun id(): String = id!!

        fun phase(): StrengthProgramme.Phase = phase!!

        fun name(): String = name!!

        fun weeks(): Int = weeks

        fun days(): List<Day> = days!!

        override fun toString(): String =
            "WeekBlock[id=$id, phase=$phase, name=$name, weeks=$weeks, days=$days]"
    }

    init {
        GymWorkoutDefinition.requireId(id, "draft")
        GymWorkoutDefinition.requireId(programmeId, "programme")
        require(revision >= 1 && baseRevision >= 0) { "Invalid draft revision." }
        require(name != null && name.length <= 120) { "Programme name is too long." }
        if (settings == null) throw NullPointerException()
        if (start == null) throw NullPointerException()
        if (origin == null) throw NullPointerException()
        if (origin == StrengthProgramme.Origin.TEMPLATE)
            GymWorkoutDefinition.requireId(templateId, "template")
        else require(templateId == null) { "Only a template has template provenance." }
        blocks = Immutable.copyList(blocks ?: throw NullPointerException())
        require(blocks!!.size <= 12 && blocks!!.sumOf { it.weeks() } <= 52) {
            "Use up to 12 blocks across 52 weeks."
        }
        val ids = mutableSetOf<String>()
        for (b in blocks!!) {
            require(ids.add(b.id())) { "Duplicate draft block." }
            for (d in b.days()) require(ids.add(d.id())) { "Duplicate draft workout." }
        }
    }

    fun id(): String = id!!

    fun revision(): Int = revision

    fun programmeId(): String = programmeId!!

    fun baseRevision(): Int = baseRevision

    fun name(): String = name!!

    fun settings(): StrengthProgramme.Settings = settings!!

    fun start(): LocalDate = start!!

    fun blocks(): List<WeekBlock> = blocks!!

    fun origin(): StrengthProgramme.Origin = origin!!

    fun templateId(): String? = templateId

    /** No actuals, activation, generated load, or fabricated custom-exercise guidance. */
    fun preview(
        at: Instant?,
        excluded: Set<String>?,
        custom: Map<String, CustomStrengthExercise>?,
    ): StrengthProgramme {
        if (excluded == null) throw NullPointerException()
        if (custom == null) throw NullPointerException()
        val result = mutableListOf<StrengthProgramme.Block>()
        var first = start()
        val tradeoffs = linkedSetOf<String>()
        tradeoffs.add("Custom targets are athlete-authored, not evidence of completed training.")
        tradeoffs.add(StrengthWorkoutBudget.explanation())
        for (block in blocks()) {
            require(block.days().size == settings().frequency()) {
                "Each block must match the chosen weekly frequency."
            }
            val weekdays = mutableSetOf<DayOfWeek>()
            val workouts = mutableListOf<StrengthProgramme.Workout>()
            for (day in block.days()) {
                require(weekdays.add(day.weekday()) && day.weekday() in settings().days()) {
                    "Choose distinct available days for each workout."
                }
                require(day.slots().isNotEmpty()) { "Add an exercise before previewing." }
                for (slot in day.slots()) {
                    require(slot.target() != null) {
                        "Review a target for every programme exercise."
                    }
                    require(slot.exercise().id() !in excluded) {
                        "Restore an excluded exercise before programming it."
                    }
                    val metadata = ExerciseMetadataCatalog.find(slot.exercise().id())
                    val requiredEquipment: ExerciseMetadata.EquipmentRequirement
                    if (metadata == null) {
                        val definition = custom[slot.exercise().id()]
                        require(definition != null && definition.exercise() == slot.exercise()) {
                            "Review the custom exercise metadata."
                        }
                        requiredEquipment = definition.equipment()
                    } else {
                        require(
                            ExerciseCatalog.definition(slot.exercise().id())!!.exercise() ==
                                slot.exercise()
                        ) {
                            "The canonical exercise definition is inconsistent."
                        }
                        requiredEquipment = metadata.equipment()
                    }
                    require(requiredEquipment.availableWith(settings().equipment())) {
                        "Confirm the complete equipment setup for each exercise."
                    }
                }
                for (week in 0 until block.weeks()) {
                    val offset = (day.weekday().getValue() - first.getDayOfWeek().getValue()).mod(7)
                    val date = first.plusWeeks(week.toLong()).plusDays(offset.toLong())
                    val workoutId = "${day.id()}-$date"
                    val minutes = StrengthWorkoutBudget.workoutMinutes(day.slots())
                    if (minutes > settings().sessionMinutes())
                        tradeoffs.add(
                            "${day.title()}: estimated $minutes minutes exceeds your ${settings().sessionMinutes()} minute session budget. Review targets before activation."
                        )
                    workouts.add(
                        StrengthProgramme.Workout(
                            workoutId,
                            date,
                            minutes,
                            GymWorkoutDefinition(
                                workoutId,
                                baseRevision + 1,
                                at,
                                day.title(),
                                day.slots(),
                            ),
                            day.notes(),
                        )
                    )
                }
            }
            result.add(
                StrengthProgramme.Block(
                    block.id(),
                    block.phase(),
                    block.name(),
                    first,
                    block.weeks(),
                    workouts,
                    "Athlete-authored block. Review targets and recovery before activation.",
                )
            )
            first = first.plusWeeks(block.weeks().toLong())
        }
        return StrengthProgramme(
            programmeId,
            baseRevision + 1,
            baseRevision,
            at,
            name,
            origin,
            templateId,
            settings,
            result,
            start,
            "Athlete reviewed a custom programme preview; not yet activated.",
            tradeoffs.toList(),
        )
    }

    fun replaceBlock(block: WeekBlock): StrengthProgrammeDraft {
        val next = blocks().toMutableList()
        val index = next.indexOfLast { it.id() == block.id() }
        if (index < 0) next.add(block) else next[index] = block
        return nextRevision(next)
    }

    fun deleteBlock(blockId: String?): StrengthProgrammeDraft {
        val next = blocks().filterNot { it.id() == blockId }
        require(next.size != blocks().size) { "Block not found." }
        return nextRevision(next)
    }

    fun duplicateBlock(blockId: String?, newId: String?): StrengthProgrammeDraft {
        require(blocks().none { it.id() == newId }) { "Choose a new block identity." }
        val original = block(blockId)
        val days =
            original.days().mapIndexed { index, day ->
                Day("$newId-day-$index", day.weekday(), day.title(), day.slots(), day.notes())
            }
        return replaceBlock(
            WeekBlock(newId, original.phase(), original.name(), original.weeks(), days)
        )
    }

    fun replaceDay(blockId: String?, day: Day): StrengthProgrammeDraft {
        val block = block(blockId)
        val days = block.days().toMutableList()
        val index = days.indexOfLast { it.id() == day.id() }
        if (index < 0) days.add(day) else days[index] = day
        return replaceBlock(WeekBlock(block.id(), block.phase(), block.name(), block.weeks(), days))
    }

    fun duplicateDay(
        blockId: String?,
        dayId: String?,
        newId: String?,
        weekday: DayOfWeek?,
    ): StrengthProgrammeDraft {
        require(blocks().flatMap { it.days() }.none { it.id() == newId }) {
            "Choose a new workout identity."
        }
        val original = day(blockId, dayId)
        return replaceDay(
            blockId,
            Day(newId, weekday, original.title(), original.slots(), original.notes()),
        )
    }

    fun deleteDay(blockId: String?, dayId: String?): StrengthProgrammeDraft {
        val b = block(blockId)
        val days = b.days().filterNot { it.id() == dayId }
        require(days.size != b.days().size) { "Workout not found." }
        return replaceBlock(WeekBlock(b.id(), b.phase(), b.name(), b.weeks(), days))
    }

    fun moveExercise(
        fromBlock: String?,
        fromDay: String?,
        toBlock: String?,
        toDay: String?,
        slotId: String?,
        position: Int,
    ): StrengthProgrammeDraft {
        val from = day(fromBlock, fromDay)
        val to = day(toBlock, toDay)
        val source = from.slots().toMutableList()
        val slot =
            source.firstOrNull { it.id() == slotId }
                ?: throw IllegalArgumentException("Exercise not found.")
        source.remove(slot)
        if (fromBlock == toBlock && fromDay == toDay) {
            require(position in 0..source.size) { "Invalid exercise position." }
            source.add(position, slot)
            return replaceDay(
                fromBlock,
                Day(from.id(), from.weekday(), from.title(), source, from.notes()),
            )
        }
        val target = to.slots().toMutableList()
        require(position in 0..target.size) { "Invalid exercise position." }
        target.add(position, slot)
        return replaceDay(
                fromBlock,
                Day(from.id(), from.weekday(), from.title(), source, from.notes()),
            )
            .replaceDay(toBlock, Day(to.id(), to.weekday(), to.title(), target, to.notes()))
    }

    fun reorderBlock(id: String?, position: Int): StrengthProgrammeDraft {
        val b = block(id)
        val next = blocks().toMutableList()
        next.remove(b)
        require(position in 0..next.size) { "Invalid block position." }
        next.add(position, b)
        return nextRevision(next)
    }

    private fun block(id: String?): WeekBlock =
        blocks().firstOrNull { it.id() == id } ?: throw IllegalArgumentException("Block not found.")

    private fun day(blockId: String?, id: String?): Day =
        block(blockId).days().firstOrNull { it.id() == id }
            ?: throw IllegalArgumentException("Workout not found.")

    private fun nextRevision(next: List<WeekBlock>): StrengthProgrammeDraft =
        StrengthProgrammeDraft(
            id,
            revision + 1,
            programmeId,
            baseRevision,
            name,
            settings,
            start,
            next,
            origin,
            templateId,
        )

    override fun toString(): String =
        "StrengthProgrammeDraft[id=$id, revision=$revision, programmeId=$programmeId, baseRevision=$baseRevision, name=$name, settings=$settings, start=$start, blocks=$blocks, origin=$origin, templateId=$templateId]"
}
