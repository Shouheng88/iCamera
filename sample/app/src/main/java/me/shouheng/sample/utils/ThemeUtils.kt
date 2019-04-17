package me.shouheng.sample.utils

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.os.Build.VERSION
import android.support.annotation.ColorInt
import android.view.View
import android.view.Window
import android.view.WindowManager

/**
 * @author shouh
 * @version $Id: ThemeUtils, v 0.1 2018/6/7 22:10 shouh Exp$
 */
object ThemeUtils {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setStatusBarColor(activity: Activity, @ColorInt color: Int) {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.statusBarColor = color
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun setStatusBarLightMode(window: Window, isLightMode: Boolean) {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = window.decorView
            var vis = decorView.systemUiVisibility
            vis = if (isLightMode) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                vis or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                vis and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            decorView.systemUiVisibility = vis
        }
    }
}
