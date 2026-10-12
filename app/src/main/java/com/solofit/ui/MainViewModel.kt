package com.solofit.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solofit.data.PlayerRepository
import com.solofit.game.Difficulty
import com.solofit.game.ExerciseType
import com.solofit.game.GameEngine
import com.solofit.game.GameEvent
import com.solofit.game.PlayerState
import com.solofit.game.Stat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlayerRepository(app)

    private val _state = MutableStateFlow(repo.load())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Очередь системных окон: показываем по одному. */
    private val _events = MutableStateFlow<List<GameEvent>>(emptyList())
    val events: StateFlow<List<GameEvent>> = _events.asStateFlow()

    private val _restLeft = MutableStateFlow(0)
    val restLeft: StateFlow<Int> = _restLeft.asStateFlow()

    /** Секунды секундомера планки; null — секундомер не запущен. */
    private val _plankElapsed = MutableStateFlow<Int?>(null)
    val plankElapsed: StateFlow<Int?> = _plankElapsed.asStateFlow()

    private var restJob: Job? = null
    private var plankJob: Job? = null
    private var plankStartedAt = 0L

    init {
        refresh()
    }

    private fun today(): LocalDate = LocalDate.now()

    private fun commit(state: PlayerState, events: List<GameEvent> = emptyList()) {
        _state.value = state
        repo.save(state)
        if (events.isNotEmpty()) _events.value = _events.value + events
    }

    fun refresh() {
        val r = GameEngine.refresh(_state.value, today())
        if (r.state != _state.value || r.events.isNotEmpty()) commit(r.state, r.events)
    }

    fun accept(name: String) {
        commit(GameEngine.accept(_state.value, name))
        refresh()
    }

    fun log(type: ExerciseType, amount: Int) {
        val r = GameEngine.logProgress(_state.value, type, amount, today())
        if (r.state == _state.value) return
        commit(r.state, r.events)
        if (type != ExerciseType.RUN && type != ExerciseType.PLANK) startRest()
    }

    fun logPenalty(amount: Int) {
        val r = GameEngine.logPenalty(_state.value, amount)
        commit(r.state, r.events)
        startRest()
    }

    fun useRestDay() = commit(GameEngine.useRestDay(_state.value, today()))

    fun allocate(stat: Stat) = commit(GameEngine.allocate(_state.value, stat))

    fun dismissEvent() {
        _events.value = _events.value.drop(1)
    }

    // ------------------------------------------------------------------ настройки

    fun setName(name: String) {
        if (name.isNotBlank()) commit(_state.value.copy(name = name.trim()))
    }

    fun setDifficulty(d: Difficulty) = commit(_state.value.copy(difficulty = d))
    fun setRestSeconds(s: Int) = commit(_state.value.copy(restSeconds = s))
    fun setReminderHour(h: Int) = commit(_state.value.copy(reminderHour = h))

    fun resetAll() {
        stopTimers()
        repo.clear()
        _events.value = emptyList()
        _state.value = PlayerState()
    }

    // ------------------------------------------------------------------ таймеры

    private fun startRest() {
        restJob?.cancel()
        val seconds = _state.value.restSeconds
        if (seconds <= 0) return
        val end = SystemClock.elapsedRealtime() + seconds * 1000L
        restJob = viewModelScope.launch {
            while (true) {
                val left = ((end - SystemClock.elapsedRealtime() + 999) / 1000).toInt()
                if (left <= 0) break
                _restLeft.value = left
                delay(250)
            }
            _restLeft.value = 0
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _restLeft.value = 0
    }

    fun startPlank() {
        if (plankJob?.isActive == true) return
        plankStartedAt = SystemClock.elapsedRealtime()
        _plankElapsed.value = 0
        plankJob = viewModelScope.launch {
            while (true) {
                _plankElapsed.value = ((SystemClock.elapsedRealtime() - plankStartedAt) / 1000).toInt()
                delay(250)
            }
        }
    }

    fun stopPlank() {
        plankJob?.cancel()
        val seconds = ((SystemClock.elapsedRealtime() - plankStartedAt) / 1000).toInt()
        _plankElapsed.value = null
        if (seconds >= 1) log(ExerciseType.PLANK, seconds)
    }

    private fun stopTimers() {
        restJob?.cancel()
        plankJob?.cancel()
        _restLeft.value = 0
        _plankElapsed.value = null
    }
}
