package me.shouheng.sample.creator

import android.content.Context
import me.shouheng.shining.config.creator.CameraManagerCreator
import me.shouheng.shining.manager.CameraManager
import me.shouheng.shining.manager.impl.Camera1Manager
import me.shouheng.shining.preview.CameraPreview

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/20 11:57
 */
class Camera1OnlyCreator : CameraManagerCreator {

    override fun create(context: Context?, cameraPreview: CameraPreview?): CameraManager = Camera1Manager(cameraPreview)

}