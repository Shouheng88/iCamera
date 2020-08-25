package me.shouheng.icamerasample.creator

import android.content.Context
import me.shouheng.icamera.config.creator.CameraManagerCreator
import me.shouheng.icamera.manager.CameraManager
import me.shouheng.icamera.manager.impl.Camera1Manager
import me.shouheng.icamera.preview.CameraPreview

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