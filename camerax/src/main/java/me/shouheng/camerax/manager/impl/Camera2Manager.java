package me.shouheng.camerax.manager.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.Surface;
import android.view.SurfaceHolder;
import me.shouheng.camerax.config.ConfigurationProvider;
import me.shouheng.camerax.config.calculator.CameraSizeCalculator;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.config.sizes.SizeMap;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.enums.Flash;
import me.shouheng.camerax.enums.Media;
import me.shouheng.camerax.enums.Preview;
import me.shouheng.camerax.listener.CameraOpenListener;
import me.shouheng.camerax.listener.CameraPhotoListener;
import me.shouheng.camerax.listener.CameraVideoListener;
import me.shouheng.camerax.preview.CameraPreview;
import me.shouheng.camerax.preview.CameraPreviewCallback;
import me.shouheng.camerax.util.CameraHelper;
import me.shouheng.camerax.util.Logger;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:52
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("MissingPermission")
public class Camera2Manager extends BaseCameraManager<String> implements ImageReader.OnImageAvailableListener {

    private static final String TAG = "Camera2Manager";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW                      = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK                 = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRE_CAPTURE          = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRE_CAPTURE      = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN                = 4;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;

    private CameraCharacteristics frontCameraCharacteristics;
    private CameraCharacteristics rearCameraCharacteristics;
    private StreamConfigurationMap frontStreamConfigurationMap;
    private StreamConfigurationMap rearStreamConfigurationMap;

    private ImageReader imageReader;

    private SurfaceHolder surfaceHolder;
    private SurfaceTexture surfaceTexture;
    private Surface workingSurface;

    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;

    @CameraState
    private int cameraPreviewState;

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        private void processCaptureResult(@NonNull CaptureResult result) {
            switch (cameraPreviewState) {
                case STATE_PREVIEW:
                    break;
                case STATE_WAITING_LOCK: {
                    final Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                            || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                            || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState
                            || CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            cameraPreviewState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPreCaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRE_CAPTURE: {
                    final Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        cameraPreviewState = STATE_WAITING_NON_PRE_CAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRE_CAPTURE: {
                    final Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        cameraPreviewState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
                case STATE_PICTURE_TAKEN:
                    break;
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            processCaptureResult(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            processCaptureResult(result);
        }
    };

    public Camera2Manager(CameraPreview cameraPreview) {
        super(cameraPreview);
        cameraPreview.setCameraPreviewCallback(new CameraPreviewCallback() {
            @Override
            public void onAvailable(CameraPreview cameraPreview) {
                if (isCameraOpened()) {
                    setupPreview();
                }
            }
        });
    }

    @Override
    public void initialize(Context context) {
        super.initialize(context);
        initCameraInfo(context);
    }

    @Override
    public void openCamera(CameraOpenListener cameraOpenListener) {
        super.openCamera(cameraOpenListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                prepareCameraOutputs();
                adjustCameraConfiguration();
                try {
                    cameraManager.openCamera(currentCameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            Logger.d(TAG, "Camera opened.");
                            cameraDevice = camera;
                            if (cameraPreview.isAvailable()) {
                                setupPreview();
                            }
                            notifyCameraOpened();
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            Logger.e(TAG, "Camera disconnected.");
                            camera.close();
                            cameraDevice = null;
                            notifyCameraOpenError(new RuntimeException("Camera disconnected."));
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            Logger.e(TAG, "Camera open error : " + error);
                            camera.close();
                            cameraDevice = null;
                            notifyCameraOpenError(new RuntimeException("Camera error : " + error));
                        }
                    }, backgroundHandler);
                } catch (Exception ex) {
                    Logger.e(TAG, "error : " + ex);
                    notifyCameraOpenError(ex);
                }
            }
        });
    }

    @Override
    public boolean isCameraOpened() {
        return cameraDevice != null;
    }

    @Override
    public void switchCamera(int cameraFace) {
        super.switchCamera(cameraFace);
    }

    @Override
    public void setMediaType(int mediaType) {
        if (this.mediaType == mediaType) {
            return;
        }
        this.mediaType = mediaType;
    }

    @Override
    public void setVoiceEnable(boolean voiceEnable) {
        if (this.voiceEnabled == voiceEnable) {
            return;
        }
        this.voiceEnabled = voiceEnable;
    }

    @Override
    public boolean isVoiceEnable() {
        return voiceEnabled;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {
        if (this.isAutoFocus == autoFocus) {
            return;
        }
        this.isAutoFocus = autoFocus;
        if (isCameraOpened() && previewRequestBuilder != null) {
            setAutoFocusInternal();
            previewRequest = previewRequestBuilder.build();
            if (captureSession != null) {
                try {
                    captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                } catch (CameraAccessException e) {
                    Logger.e(TAG, "setAutoFocus error : " + e);
                }
            } else {
                Logger.i(TAG, "setAutoFocus captureSession is null.");
            }
        } else {
            Logger.i(TAG, "setAutoFocus camera not open or previewRequestBuilder is null");
        }
    }

    @Override
    public boolean isAutoFocus() {
        return isAutoFocus;
    }

    @Override
    public void setFlashMode(@Flash.FlashMode int flashMode) {
        if (this.flashMode == flashMode) {
            return;
        }
        this.flashMode = flashMode;
        if (isCameraOpened() && previewRequestBuilder != null) {
            boolean succeed = setFlashModeInternal();
            if (succeed) {
                previewRequest = previewRequestBuilder.build();
                if (captureSession != null) {
                    try {
                        captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Logger.e(TAG, "setFlashMode error : " + e);
                    }
                } else {
                    Logger.i(TAG, "setFlashMode captureSession is null.");
                }
            } else {
                Logger.i(TAG, "setFlashMode failed.");
            }
        } else {
            Logger.i(TAG, "setFlashMode camera not open or previewRequestBuilder is null");
        }
    }

    @Override
    public int getFlashMode() {
        return flashMode;
    }

    @Override
    public void setZoom(float zoom) {
        if (zoom == this.zoom || zoom > getMaxZoom() || zoom < 1.f) {
            return;
        }
        this.zoom = zoom;
        if (isCameraOpened()) {
            boolean succeed = setZoomInternal();
            if (succeed) {
                previewRequest = previewRequestBuilder.build();
                if (captureSession != null) {
                    try {
                        captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Logger.e(TAG, "setZoom error : " + e);
                    }
                } else {
                    Logger.i(TAG, "setZoom captureSession is null.");
                }
            } else {
                Logger.i(TAG, "setZoom failed : setZoomInternal failed.");
            }
        } else {
            Logger.i(TAG, "setZoom failed : camera not open.");
        }
    }

    @Override
    public float getZoom() {
        return zoom;
    }

    @Override
    public float getMaxZoom() {
        if (maxZoom == 0) {
            CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            Float fMaxZoom = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            if (fMaxZoom == null) {
                maxZoom = 1.0f;
            } else {
                maxZoom = fMaxZoom;
            }
        }
        return maxZoom;
    }

    @Override
    public Size getSize(@Camera.SizeFor int sizeFor) {
        switch (sizeFor) {
            case Camera.SIZE_FOR_PREVIEW:
                return previewSize;
            case Camera.SIZE_FOR_PICTURE:
                return pictureSize;
            case Camera.SIZE_FOR_VIDEO:
                return videoSize;
            default:
                return null;
        }
    }

    @Override
    public SizeMap getSizes(@Camera.SizeFor int sizeFor) {
        switch (sizeFor) {
            case Camera.SIZE_FOR_PREVIEW:
                if (previewSizeMap == null) {
                    previewSizeMap = CameraHelper.getSizeMapFromSizes(previewSizes);
                }
                return previewSizeMap;
            case Camera.SIZE_FOR_PICTURE:
                if (pictureSizeMap == null) {
                    pictureSizeMap = CameraHelper.getSizeMapFromSizes(pictureSizes);
                }
                return pictureSizeMap;
            case Camera.SIZE_FOR_VIDEO:
                if (videoSizeMap == null) {
                    videoSizeMap = CameraHelper.getSizeMapFromSizes(videoSizes);
                }
                return videoSizeMap;
            default:
                return null;
        }
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        if (this.displayOrientation == displayOrientation) {
            return;
        }
        this.displayOrientation = displayOrientation;
        if (isCameraOpened()) {
            // empty
        }
    }

    @Override
    public void takePicture(CameraPhotoListener cameraPhotoListener) {
        super.takePicture(cameraPhotoListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                    cameraPreviewState = STATE_WAITING_LOCK;
                    captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
                    if (voiceEnabled) {
                        new MediaActionSound().play(MediaActionSound.SHUTTER_CLICK);
                    }
                } catch (Exception ex) {
                    notifyCameraCaptureFailed(ex);
                }
            }
        });
    }

    @Override
    public void startVideoRecord(File file, CameraVideoListener cameraVideoListener) {
        super.startVideoRecord(file, cameraVideoListener);
    }

    @Override
    public void stopVideoRecord() {
    }

    @Override
    public void resumePreview() {
        if (isCameraOpened()) {
            try {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
                cameraPreviewState = STATE_PREVIEW;
                captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
            } catch (Exception e) {
                Logger.e(TAG, "resumePreview : error during focus unlocking");
            }
        }
    }

    @Override
    public void closeCamera() {
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        try {
            try (Image image = reader.acquireNextImage()) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                notifyCameraPictureTaken(bytes);
            } catch (IllegalStateException e) {
                Logger.e(TAG, "onImageAvailable error" + e);
                notifyCameraCaptureFailed(e);
            }
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
            cameraPreviewState = STATE_PREVIEW;
            captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
        } catch (Exception e) {
            Logger.e(TAG, "onImageAvailable error " + e);
            notifyCameraCaptureFailed(e);
        }
    }

    /*---------------------------------inner methods------------------------------------*/

    private void initCameraInfo(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        long start = System.currentTimeMillis();
        try {
            assert cameraManager != null;
            final String[] ids = cameraManager.getCameraIdList();
            numberOfCameras = ids.length;
            for (String id : ids) {
                final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = id;
                    Integer iFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    frontCameraOrientation = iFrontCameraOrientation == null ? 0 : frontCameraOrientation;
                    frontCameraCharacteristics = characteristics;
                } else if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK){
                    rearCameraId = id;
                    Integer iRearCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    rearCameraOrientation = iRearCameraOrientation == null ? 0 : rearCameraOrientation;
                    rearCameraCharacteristics = characteristics;
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "initCameraInfo error " + e);
        }
        Logger.d(TAG, "initCameraInfo basic cost : " + (System.currentTimeMillis() - start) + " ms");

        currentCameraId = cameraFace == Camera.FACE_REAR ? rearCameraId : frontCameraId;
    }

    private void prepareCameraOutputs() {
        boolean isFrontCamera = cameraFace == Camera.FACE_REAR;
        long start = System.currentTimeMillis();
        try {
            final CameraCharacteristics characteristics = isFrontCamera ? frontCameraCharacteristics : rearCameraCharacteristics;
            if (isFrontCamera && frontStreamConfigurationMap == null) {
                frontStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            } else if (!isFrontCamera && rearStreamConfigurationMap == null) {
                rearStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            }
        } catch (Exception ex) {
            Logger.e(TAG, "initCameraInfo error " + ex);
            notifyCameraOpenError(new RuntimeException(ex));
        }
        Logger.d(TAG, "initCameraInfo get map cost : " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        try {
            final StreamConfigurationMap map = isFrontCamera ? frontStreamConfigurationMap : rearStreamConfigurationMap;
            previewSizes = ConfigurationProvider.get().getPreviewSizes(map);
            pictureSizes = ConfigurationProvider.get().getPictureSizes(map);
            videoSizes = ConfigurationProvider.get().getVideoSizes(map);
        } catch (Exception ex) {
            Logger.e(TAG, "error : " + ex);
            notifyCameraOpenError(new RuntimeException(ex));
        }
        Logger.d(TAG, "prepareCameraOutputs cost : " + (System.currentTimeMillis() - start) + " ms");
    }

    private void adjustCameraConfiguration() {
        CameraSizeCalculator cameraSizeCalculator = ConfigurationProvider.get().getCameraSizeCalculator();
        if (mediaType == Media.TYPE_PICTURE && pictureSize == null) {
            pictureSize = cameraSizeCalculator.getPictureSize(pictureSizes, expectAspectRatio, expectSize);
            previewSize = cameraSizeCalculator.getPicturePreviewSize(previewSizes, pictureSize);
            notifyPictureSizeUpdated(pictureSize);
        } else if (mediaType == Media.TYPE_VIDEO && videoSize == null) {
            videoSize = cameraSizeCalculator.getVideoSize(videoSizes, expectAspectRatio, expectSize);
            previewSize = cameraSizeCalculator.getVideoPreviewSize(previewSizes, videoSize);
            notifyVideoSizeUpdated(videoSize);
        }
        notifyPreviewSizeUpdated(previewSize);

        imageReader = ImageReader.newInstance(pictureSize.width, pictureSize.height, ImageFormat.JPEG, /*maxImages*/2);
        imageReader.setOnImageAvailableListener(this, backgroundHandler);
    }

    private void setupPreview() {
        try {
            // TODO test if the surface view is available
            if (cameraPreview.getPreviewType() == Preview.TEXTURE_VIEW) {
                this.surfaceTexture = cameraPreview.getSurfaceTexture();
                assert surfaceTexture != null;
                surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height);
                workingSurface = cameraPreview.getSurface();
            } else if (cameraPreview.getPreviewType() == Preview.SURFACE_VIEW) {
                surfaceHolder = cameraPreview.getSurfaceHolder();
                assert surfaceHolder != null;
                surfaceHolder.setFixedSize(previewSize.width, previewSize.height);
                workingSurface = cameraPreview.getSurface();
            }

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(workingSurface);
            cameraDevice.createCaptureSession(Arrays.asList(workingSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (isCameraOpened()) {
                                captureSession = cameraCaptureSession;
                                setFlashModeInternal();
                                previewRequest = previewRequestBuilder.build();
                                try {
                                    captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                                } catch (CameraAccessException ex) {
                                    Logger.e(TAG, "SetupPreview error " + ex);
                                    notifyCameraOpenError(ex);
                                }
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Logger.d(TAG, "onConfigureFailed");
                            notifyCameraOpenError(new Throwable("Camera capture session configure failed."));
                        }
                    }, null);
        } catch (Exception ex) {
            Logger.e(TAG, "SetupPreview error " + ex);
            notifyCameraOpenError(ex);
        }
    }

    private void captureStillPicture() {
        if (isCameraOpened()) {
            try {
                final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(imageReader.getSurface());

                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getPhotoOrientation(configurationProvider.getSensorPosition()));

                captureSession.stopRepeating();
                captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {
                        Logger.d(TAG, "onCaptureCompleted: ");
                    }
                }, null);
            } catch (CameraAccessException e) {
                Logger.e(TAG, "Error during capturing picture");
                notifyCameraCaptureFailed(e);
            }
        } else {
            notifyCameraCaptureFailed(new RuntimeException("Camera not open."));
        }
    }

    private void runPreCaptureSequence() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            cameraPreviewState = STATE_WAITING_PRE_CAPTURE;
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Logger.e(TAG, "runPreCaptureSequence error " + e);
        }
    }

    private boolean setFlashModeInternal() {
        try {
            CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            Boolean isFlashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (isFlashAvailable == null || !isFlashAvailable) {
                Logger.i(TAG, "Flash is not available.");
                return false;
            }
            switch (flashMode) {
                case Flash.FLASH_ON:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
                case Flash.FLASH_OFF:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                case Flash.FLASH_AUTO:
                default:
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    break;
            }
            return true;
        } catch (Exception ex) {
            Logger.e(TAG, "setFlashMode error : " + ex);
        }
        return false;
    }

    private void setAutoFocusInternal() {
        if (isAutoFocus) {
            CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
                    frontCameraCharacteristics : rearCameraCharacteristics;
            int[] modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if (modes == null || modes.length == 0 ||
                    (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                isAutoFocus = false;
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            } else if (mediaType == Media.TYPE_PICTURE) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else if (mediaType == Media.TYPE_VIDEO) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            }
        } else {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    private boolean setZoomInternal() {
        float maxZoom = getMaxZoom();
        if (maxZoom == 1.0f || previewRequestBuilder == null) {
            return false;
        }

        CameraCharacteristics cameraCharacteristics = cameraFace == Camera.FACE_FRONT ?
                frontCameraCharacteristics : rearCameraCharacteristics;
        Rect rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (rect == null) {
            return false;
        }

        zoom = zoom < 1.f ? 1.f : zoom;
        zoom = zoom > maxZoom ? maxZoom : zoom;

        int cropW = (rect.width() - (int) ((float) rect.width() / zoom)) / 2;
        int cropH = (rect.height() - (int) ((float) rect.height() / zoom)) / 2;

        Rect zoomRect = new Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH);
        previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
        return true;
    }

    @IntDef({STATE_PREVIEW, STATE_WAITING_LOCK, STATE_WAITING_PRE_CAPTURE, STATE_WAITING_NON_PRE_CAPTURE, STATE_PICTURE_TAKEN})
    @Retention(RetentionPolicy.SOURCE)
    @interface CameraState {
    }
}
