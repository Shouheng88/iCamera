package me.shouheng.icamerasample.creator

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import me.shouheng.icamera.config.creator.CameraManagerCreator
import me.shouheng.icamera.manager.CameraManager
import me.shouheng.icamera.manager.impl.Camera2Manager
import me.shouheng.icamera.preview.CameraPreview

/**
 * The camera manager creator that only create for camera2
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/20 11:57
 */
class Camera2OnlyCreator : CameraManagerCreator {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun create(context: Context?, cameraPreview: CameraPreview?)
            : CameraManager = Camera2Manager(cameraPreview)

}