package com.solofit.game

import java.util.Locale

enum class Stat(val title: String, val short: String) {
    STR("Сила", "STR"),
    AGI("Ловкость", "AGI"),
    VIT("Выносливость", "VIT"),
    INT("Интеллект", "INT")
}

enum class Difficulty(val title: String, val mult: Double, val hint: String) {
    EASY("Новичок", 0.7, "70% от нормы"),
    NORMAL("Охотник", 1.0, "100% от нормы"),
    HARD("Монарх", 1.3, "130% от нормы")
}

/**
 * base/growth/cap — норма на 1 уровне, прирост за уровень и потолок (как «100/100/100/10 км» из оригинала).
 * round — шаг округления цели. damagePerUnit — урон боссу за единицу.
 */
enum class ExerciseType(
    val title: String,
    val unit: String,
    val stat: Stat,
    val base: Int,
    val growth: Int,
    val cap: Int,
    val round: Int,
    val damagePerUnit: Double,
    val timed: Boolean = false
) {
    PUSHUPS("Отжимания", "повт.", Stat.STR, 10, 2, 100, 1, 1.0),
    SITUPS("Пресс", "повт.", Stat.VIT, 10, 2, 100, 1, 1.0),
    SQUATS("Приседания", "повт.", Stat.AGI, 10, 2, 100, 1, 1.0),
    RUN("Бег", "км", Stat.VIT, 1000, 200, 10000, 100, 0.1),
    PLANK("Планка", "", Stat.INT, 30, 5, 300, 5, 0.5, timed = true),
    BURPEES("Берпи", "повт.", Stat.STR, 10, 1, 100, 1, 1.0);

    companion object {
        val daily = listOf(PUSHUPS, SITUPS, SQUATS, RUN, PLANK)
    }
}

/** Значение в «хранимых» единицах (бег — метры, планка — секунды) в читаемый вид. */
fun ExerciseType.valueText(v: Int): String = when (this) {
    ExerciseType.RUN -> String.format(Locale.US, "%.1f", v / 1000.0)
    ExerciseType.PLANK -> String.format(Locale.US, "%d:%02d", v / 60, v % 60)
    else -> v.toString()
}

val ExerciseType.quickSteps: List<Int>
    get() = when (this) {
        ExerciseType.RUN -> listOf(500, 1000, 2000)
        ExerciseType.PLANK -> listOf(15, 30, 60)
        else -> listOf(5, 10, 20)
    }

fun ExerciseType.stepLabel(v: Int): String = when (this) {
    ExerciseType.RUN -> "+" + valueText(v) + " км"
    ExerciseType.PLANK -> "+${v}с"
    else -> "+$v"
}

enum class Rank(val letter: String, val title: String, val minLevel: Int) {
    E("E", "Слабейший охотник", 1),
    D("D", "Охотник", 10),
    C("C", "Опытный охотник", 20),
    B("B", "Элитный охотник", 35),
    A("A", "Охотник национального уровня", 50),
    S("S", "Монарх", 70)
}

fun rankOf(level: Int): Rank = Rank.entries.last { level >= it.minLevel }

data class Shadow(val name: String, val tier: String, val bonusPct: Int)

val SHADOW_ROSTER = listOf(
    Shadow("Iron", "Рядовой", 2),
    Shadow("Tank", "Рядовой", 2),
    Shadow("Fangs", "Рядовой", 3),
    Shadow("Tusk", "Элита", 3),
    Shadow("Greed", "Элита", 4),
    Shadow("Kaisel", "Элита", 5),
    Shadow("Igris", "Маршал", 6),
    Shadow("Bellion", "Маршал", 7),
    Shadow("Beru", "Маршал", 8)
)

val BOSS_NAMES = listOf(
    "Каменный голем",
    "Король ледяных медведей",
    "Гигантский паук",
    "Повелитель гоблинов",
    "Красный страж",
    "Тёмный жрец",
    "Страж врат",
    "Королева муравьёв"
)

data class DailyQuest(
    val date: String = "",
    val targets: Map<ExerciseType, Int> = emptyMap(),
    val progress: Map<ExerciseType, Int> = emptyMap(),
    val completed: Boolean = false,
    val rested: Boolean = false,
    val statsAwarded: Set<ExerciseType> = emptySet()
)

data class PenaltyQuest(val target: Int, val progress: Int = 0)

const val EXTRACTION_FAILED = "FAILED"

data class Dungeon(
    val weekStart: String,
    val bossName: String,
    val rank: Rank,
    val maxHp: Int,
    val damage: Int = 0,
    val defeated: Boolean = false,
    /** null — ещё не было попытки; EXTRACTION_FAILED — неудача; иначе имя тени. */
    val extraction: String? = null
)

data class PlayerState(
    val accepted: Boolean = false,
    val name: String = "Игрок",
    val level: Int = 1,
    val xp: Int = 0,
    val stats: Map<Stat, Int> = Stat.entries.associateWith { 10 },
    val freePoints: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val totalQuests: Int = 0,
    val restTokens: Int = 1,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val restSeconds: Int = 60,
    val reminderHour: Int = 19,
    val quest: DailyQuest = DailyQuest(),
    val penalty: PenaltyQuest? = null,
    val dungeon: Dungeon? = null,
    val shadows: List<String> = emptyList(),
    val extractionPity: Int = 0
)

sealed interface GameEvent {
    data class QuestComplete(val xp: Int, val streak: Int) : GameEvent
    data class LevelUp(val level: Int, val points: Int) : GameEvent
    data class RankUp(val rank: Rank) : GameEvent
    data class PenaltyApplied(val missedDays: Int, val xpLost: Int, val burpees: Int) : GameEvent
    data object PenaltyCleared : GameEvent
    data class BossDefeated(val name: String, val xp: Int) : GameEvent
    data class ShadowExtracted(val shadow: Shadow) : GameEvent
    data class ExtractionFailed(val chancePct: Int) : GameEvent
    data object RestTokenEarned : GameEvent
}

fun GameEvent.title(): String = when (this) {
    is GameEvent.QuestComplete -> "КВЕСТ ВЫПОЛНЕН"
    is GameEvent.LevelUp -> "УРОВЕНЬ ПОВЫШЕН"
    is GameEvent.RankUp -> "РАНГ ПОВЫШЕН"
    is GameEvent.PenaltyApplied -> "ШТРАФНАЯ ЗОНА"
    GameEvent.PenaltyCleared -> "ШТРАФ СНЯТ"
    is GameEvent.BossDefeated -> "ПОДЗЕМЕЛЬЕ ЗАЧИЩЕНО"
    is GameEvent.ShadowExtracted -> "ИЗВЛЕЧЕНИЕ ТЕНИ"
    is GameEvent.ExtractionFailed -> "ИЗВЛЕЧЕНИЕ ПРОВАЛЕНО"
    GameEvent.RestTokenEarned -> "ДЕНЬ ОТДЫХА"
}

fun GameEvent.text(): String = when (this) {
    is GameEvent.QuestComplete ->
        "Ежедневный квест завершён.\nБонус: +$xp XP\nСерия: $streak дн."
    is GameEvent.LevelUp ->
        "Вы достигли уровня $level.\nПолучено свободных очков: $points"
    is GameEvent.RankUp ->
        "Ваш ранг: ${rank.letter} — ${rank.title}."
    is GameEvent.PenaltyApplied ->
        "Квест не выполнен (пропущено дней: $missedDays).\nСерия сброшена, потеряно $xpLost XP.\n" +
            "Выживите в штрафной зоне: $burpees берпи.\nПока штраф активен, опыт уменьшен вдвое."
    GameEvent.PenaltyCleared ->
        "Вы выжили в штрафной зоне.\nОпыт снова начисляется полностью."
    is GameEvent.BossDefeated ->
        "Босс «$name» повержен.\nНаграда: +$xp XP"
    is GameEvent.ShadowExtracted ->
        "«${shadow.name}» (${shadow.tier}) присягает вам на верность.\nБонус к опыту: +${shadow.bonusPct}%\nВстань."
    is GameEvent.ExtractionFailed ->
        "Тень рассеялась. Шанс был $chancePct%.\nСледующая попытка будет увереннее."
    GameEvent.RestTokenEarned ->
        "Серия достигла недели. Вы получили жетон дня отдыха."
}
