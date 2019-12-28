package me.shouheng.sample.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import me.shouheng.mvvm.base.CommonActivity
import me.shouheng.mvvm.base.anno.ActivityConfiguration
import me.shouheng.mvvm.comn.EmptyViewModel
import me.shouheng.sample.R
import me.shouheng.sample.creator.Camera1OnlyCreator
import me.shouheng.sample.creator.Camera2OnlyCreator
import me.shouheng.sample.creator.SurfaceOnlyCreator
import me.shouheng.sample.creator.TextureOnlyCreator
import me.shouheng.sample.databinding.ActivityMainBinding
import me.shouheng.utils.app.ActivityUtils
import me.shouheng.utils.permission.Permission
import me.shouheng.utils.permission.PermissionUtils
import me.shouheng.utils.permission.callback.OnGetPermissionCallback
import me.shouheng.utils.ui.BarUtils
import me.shouheng.xcamera.config.ConfigurationProvider
import me.shouheng.xcamera.config.creator.impl.CameraManagerCreatorImpl
import me.shouheng.xcamera.config.creator.impl.CameraPreviewCreatorImpl

/**
 * Main activity
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:42
 */
@ActivityConfiguration(layoutResId = R.layout.activity_main)
class MainActivity : CommonActivity<ActivityMainBinding, EmptyViewModel>() {

    override fun doCreateView(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "doCreateView")
        BarUtils.setStatusBarLightMode(window, false)
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
        PermissionUtils.checkPermissions(this, OnGetPermissionCallback {
            ActivityUtils.start(this, CameraActivity::class.java)
        }, Permission.CAMERA, Permission.STORAGE, Permission.MICROPHONE)
    }
}