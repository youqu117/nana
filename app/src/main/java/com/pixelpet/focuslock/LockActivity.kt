package com.pixelpet.focuslock

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pixelpet.R

class LockActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var isHolding = false
    private var holdProgress = 0
    private val holdDuration = 3000L // 3 seconds to unlock
    private val updateInterval = 50L
    
    private lateinit var progressBar: ProgressBar
    private lateinit var btnUnlock: Button

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }
    
    private val holdRunnable = object : Runnable {
        override fun run() {
            if (isHolding) {
                holdProgress += (100 * updateInterval / holdDuration).toInt()
                progressBar.progress = holdProgress
                if (holdProgress >= 100) {
                    unlock()
                } else {
                    handler.postDelayed(this, updateInterval)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen flags
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContentView(R.layout.activity_lock)
        
        progressBar = findViewById(R.id.progressBarUnlock)
        btnUnlock = findViewById(R.id.btnGiveUp)
        
        setupUnlockButton()
    }

    private fun setupUnlockButton() {
        btnUnlock.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isHolding = true
                    holdProgress = 0
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 0
                    handler.post(holdRunnable)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isHolding = false
                    progressBar.visibility = View.INVISIBLE
                    handler.removeCallbacks(holdRunnable)
                    true
                }
                else -> false
            }
        }
    }

    private fun unlock() {
        isHolding = false
        handler.removeCallbacks(holdRunnable)
        Toast.makeText(this, "Emergency Unlock", Toast.LENGTH_SHORT).show()
        FocusManager.stopFocus(this, completed = false)
        finish()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        updateTime()
        handler.post(updateRunnable)
    }
    
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Soft lock - do nothing
    }

    private fun updateTime() {
        if (FocusManager.checkAndCompleteIfNeeded(this)) {
            finish()
            return
        }
        if (FocusManager.currentState != FocusManager.State.RUNNING) {
            finish()
            return
        }
        val remaining = FocusManager.getRemainingTimeSeconds()
        val min = remaining / 60
        val sec = remaining % 60
        findViewById<TextView>(R.id.tvRemainingTime).text = String.format("%02d:%02d", min, sec)
    }
}

