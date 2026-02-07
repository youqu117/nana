package com.pixelpet.focuslock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.content.ComponentName
import android.os.Handler
import android.os.Looper

class FocusCoreService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            try {
                FocusManager.checkAndCompleteIfNeeded(applicationContext)
            } finally {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        FocusManager.init(applicationContext)
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (FocusManager.checkAndCompleteIfNeeded(applicationContext)) return
        if (FocusManager.currentState != FocusManager.State.RUNNING) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val className = event.className?.toString()

            if (FocusManager.isBlocked(packageName, className)) {
                val intent = Intent(this, LockActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        super.onDestroy()
    }
}

