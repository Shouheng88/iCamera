package me.shouheng.icamera.util

import android.util.Log

/**
 * A simple logger to use in library.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 9:33
 */
object XLog {

    private var isDebug = false

    fun v(tag: String, msg: String?) {
        if (isDebug) Log.v("XLog-$tag", msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String?) {
        if (isDebug) Log.d("XLog-$tag", msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String?) {
        if (isDebug) Log.i("XLog-$tag", msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String?) {
        if (isDebug) Log.w("XLog-$tag", msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String?) {
        if (isDebug) Log.e("XLog-$tag", msg)
    }

    @JvmStatic
    fun setDebug(debug: Boolean) {
        isDebug = debug
    }
}