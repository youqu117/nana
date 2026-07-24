package com.pixelpet.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import com.pixelpet.content.AssetLoader
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * 统一的 Bitmap 解码工具，消除原先散落在 PetView / Adapters / SettingsFragment 的
 * "bounds -> inSampleSize -> decode -> createScaledBitmap" 重复样板代码。
 */
object BitmapUtils {

    /** 解码 assets/filesDir 中的图片，按需求尺寸采样并精确缩放。 */
    fun decodeSampledFromPack(
        context: Context,
        path: String,
        targetW: Int? = null,
        targetH: Int? = null,
        maxDimPx: Int = MAX_BITMAP_DIM_PX
    ): Bitmap? {
        return decodeSampled(maxDimPx) { AssetLoader.openStream(context, path) }
    }

    /** 解码任意 InputStream（如 contentResolver 打开的图片）。 */
    fun decodeSampledFromStream(
        streamFactory: () -> InputStream,
        targetW: Int? = null,
        targetH: Int? = null,
        maxDimPx: Int = MAX_BITMAP_DIM_PX
    ): Bitmap? {
        return decodeSampled(maxDimPx, streamFactory)
    }

    private inline fun decodeSampled(
        maxDimPx: Int,
        streamFactory: () -> InputStream
    ): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            streamFactory().use { BitmapFactory.decodeStream(it, null, boundsOptions) }

            val sourceW = boundsOptions.outWidth
            val sourceH = boundsOptions.outHeight
            if (sourceW <= 0 || sourceH <= 0) return null

            val desiredW = sourceW.coerceAtMost(maxDimPx)
            val desiredH = sourceH.coerceAtMost(maxDimPx)
            val sampleSize = calculateInSampleSize(sourceW, sourceH, desiredW, desiredH)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = streamFactory().use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            if (decoded.width <= desiredW && decoded.height <= desiredH) {
                decoded
            } else {
                scaleInto(decoded, desiredW, desiredH)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 把 bitmap 等比缩放到不超过目标尺寸，原 bitmap 若被替换则回收。 */
    private fun scaleInto(decoded: Bitmap, desiredW: Int, desiredH: Int): Bitmap {
        val ratio = min(desiredW.toFloat() / decoded.width, desiredH.toFloat() / decoded.height)
        val scaledW = max(1, (decoded.width * ratio).toInt())
        val scaledH = max(1, (decoded.height * ratio).toInt())
        val scaled = Bitmap.createScaledBitmap(decoded, scaledW, scaledH, false)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    /** 把 ImageView 的 drawable 设为非过滤（保持像素硬边），用于像素风预览。 */
    fun applyPixelScaling(imageView: ImageView) {
        (imageView.drawable as? BitmapDrawable)?.isFilterBitmap = false
    }

    private fun calculateInSampleSize(sourceW: Int, sourceH: Int, reqW: Int, reqH: Int): Int {
        var sampleSize = 1
        val halfW = sourceW / 2
        val halfH = sourceH / 2
        while (halfW / sampleSize >= reqW && halfH / sampleSize >= reqH) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    /** PetView / 小屋使用的最大解码尺寸。 */
    const val MAX_BITMAP_DIM_PX = 512

    /** 列表预览使用的最大解码尺寸（较小以节省内存）。 */
    const val PREVIEW_MAX_DIM_PX = 256
}
