package com.pixelpet.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pixelpet.R
import com.pixelpet.core.AppContainer
import com.pixelpet.core.SettingKeys
import com.pixelpet.data.AssetScanner
import com.pixelpet.data.PetRepository
import com.pixelpet.focuslock.FocusCoreService
import com.pixelpet.ui.home.HomeFragment
import com.pixelpet.ui.pets.MyPetsFragment
import com.pixelpet.ui.settings.SettingsFragment
import com.pixelpet.ui.shop.ShopFragment
import com.pixelpet.util.PermissionHelper
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var repository: PetRepository
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = AppContainer.get(this)

        setupViewPager()

        lifecycleScope.launch {
            try {
                AssetScanner.scanAndPopulate(applicationContext, repository)
            } catch (e: Exception) {
                com.pixelpet.core.LogUtils.e(TAG, "scanAndPopulate failed", e)
            }
        }

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

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { viewPager.currentItem = 0; true }
                R.id.nav_pets -> { viewPager.currentItem = 1; true }
                R.id.nav_shop -> { viewPager.currentItem = 2; true }
                R.id.nav_settings -> { viewPager.currentItem = 3; true }
                else -> false
            }
        }
    }

    private fun checkInitialPermissions() {
        val hasOverlay = PermissionHelper.hasOverlayPermission(this)
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(
            this, FocusCoreService::class.java
        )
        if (hasOverlay && hasAccessibility) return

        lifecycleScope.launch {
            val prompted = repository.getSetting(SettingKeys.PERMISSION_PROMPTED_ONCE)
                .firstOrNull()?.toBoolean() ?: false
            if (prompted) return@launch
            repository.setSetting(SettingKeys.PERMISSION_PROMPTED_ONCE, "true")
            try {
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.section_permissions))
                    .setMessage(getString(R.string.home_permission_rationale))
                    .setPositiveButton(getString(R.string.home_permission_go_settings)) { _, _ ->
                        if (!hasOverlay) {
                            PermissionHelper.requestOverlayPermission(this@MainActivity)
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.home_permission_overlay_first),
                                Toast.LENGTH_LONG
                            ).show()
                        } else if (!hasAccessibility) {
                            PermissionHelper.openAccessibilitySettings(this@MainActivity)
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.home_permission_accessibility_first),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .setNegativeButton(getString(R.string.home_permission_later), null)
                    .show()
            } catch (e: Exception) {
                com.pixelpet.core.LogUtils.e(TAG, "show permission dialog failed", e)
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
        private const val TAG = "MainActivity"
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val TAB_HOME = 0
        const val TAB_PETS = 1
        const val TAB_SHOP = 2
        const val TAB_SETTINGS = 3
    }
}
