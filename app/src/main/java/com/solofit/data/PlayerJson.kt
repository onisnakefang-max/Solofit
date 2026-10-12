package com.solofit.data

import com.solofit.game.DailyQuest
import com.solofit.game.Difficulty
import com.solofit.game.Dungeon
import com.solofit.game.ExerciseType
import com.solofit.game.PenaltyQuest
import com.solofit.game.PlayerState
import com.solofit.game.Rank
import com.solofit.game.Stat
import org.json.JSONArray
import org.json.JSONObject

private fun <K : Enum<K>> Map<K, Int>.toJson(): JSONObject =
    JSONObject().also { o -> forEach { (k, v) -> o.put(k.name, v) } }

private inline fun <reified K : Enum<K>> JSONObject?.toEnumMap(): Map<K, Int> {
    if (this == null) return emptyMap()
    val result = mutableMapOf<K, Int>()
    for (key in keys()) {
        val k = enumValues<K>().firstOrNull { it.name == key } ?: continue
        result[k] = optInt(key)
    }
    return result
}

fun PlayerState.toJson(): String {
    val o = JSONObject()
    o.put("accepted", accepted)
    o.put("name", name)
    o.put("level", level)
    o.put("xp", xp)
    o.put("stats", stats.toJson())
    o.put("freePoints", freePoints)
    o.put("streak", streak)
    o.put("bestStreak", bestStreak)
    o.put("totalQuests", totalQuests)
    o.put("restTokens", restTokens)
    o.put("difficulty", difficulty.name)
    o.put("restSeconds", restSeconds)
    o.put("reminderHour", reminderHour)
    o.put("extractionPity", extractionPity)
    o.put("shadows", JSONArray(shadows))
    o.put(
        "quest",
        JSONObject().apply {
            put("date", quest.date)
            put("targets", quest.targets.toJson())
            put("progress", quest.progress.toJson())
            put("completed", quest.completed)
            put("rested", quest.rested)
            put("statsAwarded", JSONArray(quest.statsAwarded.map { it.name }))
        }
    )
    penalty?.let {
        o.put("penalty", JSONObject().put("target", it.target).put("progress", it.progress))
    }
    dungeon?.let {
        o.put(
            "dungeon",
            JSONObject().apply {
                put("weekStart", it.weekStart)
                put("bossName", it.bossName)
                put("rank", it.rank.name)
                put("maxHp", it.maxHp)
                put("damage", it.damage)
                put("defeated", it.defeated)
                it.extraction?.let { e -> put("extraction", e) }
            }
        )
    }
    return o.toString()
}

fun playerStateFromJson(json: String): PlayerState {
    val o = JSONObject(json)
    val def = PlayerState()

    val stats = def.stats + o.optJSONObject("stats").toEnumMap<Stat>()

    val qo = o.optJSONObject("quest")
    val quest = if (qo == null) DailyQuest() else DailyQuest(
        date = qo.optString("date"),
        targets = qo.optJSONObject("targets").toEnumMap<ExerciseType>(),
        progress = qo.optJSONObject("progress").toEnumMap<ExerciseType>(),
        completed = qo.optBoolean("completed"),
        rested = qo.optBoolean("rested"),
        statsAwarded = qo.optJSONArray("statsAwarded").strings().mapNotNull { n ->
            ExerciseType.entries.firstOrNull { it.name == n }
        }.toSet()
    )

    val po = o.optJSONObject("penalty")
    val penalty = po?.let { PenaltyQuest(it.optInt("target"), it.optInt("progress")) }

    val dobj = o.optJSONObject("dungeon")
    val dungeon = dobj?.let {
        Dungeon(
            weekStart = it.optString("weekStart"),
            bossName = it.optString("bossName"),
            rank = Rank.entries.firstOrNull { r -> r.name == it.optString("rank") } ?: Rank.E,
            maxHp = it.optInt("maxHp"),
            damage = it.optInt("damage"),
            defeated = it.optBoolean("defeated"),
            extraction = if (it.has("extraction")) it.optString("extraction") else null
        )
    }

    return PlayerState(
        accepted = o.optBoolean("accepted"),
        name = o.optString("name", def.name),
        level = o.optInt("level", 1),
        xp = o.optInt("xp"),
        stats = stats,
        freePoints = o.optInt("freePoints"),
        streak = o.optInt("streak"),
        bestStreak = o.optInt("bestStreak"),
        totalQuests = o.optInt("totalQuests"),
        restTokens = o.optInt("restTokens", def.restTokens),
        difficulty = Difficulty.entries.firstOrNull { it.name == o.optString("difficulty") } ?: def.difficulty,
        restSeconds = o.optInt("restSeconds", def.restSeconds),
        reminderHour = o.optInt("reminderHour", def.reminderHour),
        quest = quest,
        penalty = penalty,
        dungeon = dungeon,
        shadows = o.optJSONArray("shadows").strings(),
        extractionPity = o.optInt("extractionPity")
    )
}

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else List(length()) { optString(it) }
