package me.shouheng.camerax.manager.impl;

import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import me.shouheng.camerax.configuration.Configuration;
import me.shouheng.camerax.configuration.SizeCalculateStrategy;
import me.shouheng.camerax.preview.CameraPreview;

import java.util.List;

public class Camera1Manager extends AbstractCameraManager<Integer> {

    private static final String TAG = "Camera1Manager";

    private Camera.PreviewCallback previewCallback;
    private Camera.Parameters cameraParameters;

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
        callback.onCameraOpened();
    }

    /**
     * TODO Test the {@link Camera#setDisplayOrientation(int)} method for output and preview images.
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

                try {
                    camera.setParameters(cameraParameters);
                    if (showingPreview) {
                        camera.startPreview();
                    }
                } catch (Exception e) {
                    Log.d(TAG, "adjustCameraParameters: " + e);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "adjustCameraParameters: " + e);
        }
    }

    private void setupPreview() {

    }

    private void setAutoFocus(Camera camera, Camera.Parameters parameters) {
        try {
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            }
        } catch (Exception ignore) {
        }
    }

    private boolean setAutoFocusInternal(boolean autoFocus) {
        configuration.setAutoFocus(autoFocus);
        if (isCameraOpened()) {
            final List<String> modes = cameraParameters.getSupportedFocusModes();
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                attachFocusTapListener();
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                detachFocusTapListener();
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                detachFocusTapListener();
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                detachFocusTapListener();
                cameraParameters.setFocusMode(modes.get(0));
            }
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
