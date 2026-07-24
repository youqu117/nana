package com.pixelpet.ui.petroom

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pixelpet.R
import com.pixelpet.data.PetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

data class RoomDecorItem(
    val id: String,
    val path: String,
    var nx: Float,
    var ny: Float,
    var scale: Float = 1.0f,
    var alpha: Float = 1.0f
)

data class DecorEntry(
    val label: String,
    val resId: Int? = null,
    val path: String? = null
)

class RoomDecorManager(
    private val context: Context,
    private val repository: PetRepository,
    private val coroutineScope: CoroutineScope
) {
    val decorItems = mutableListOf<RoomDecorItem>()
    var decorBoundInstanceId: Long = -1L

    suspend fun loadDecorForInstance(instanceId: Long) {
        decorBoundInstanceId = instanceId
        val key = roomDecorKey(instanceId)
        val raw = repository.getSetting(key).firstOrNull()
        val items = parseDecorJson(raw)
        decorItems.clear()
        if (items.isEmpty()) {
            decorItems.addAll(defaultDecorTemplate())
            saveRoomDecorForInstance(instanceId)
        } else {
            decorItems.addAll(items)
        }
    }

    suspend fun saveRoomDecorForInstance(instanceId: Long) {
        val key = roomDecorKey(instanceId)
        repository.setSetting(key, decorToJson(decorItems))
    }

    fun renderRoomDecor(
        activity: AppCompatActivity,
        decorLayer: FrameLayout,
        currentInstanceId: Long
    ) {
        decorLayer.removeAllViews()
        val w = decorLayer.width
        val h = decorLayer.height
        if (w <= 0 || h <= 0) {
            decorLayer.post { renderRoomDecor(activity, decorLayer, currentInstanceId) }
            return
        }

        val density = context.resources.displayMetrics.density
        decorItems.forEach { item ->
            val resId = resolveDecorResId(item.path)
            if (resId == null && !File(item.path).exists()) return@forEach
            val basePx = (64 * density * item.scale).roundToInt().coerceIn(40, 140)
            val iv = ImageView(context).apply {
                if (resId != null) {
                    setImageResource(resId)
                } else {
                    setImageURI(Uri.fromFile(File(item.path)))
                }
                alpha = item.alpha.coerceIn(0.5f, 1f)
                layoutParams = FrameLayout.LayoutParams(basePx, basePx).apply {
                    gravity = Gravity.TOP or Gravity.START
                    leftMargin = (item.nx * (w - basePx)).roundToInt().coerceIn(0, max(0, w - basePx))
                    topMargin = (item.ny * (h - basePx)).roundToInt().coerceIn(0, max(0, h - basePx))
                }

                setOnTouchListener(object : View.OnTouchListener {
                    private var downX = 0f
                    private var downY = 0f
                    private var startL = 0
                    private var startT = 0
                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        val lp = v.layoutParams as FrameLayout.LayoutParams
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                downX = event.rawX
                                downY = event.rawY
                                startL = lp.leftMargin
                                startT = lp.topMargin
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                return true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val nx = (startL + (event.rawX - downX)).roundToInt()
                                val ny = (startT + (event.rawY - downY)).roundToInt()
                                val clampedL = nx.coerceIn(0, max(0, w - basePx))
                                val clampedT = ny.coerceIn(0, max(0, h - basePx))
                                v.translationX = (clampedL - startL).toFloat()
                                v.translationY = (clampedT - startT).toFloat()
                                return true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                val finalL = (startL + v.translationX).roundToInt().coerceIn(0, max(0, w - basePx))
                                val finalT = (startT + v.translationY).roundToInt().coerceIn(0, max(0, h - basePx))
                                v.translationX = 0f
                                v.translationY = 0f
                                lp.leftMargin = finalL
                                lp.topMargin = finalT
                                v.layoutParams = lp
                                v.setLayerType(View.LAYER_TYPE_NONE, null)
                                item.nx = if (w - basePx > 0) finalL.toFloat() / (w - basePx).toFloat() else 0.5f
                                item.ny = if (h - basePx > 0) finalT.toFloat() / (h - basePx).toFloat() else 0.5f
                                coroutineScope.launch(Dispatchers.IO) {
                                    saveRoomDecorForInstance(currentInstanceId)
                                }
                                return true
                            }
                        }
                        return false
                    }
                })

                setOnLongClickListener {
                    decorItems.removeAll { it.id == item.id }
                    coroutineScope.launch(Dispatchers.IO) {
                        saveRoomDecorForInstance(currentInstanceId)
                        withContext(Dispatchers.Main) {
                            renderRoomDecor(activity, decorLayer, currentInstanceId)
                        }
                    }
                    Toast.makeText(context, "已删除该装饰", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            decorLayer.addView(iv)
        }
    }

    fun parseDecorJson(raw: String?): List<RoomDecorItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val path = o.optString("path")
                    if (path.isBlank()) continue
                    add(
                        RoomDecorItem(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            path = path,
                            nx = o.optDouble("nx", 0.5).toFloat(),
                            ny = o.optDouble("ny", 0.5).toFloat(),
                            scale = o.optDouble("scale", 1.0).toFloat(),
                            alpha = o.optDouble("alpha", 1.0).toFloat()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun decorToJson(items: List<RoomDecorItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val o = JSONObject()
            o.put("id", item.id)
            o.put("path", item.path)
            o.put("nx", item.nx.toDouble())
            o.put("ny", item.ny.toDouble())
            o.put("scale", item.scale.toDouble())
            o.put("alpha", item.alpha.toDouble())
            arr.put(o)
        }
        return arr.toString()
    }

    fun copyDecorToInternal(uri: Uri): String? {
        val dir = File(context.filesDir, "room_decor/library")
        if (!dir.exists()) dir.mkdirs()
        val outFile = File(dir, "decor_${System.currentTimeMillis()}_${UUID.randomUUID()}.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        } ?: return null
        return outFile.absolutePath
    }

    fun resolveDecorResId(path: String): Int? {
        if (!path.startsWith("res://")) return null
        val name = path.removePrefix("res://").trim()
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        return if (resId != 0) resId else null
    }

    fun builtInDecorEntries(): List<DecorEntry> {
        return listOf(
            DecorEntry("气球", resId = R.drawable.fx_balloon_color),
            DecorEntry("礼物", resId = R.drawable.fx_gift_color),
            DecorEntry("蛋糕", resId = R.drawable.fx_cake_color),
            DecorEntry("星光", resId = R.drawable.fx_star_color),
            DecorEntry("彩带", resId = R.drawable.fx_confetti_color),
            DecorEntry("派对喷花", resId = R.drawable.fx_party_color),
            DecorEntry("爱心", resId = R.drawable.fx_heart_color),
            DecorEntry("音乐符号", resId = R.drawable.fx_music_notes_color),
            DecorEntry("火焰", resId = R.drawable.fx_fire_color),
            DecorEntry("睡觉 Zzz", resId = R.drawable.fx_sleep_color),
            DecorEntry("微笑", resId = R.drawable.fx_smile_color),
            DecorEntry("思考", resId = R.drawable.fx_think_color),
            DecorEntry("惊叹", resId = R.drawable.fx_exclaim_color),
            DecorEntry("波纹", resId = R.drawable.fx_wave_color),
            DecorEntry("阳光", resId = R.drawable.fx_sun_color),
            DecorEntry("食物", resId = R.drawable.fx_food_color),
            DecorEntry("像素砖 1", resId = R.drawable.decor_kenney_tile_0),
            DecorEntry("像素砖 2", resId = R.drawable.decor_kenney_tile_1),
            DecorEntry("像素砖 3", resId = R.drawable.decor_kenney_tile_2),
            DecorEntry("像素砖 4", resId = R.drawable.decor_kenney_tile_3),
            DecorEntry("像素砖 5", resId = R.drawable.decor_kenney_tile_4),
            DecorEntry("像素砖 6", resId = R.drawable.decor_kenney_tile_5),
            DecorEntry("像素砖 7", resId = R.drawable.decor_kenney_tile_6),
            DecorEntry("像素砖 8", resId = R.drawable.decor_kenney_tile_7),
            DecorEntry("像素砖 9", resId = R.drawable.decor_kenney_tile_8),
            DecorEntry("像素砖 10", resId = R.drawable.decor_kenney_tile_9),
            DecorEntry("像素砖 11", resId = R.drawable.decor_kenney_tile_10),
            DecorEntry("像素砖 12", resId = R.drawable.decor_kenney_tile_11)
        )
    }

    private fun defaultDecorTemplate(): List<RoomDecorItem> {
        return listOf(
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_star_color", 0.08f, 0.09f, 0.95f, 0.9f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_balloon_color", 0.82f, 0.10f, 1.10f, 0.95f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_music_note_color", 0.16f, 0.32f, 0.90f, 0.9f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_heart_color", 0.74f, 0.34f, 0.88f, 0.9f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_spark_color", 0.52f, 0.14f, 0.82f, 0.9f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_gift_color", 0.10f, 0.70f, 1.02f, 0.95f),
            RoomDecorItem(UUID.randomUUID().toString(), "res://fx_cake_color", 0.80f, 0.72f, 1.02f, 0.95f)
        )
    }

    suspend fun loadDecorLibrary(): List<String> {
        val raw = repository.getSetting("decor_library").firstOrNull()
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val originalCount = arr.length()
            val cleaned = buildList {
                for (i in 0 until originalCount) {
                    val path = arr.optString(i)
                    if (path.isNotBlank() && File(path).exists()) add(path)
                }
            }
            if (cleaned.isNotEmpty() && cleaned.size < originalCount) {
                val out = JSONArray()
                cleaned.forEach { out.put(it) }
                repository.setSetting("decor_library", out.toString())
            }
            cleaned
        }.getOrDefault(emptyList())
    }

    suspend fun addToDecorLibrary(path: String) {
        if (!File(path).exists()) return
        val existing = loadDecorLibrary().toMutableList()
        if (!existing.contains(path)) {
            existing.add(0, path)
            val arr = JSONArray()
            existing.take(80).forEach { arr.put(it) }
            repository.setSetting("decor_library", arr.toString())
        }
    }

    private fun roomDecorKey(instanceId: Long): String = "room_decor_$instanceId"
}
