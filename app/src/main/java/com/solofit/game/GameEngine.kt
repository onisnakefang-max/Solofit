package com.solofit.game

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class EngineResult(val state: PlayerState, val events: List<GameEvent> = emptyList())

/** Чистая игровая логика без Android-зависимостей: легко тестируется на JVM. */
object GameEngine {
    const val QUEST_SIZE = 5
    const val MAX_REST_TOKENS = 2
    const val POINTS_PER_LEVEL = 3
    const val BASE_STAT = 10

    fun xpToNext(level: Int) = 100 + 40 * (level - 1)
    fun questXp(level: Int) = 60 + 8 * level

    fun target(type: ExerciseType, level: Int, diff: Difficulty): Int {
        val raw = min(type.cap, type.base + type.growth * (level - 1)) * diff.mult
        return max(type.round, (raw / type.round).roundToInt() * type.round)
    }

    fun penaltyTarget(level: Int, missedDays: Int) =
        min(150, (10 + level) * min(missedDays, 3))

    fun shadowBonusPct(s: PlayerState) =
        s.shadows.sumOf { n -> SHADOW_ROSTER.firstOrNull { it.name == n }?.bonusPct ?: 0 }

    fun streakBonusPct(s: PlayerState) = min(s.streak, 30)

    fun xpMultiplier(s: PlayerState) = 1.0 + (shadowBonusPct(s) + streakBonusPct(s)) / 100.0

    fun extractionChancePct(s: PlayerState) =
        min(90, 40 + (s.stats[Stat.INT] ?: BASE_STAT) + s.extractionPity * 10)

    fun dailyDamage(level: Int, diff: Difficulty): Int =
        ExerciseType.daily.sumOf { (target(it, level, diff) * it.damagePerUnit).roundToInt() }

    fun weekStart(today: LocalDate): LocalDate =
        today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    // ---------------------------------------------------------------- публичный API

    fun accept(state: PlayerState, name: String): PlayerState =
        state.copy(accepted = true, name = name.trim().ifEmpty { "Игрок" })

    /** Вызывать при каждом открытии приложения: смена дня, штрафы, новый квест, новая неделя подземелья. */
    fun refresh(state: PlayerState, today: LocalDate, rnd: Random = Random.Default): EngineResult {
        if (!state.accepted) return EngineResult(state)
        val c = Ctx(state, rnd)
        val todayS = today.toString()

        val q = c.s.quest
        if (q.date != todayS) {
            if (q.date.isNotEmpty()) {
                val gap = ChronoUnit.DAYS.between(LocalDate.parse(q.date), today).toInt()
                if (gap < 0) return EngineResult(state) // часы переведены назад — ничего не трогаем
                val lastOk = q.completed || q.rested
                val missed = (if (lastOk) 0 else 1) + (gap - 1)
                if (missed > 0) c.applyPenalty(missed)
            }
            c.s = c.s.copy(quest = newQuest(c.s, todayS))
        }

        val ws = weekStart(today)
        if (c.s.dungeon?.weekStart != ws.toString()) {
            val level = c.s.level
            val idx = Math.floorMod(ws.toEpochDay() / 7, BOSS_NAMES.size.toLong()).toInt()
            c.s = c.s.copy(
                dungeon = Dungeon(
                    weekStart = ws.toString(),
                    bossName = BOSS_NAMES[idx],
                    rank = rankOf(level),
                    maxHp = dailyDamage(level, c.s.difficulty) * 5
                )
            )
        }
        return EngineResult(c.s, c.events)
    }

    fun logProgress(
        state: PlayerState,
        type: ExerciseType,
        amount: Int,
        today: LocalDate,
        rnd: Random = Random.Default
    ): EngineResult {
        val q = state.quest
        val target = q.targets[type]
        if (!state.accepted || amount <= 0 || q.date != today.toString() || q.rested || target == null) {
            return EngineResult(state)
        }
        val c = Ctx(state, rnd)
        val old = q.progress[type] ?: 0
        val new = old + amount
        val share = questXp(state.level) / QUEST_SIZE
        val gain = share * min(new, target) / target - share * min(old, target) / target

        c.s = c.s.copy(quest = q.copy(progress = q.progress + (type to new)))
        c.grantXp(gain)

        if (new >= target && type !in c.s.quest.statsAwarded) {
            val stats = c.s.stats + (type.stat to ((c.s.stats[type.stat] ?: BASE_STAT) + 1))
            c.s = c.s.copy(stats = stats, quest = c.s.quest.copy(statsAwarded = c.s.quest.statsAwarded + type))
        }

        val cq = c.s.quest
        if (!cq.completed && cq.targets.all { (t, v) -> (cq.progress[t] ?: 0) >= v }) c.completeQuest()

        c.damageBoss(type, amount)
        return EngineResult(c.s, c.events)
    }

    fun logPenalty(state: PlayerState, amount: Int): EngineResult {
        val p = state.penalty ?: return EngineResult(state)
        if (amount <= 0) return EngineResult(state)
        val progress = p.progress + amount
        return if (progress >= p.target) {
            EngineResult(state.copy(penalty = null), listOf(GameEvent.PenaltyCleared))
        } else {
            EngineResult(state.copy(penalty = p.copy(progress = progress)))
        }
    }

    fun useRestDay(state: PlayerState, today: LocalDate): PlayerState {
        val q = state.quest
        if (state.restTokens <= 0 || q.date != today.toString() || q.completed || q.rested) return state
        return state.copy(restTokens = state.restTokens - 1, quest = q.copy(rested = true))
    }

    fun allocate(state: PlayerState, stat: Stat): PlayerState {
        if (state.freePoints <= 0) return state
        return state.copy(
            freePoints = state.freePoints - 1,
            stats = state.stats + (stat to ((state.stats[stat] ?: BASE_STAT) + 1))
        )
    }

    // ---------------------------------------------------------------- внутренности

    private fun newQuest(s: PlayerState, date: String) = DailyQuest(
        date = date,
        targets = ExerciseType.daily.associateWith { target(it, s.level, s.difficulty) }
    )

    private class Ctx(var s: PlayerState, val rnd: Random) {
        val events = mutableListOf<GameEvent>()

        fun grantXp(base: Int): Int {
            if (base <= 0) return 0
            var amount = (base * xpMultiplier(s)).roundToInt()
            if (s.penalty != null) amount /= 2
            var xp = s.xp + amount
            var level = s.level
            var points = s.freePoints
            val oldRank = rankOf(level)
            while (xp >= xpToNext(level)) {
                xp -= xpToNext(level)
                level++
                points += POINTS_PER_LEVEL
                events += GameEvent.LevelUp(level, POINTS_PER_LEVEL)
            }
            s = s.copy(xp = xp, level = level, freePoints = points)
            if (rankOf(level) != oldRank) events += GameEvent.RankUp(rankOf(level))
            return amount
        }

        fun completeQuest() {
            val streak = s.streak + 1
            s = s.copy(
                quest = s.quest.copy(completed = true),
                streak = streak,
                bestStreak = max(s.bestStreak, streak),
                totalQuests = s.totalQuests + 1
            )
            val idx = events.size
            val bonus = grantXp(questXp(s.level) / 2)
            events.add(idx, GameEvent.QuestComplete(bonus, streak))
            if (streak % 7 == 0 && s.restTokens < MAX_REST_TOKENS) {
                s = s.copy(restTokens = s.restTokens + 1)
                events += GameEvent.RestTokenEarned
            }
        }

        fun applyPenalty(missed: Int) {
            val loss = min(s.xp, xpToNext(s.level) / 4 * min(missed, 4))
            val add = penaltyTarget(s.level, missed)
            val prev = s.penalty
            val penalty = if (prev == null) PenaltyQuest(add) else prev.copy(target = min(300, prev.target + add))
            s = s.copy(xp = s.xp - loss, streak = 0, penalty = penalty)
            events += GameEvent.PenaltyApplied(missed, loss, penalty.target - penalty.progress)
        }

        fun damageBoss(type: ExerciseType, amount: Int) {
            val d = s.dungeon ?: return
            if (d.defeated) return
            val total = d.damage + (amount * type.damagePerUnit).roundToInt()
            if (total < d.maxHp) {
                s = s.copy(dungeon = d.copy(damage = total))
                return
            }
            val reward = questXp(s.level) * 3
            var dungeon = d.copy(damage = d.maxHp, defeated = true)
            val gained = grantXp(reward)
            events += GameEvent.BossDefeated(d.bossName, gained)

            val next = SHADOW_ROSTER.firstOrNull { it.name !in s.shadows }
            if (next != null) {
                val chance = extractionChancePct(s)
                if (rnd.nextInt(100) < chance) {
                    s = s.copy(shadows = s.shadows + next.name, extractionPity = 0)
                    dungeon = dungeon.copy(extraction = next.name)
                    events += GameEvent.ShadowExtracted(next)
                } else {
                    s = s.copy(extractionPity = s.extractionPity + 1)
                    dungeon = dungeon.copy(extraction = EXTRACTION_FAILED)
                    events += GameEvent.ExtractionFailed(chance)
                }
            }
            s = s.copy(dungeon = dungeon)
        }
    }
}
