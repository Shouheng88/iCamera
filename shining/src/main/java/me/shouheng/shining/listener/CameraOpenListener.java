package me.shouheng.shining.listener;

import me.shouheng.shining.enums.Camera;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 10:40
 */
public interface CameraOpenListener {

    void onCameraOpened(@Camera.Face int cameraFace);

    void onCameraOpenError(Throwable throwable);
}
