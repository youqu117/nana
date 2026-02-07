package com.pixelpet.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.pixelpet.data.AppDatabase
import com.pixelpet.data.AssetScanner
import com.pixelpet.data.PetRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pixelpet.R
import com.pixelpet.ui.home.HomeFragment
import com.pixelpet.ui.pets.MyPetsFragment
import com.pixelpet.ui.settings.SettingsFragment
import com.pixelpet.ui.shop.ShopFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
    lateinit var repository: PetRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        try {
            val db = AppDatabase.getDatabase(this)
            repository = PetRepository(db.petDao(), db.settingsDao())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
            // Return to avoid crashing on repository usage
            return
        }

        setupViewPager()
        
        lifecycleScope.launch {
            try {
                AssetScanner.scanAndPopulate(applicationContext, repository)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // FocusLock Permissions Check
        // Only run this if we successfully initialized
        checkInitialPermissions()
    }

    private fun setupViewPager() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 3
        
        val startTab = intent?.getIntExtra(EXTRA_OPEN_TAB, TAB_HOME) ?: TAB_HOME
        val startIndex = startTab.coerceIn(TAB_HOME, TAB_SETTINGS)
        viewPager.currentItem = startIndex
        bottomNav.selectedItemId = when (startIndex) {
            TAB_PETS -> R.id.nav_pets
            TAB_SHOP -> R.id.nav_shop
            TAB_SETTINGS -> R.id.nav_settings
            else -> R.id.nav_home
        }

        // Sync ViewPager -> BottomNav
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val itemId = when (position) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_pets
                    2 -> R.id.nav_shop
                    3 -> R.id.nav_settings
                    else -> R.id.nav_home
                }
                if (bottomNav.selectedItemId != itemId) {
                    bottomNav.selectedItemId = itemId
                }
            }
        })

        // Sync BottomNav -> ViewPager
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.nav_pets -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.nav_shop -> {
                    viewPager.currentItem = 2
                    true
                }
                R.id.nav_settings -> {
                    viewPager.currentItem = 3
                    true
                }
                else -> false
            }
        }
    }

    private fun checkInitialPermissions() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
        
        // Check Accessibility (FocusLock FocusCoreService)
        val expectedComponentName = android.content.ComponentName(this, com.pixelpet.focuslock.FocusCoreService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        var hasAccessibility = false
        if (enabledServicesSetting != null) {
            val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledComponent = android.content.ComponentName.unflattenFromString(componentNameString)
                if (enabledComponent != null && enabledComponent == expectedComponentName) {
                    hasAccessibility = true
                    break
                }
            }
        }

        if (hasOverlay && hasAccessibility) return

        lifecycleScope.launch {
            val prompted = repository.getSetting("permission_prompted_once").firstOrNull()?.toBoolean() ?: false
            if (prompted) return@launch
            repository.setSetting("permission_prompted_once", "true")
            try {
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.section_permissions))
                    .setMessage("为了实现桌宠悬浮和专注锁屏功能，App 需要以下权限：\n\n1. 悬浮窗权限 (显示桌宠)\n2. 无障碍服务 (专注锁屏检测)\n\n请点击“去设置”依次开启。")
                    .setPositiveButton("去设置") { _, _ ->
                        if (!hasOverlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                            startActivity(intent)
                            Toast.makeText(this@MainActivity, "请先开启悬浮窗权限", Toast.LENGTH_LONG).show()
                        } else if (!hasAccessibility) {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent)
                            Toast.makeText(this@MainActivity, "请开启 Todo 无障碍服务", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("稍后", null)
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private inner class MainPagerAdapter(fa: androidx.fragment.app.FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> MyPetsFragment()
                2 -> ShopFragment()
                3 -> SettingsFragment()
                else -> HomeFragment()
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val TAB_HOME = 0
        const val TAB_PETS = 1
        const val TAB_SHOP = 2
        const val TAB_SETTINGS = 3
    }
}

