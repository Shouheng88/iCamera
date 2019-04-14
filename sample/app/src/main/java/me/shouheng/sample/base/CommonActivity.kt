package me.shouheng.sample.base

import android.annotation.TargetApi
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import me.shouheng.sample.R

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:10
 */
abstract class CommonActivity<T : ViewDataBinding> : AppCompatActivity() {

    lateinit var binding : T

    abstract fun getLayoutResId() : Int

    abstract fun doCreateView(savedInstanceState: Bundle?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.inflate(LayoutInflater.from(this),
            getLayoutResId(), null, false)
        setContentView(binding.root)
        doCreateView(savedInstanceState)
    }

    private fun customStatusBar() {
        // Above 6.0
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                setStatusBarColor(Color.TRANSPARENT)
                val decorView = window.decorView
                val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                decorView.systemUiVisibility = option
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                // 5.0 and above
                val window = window
                val decorView = window.decorView
                val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                decorView.systemUiVisibility = option
                window.statusBarColor = Color.TRANSPARENT
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                // 4.4 to 5.0
                val localLayoutParams = window.attributes
                localLayoutParams.flags = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or localLayoutParams.flags
                theme.applyStyle(R.style.AppTheme444, true)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected fun setStatusBarColor(@ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = color
        }
    }
}