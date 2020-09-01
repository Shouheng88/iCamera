package me.shouheng.icamera;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.shouheng.icamera.config.ConfigurationProvider;
import me.shouheng.icamera.config.calculator.CameraSizeCalculator;
import me.shouheng.icamera.config.size.AspectRatio;
import me.shouheng.icamera.config.size.Size;
import me.shouheng.icamera.config.size.SizeMap;
import me.shouheng.icamera.enums.CameraFace;
import me.shouheng.icamera.enums.CameraSizeFor;
import me.shouheng.icamera.enums.DeviceDefaultOrientation;
import me.shouheng.icamera.enums.FlashMode;
import me.shouheng.icamera.enums.MediaQuality;
import me.shouheng.icamera.enums.MediaType;
import me.shouheng.icamera.enums.PreviewAdjustType;
import me.shouheng.icamera.enums.SensorPosition;
import me.shouheng.icamera.listener.CameraCloseListener;
import me.shouheng.icamera.listener.CameraOpenListener;
import me.shouheng.icamera.listener.CameraPhotoListener;
import me.shouheng.icamera.listener.CameraPreviewListener;
import me.shouheng.icamera.listener.CameraSizeListener;
import me.shouheng.icamera.listener.CameraVideoListener;
import me.shouheng.icamera.listener.DisplayOrientationDetector;
import me.shouheng.icamera.listener.OnMoveListener;
import me.shouheng.icamera.listener.OnOrientationChangedListener;
import me.shouheng.icamera.manager.CameraManager;
import me.shouheng.icamera.preview.CameraPreview;
import me.shouheng.icamera.util.CameraHelper;
import me.shouheng.icamera.util.XLog;
import me.shouheng.icamera.widget.FocusMarkerLayout;

import static me.shouheng.icamera.enums.PreviewAdjustType.HEIGHT_FIRST;
import static me.shouheng.icamera.enums.PreviewAdjustType.LARGER_FIRST;
import static me.shouheng.icamera.enums.PreviewAdjustType.NONE;
import static me.shouheng.icamera.enums.PreviewAdjustType.SMALLER_FIRST;
import static me.shouheng.icamera.enums.PreviewAdjustType.WIDTH_FIRST;

/**
 * It's over :D
 *
 * ___   _______  _______  __   __  _______  ______    _______
 * |   | |       ||   _   ||  |_|  ||       ||    _ |  |   _   |
 * |   | |       ||  |_|  ||       ||    ___||   | ||  |  |_|  |
 * |   | |       ||       ||       ||   |___ |   |_||_ |       |
 * |   | |      _||       ||       ||    ___||    __  ||       |
 * |   | |     |_ |   _   || ||_|| ||   |___ |   |  | ||   _   |
 * |___| |_______||__| |__||_|   |_||_______||___|  |_||__| |__|
 *
 *                         = WngShhng ==
 *
 *              THE ADVANCED CAMERA LIBRARY FOR ANDROID.
 *  '
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:43
 */
public class CameraView extends FrameLayout {
    private static final String TAG = "CameraView";

    private CameraManager cameraManager;
    private CameraPreview cameraPreview;

    private boolean clipScreen;
    private boolean adjustViewBounds;
    @PreviewAdjustType
    private int adjustType = NONE;
    private AspectRatio aspectRatio;
    private FocusMarkerLayout focusMarkerLayout;
    private DisplayOrientationDetector displayOrientationDetector;
    private SensorManager sensorManager = null;
    private List<OnOrientationChangedListener> orientationChangedListeners = new ArrayList<>();
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            synchronized (this) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    if (sensorEvent.values[0] < 4 && sensorEvent.values[0] > -4) {
                        if (sensorEvent.values[1] > 0) {
                            // UP
                            ConfigurationProvider.get().setSensorPosition(SensorPosition.SENSOR_POSITION_UP);
                            ConfigurationProvider.get().setDegrees(ConfigurationProvider.get().getDeviceDefaultOrientation()
                                    == DeviceDefaultOrientation.ORIENTATION_PORTRAIT ? 0 : 90);
                        } else if (sensorEvent.values[1] < 0) {
                            // UP SIDE DOWN
                            ConfigurationProvider.get().setSensorPosition(SensorPosition.SENSOR_POSITION_UP_SIDE_DOWN);
                            ConfigurationProvider.get().setDegrees(ConfigurationProvider.get().getDeviceDefaultOrientation()
                                    == DeviceDefaultOrientation.ORIENTATION_PORTRAIT ? 180 : 270);
                        }
                    } else if (sensorEvent.values[1] < 4 && sensorEvent.values[1] > -4) {
                        if (sensorEvent.values[0] > 0) {
                            // LEFT
                            ConfigurationProvider.get().setSensorPosition(SensorPosition.SENSOR_POSITION_LEFT);
                            ConfigurationProvider.get().setDegrees(ConfigurationProvider.get().getDeviceDefaultOrientation()
                                    == DeviceDefaultOrientation.ORIENTATION_PORTRAIT ? 90 : 180);
                        } else if (sensorEvent.values[0] < 0) {
                            // RIGHT
                            ConfigurationProvider.get().setSensorPosition(SensorPosition.SENSOR_POSITION_RIGHT);
                            ConfigurationProvider.get().setDegrees(ConfigurationProvider.get().getDeviceDefaultOrientation()
                                    == DeviceDefaultOrientation.ORIENTATION_PORTRAIT ? 270 : 0);
                        }
                    }
                    // notify screen orientation changed
                    if (!orientationChangedListeners.isEmpty()) {
                        for (OnOrientationChangedListener listener : orientationChangedListeners) {
                            listener.onOrientationChanged(ConfigurationProvider.get().getDegrees());
                        }
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { /*noop*/ }
    };

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
        // set the device default orientation and register orientation change sensor events
        final int defaultOrientation = CameraHelper.getDeviceDefaultOrientation(getContext());
        if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ConfigurationProvider.get().setDeviceDefaultOrientation(DeviceDefaultOrientation.ORIENTATION_LANDSCAPE);
        } else {
            ConfigurationProvider.get().setDeviceDefaultOrientation(DeviceDefaultOrientation.ORIENTATION_PORTRAIT);
        }
        this.sensorManager = (SensorManager) getContext().getSystemService(Activity.SENSOR_SERVICE);
        sensorManager.registerListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        // initialize camera
        cameraPreview = ConfigurationProvider.get().getCameraPreviewCreator().create(context, this);
        cameraManager = ConfigurationProvider.get().getCameraManagerCreator().create(context, cameraPreview);
        cameraManager.initialize(context);
        cameraManager.addCameraSizeListener(new CameraSizeListener() {
            @Override
            public void onPreviewSizeUpdated(Size previewSize) {
                aspectRatio = cameraManager.getAspectRatio(CameraSizeFor.SIZE_FOR_PREVIEW);
                if (displayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
                    aspectRatio = aspectRatio.inverse();
                }
                XLog.d(TAG, "onPreviewSizeUpdated : " + previewSize);
                requestLayout();
            }

            @Override
            public void onVideoSizeUpdated(Size videoSize) { /*noop*/ }

            @Override
            public void onPictureSizeUpdated(Size pictureSize) { /*noop*/ }
        });

        focusMarkerLayout = new FocusMarkerLayout(context);
        focusMarkerLayout.setCameraView(this);
        focusMarkerLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.addView(focusMarkerLayout);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr,
                R.style.Widget_CameraView);
        adjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false);
        cameraManager.switchCamera(a.getInt(R.styleable.CameraView_cameraFace, CameraFace.FACE_REAR));
        cameraManager.setMediaType(a.getInt(R.styleable.CameraView_mediaType, MediaType.TYPE_PICTURE));
        cameraManager.setVoiceEnable(a.getBoolean(R.styleable.CameraView_voiceEnable, true));
        String strAspectRatio = a.getString(R.styleable.CameraView_aspectRatio);
        aspectRatio = TextUtils.isEmpty(strAspectRatio) ?
                ConfigurationProvider.get().getDefaultAspectRatio() : AspectRatio.parse(strAspectRatio);
        cameraManager.setExpectAspectRatio(aspectRatio);
        cameraManager.setAutoFocus(a.getBoolean(R.styleable.CameraView_autoFocus, true));
        cameraManager.setFlashMode(a.getInt(R.styleable.CameraView_flash, FlashMode.FLASH_AUTO));
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
        XLog.d(TAG, "onMeasure");
        if (isInEditMode()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        XLog.d(TAG, "clipScreen: " + clipScreen + " adjustViewBounds: " + adjustViewBounds);
        if (clipScreen) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            XLog.d(TAG, "width: " + width + " height: " + height);
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
            // fix 2020-08-31 : preview is distorted when switch face for camera2 while it's not opened
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            XLog.d(TAG, "widthMode: " + widthMode + " heightMode: " + heightMode);
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
        XLog.d(TAG, "width: " + width + " height: " + height);
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

    @CameraFace
    public int getCameraFace() {
        return cameraManager.getCameraFace();
    }

    /**
     * Switch camera to given face.
     *
     * @param cameraFace camera face
     */
    public void switchCamera(@CameraFace int cameraFace) {
        cameraManager.switchCamera(cameraFace);
    }

    /**
     * Set media type, video, picture etc.
     *
     * @param mediaType media type
     */
    public void setMediaType(@MediaType int mediaType) {
        cameraManager.setMediaType(mediaType);
    }

    @MediaType
    public int getMediaType() {
        return cameraManager.getMediaType();
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

    public void setFlashMode(@FlashMode int flashMode) {
        cameraManager.setFlashMode(flashMode);
    }

    @FlashMode
    public int getFlashMode() {
        return cameraManager.getFlashMode();
    }

    /**
     * Set zoom for camera with minimum value 1.0 to {@link #getMaxZoom()}
     *
     * @param zoom zoom ratio
     */
    public void setZoom(@FloatRange(from = 1.0f) float zoom) {
        cameraManager.setZoom(zoom);
    }

    public float getZoom() {
        return cameraManager.getZoom();
    }

    public float getMaxZoom() {
        return cameraManager.getMaxZoom();
    }

    /**
     * Set expect picture or video size, the final output size
     * will be calculated from {@link me.shouheng.icamera.config.calculator.CameraSizeCalculator}.
     * You can implement this interface and set your own implementation by
     * {@link ConfigurationProvider#setCameraSizeCalculator(CameraSizeCalculator)}
     *
     * @param expectSize expect output size
     */
    public void setExpectSize(Size expectSize) {
        cameraManager.setExpectSize(expectSize);
    }

    /**
     * Set expect aspect ratio of output, final result logic was same as {@link #setExpectSize(Size)}.
     *
     * @param aspectRatio expect aspect ratio of output
     */
    public void setExpectAspectRatio(AspectRatio aspectRatio) {
        cameraManager.setExpectAspectRatio(aspectRatio);
    }

    /**
     * Set expected media quality
     *
     * @param mediaQuality media quality
     */
    public void setMediaQuality(@MediaQuality int mediaQuality) {
        cameraManager.setMediaQuality(mediaQuality);
    }

    /**
     * Current using size of camera
     *
     * @param sizeFor the size for
     * @return        the size
     */
    public Size getSize(@CameraSizeFor int sizeFor) {
        return cameraManager.getSize(sizeFor);
    }

    /**
     * Get all support sizes of camera
     *
     * @param sizeFor the size for
     * @return        the sizes
     */
    public SizeMap getSizes(@CameraSizeFor int sizeFor) {
        return cameraManager.getSizes(sizeFor);
    }

    /**
     * Get current aspect ratio of preview, picture or video. Since the size might not be calculated
     * when the time trying to get the ratio, the returned value might be null.
     *
     * @param sizeFor the aspect ratio for
     * @return        the aspect ratio
     */
    @Nullable
    public AspectRatio getAspectRatio(@CameraSizeFor int sizeFor) {
        return cameraManager.getAspectRatio(sizeFor);
    }

    public void addCameraSizeListener(CameraSizeListener cameraSizeListener) {
        cameraManager.addCameraSizeListener(cameraSizeListener);
    }

    /**
     * Set the camera preview listener
     *
     * @param cameraPreviewListener the listener
     */
    public void setCameraPreviewListener(CameraPreviewListener cameraPreviewListener) {
        cameraManager.setCameraPreviewListener(cameraPreviewListener);
    }

    /**
     * Add screen orientation change listener. You may listen and update rotate the controls
     * according to the screen change.
     *
     * @param orientationChangedListener the orientation change listener
     */
    public void addOrientationChangedListener(OnOrientationChangedListener orientationChangedListener) {
        if (!orientationChangedListeners.contains(orientationChangedListener)) {
            orientationChangedListeners.add(orientationChangedListener);
        }
    }

    public void removeOrientationChangedListener(OnOrientationChangedListener orientationChangedListener) {
        orientationChangedListeners.remove(orientationChangedListener);
    }

    /**
     * Call to take a picture, you can get the output from the callback.
     *
     * @param fileToSave          the file to save picture
     * @param cameraPhotoListener the result callback
     */
    public void takePicture(File fileToSave, CameraPhotoListener cameraPhotoListener) {
        cameraManager.takePicture(fileToSave, cameraPhotoListener);
    }

    /**
     * Set the maximum file size (in bytes) of the recording video.
     *
     * @param videoFileSize the maximum file size in bytes (if zero or negative, the limit will be disabled)
     */
    public void setVideoFileSize(long videoFileSize) {
        cameraManager.setVideoFileSize(videoFileSize);
    }

    /**
     * Set the maximum duration (in ms) of the recording video.
     *
     * @param videoDuration the maximum duration in ms (if zero or negative, the duration limit will be disabled)
     */
    public void setVideoDuration(int videoDuration) {
        cameraManager.setVideoDuration(videoDuration);
    }

    /**
     * Start video record
     *
     * @param file                the file to save video data
     * @param cameraVideoListener the result callback
     */
    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {
        cameraManager.startVideoRecord(file, cameraVideoListener);
    }

    /**
     * Stop video record
     */
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
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
            orientationChangedListeners.clear();
        }
    }

    /**
     * Set a callback when the use made a move event on the maker.
     *
     * @param onMoveListener the callback
     */
    public void setOnMoveListener(OnMoveListener onMoveListener) {
        focusMarkerLayout.setOnMoveListener(onMoveListener);
    }

    public void setTouchAngle(int touchAngle) {
        if (focusMarkerLayout != null) {
            focusMarkerLayout.setTouchAngle(touchAngle);
        }
    }

    /**
     * Set scale rate when user try to scale on maker.
     *
     * @param scaleRate the scale rate
     */
    public void setScaleRate(@IntRange(from = 0) int scaleRate) {
        focusMarkerLayout.setScaleRate(scaleRate);
    }

    public void setTouchZoomEnable(boolean touchZoomEnable) {
        focusMarkerLayout.setTouchZoomEnable(touchZoomEnable);
    }

    public void setUseTouchFocus(boolean useTouchFocus) {
        focusMarkerLayout.setUseTouchFocus(useTouchFocus);
    }

}
