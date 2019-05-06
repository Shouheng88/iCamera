package me.shouheng.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import me.shouheng.camerax.config.ConfigurationProvider
import me.shouheng.camerax.config.creator.impl.CameraManagerCreatorImpl
import me.shouheng.camerax.config.creator.impl.CameraPreviewCreatorImpl
import me.shouheng.sample.base.CommonActivity
import me.shouheng.sample.creator.Camera1OnlyCreator
import me.shouheng.sample.creator.Camera2OnlyCreator
import me.shouheng.sample.creator.SurfaceOnlyCreator
import me.shouheng.sample.creator.TextureOnlyCreator
import me.shouheng.sample.databinding.ActivityMainBinding
import me.shouheng.sample.utils.ThemeUtils
import me.shouheng.utils.activity.ActivityHelper
import me.shouheng.utils.permission.Permission
import me.shouheng.utils.permission.PermissionUtils

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

        binding.rbSurface.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ConfigurationProvider.get().cameraPreviewCreator = SurfaceOnlyCreator()
            }
        }
        binding.rbTexture.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ConfigurationProvider.get().cameraPreviewCreator = TextureOnlyCreator()
            }
        }
        binding.rbPlatform.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ConfigurationProvider.get().cameraPreviewCreator = CameraPreviewCreatorImpl()
            }
        }
    }

    fun openCamera(v: View) {
        PermissionUtils.checkPermissions(this,
            intArrayOf(Permission.CAMERA, Permission.STORAGE, Permission.MICROPHONE)
        ) {
            ActivityHelper.start(this, CameraActivity::class.java)
        }
    }
}