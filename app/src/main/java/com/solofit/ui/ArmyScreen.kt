package com.solofit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solofit.game.GameEngine
import com.solofit.game.PlayerState
import com.solofit.game.SHADOW_ROSTER

@Composable
fun ArmyScreen(state: PlayerState) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).screenPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SystemWindow("АРМИЯ ТЕНЕЙ", accent = Sys.Purple) {
            SysText("Теней: ${state.shadows.size} / ${SHADOW_ROSTER.size}", color = Sys.Purple, bold = true)
            SysText("Суммарный бонус к опыту: +${GameEngine.shadowBonusPct(state)}%", color = Sys.Gold)
            Spacer(Modifier.height(10.dp))
            SHADOW_ROSTER.forEach { shadow ->
                val owned = shadow.name in state.shadows
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        SysText(if (owned) shadow.name else "???", color = if (owned) Sys.Purple else Sys.Dim, size = 16, bold = true)
                        SysText(if (owned) shadow.tier else "не извлечена", color = Sys.Dim, size = 12)
                    }
                    SysText(if (owned) "+${shadow.bonusPct}%" else "", color = Sys.Gold, bold = true)
                }
            }
        }
    }
}
