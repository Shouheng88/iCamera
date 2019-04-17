package me.shouheng.camerax.manager.impl;

import android.content.Context;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.config.sizes.SizeMap;
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
    public void switchCamera(int cameraFace) {
        super.switchCamera(cameraFace);
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
    public void setAutoFocus(boolean autoFocus) {

    }

    @Override
    public boolean isAutoFocus() {
        return false;
    }

    @Override
    public void setFlashMode(int flashMode) {

    }

    @Override
    public int getFlashMode() {
        return 0;
    }

    @Override
    public void setZoom(float zoom) {

    }

    @Override
    public float getZoom() {
        return 0;
    }

    @Override
    public float getMaxZoom() {
        return 0;
    }

    @Override
    public Size getSize(int sizeFor) {
        return null;
    }

    @Override
    public SizeMap getSizes(int sizeFor) {
        return null;
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {

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
