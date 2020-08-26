package me.shouheng.icamerasample.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import me.shouheng.icamera.config.ConfigurationProvider
import me.shouheng.icamera.config.creator.impl.CameraManagerCreatorImpl
import me.shouheng.icamera.config.creator.impl.CameraPreviewCreatorImpl
import me.shouheng.icamerasample.R
import me.shouheng.icamerasample.creator.Camera1OnlyCreator
import me.shouheng.icamerasample.creator.Camera2OnlyCreator
import me.shouheng.icamerasample.creator.SurfaceOnlyCreator
import me.shouheng.icamerasample.creator.TextureOnlyCreator
import me.shouheng.icamerasample.databinding.ActivityMainBinding
import me.shouheng.utils.permission.Permission
import me.shouheng.utils.permission.PermissionUtils
import me.shouheng.utils.permission.callback.OnGetPermissionCallback
import me.shouheng.utils.ui.BarUtils
import me.shouheng.vmlib.base.CommonActivity
import me.shouheng.vmlib.comn.EmptyViewModel

/**
 * Main activity
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:42
 */
class MainActivity : CommonActivity<EmptyViewModel, ActivityMainBinding>() {

    override fun getLayoutResId(): Int = R.layout.activity_main

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
            startActivity(CameraActivity::class.java)
        }, Permission.CAMERA, Permission.STORAGE, Permission.MICROPHONE)
    }
}