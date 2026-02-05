package app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import app.data.AppDatabase
import app.data.PetRepository
import com.pixelpet.R

class OverlayService : Service() {
    private lateinit var overlayWindowManager: OverlayWindowManager
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        
        // Android 14 (SDK 34) requires specifying the service type
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        
        val db = AppDatabase.getDatabase(this)
        val repo = PetRepository(db.petDao(), db.settingsDao())
        
        overlayWindowManager = OverlayWindowManager(this, repo, scope)
        
        // Load enabled pets
        // For now, we just show the window manager which defaults to a pet
        // In future, we pass the instance data to the manager
        overlayWindowManager.show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isServiceRunning = false
        overlayWindowManager.hide()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = android.app.PendingIntent.getService(this, 0, stopIntent, android.app.PendingIntent.FLAG_IMMUTABLE)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_TEXT)
            .setSmallIcon(android.R.drawable.star_on)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "overlay_pet_channel"
        private const val CHANNEL_NAME = "桌宠悬浮服务"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_TITLE = "桌宠正在运行"
        private const val NOTIFICATION_TEXT = "点击返回应用进行设置"
        private const val ACTION_STOP = "app.overlay.ACTION_STOP"

        var isServiceRunning = false


        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }
}