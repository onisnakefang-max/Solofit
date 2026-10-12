package com.solofit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.solofit.game.GameEngine
import com.solofit.game.PlayerState
import com.solofit.game.Rank
import com.solofit.game.Stat
import com.solofit.game.rankOf

fun rankColor(rank: Rank): Color = when (rank) {
    Rank.E -> Sys.Dim
    Rank.D -> Sys.Green
    Rank.C -> Sys.Blue
    Rank.B -> Sys.Purple
    Rank.A -> Sys.Gold
    Rank.S -> Sys.Red
}

@Composable
fun StatusScreen(state: PlayerState, onAllocate: (Stat) -> Unit) {
    val rank = rankOf(state.level)
    val rc = rankColor(rank)
    Column(
        Modifier.verticalScroll(rememberScrollState()).screenPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SystemWindow("СТАТУС") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp)
                        .background(Sys.Bg, RoundedCornerShape(4.dp))
                        .border(2.dp, rc, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(rank.letter, color = rc, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    SysText(state.name, size = 20, bold = true)
                    SysText(rank.title, color = rc)
                    SysText("Уровень ${state.level}", color = Sys.Cyan, bold = true)
                }
            }
            Spacer(Modifier.height(14.dp))
            val need = GameEngine.xpToNext(state.level)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SysText("XP", color = Sys.Dim)
                SysText("${state.xp} / $need", color = Sys.Dim)
            }
            Spacer(Modifier.height(4.dp))
            NeonBar(state.xp.toFloat() / need, Sys.Cyan, height = 10)
        }

        SystemWindow("ХАРАКТЕРИСТИКИ") {
            Stat.entries.forEach { stat ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SysText("${stat.title} (${stat.short})")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SysText("${state.stats[stat] ?: 0}", color = Sys.Cyan, size = 18, bold = true)
                        if (state.freePoints > 0) {
                            Spacer(Modifier.size(10.dp))
                            SysButton("+", { onAllocate(stat) }, accent = Sys.Gold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SysText(
                if (state.freePoints > 0) "Свободных очков: ${state.freePoints}" else "Свободных очков нет. Получайте их за уровни.",
                color = if (state.freePoints > 0) Sys.Gold else Sys.Dim,
                size = 12
            )
            Spacer(Modifier.height(4.dp))
            SysText("Статы растут за выполненные упражнения. Интеллект повышает шанс извлечения теней.", color = Sys.Dim, size = 12)
        }

        SystemWindow("ДОСЬЕ") {
            InfoRow("Серия", "${state.streak} дн.")
            InfoRow("Лучшая серия", "${state.bestStreak} дн.")
            InfoRow("Квестов выполнено", "${state.totalQuests}")
            InfoRow("Бонус серии", "+${GameEngine.streakBonusPct(state)}%")
            InfoRow("Бонус теней", "+${GameEngine.shadowBonusPct(state)}%")
            InfoRow("Жетоны отдыха", "${state.restTokens}")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        SysText(label, color = Sys.Dim)
        SysText(value, bold = true)
    }
}
