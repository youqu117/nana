package com.pixelpet.focuslock

import android.content.Context
import android.os.SystemClock

object FocusManager {
    const val PREFS_NAME = "focus_prefs"
    const val KEY_END_TIME = "end_time"
    const val KEY_TASK_NAME = "task_name"
    const val KEY_DURATION_MIN = "duration_min"
    const val KEY_TASK_ID = "task_id"
    const val KEY_STATS_DATE = "stats_date"
    const val KEY_STATS_MINUTES = "stats_minutes"
    const val KEY_STATS_POMODOROS = "stats_pomodoros"
    const val KEY_ALLOWED_PACKAGES = "allowed_packages"

    enum class State {
        IDLE, RUNNING
    }

    var currentState = State.IDLE
    var sessionEndTime = 0L
    var currentTaskName: String? = null
    var currentDurationMinutes = 0
    var currentTaskId: Long? = null
    
    val blockList = mutableSetOf<String>()
    private val allowedPackages = mutableSetOf<String>()

    // Default blocked apps (for focus we block specific distractions)
    private val blockedApps = setOf(
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.aweme", // Douyin
        "com.tencent.mm", // WeChat (Example)
        "com.tencent.mobileqq" // QQ
    )

    private val defaultAllowedApps = setOf(
        "com.android.systemui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.contacts"
    )

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedEndTime = prefs.getLong(KEY_END_TIME, 0)
            
            // Validation: If end time is too far in the future (e.g. > 24 hours) or in the past, reset it
            val now = System.currentTimeMillis()
            val maxDuration = 24 * 60 * 60 * 1000L // 24 hours
            
            if (savedEndTime > now) {
                if (savedEndTime - now > maxDuration) {
                    // Invalid state, reset
                    stopFocus(context, completed = false)
                } else {
                    // Re-calculate remaining time based on wall clock
                    val remainingMillis = savedEndTime - now
                    sessionEndTime = SystemClock.elapsedRealtime() + remainingMillis
                    currentState = State.RUNNING
                    blockList.addAll(blockedApps)
                    allowedPackages.clear()
                    allowedPackages.addAll(loadAllowedPackages(context))
                    currentTaskName = prefs.getString(KEY_TASK_NAME, null)
                    currentDurationMinutes = prefs.getInt(KEY_DURATION_MIN, 0)
                    val storedTaskId = prefs.getLong(KEY_TASK_ID, -1L)
                    currentTaskId = if (storedTaskId > 0) storedTaskId else null
                }
            } else {
                // Clear expired session
                if (savedEndTime != 0L) {
                    stopFocus(context, completed = false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Reset state on error
            currentState = State.IDLE
        }
    }

    fun startFocus(context: Context, durationMinutes: Int, taskName: String?, taskId: Long? = null) {
        val durationMillis = durationMinutes * 60 * 1000L
        sessionEndTime = SystemClock.elapsedRealtime() + durationMillis
        currentState = State.RUNNING
        currentDurationMinutes = durationMinutes
        currentTaskName = taskName?.takeIf { it.isNotBlank() }
        currentTaskId = taskId

        // For MVP we hardcode the block list or load from prefs
        blockList.clear()
        blockList.addAll(blockedApps)
        allowedPackages.clear()
        allowedPackages.addAll(loadAllowedPackages(context))

        // Save to prefs (Wall Clock)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_END_TIME, System.currentTimeMillis() + durationMillis)
            .putString(KEY_TASK_NAME, currentTaskName)
            .putInt(KEY_DURATION_MIN, currentDurationMinutes)
            .putLong(KEY_TASK_ID, taskId ?: -1L)
            .apply()
    }

    fun stopFocus(context: Context? = null, completed: Boolean = false) {
        if (completed && context != null) {
            updateTodayStats(context)
            completeTodoIfNeeded(context)
        }

        currentState = State.IDLE
        blockList.clear()
        sessionEndTime = 0
        currentTaskName = null
        currentDurationMinutes = 0
        currentTaskId = null

        context
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(KEY_END_TIME)
            ?.remove(KEY_TASK_NAME)
            ?.remove(KEY_DURATION_MIN)
            ?.remove(KEY_TASK_ID)
            ?.apply()
    }

    fun isBlocked(packageName: String, className: String? = null): Boolean {
        if (currentState != State.RUNNING) return false

        // Only allow our own focus module screens.
        if (packageName == "com.pixelpet") {
            val cls = className?.lowercase().orEmpty()
            if (cls.isBlank()) return false
            return !(cls.contains("focuslock") || cls.contains("lockactivity") || cls.contains("todoactivity"))
        }

        if (allowedPackages.contains(packageName)) return false
        if (packageName.contains("dialer") || packageName.contains("phone")) return false
        return true
    }

    fun getAllowedPackages(context: Context): Set<String> = loadAllowedPackages(context)

    fun saveAllowedPackages(context: Context, packages: Set<String>) {
        val cleaned = packages.map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
        cleaned.addAll(defaultAllowedApps)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALLOWED_PACKAGES, cleaned.joinToString(","))
            .apply()
        allowedPackages.clear()
        allowedPackages.addAll(cleaned)
    }

    fun checkAndCompleteIfNeeded(context: Context): Boolean {
        if (currentState != State.RUNNING) return false
        if (SystemClock.elapsedRealtime() > sessionEndTime) {
            stopFocus(context, completed = true)
            return true
        }
        return false
    }

    fun getRemainingTimeSeconds(): Long {
        if (currentState != State.RUNNING) return 0
        val remaining = sessionEndTime - SystemClock.elapsedRealtime()
        return if (remaining > 0) remaining / 1000 else 0
    }

    data class TodayStats(val totalMinutes: Int, val pomodoroCount: Int)

    fun getTodayStats(context: Context): TodayStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayKey = currentDateKey()
        val storedDate = prefs.getString(KEY_STATS_DATE, null)
        return if (storedDate == todayKey) {
            TodayStats(
                totalMinutes = prefs.getInt(KEY_STATS_MINUTES, 0),
                pomodoroCount = prefs.getInt(KEY_STATS_POMODOROS, 0)
            )
        } else {
            TodayStats(0, 0)
        }
    }

    private fun updateTodayStats(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayKey = currentDateKey()
        val storedDate = prefs.getString(KEY_STATS_DATE, null)
        
        var minutes = 0
        var count = 0
        
        if (storedDate == todayKey) {
            minutes = prefs.getInt(KEY_STATS_MINUTES, 0)
            count = prefs.getInt(KEY_STATS_POMODOROS, 0)
        }
        
        minutes += currentDurationMinutes
        count += 1
        
        prefs.edit()
            .putString(KEY_STATS_DATE, todayKey)
            .putInt(KEY_STATS_MINUTES, minutes)
            .putInt(KEY_STATS_POMODOROS, count)
            .apply()
    }

    private fun completeTodoIfNeeded(context: Context) {
        val taskId = currentTaskId ?: return
        try {
            val repo = TodoRepository(context)
            repo.markCompleted(taskId, completed = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun currentDateKey(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun loadAllowedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ALLOWED_PACKAGES, "").orEmpty()
        val fromPrefs = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        return (fromPrefs + defaultAllowedApps)
    }
}

