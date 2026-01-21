package com.arv.buscaminas.data

import android.content.Context
import com.google.gson.Gson

class StorageManager(context: Context) {
    private val prefs = context.getSharedPreferences("minesweeper_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveGame(data: SaveData) {
        val json = gson.toJson(data)
        prefs.edit().putString("saved_game", json).apply()
    }

    fun loadGame(): SaveData? {
        val json = prefs.getString("saved_game", null) ?: return null
        return try {
            gson.fromJson(json, SaveData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun hasSavedGame(): Boolean = prefs.contains("saved_game")

    fun clearSave() {
        prefs.edit().remove("saved_game").apply()
    }
}