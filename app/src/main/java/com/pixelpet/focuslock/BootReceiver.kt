package com.pixelpet.focuslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                FocusManager.init(context)
                
                if (FocusManager.currentState == FocusManager.State.RUNNING) {
                    // Try to start service to ensure it's running as foreground
                    val serviceIntent = Intent(context, FocusCoreService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

