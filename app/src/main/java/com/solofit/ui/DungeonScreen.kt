package com.solofit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solofit.game.EXTRACTION_FAILED
import com.solofit.game.GameEngine
import com.solofit.game.PlayerState
import com.solofit.game.SHADOW_ROSTER
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun DungeonScreen(state: PlayerState) {
    val d = state.dungeon
    Column(
        Modifier.verticalScroll(rememberScrollState()).screenPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (d == null) {
            SystemWindow("ВРАТА") { SysText("Врата ещё не открылись.", color = Sys.Dim) }
            return@Column
        }
        val accent = if (d.defeated) Sys.Green else rankColor(d.rank)
        SystemWindow("ВРАТА ПОДЗЕМЕЛЬЯ · РАНГ ${d.rank.letter}", accent = accent) {
            SysText(d.bossName, size = 22, bold = true)
            Spacer(Modifier.height(10.dp))
            val hp = (d.maxHp - d.damage).coerceAtLeast(0)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SysText("HP", color = Sys.Dim)
                SysText("$hp / ${d.maxHp}", color = Sys.Red, bold = true)
            }
            Spacer(Modifier.height(4.dp))
            NeonBar(hp.toFloat() / d.maxHp, Sys.Red, height = 12)
            Spacer(Modifier.height(12.dp))

            if (d.defeated) {
                SysText("ПОДЗЕМЕЛЬЕ ЗАЧИЩЕНО", color = Sys.Green, bold = true)
                Spacer(Modifier.height(4.dp))
                when (d.extraction) {
                    null -> SysText("Тени закончились: вся армия уже собрана.", color = Sys.Dim)
                    EXTRACTION_FAILED -> SysText("Извлечение тени не удалось. Повезёт в следующий раз.", color = Sys.Dim)
                    else -> SysText("Извлечена тень: ${d.extraction}", color = Sys.Purple, bold = true)
                }
            } else {
                val left = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(d.weekStart).plusDays(7)).toInt()
                SysText("Каждое повторение, километр и секунда планки бьют босса. До закрытия врат: $left дн.", color = Sys.Dim)
            }
        }

        SystemWindow("ИЗВЛЕЧЕНИЕ ТЕНЕЙ", accent = Sys.Purple) {
            if (state.shadows.size >= SHADOW_ROSTER.size) {
                SysText("Вся армия в сборе.", color = Sys.Purple)
            } else {
                SysText("Победите босса за неделю, чтобы получить шанс извлечь тень.", color = Sys.Dim)
                Spacer(Modifier.height(6.dp))
                SysText("Шанс сейчас: ${GameEngine.extractionChancePct(state)}%", color = Sys.Purple, bold = true)
                SysText("Растёт с интеллектом и после каждой неудачи.", color = Sys.Dim, size = 12)
            }
        }
    }
}
