package me.shouheng.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import me.shouheng.camerax.config.ConfigurationProvider
import me.shouheng.camerax.config.creator.impl.CameraManagerCreatorImpl
import me.shouheng.sample.base.CommonActivity
import me.shouheng.sample.creator.Camera1OnlyCreator
import me.shouheng.sample.creator.Camera2OnlyCreator
import me.shouheng.sample.databinding.ActivityMainBinding
import me.shouheng.sample.utils.ThemeUtils

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:42
 */
class MainActivity : CommonActivity<ActivityMainBinding>() {

    override fun getLayoutResId() = R.layout.activity_main

    override fun doCreateView(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "doCreateView")
        ThemeUtils.setStatusBarLightMode(window, false)
        ConfigurationProvider.get().isDebug = true
        setSupportActionBar(binding.toolbar)

        binding.rbCamera.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ConfigurationProvider.get().cameraManagerCreator = CameraManagerCreatorImpl()
            }
        }
        binding.rbCamera1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ConfigurationProvider.get().cameraManagerCreator = Camera1OnlyCreator()
            }
        }
        binding.rbCamera2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ConfigurationProvider.get().cameraManagerCreator = Camera2OnlyCreator()
            }
        }
    }

    fun openCamera(v: View) {
        startActivity(Intent(this, CameraActivity::class.java))
    }
}