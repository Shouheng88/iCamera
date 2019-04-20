package me.shouheng.sample.creator

import android.content.Context
import me.shouheng.camerax.config.creator.CameraManagerCreator
import me.shouheng.camerax.manager.CameraManager
import me.shouheng.camerax.manager.impl.Camera1Manager
import me.shouheng.camerax.preview.CameraPreview

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/20 11:57
 */
class Camera1OnlyCreator : CameraManagerCreator {

    override fun create(context: Context?, cameraPreview: CameraPreview?): CameraManager = Camera1Manager(cameraPreview)

}