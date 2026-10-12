package com.solofit.data

import android.content.Context
import com.solofit.game.PlayerState

class PlayerRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("solofit", Context.MODE_PRIVATE)

    fun load(): PlayerState = try {
        prefs.getString(KEY, null)?.let { playerStateFromJson(it) } ?: PlayerState()
    } catch (e: Exception) {
        PlayerState()
    }

    fun save(state: PlayerState) {
        prefs.edit().putString(KEY, state.toJson()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "state"
    }
}
