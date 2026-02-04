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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val permissionButton = Button(this).apply {
            text = "授权悬浮窗"
            setOnClickListener { requestOverlayPermission() }
        }

        val showButton = Button(this).apply {
            text = "显示桌宠"
            setOnClickListener { OverlayService.start(this@MainActivity) }
        }

        val hideButton = Button(this).apply {
            text = "隐藏桌宠"
            setOnClickListener { OverlayService.stop(this@MainActivity) }
        }

        layout.addView(permissionButton)
        layout.addView(showButton)
        layout.addView(hideButton)

        setContentView(layout)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
