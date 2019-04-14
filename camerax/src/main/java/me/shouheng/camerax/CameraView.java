package me.shouheng.camerax;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import me.shouheng.camerax.config.ConfigurationProvider;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.listener.CameraOpenListener;
import me.shouheng.camerax.listener.CameraPhotoListener;
import me.shouheng.camerax.listener.CameraVideoListener;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.util.Logger;

import java.io.File;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:43
 */
public class CameraView extends FrameLayout {

    private static final String TAG = "CameraView";

    private CameraManager cameraManager;

    private CameraPreview cameraPreview;

    public CameraView(@NonNull Context context) {
        this(context, null);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCameraView(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initCameraView(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initCameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        cameraPreview = ConfigurationProvider.get().getCameraPreviewCreator().create(getContext(), this);
        cameraManager = ConfigurationProvider.get().getCameraManagerCreator().create(cameraPreview);
        cameraManager.initialize(context);
    }

    public void openCamera() {
        cameraManager.openCamera(new CameraOpenListener() {
            @Override
            public void onCameraOpened() {

            }

            @Override
            public void onCameraOpenError(Throwable throwable) {
                Logger.d(TAG, "error : " + throwable);
            }
        });
    }

    public void setMediaType(@Media.Type int mediaType) {
        cameraManager.setMediaType(mediaType);
    }

    /**
     * Whether use shutter when capture. The final result is not only affected by
     * this value, but also subject to your phone circumstance. If your phone was
     * in SILENT mode, there will be no voice even you set the voiceEnable true.
     *
     * @param voiceEnable true to use the voice
     */
    public void setVoiceEnable(boolean voiceEnable) {
        cameraManager.setVoiceEnable(voiceEnable);
    }

    public boolean isVoiceEnable() {
        return cameraManager.isVoiceEnable();
    }

    public void takePicture(CameraPhotoListener cameraPhotoListener) {
        cameraManager.takePicture(cameraPhotoListener);
    }

    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {
        cameraManager.startVideoRecord(file, cameraVideoListener);
    }

    public void stopVideoRecord() {
        cameraManager.stopVideoRecord();
    }

    public void resumePreview() {
        cameraManager.resumePreview();
    }

    public void closeCamera() {

    }

    public void releaseCamera() {

    }
}
