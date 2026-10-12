package com.solofit.game

import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineTest {
    private val day1 = LocalDate.of(2026, 3, 2) // понедельник
    private val rnd = Random(1)

    private fun started(): PlayerState {
        val s = GameEngine.accept(PlayerState(), "Тест")
        return GameEngine.refresh(s, day1, rnd).state
    }

    private fun completeAll(s0: PlayerState, day: LocalDate): EngineResult {
        var s = s0
        val events = mutableListOf<GameEvent>()
        for ((type, t) in s.quest.targets) {
            val r = GameEngine.logProgress(s, type, t, day, rnd)
            s = r.state
            events += r.events
        }
        return EngineResult(s, events)
    }

    @Test
    fun targetsScaleAndCap() {
        assertEquals(10, GameEngine.target(ExerciseType.PUSHUPS, 1, Difficulty.NORMAL))
        assertEquals(100, GameEngine.target(ExerciseType.PUSHUPS, 200, Difficulty.NORMAL))
        assertEquals(10000, GameEngine.target(ExerciseType.RUN, 200, Difficulty.NORMAL))
        assertEquals(1000, GameEngine.target(ExerciseType.RUN, 1, Difficulty.NORMAL))
        assertTrue(GameEngine.target(ExerciseType.PUSHUPS, 1, Difficulty.EASY) < 10)
    }

    @Test
    fun refreshCreatesQuestAndDungeon() {
        val s = started()
        assertEquals(5, s.quest.targets.size)
        assertNotNull(s.dungeon)
        assertEquals(day1.toString(), s.dungeon!!.weekStart)
    }

    @Test
    fun progressGivesXpAndCompletesQuest() {
        val r = completeAll(started(), day1)
        assertTrue(r.state.quest.completed)
        assertEquals(1, r.state.streak)
        assertTrue(r.events.any { it is GameEvent.QuestComplete })
        assertTrue(r.state.level > 1 || r.state.xp > 0)
        assertEquals(11, r.state.stats[Stat.STR])
    }

    @Test
    fun overflowDoesNotGiveExtraXp() {
        var s = started()
        val t = s.quest.targets.getValue(ExerciseType.PUSHUPS)
        s = GameEngine.logProgress(s, ExerciseType.PUSHUPS, t, day1, rnd).state
        val xp = s.xp + s.level * 1000
        val after = GameEngine.logProgress(s, ExerciseType.PUSHUPS, 50, day1, rnd).state
        assertEquals(xp, after.xp + after.level * 1000)
    }

    @Test
    fun missedDayTriggersPenalty() {
        var s = started()
        s = GameEngine.logProgress(s, ExerciseType.PUSHUPS, 5, day1, rnd).state
        val r = GameEngine.refresh(s, day1.plusDays(1), rnd)
        assertEquals(0, r.state.streak)
        assertNotNull(r.state.penalty)
        assertTrue(r.events.any { it is GameEvent.PenaltyApplied })
    }

    @Test
    fun completedYesterdayKeepsStreakWithoutPenalty() {
        val done = completeAll(started(), day1).state
        val r = GameEngine.refresh(done, day1.plusDays(1), rnd)
        assertNull(r.state.penalty)
        assertEquals(1, r.state.streak)
    }

    @Test
    fun skippedDaysAccumulatePenalty() {
        val done = completeAll(started(), day1).state
        val r = GameEngine.refresh(done, day1.plusDays(4), rnd)
        val e = r.events.filterIsInstance<GameEvent.PenaltyApplied>().single()
        assertEquals(3, e.missedDays)
    }

    @Test
    fun penaltyHalvesXpUntilCleared() {
        var s = started()
        s = GameEngine.refresh(s, day1.plusDays(2), rnd).state
        val p = s.penalty!!
        val withPenalty = GameEngine.logProgress(s, ExerciseType.SQUATS, 5, day1.plusDays(2), rnd).state
        val noPenalty = GameEngine.logProgress(s.copy(penalty = null), ExerciseType.SQUATS, 5, day1.plusDays(2), rnd).state
        assertTrue(withPenalty.xp + withPenalty.level * 1000 <= noPenalty.xp + noPenalty.level * 1000)
        val cleared = GameEngine.logPenalty(s, p.target)
        assertNull(cleared.state.penalty)
        assertTrue(cleared.events.contains(GameEvent.PenaltyCleared))
    }

    @Test
    fun restDayPreventsPenalty() {
        var s = started()
        s = GameEngine.useRestDay(s, day1)
        assertTrue(s.quest.rested)
        assertEquals(0, s.restTokens)
        val r = GameEngine.refresh(s, day1.plusDays(1), rnd)
        assertNull(r.state.penalty)
    }

    @Test
    fun bossDefeatedByVolume() {
        var s = started()
        val hp = s.dungeon!!.maxHp
        val alwaysLucky = object : Random() { override fun nextBits(bitCount: Int) = 0 }
        val r = GameEngine.logProgress(s, ExerciseType.PUSHUPS, hp, day1, alwaysLucky)
        assertTrue(r.state.dungeon!!.defeated)
        assertTrue(r.events.any { it is GameEvent.BossDefeated })
        assertEquals(listOf("Iron"), r.state.shadows)
        assertFalse(r.events.any { it is GameEvent.ExtractionFailed })
    }

    @Test
    fun levelUpGivesFreePointsAndAllocates() {
        var s = started().copy(xp = GameEngine.xpToNext(1) - 1)
        s = GameEngine.logProgress(s, ExerciseType.PUSHUPS, 10, day1, rnd).state
        assertEquals(2, s.level)
        assertEquals(3, s.freePoints)
        val a = GameEngine.allocate(s, Stat.AGI)
        assertEquals(2, a.freePoints)
        assertEquals(11, a.stats[Stat.AGI])
    }

    @Test
    fun logIgnoredForWrongDay() {
        val s = started()
        val r = GameEngine.logProgress(s, ExerciseType.PUSHUPS, 5, day1.plusDays(1), rnd)
        assertEquals(s, r.state)
    }
}
