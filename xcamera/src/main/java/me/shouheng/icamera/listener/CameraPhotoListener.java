package me.shouheng.icamera.listener;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 14:30
 */
public interface CameraPhotoListener {

    void onPictureTaken(byte[] data);

    void onCaptureFailed(Throwable throwable);
}
