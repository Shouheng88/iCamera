package me.shouheng.camerax;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import me.shouheng.camerax.config.ConfigurationProvider;
import me.shouheng.camerax.enums.Flash;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.listener.CameraOpenListener;
import me.shouheng.camerax.listener.CameraPhotoListener;
import me.shouheng.camerax.listener.CameraVideoListener;
import me.shouheng.camerax.listener.OnMoveListener;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.util.Logger;
import me.shouheng.camerax.widget.FocusMarkerLayout;

import java.io.File;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:43
 */
public class CameraView extends FrameLayout {

    private static final String TAG = "CameraView";

    private CameraManager cameraManager;

    private FocusMarkerLayout focusMarkerLayout;

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
        CameraPreview cameraPreview = ConfigurationProvider.get().getCameraPreviewCreator().create(getContext(), this);
        cameraManager = ConfigurationProvider.get().getCameraManagerCreator().create(cameraPreview);
        cameraManager.initialize(context);

        focusMarkerLayout = new FocusMarkerLayout(context);
        focusMarkerLayout.setCameraView(this);
        focusMarkerLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.addView(focusMarkerLayout);
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

    public void setAutoFocus(boolean autoFocus) {
        cameraManager.setAutoFocus(autoFocus);
    }

    public boolean isAutoFocus() {
        return cameraManager.isAutoFocus();
    }

    public void setFlashMode(@Flash.FlashMode int flashMode) {
        cameraManager.setFlashMode(flashMode);
    }

    @Flash.FlashMode
    public int getFlashMode() {
        return cameraManager.getFlashMode();
    }

    public void setZoom(float zoom) {
        cameraManager.setZoom(zoom);
    }

    public float getZoom() {
        return cameraManager.getZoom();
    }

    public float getMaxZoom() {
        return cameraManager.getMaxZoom();
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

    public void setOnMoveListener(OnMoveListener onMoveListener) {
        focusMarkerLayout.setOnMoveListener(onMoveListener);
    }

    public void setTouchAngle(int touchAngle) {
        if (focusMarkerLayout != null) {
            focusMarkerLayout.setTouchAngle(touchAngle);
        }
    }

    public void setScaleRate(int scaleRate) {
        focusMarkerLayout.setScaleRate(scaleRate);
    }

    public void setTouchZoomEnable(boolean touchZoomEnable) {
        focusMarkerLayout.setTouchZoomEnable(touchZoomEnable);
    }

    public void setUseTouchFocus(boolean useTouchFocus) {
        focusMarkerLayout.setUseTouchFocus(useTouchFocus);
    }

}
