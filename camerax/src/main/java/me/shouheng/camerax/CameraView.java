package me.shouheng.camerax;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import me.shouheng.camerax.config.ConfigurationProvider;
import me.shouheng.camerax.config.sizes.AspectRatio;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.config.sizes.SizeMap;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.enums.Flash;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.enums.Preview;
import me.shouheng.camerax.listener.*;
import me.shouheng.camerax.manager.CameraManager;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.widget.FocusMarkerLayout;

import java.io.File;

import static me.shouheng.camerax.enums.Preview.*;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:43
 */
public class CameraView extends FrameLayout {

    private static final String TAG = "CameraView";

    private CameraManager cameraManager;

    private boolean clipScreen;
    @Preview.AdjustType
    private int adjustType;
    private AspectRatio aspectRatio;
    private FocusMarkerLayout focusMarkerLayout;
    private DisplayOrientationDetector displayOrientationDetector;

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
        final CameraPreview cameraPreview = ConfigurationProvider.get().getCameraPreviewCreator().create(getContext(), this);
        cameraManager = ConfigurationProvider.get().getCameraManagerCreator().create(context, cameraPreview);
        cameraManager.initialize(context);
        cameraManager.addCameraSizeListener(new CameraSizeListener() {
            @Override
            public void onPreviewSizeUpdated(Size previewSize) {
                aspectRatio = cameraManager.getAspectRatio();
                if (displayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
                    aspectRatio = aspectRatio.inverse();
                }
                requestLayout();
            }

            @Override
            public void onVideoSizeUpdated(Size videoSize) {
            }

            @Override
            public void onPictureSizeUpdated(Size pictureSize) {
            }
        });

        // prepare parameters
        clipScreen = true;
        aspectRatio = ConfigurationProvider.get().getDefaultAspectRatio();
        adjustType = SCALE_SMALLER;

        displayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                cameraManager.setDisplayOrientation(displayOrientation);
            }
        };

        focusMarkerLayout = new FocusMarkerLayout(context);
        focusMarkerLayout.setCameraView(this);
        focusMarkerLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.addView(focusMarkerLayout);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            displayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            displayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (clipScreen) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            switch (adjustType) {
                case FIXED_WIDTH:
                    height = width * aspectRatio.heightRatio / aspectRatio.widthRatio;
                    break;
                case FIXED_HEIGHT:
                    width = height * aspectRatio.widthRatio / aspectRatio.heightRatio;
                    break;
                case SCALE_SMALLER:
                    if (width * aspectRatio.heightRatio < height * aspectRatio.widthRatio) {
                        height = width * aspectRatio.heightRatio /aspectRatio. widthRatio;
                    } else {
                        width = height * aspectRatio.widthRatio / aspectRatio.heightRatio;
                    }
                    break;
                case SCALE_LARGER:
                    if (width * aspectRatio.heightRatio < height * aspectRatio.widthRatio) {
                        width = height * aspectRatio.widthRatio / aspectRatio.heightRatio;
                    } else {
                        height = width * aspectRatio.heightRatio / aspectRatio.widthRatio;
                    }
                    break;
                case NONE:
                default:
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            return;
        }
    }

    public void openCamera(CameraOpenListener cameraOpenListener) {
        cameraManager.openCamera(cameraOpenListener);
    }

    public boolean isCameraOpened() {
        return cameraManager.isCameraOpened();
    }

    @Camera.Face
    public int getCameraFace() {
        return cameraManager.getCameraFace();
    }

    public void switchCamera(@Camera.Face int cameraFace) {
        cameraManager.switchCamera(cameraFace);
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

    public void setExpectSize(Size expectSize) {
        cameraManager.setExpectSize(expectSize);
    }

    public void setExpectAspectRatio(AspectRatio aspectRatio) {
        cameraManager.setExpectAspectRatio(aspectRatio);
    }

    public Size getSize(@Camera.SizeFor int sizeFor) {
        return cameraManager.getSize(sizeFor);
    }

    public SizeMap getSizes(@Camera.SizeFor int sizeFor) {
        return cameraManager.getSizes(sizeFor);
    }

    public AspectRatio getAspectRatio() {
        return cameraManager.getAspectRatio();
    }

    public void addCameraSizeListener(CameraSizeListener cameraSizeListener) {
        cameraManager.addCameraSizeListener(cameraSizeListener);
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
        cameraManager.closeCamera();
    }

    public void releaseCamera() {
        cameraManager.releaseCamera();
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
