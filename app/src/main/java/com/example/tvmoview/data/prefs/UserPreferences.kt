package com.example.tvmoview.data.prefs

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    var sortBy: String
        get() = prefs.getString("sort_by", "NAME") ?: "NAME"
        set(value) { prefs.edit().putString("sort_by", value).apply() }

    var sortOrder: String
        get() = prefs.getString("sort_order", "DESC") ?: "DESC"
        set(value) { prefs.edit().putString("sort_order", value).apply() }

    var tileColumns: Int
        get() = prefs.getInt("tile_columns", 4)
        set(value) { prefs.edit().putInt("tile_columns", value).apply() }
}
