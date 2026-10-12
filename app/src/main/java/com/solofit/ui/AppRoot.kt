package com.solofit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solofit.game.GameEvent
import com.solofit.game.text
import com.solofit.game.title

private enum class Tab(val label: String, val icon: ImageVector) {
    QUEST("Квест", Icons.Filled.CheckCircle),
    STATUS("Статус", Icons.Filled.Person),
    DUNGEON("Врата", Icons.Filled.Warning),
    ARMY("Армия", Icons.Filled.Favorite),
    SETTINGS("Опции", Icons.Filled.Settings)
}

@Composable
fun AppRoot(vm: MainViewModel, onAccepted: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val restLeft by vm.restLeft.collectAsStateWithLifecycle()
    val plank by vm.plankElapsed.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Sys.BgTop, Sys.Bg)))
    ) {
        if (!state.accepted) {
            WelcomeScreen(onAccept = { name -> vm.accept(name); onAccepted() })
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = Sys.Panel) {
                        Tab.entries.forEachIndexed { i, t ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Icon(t.icon, contentDescription = t.label) },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Sys.Bg,
                                    selectedTextColor = Sys.Blue,
                                    indicatorColor = Sys.Blue,
                                    unselectedIconColor = Sys.Dim,
                                    unselectedTextColor = Sys.Dim
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).systemBarsPadding().fillMaxSize()) {
                    when (Tab.entries[tab]) {
                        Tab.QUEST -> QuestScreen(
                            state, restLeft, plank,
                            onLog = vm::log, onLogPenalty = vm::logPenalty, onRestDay = vm::useRestDay,
                            onSkipRest = vm::skipRest, onStartPlank = vm::startPlank, onStopPlank = vm::stopPlank
                        )
                        Tab.STATUS -> StatusScreen(state, vm::allocate)
                        Tab.DUNGEON -> DungeonScreen(state)
                        Tab.ARMY -> ArmyScreen(state)
                        Tab.SETTINGS -> SettingsScreen(
                            state, vm::setName, vm::setDifficulty, vm::setRestSeconds, vm::setReminderHour, vm::resetAll
                        )
                    }
                }
            }
        }

        events.firstOrNull()?.let { e ->
            SystemDialog(
                title = e.title(),
                accent = eventColor(e),
                buttonText = if (e is GameEvent.PenaltyApplied) "Принять наказание" else "Принять",
                onClose = vm::dismissEvent
            ) {
                SysText(e.text(), color = Sys.Text)
            }
        }
    }
}

private fun eventColor(e: GameEvent): Color = when (e) {
    is GameEvent.PenaltyApplied, is GameEvent.ExtractionFailed -> Sys.Red
    is GameEvent.ShadowExtracted -> Sys.Purple
    is GameEvent.LevelUp, is GameEvent.RankUp, is GameEvent.RestTokenEarned -> Sys.Gold
    is GameEvent.QuestComplete, GameEvent.PenaltyCleared, is GameEvent.BossDefeated -> Sys.Green
}

@Composable
private fun WelcomeScreen(onAccept: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).screenPadding(),
        verticalArrangement = Arrangement.Center
    ) {
        SystemWindow("УВЕДОМЛЕНИЕ") {
            SysText("Вы получили квалификацию Игрока.", bold = true, size = 16)
            Spacer(Modifier.height(8.dp))
            SysText(
                "Каждый день вас ждёт квест: отжимания, пресс, приседания, бег и планка. " +
                    "Выполнение даёт опыт, статы и ранги. Пропуск отправляет в штрафную зону.",
                color = Sys.Dim
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text("Имя Игрока") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            SysButton("Принять", { onAccept(name) }, Modifier.fillMaxWidth())
        }
    }
}
