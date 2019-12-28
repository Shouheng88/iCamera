package me.shouheng.sample.creator

import android.content.Context
import me.shouheng.xcamera.config.creator.CameraManagerCreator
import me.shouheng.xcamera.manager.CameraManager
import me.shouheng.xcamera.manager.impl.Camera1Manager
import me.shouheng.xcamera.preview.CameraPreview

/**
 * The camera manager creator that only create for camera1
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/20 11:57
 */
class Camera1OnlyCreator : CameraManagerCreator {

    override fun create(context: Context?, cameraPreview: CameraPreview?)
            : CameraManager = Camera1Manager(cameraPreview)

}