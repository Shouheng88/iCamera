package me.shouheng.icamera.config.creator.impl

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import me.shouheng.icamera.config.creator.CameraManagerCreator
import me.shouheng.icamera.manager.CameraManager
import me.shouheng.icamera.manager.impl.Camera2Manager
import me.shouheng.icamera.preview.CameraPreview

/**
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2020-08-31 10:48
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class Camera2OnlyCreator : CameraManagerCreator {
    override fun create(
        context: Context,
        cameraPreview: CameraPreview
    ): CameraManager {
        return Camera2Manager(cameraPreview)
    }
}