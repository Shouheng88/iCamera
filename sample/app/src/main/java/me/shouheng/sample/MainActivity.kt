package me.shouheng.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import me.shouheng.camerax.config.ConfigurationProvider
import me.shouheng.sample.base.CommonActivity
import me.shouheng.sample.databinding.ActivityMainBinding

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:42
 */
class MainActivity : CommonActivity<ActivityMainBinding>() {

    override fun getLayoutResId() = R.layout.activity_main

    override fun doCreateView(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "doCreateView")
        ConfigurationProvider.get().isDebug = true
        setSupportActionBar(binding.toolbar)
    }

    fun openCamera(v: View) {
        startActivity(Intent(this, CameraActivity::class.java))
    }
}