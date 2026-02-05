package app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import app.overlay.OverlayService

class MainActivity : AppCompatActivity() {
    private var isOverlayRunning = false
    private lateinit var toggleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isOverlayRunning = getPreferences(MODE_PRIVATE).getBoolean(PREF_OVERLAY_ENABLED, false)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        toggleButton = Button(this).apply {
            setOnClickListener { toggleOverlay() }
        }
        updateButtonText()

        layout.addView(toggleButton)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        if (isOverlayRunning && hasOverlayPermission()) {
            OverlayService.start(this)
        }
        updateButtonText()
    }

    private fun toggleOverlay() {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return
        }

        isOverlayRunning = !isOverlayRunning
        if (isOverlayRunning) {
            OverlayService.start(this)
        } else {
            OverlayService.stop(this)
        }
        persistOverlayState()
        updateButtonText()
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun updateButtonText() {
        toggleButton.text = if (isOverlayRunning) "停止桌宠" else "启动桌宠"
    }

    private fun persistOverlayState() {
        getPreferences(MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_OVERLAY_ENABLED, isOverlayRunning)
            .apply()
    }

    companion object {
        private const val PREF_OVERLAY_ENABLED = "pref_overlay_enabled"
    }
}
