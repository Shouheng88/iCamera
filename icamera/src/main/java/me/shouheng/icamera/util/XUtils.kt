package me.shouheng.icamera.util

import android.content.Context

/**
 * Utils class for current library.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 9:34
 */
object XUtils {

    fun dp2Px(context: Context, dpValues: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValues * scale + 0.5f).toInt()
    }

    fun sp2Px(context: Context, spValues: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValues * fontScale + 0.5f).toInt()
    }
}