package me.shouheng.camerax.manager;

import android.content.Context;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.listener.CameraOpenListener;
import me.shouheng.camerax.listener.CameraPhotoListener;
import me.shouheng.camerax.listener.CameraVideoListener;

import java.io.File;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:49
 */
public interface CameraManager {

    void initialize(Context context);

    void openCamera(CameraOpenListener cameraOpenListener);

    boolean isCameraOpened();

    void setMediaType(@Media.Type int mediaType);

    void setVoiceEnable(boolean voiceEnable);

    boolean isVoiceEnable();

    void setAutoFocus(boolean autoFocus);

    boolean isAutoFocus();

    void takePicture(CameraPhotoListener cameraPhotoListener);

    void startVideoRecord(File file, CameraVideoListener cameraVideoListener);

    void stopVideoRecord();

    void resumePreview();

    void closeCamera();

    void releaseCamera();
}
