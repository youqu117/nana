package com.pixelpet.focuslock

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    var text: String,
    var isCompleted: Boolean = false
)

class TodoRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "todo_list"

    fun getTodos(): MutableList<TodoItem> {
        val json = prefs.getString(key, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<TodoItem>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveTodos(list: List<TodoItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }

    fun markCompleted(id: Long, completed: Boolean = true): Boolean {
        val list = getTodos()
        val item = list.firstOrNull { it.id == id } ?: return false
        item.isCompleted = completed
        saveTodos(list)
        return true
    }
}

