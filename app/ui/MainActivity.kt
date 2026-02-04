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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val toggleButton = Button(this).apply {
            text = "启动桌宠"
            setOnClickListener { toggleOverlay(this) }
        }

        layout.addView(toggleButton)
        setContentView(layout)
    }

    private fun toggleOverlay(button: Button) {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return
        }

        isOverlayRunning = !isOverlayRunning
        if (isOverlayRunning) {
            OverlayService.start(this)
            button.text = "停止桌宠"
        } else {
            OverlayService.stop(this)
            button.text = "启动桌宠"
        }
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
}
