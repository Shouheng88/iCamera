package me.shouheng.camerax;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
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
    private CameraPreview cameraPreview;

    private boolean clipScreen;
    private boolean adjustViewBounds;
    @Preview.AdjustType
    private int adjustType = NONE;
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
        cameraPreview = ConfigurationProvider.get().getCameraPreviewCreator().create(getContext(), this);
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

        focusMarkerLayout = new FocusMarkerLayout(context);
        focusMarkerLayout.setCameraView(this);
        focusMarkerLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.addView(focusMarkerLayout);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr,
                R.style.Widget_CameraView);
        adjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false);
        cameraManager.switchCamera(a.getInt(R.styleable.CameraView_cameraFace, Camera.FACE_REAR));
        cameraManager.setMediaType(a.getInt(R.styleable.CameraView_mediaType, Media.TYPE_PICTURE));
        cameraManager.setVoiceEnable(a.getBoolean(R.styleable.CameraView_voiceEnable, true));
        String strAspectRatio = a.getString(R.styleable.CameraView_aspectRatio);
        aspectRatio = TextUtils.isEmpty(strAspectRatio) ?
                ConfigurationProvider.get().getDefaultAspectRatio() : AspectRatio.parse(strAspectRatio);
        cameraManager.setExpectAspectRatio(aspectRatio);
        cameraManager.setAutoFocus(a.getBoolean(R.styleable.CameraView_autoFocus, true));
        cameraManager.setFlashMode(a.getInt(R.styleable.CameraView_flash, Flash.FLASH_AUTO));
        String zoomString = a.getString(R.styleable.CameraView_zoom);
        if (!TextUtils.isEmpty(zoomString)) {
            try {
                setZoom(Float.valueOf(zoomString));
            } catch (NumberFormatException e) {
                setZoom(1.0f);
            }
        } else {
            setZoom(1.0f);
        }
        clipScreen = a.getBoolean(R.styleable.CameraView_clipScreen, false);
        adjustType = a.getInt(R.styleable.CameraView_cameraAdjustType, adjustType);
        focusMarkerLayout.setScaleRate(a.getInt(R.styleable.CameraView_scaleRate, FocusMarkerLayout.DEFAULT_SCALE_RATE));
        focusMarkerLayout.setTouchZoomEnable(a.getBoolean(R.styleable.CameraView_touchRoom, true));
        focusMarkerLayout.setUseTouchFocus(a.getBoolean(R.styleable.CameraView_touchFocus, true));
        a.recycle();

        displayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                cameraManager.setDisplayOrientation(displayOrientation);
            }
        };
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
                case WIDTH_FIRST:
                    height = width * aspectRatio.heightRatio / aspectRatio.widthRatio;
                    break;
                case HEIGHT_FIRST:
                    width = height * aspectRatio.widthRatio / aspectRatio.heightRatio;
                    break;
                case SMALLER_FIRST:
                    if (width * aspectRatio.heightRatio < height * aspectRatio.widthRatio) {
                        height = width * aspectRatio.heightRatio / aspectRatio.widthRatio;
                    } else {
                        width = height * aspectRatio.widthRatio / aspectRatio.heightRatio;
                    }
                    break;
                case LARGER_FIRST:
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

        if (adjustViewBounds) {
            if (!isCameraOpened()) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                final AspectRatio ratio = aspectRatio;
                int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.ratio());
                if (heightMode == MeasureSpec.AT_MOST) {
                    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                }
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * aspectRatio.ratio());
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        // Always smaller first! But use the effect to the CameraPreview instead of the CameraView.
        if (height < width * aspectRatio.heightRatio / aspectRatio.widthRatio) {
            cameraPreview.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * aspectRatio.heightRatio / aspectRatio.widthRatio,
                            MeasureSpec.EXACTLY));
        } else {
            cameraPreview.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * aspectRatio.widthRatio / aspectRatio.heightRatio,
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
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

    /**
     * Sets the maximum file size (in bytes) of the recording video.
     *
     * @param videoFileSize the maximum file size in bytes (if zero or negative, disables the limit)
     */
    public void setVideoFileSize(long videoFileSize) {
        cameraManager.setVideoFileSize(videoFileSize);
    }

    /**
     * Sets the maximum duration (in ms) of the recording video.
     *
     * @param videoDuration the maximum duration in ms (if zero or negative, disables the duration limit)
     */
    public void setVideoDuration(int videoDuration) {
        cameraManager.setVideoDuration(videoDuration);
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

    public void closeCamera(CameraCloseListener cameraCloseListener) {
        cameraManager.closeCamera(cameraCloseListener);
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
