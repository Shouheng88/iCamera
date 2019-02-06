package me.shouheng.camerax.manager.impl;

import android.graphics.Rect;
import android.hardware.Camera;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import me.shouheng.camerax.configuration.Configuration;
import me.shouheng.camerax.configuration.SizeCalculateStrategy;
import me.shouheng.camerax.listeners.OnPreviewViewTouchListener;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.utils.CameraHelper;
import me.shouheng.camerax.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

import static me.shouheng.camerax.enums.Camera.*;

// TODO Handle permission!
public class Camera1Manager extends AbstractCameraManager<Integer> {

    private static final String TAG = "Camera1Manager";

    private Camera.PreviewCallback previewCallback;
    private Camera.Parameters cameraParameters;
    private Camera.AutoFocusCallback autoFocusCallback;

    /**
     * Map from the constant to string for flash mode, late initialize value.
     * @see #getFlashModeMap()
     */
    private SparseArrayCompat<String> flashModeMap = null;

    private Camera camera;
    private Surface surface;

    /**
     * Number of available cameras.
     */
    private int numberOfCameras = 0;

    /**
     * The orientation of the camera image. The value is the angle that the
     * camera image needs to be rotated clockwise so it shows correctly on
     * the display in its natural orientation. It should be 0, 90, 180, or 270.
     *
     * @see Camera.CameraInfo#orientation
     */
    private int faceFrontCameraOrientation;
    private int faceBackCameraOrientation;

    private boolean showingPreview = false;

    public static Camera1Manager getInstance(Callback callback, CameraPreview cameraPreview) {
        return new Camera1Manager(callback, cameraPreview);
    }

    private Camera1Manager(final Callback callback, CameraPreview cameraPreview) {
        super(callback, cameraPreview);
        previewCallback = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                callback.onPreviewFrame(data,
                        cameraParameters.getPreviewSize().width,
                        cameraParameters.getPreviewSize().height,
                        cameraParameters.getPreviewFormat());
            }
        };
        cameraPreview.setCallback(new CameraPreview.Callback() {
            @Override
            public void onSurfaceChanged() {
                // TODO
            }
        });
    }

    @Override
    public void initializeCameraManager(Configuration configuration, SizeCalculateStrategy sizeCalculateStrategy) {
        super.initializeCameraManager(configuration, sizeCalculateStrategy);

        // Get all camera ids for Camera1.
        numberOfCameras = Camera.getNumberOfCameras();
        for (int i=0; i<numberOfCameras; i++) {
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                faceBackCameraId = i;
                faceBackCameraOrientation = cameraInfo.orientation;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                faceFrontCameraId = i;
                faceFrontCameraOrientation = cameraInfo.orientation;
            }
        }
    }

    @Override
    public boolean isCameraOpened() {
        return camera != null;
    }

    @Override
    public boolean openCamera(final Integer cameraId) {
        this.currentCameraId = cameraId;
        try {
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    showingPreview = true;
                    doOpenCamera(cameraId);
                    if (cameraPreview.isReady()) {
                        setupPreview();
                    }
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Call the callback in ui thread.
                            callback.onCameraOpened();
                            // TODO crash when start preview here!
                            camera.startPreview();
                        }
                    });
                }
            });
        } catch (Exception e) {
            // TODO define and handle exception.
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private void doOpenCamera(final Integer cameraId) {
        if (camera != null) {
            releaseCamera();
        }
        camera = Camera.open(cameraId);
        cameraParameters = camera.getParameters();
        adjustCameraParameters();
        //  TODO Test the {@link Camera#setDisplayOrientation(int)} method for output and preview images.
        camera.setDisplayOrientation(90);
    }

    /**
     * Adjust parameters of camera.
     *
     * Prepare the preview and output image and video size.
     */
    private void adjustCameraParameters() {
        try {
            SizeCalculateStrategy.Result result = sizeCalculateStrategy.calculate(cameraParameters, configuration, this);
            camcorderProfile = result.camcorderProfile;
            videoSize = result.videoSize;
            photoSize = result.photoSize;
            previewSize = result.previewSize;
            final Camera.Size currentSize = cameraParameters.getPictureSize();
            if (currentSize.width != previewSize.getWidth() || currentSize.height != previewSize.getHeight()) {
                if (showingPreview) {
                    camera.stopPreview();
                }
                if (cameraParameters.getSupportedPreviewFormats().contains(configuration.getPreviewFormat())) {
                    cameraParameters.setPreviewFormat(configuration.getPreviewFormat());
                }
                cameraParameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
                cameraParameters.setPictureSize(photoSize.getWidth(), photoSize.getHeight());
                setAutoFocusInternal(configuration.getFocusMode());
                setFlashInternal(configuration.getFlashMode());
                setZoomInternal(configuration.getZoom());
                try {
                    camera.setParameters(cameraParameters);
                    if (showingPreview) {
                        camera.startPreview();
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG, "adjustCameraParameters: " + e);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "adjustCameraParameters: " + e);
        }
    }

    private void setupPreview() {
        try {
            if (isCameraOpened()) {
                if (cameraPreview.getOutputClass() == SurfaceView.class) {
                    if (showingPreview) {
                        camera.stopPreview();
                    }
                    camera.setPreviewDisplay(cameraPreview.getSurfaceHolder());
                    if (showingPreview) {
                        camera.startPreview();
                    }
                } else {
                    camera.setPreviewTexture(cameraPreview.getSurfaceTexture());
                }
                camera.setPreviewCallback(previewCallback);
            } else {
                LogUtils.e(TAG, "setupPreview: ");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "setupPreview: " + e);
        }
    }

    /**
     * Set auto focus mode internal.
     *
     * @param focusMode focus mode.
     * @return true if succeed.
     */
    private boolean setAutoFocusInternal(@FocusMode int focusMode) {
        configuration.setFocusMode(focusMode);
        if (isCameraOpened()) {
            if (configuration.getFocusMode() != FOCUS_MODE_NONE) {
                final List<String> modes = cameraParameters.getSupportedFocusModes();
                switch (configuration.getFocusMode()) {
                    case FOCUS_MODE_AUTO:
                        if (modes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        }
                        break;
                    case FOCUS_MODE_INFINITY:
                        if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                        }
                        break;
                    case FOCUS_MODE_MACRO:
                        if (modes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                        }
                        break;
                    case FOCUS_MODE_FIXED:
                        if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                        }
                        break;
                    case FOCUS_MODE_EDOF:
                        if (modes.contains(Camera.Parameters.FOCUS_MODE_EDOF)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
                        }
                        break;
                    case FOCUS_MODE_CONTINUOUS_VIDEO:
                        if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        }
                        break;
                    case FOCUS_MODE_CONTINUOUS_PICTURE:
                        if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                        break;
                    case FOCUS_MODE_ADAPTION:
                    default:
                        if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                        } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                        } else {
                            cameraParameters.setFocusMode(modes.get(0));
                        }
                        break;
                }
                 attachFocusTapListener();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add touch listener handler to camera preview view, to handle the zoom and focus when touch.
     */
    private void attachFocusTapListener() {
        if (isCameraOpened() && configuration.isSupportZoom()) {
            cameraPreview.getView().setOnTouchListener(
                    new OnPreviewViewTouchListener(camera, cameraParameters, autoFocusCallback));
        }
    }

    /**
     * Set flash mode for camera. If the given flash mode exist
     *
     * @param flashMode flash mode
     * @return if set success.
     */
    private boolean setFlashInternal(@FlashMode int flashMode) {
        if (isCameraOpened()) {
            List<String> modes = cameraParameters.getSupportedFlashModes();
            String mode = getFlashModeMap().get(flashMode);
            if (modes != null && modes.contains(mode)) {
                cameraParameters.setFlashMode(mode);
                configuration.setFlashMode(flashMode);
                return true;
            }
            String current = getFlashModeMap().get(configuration.getFlashMode());
            if (modes == null || !modes.contains(current)) {
                cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                configuration.setFlashMode(FLASH_OFF);
                return true;
            }
            return false;
        } else {
            configuration.setFlashMode(flashMode);
            return false;
        }
    }

    /**
     * Set zoom for camera.
     *
     * @param zoom zoom value.
     * @return is succeed
     */
    private boolean setZoomInternal(float zoom) {
        if (isCameraOpened()) {
            if (!cameraParameters.isZoomSupported()) {
                return false;
            }
            int zoomIdx = CameraHelper.getZoomIdxForZoomFactor(zoom, cameraParameters.getZoomRatios());
            cameraParameters.setZoom(zoomIdx);
            configuration.setZoom(zoom);
            return true;
        } else {
            return false;
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
            cameraParameters = null;
            // Callback.
            callback.onCameraClosed();
        }
    }

    private SparseArrayCompat<String> getFlashModeMap() {
        if (flashModeMap == null) {
            flashModeMap = new SparseArrayCompat<>();
            flashModeMap.put(FLASH_AUTO, Camera.Parameters.FLASH_MODE_OFF);
            flashModeMap.put(FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
            flashModeMap.put(FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
            flashModeMap.put(FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
            flashModeMap.put(FLASH_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE);
        }
        return flashModeMap;
    }

    @Override
    public int getNumberOfCameras() {
        return numberOfCameras;
    }

    @Override
    public int getFaceFrontCameraOrientation() {
        return faceFrontCameraOrientation;
    }

    @Override
    public int getFaceBackCameraOrientation() {
        return faceBackCameraOrientation;
    }
}
