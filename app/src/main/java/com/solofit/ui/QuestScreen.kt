package com.solofit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.solofit.game.ExerciseType
import com.solofit.game.PlayerState
import com.solofit.game.quickSteps
import com.solofit.game.stepLabel
import com.solofit.game.valueText

@Composable
fun QuestScreen(
    state: PlayerState,
    restLeft: Int,
    plankElapsed: Int?,
    onLog: (ExerciseType, Int) -> Unit,
    onLogPenalty: (Int) -> Unit,
    onRestDay: () -> Unit,
    onSkipRest: () -> Unit,
    onStartPlank: () -> Unit,
    onStopPlank: () -> Unit
) {
    var customFor by remember { mutableStateOf<ExerciseType?>(null) }
    val q = state.quest

    Column(
        Modifier.verticalScroll(rememberScrollState()).screenPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (restLeft > 0) {
            SystemWindow("ОТДЫХ", accent = Sys.Cyan) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SysText("%d:%02d".format(restLeft / 60, restLeft % 60), color = Sys.Cyan, size = 28, bold = true)
                    SysButton("Пропустить", onSkipRest, accent = Sys.Cyan)
                }
            }
        }

        state.penalty?.let { p ->
            SystemWindow("ШТРАФНАЯ ЗОНА", accent = Sys.Red) {
                SysText("Выживите. Пока штраф активен, опыт уменьшен вдвое.", color = Sys.Red)
                Spacer(Modifier.height(8.dp))
                SysText("Берпи: ${p.progress} / ${p.target}", bold = true)
                Spacer(Modifier.height(6.dp))
                NeonBar(p.progress.toFloat() / p.target, Sys.Red)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20).forEach { SysButton("+$it", { onLogPenalty(it) }, accent = Sys.Red) }
                }
            }
        }

        val accent = when {
            q.completed -> Sys.Green
            q.rested -> Sys.Dim
            else -> Sys.Blue
        }
        SystemWindow("ЕЖЕДНЕВНЫЙ КВЕСТ", accent = accent) {
            SysText("Подготовка к тому, чтобы стать сильнее", color = Sys.Dim)
            Spacer(Modifier.height(4.dp))
            SysText("Серия: ${state.streak} дн.   Бонус к опыту: +${com.solofit.game.GameEngine.streakBonusPct(state) + com.solofit.game.GameEngine.shadowBonusPct(state)}%", color = Sys.Gold)
            Spacer(Modifier.height(12.dp))

            when {
                q.completed -> SysText("КВЕСТ ВЫПОЛНЕН. Можно продолжать: лишние повторения бьют босса.", color = Sys.Green, bold = true)
                q.rested -> SysText("День отдыха. Серия сохранена.", color = Sys.Dim, bold = true)
            }
            Spacer(Modifier.height(8.dp))

            ExerciseType.daily.forEach { type ->
                val target = q.targets[type] ?: return@forEach
                val done = q.progress[type] ?: 0
                ExerciseRow(
                    type = type,
                    done = done,
                    target = target,
                    enabled = !q.rested,
                    plankElapsed = if (type == ExerciseType.PLANK) plankElapsed else null,
                    onLog = { onLog(type, it) },
                    onCustom = { customFor = type },
                    onStartPlank = onStartPlank,
                    onStopPlank = onStopPlank
                )
                Spacer(Modifier.height(14.dp))
            }

            SysText(
                "Внимание! Если ежедневный квест не выполнен, последуют соответствующие штрафы.",
                color = Sys.Red, size = 12
            )
        }

        if (!q.completed && !q.rested && state.restTokens > 0) {
            SysButton("День отдыха (жетонов: ${state.restTokens})", onRestDay, Modifier.fillMaxWidth(), accent = Sys.Dim)
        }
    }

    customFor?.let { type ->
        CustomAmountDialog(type, onDismiss = { customFor = null }, onConfirm = { onLog(type, it); customFor = null })
    }
}

@Composable
private fun ExerciseRow(
    type: ExerciseType,
    done: Int,
    target: Int,
    enabled: Boolean,
    plankElapsed: Int?,
    onLog: (Int) -> Unit,
    onCustom: () -> Unit,
    onStartPlank: () -> Unit,
    onStopPlank: () -> Unit
) {
    val complete = done >= target
    val color = if (complete) Sys.Green else Sys.Blue
    val unit = if (type.unit.isEmpty()) "" else " ${type.unit}"
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SysText(type.title, color = if (complete) Sys.Green else Sys.Text, bold = true)
            SysText("${type.valueText(done)} / ${type.valueText(target)}$unit", color = color, bold = true)
        }
        Spacer(Modifier.height(6.dp))
        NeonBar(done.toFloat() / target, color)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (type == ExerciseType.PLANK) {
                if (plankElapsed == null) {
                    SysButton("▶ Старт", onStartPlank, enabled = enabled)
                } else {
                    SysButton("■ ${type.valueText(plankElapsed)}", onStopPlank, accent = Sys.Gold)
                }
            }
            type.quickSteps.forEach { step ->
                SysButton(type.stepLabel(step), { onLog(step) }, enabled = enabled)
            }
            SysButton("…", onCustom, enabled = enabled)
        }
    }
}

@Composable
private fun CustomAmountDialog(type: ExerciseType, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val hint = when (type) {
        ExerciseType.RUN -> "Километры (например 2.5)"
        ExerciseType.PLANK -> "Секунды"
        else -> "Повторения"
    }
    val parsed: Int? = if (type == ExerciseType.RUN) {
        text.replace(',', '.').toDoubleOrNull()?.let { Math.round(it * 1000).toInt() }
    } else {
        text.toIntOrNull()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sys.Panel,
        title = { Text("[ ${type.title.uppercase()} ]", color = Sys.Blue) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(hint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (type == ExerciseType.RUN) KeyboardType.Decimal else KeyboardType.Number
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.takeIf { it > 0 }?.let(onConfirm) }, enabled = (parsed ?: 0) > 0) {
                Text("Записать", color = if ((parsed ?: 0) > 0) Sys.Blue else Sys.Dim)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Sys.Dim) } }
    )
}
