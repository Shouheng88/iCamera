package me.shouheng.camerax.listener;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 10:40
 */
public interface CameraOpenListener {

    /**
     * This method will be called when the camera is opened.
     */
    void onCameraOpened();

    /**
     * The method will be called when failed to open camera.
     *
     * @param throwable the throwable
     */
    void onCameraOpenError(Throwable throwable);
}
