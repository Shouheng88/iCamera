package me.shouheng.camerax.listener;

import me.shouheng.camerax.enums.Camera;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/17 22:47
 */
public interface CameraCloseListener {

    void onCameraClosed(@Camera.Face int cameraFace);
}
