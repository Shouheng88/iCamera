package me.shouheng.icamera.config.creator.impl

import android.content.Context
import android.os.Build
import me.shouheng.icamera.config.creator.CameraManagerCreator
import me.shouheng.icamera.manager.CameraManager
import me.shouheng.icamera.manager.impl.Camera1Manager
import me.shouheng.icamera.manager.impl.Camera2Manager
import me.shouheng.icamera.preview.CameraPreview
import me.shouheng.icamera.util.CameraHelper.hasCamera2

/**
 * Default camera manager creator implementation.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:56
 */
class CameraManagerCreatorImpl : CameraManagerCreator {
    /**
     * The default implementation for [CameraManager].
     * If the app version >= 21, the [android.hardware.camera2.CameraDevice] will be used,
     * else the [android.hardware.Camera] will be used.
     *
     * @param context context
     * @param cameraPreview the [CameraPreview]
     * @return CameraManager object.
     */
    override fun create(
        context: Context,
        cameraPreview: CameraPreview
    ): CameraManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hasCamera2(context)) {
            Camera2Manager(cameraPreview)
        } else Camera1Manager(cameraPreview)
    }
}