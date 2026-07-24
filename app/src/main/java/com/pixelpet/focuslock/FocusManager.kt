package com.pixelpet.focuslock

import android.content.Context
import android.os.SystemClock
import com.pixelpet.core.LogUtils

/**
 * 专注会话状态机。
 *
 * 拦截语义：白名单制——专注期间仅允许系统电话、用户配置的白名单应用，
 * 以及本应用的 FocusLock 相关页面，其余一律拦截。
 *
 * 说明：本对象被 [FocusCoreService]（主线程）和多个 Activity 共同访问，
 * 因此所有可变状态都通过 [stateLock] 同步，对外暴露的读取也走同一把锁。
 */
object FocusManager {
    private const val TAG = "FocusManager"
    const val PREFS_NAME = "focus_prefs"
    const val KEY_END_TIME = "end_time"
    const val KEY_TASK_NAME = "task_name"
    const val KEY_DURATION_MIN = "duration_min"
    const val KEY_TASK_ID = "task_id"
    const val KEY_STATS_DATE = "stats_date"
    const val KEY_STATS_MINUTES = "stats_minutes"
    const val KEY_STATS_POMODOROS = "stats_pomodoros"
    const val KEY_ALLOWED_PACKAGES = "allowed_packages"

    private const val MAX_DURATION_MS = 24L * 60 * 60 * 1000

    /** 本应用自身的包名，用于 [isBlocked] 中的放行判断。 */
    private const val SELF_PACKAGE = "com.pixelpet"

    enum class State { IDLE, RUNNING }

    private val stateLock = Any()

    @Volatile
    private var _currentState = State.IDLE
    val currentState: State get() = _currentState

    @Volatile
    private var _sessionEndTime = 0L
    val sessionEndTime: Long get() = _sessionEndTime

    @Volatile
    private var _currentTaskName: String? = null
    val currentTaskName: String? get() = _currentTaskName

    @Volatile
    private var _currentDurationMinutes = 0
    val currentDurationMinutes: Int get() = _currentDurationMinutes

    @Volatile
    private var _currentTaskId: Long? = null
    val currentTaskId: Long? get() = _currentTaskId

    private val allowedPackages = mutableSetOf<String>()

    // 系统级必须放行的应用（电话/联系人/系统 UI），无法被用户移除。
    private val defaultAllowedApps = setOf(
        "com.android.systemui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.contacts",
        "com.android.phone",
        "com.android.incallui"
    )

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedEndTime = prefs.getLong(KEY_END_TIME, 0)
            val now = System.currentTimeMillis()

            if (savedEndTime > now) {
                if (savedEndTime - now > MAX_DURATION_MS) {
                    // 异常状态，重置。
                    stopFocus(context, completed = false)
                } else {
                    val remainingMillis = savedEndTime - now
                    synchronized(stateLock) {
                        _sessionEndTime = SystemClock.elapsedRealtime() + remainingMillis
                        _currentState = State.RUNNING
                        allowedPackages.clear()
                        allowedPackages.addAll(loadAllowedPackages(context))
                        _currentTaskName = prefs.getString(KEY_TASK_NAME, null)
                        _currentDurationMinutes = prefs.getInt(KEY_DURATION_MIN, 0)
                        val storedTaskId = prefs.getLong(KEY_TASK_ID, -1L)
                        _currentTaskId = if (storedTaskId > 0) storedTaskId else null
                    }
                }
            } else {
                if (savedEndTime != 0L) stopFocus(context, completed = false)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "init failed", e)
            synchronized(stateLock) { _currentState = State.IDLE }
        }
    }

    fun startFocus(context: Context, durationMinutes: Int, taskName: String?, taskId: Long? = null) {
        val durationMillis = durationMinutes * 60 * 1000L
        synchronized(stateLock) {
            _sessionEndTime = SystemClock.elapsedRealtime() + durationMillis
            _currentState = State.RUNNING
            _currentDurationMinutes = durationMinutes
            _currentTaskName = taskName?.takeIf { it.isNotBlank() }
            _currentTaskId = taskId

            allowedPackages.clear()
            allowedPackages.addAll(loadAllowedPackages(context))
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_END_TIME, System.currentTimeMillis() + durationMillis)
            .putString(KEY_TASK_NAME, _currentTaskName)
            .putInt(KEY_DURATION_MIN, _currentDurationMinutes)
            .putLong(KEY_TASK_ID, taskId ?: -1L)
            .apply()
    }

    fun stopFocus(context: Context? = null, completed: Boolean = false) {
        if (completed && context != null) {
            updateTodayStats(context)
            completeTodoIfNeeded(context)
        }

        synchronized(stateLock) {
            _currentState = State.IDLE
            allowedPackages.clear()
            _sessionEndTime = 0
            _currentTaskName = null
            _currentDurationMinutes = 0
            _currentTaskId = null
        }

        context
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(KEY_END_TIME)
            ?.remove(KEY_TASK_NAME)
            ?.remove(KEY_DURATION_MIN)
            ?.remove(KEY_TASK_ID)
            ?.apply()
    }

    /**
     * 判断 [packageName] 在专注期间是否应被拦截。
     *
     * 1. 未在专注中 -> 不拦截。
     * 2. 本应用自身：仅放行 FocusLock 相关页面（锁屏/任务/专注入口），其余本应用页面一律拦截，
     *    防止用户在专注期间切回应用主界面逃避。
     * 3. 系统电话/拨号 -> 放行（紧急情况）。
     * 4. 用户配置的白名单 -> 放行。
     * 5. 其余应用 -> 拦截（专注默认封闭）。
     */
    fun isBlocked(packageName: String, className: String? = null): Boolean {
        if (currentState != State.RUNNING) return false

        if (packageName == SELF_PACKAGE) {
            val cls = className?.lowercase().orEmpty()
            if (cls.isBlank()) return false
            // 仅放行专注锁相关 Activity。
            val isFocusUi = cls.contains("focuslock") ||
                cls.contains("lockactivity") ||
                cls.contains("todoactivity") ||
                cls.contains("focusmainactivity")
            return !isFocusUi
        }

        if (packageName.contains("dialer") || packageName.contains("phone")) return false

        val allowedSnapshot: Set<String>
        synchronized(stateLock) {
            allowedSnapshot = allowedPackages.toSet()
        }
        if (allowedSnapshot.contains(packageName)) return false
        // 专注模式默认封闭：非白名单应用一律拦截。
        return true
    }

    fun getAllowedPackages(context: Context): Set<String> = loadAllowedPackages(context)

    fun saveAllowedPackages(context: Context, packages: Set<String>) {
        val cleaned = packages.map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        cleaned.addAll(defaultAllowedApps)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALLOWED_PACKAGES, cleaned.joinToString(","))
            .apply()
        synchronized(stateLock) {
            allowedPackages.clear()
            allowedPackages.addAll(cleaned)
        }
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
            TodoRepository(context).markCompleted(taskId, completed = true)
        } catch (e: Exception) {
            LogUtils.e(TAG, "completeTodoIfNeeded failed for taskId=$taskId", e)
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
        return fromPrefs + defaultAllowedApps
    }
}
