package com.solofit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solofit.game.Difficulty
import com.solofit.game.PlayerState

@Composable
private fun <T> ChipRow(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = { Text(label(item)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Sys.Blue,
                    selectedLabelColor = Sys.Bg,
                    labelColor = Sys.Text
                )
            )
        }
    }
}

@Composable
fun SettingsScreen(
    state: PlayerState,
    onName: (String) -> Unit,
    onDifficulty: (Difficulty) -> Unit,
    onRestSeconds: (Int) -> Unit,
    onReminderHour: (Int) -> Unit,
    onReset: () -> Unit
) {
    var name by remember(state.name) { mutableStateOf(state.name) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).screenPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SystemWindow("ИГРОК") {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            SysButton("Сохранить имя", { onName(name) }, enabled = name.isNotBlank() && name.trim() != state.name)
        }

        SystemWindow("СЛОЖНОСТЬ") {
            ChipRow(Difficulty.entries, state.difficulty, { it.title }, onDifficulty)
            Spacer(Modifier.height(6.dp))
            SysText("${state.difficulty.hint}. Новая сложность применится к завтрашнему квесту и следующим вратам.", color = Sys.Dim, size = 12)
        }

        SystemWindow("ОТДЫХ МЕЖДУ ПОДХОДАМИ") {
            ChipRow(listOf(0, 30, 60, 90, 120), state.restSeconds, { if (it == 0) "выкл" else "$it с" }, onRestSeconds)
        }

        SystemWindow("НАПОМИНАНИЯ") {
            ChipRow(listOf(16, 18, 19, 20, 21), state.reminderHour, { "$it:00" }, onReminderHour)
            Spacer(Modifier.height(6.dp))
            SysText("Если к этому часу квест не выполнен, Система напомнит о штрафе (примерно раз в 2 часа). Нужно разрешение на уведомления.", color = Sys.Dim, size = 12)
        }

        SysButton("Сбросить Систему", { confirmReset = true }, Modifier.fillMaxWidth(), accent = Sys.Red)
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Sys.Panel,
            title = { Text("[ СБРОС ]", color = Sys.Red) },
            text = { Text("Весь прогресс будет удалён без возможности восстановления.", color = Sys.Text) },
            confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text("Сбросить", color = Sys.Red) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена", color = Sys.Dim) } }
        )
    }
}
