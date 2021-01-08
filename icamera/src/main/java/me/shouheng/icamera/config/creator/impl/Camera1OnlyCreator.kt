package me.shouheng.icamera.config.creator.impl

import android.content.Context
import me.shouheng.icamera.config.creator.CameraManagerCreator
import me.shouheng.icamera.manager.CameraManager
import me.shouheng.icamera.manager.impl.Camera1Manager
import me.shouheng.icamera.preview.CameraPreview

/**
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2020-08-31 10:47
 */
class Camera1OnlyCreator : CameraManagerCreator {
    override fun create(
        context: Context,
        cameraPreview: CameraPreview
    ): CameraManager {
        return Camera1Manager(cameraPreview)
    }
}