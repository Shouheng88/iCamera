package me.shouheng.icamera.listener;

import me.shouheng.icamera.enums.CameraFace;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 10:40
 */
public interface CameraOpenListener {

    void onCameraOpened(@CameraFace int cameraFace);

    void onCameraOpenError(Throwable throwable);
}
