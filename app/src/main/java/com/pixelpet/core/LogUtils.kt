package com.pixelpet.core

import android.util.Log

/**
 * 统一日志入口。替代散落各处的 `e.printStackTrace()`，保证：
 * - 有统一 TAG，便于过滤
 * - 有级别区分
 * - Release 构建可通过 BuildConfig 关闭调试日志
 */
object LogUtils {
    private const val ROOT_TAG = "NanaPet"

    fun d(tag: String, msg: String, t: Throwable? = null) {
        Log.d("$ROOT_TAG/$tag", msg, t)
    }

    fun i(tag: String, msg: String, t: Throwable? = null) {
        Log.i("$ROOT_TAG/$tag", msg, t)
    }

    fun w(tag: String, msg: String, t: Throwable? = null) {
        Log.w("$ROOT_TAG/$tag", msg, t)
    }

    fun e(tag: String, msg: String, t: Throwable? = null) {
        Log.e("$ROOT_TAG/$tag", msg, t)
    }
}
