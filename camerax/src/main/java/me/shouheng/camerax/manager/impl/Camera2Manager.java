package me.shouheng.camerax.manager.impl;

import android.content.Context;
import me.shouheng.camerax.listener.CameraOpenListener;
import me.shouheng.camerax.listener.CameraPhotoListener;
import me.shouheng.camerax.listener.CameraVideoListener;
import me.shouheng.camerax.preview.CameraPreview;

import java.io.File;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:52
 */
public class Camera2Manager extends BaseCameraManager {

    public Camera2Manager(CameraPreview cameraPreview) {
        super(cameraPreview);
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public void openCamera(CameraOpenListener cameraOpenListener) {

    }

    @Override
    public boolean isCameraOpened() {
        return false;
    }

    @Override
    public void setMediaType(int mediaType) {

    }

    @Override
    public void setVoiceEnable(boolean voiceEnable) {

    }

    @Override
    public boolean isVoiceEnable() {
        return false;
    }

    @Override
    public void takePicture(CameraPhotoListener cameraPhotoListener) {

    }

    @Override
    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {

    }

    @Override
    public void stopVideoRecord() {

    }

    @Override
    public void resumePreview() {

    }

    @Override
    public void closeCamera() {

    }

}
