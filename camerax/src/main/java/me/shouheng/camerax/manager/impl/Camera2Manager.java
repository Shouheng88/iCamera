package me.shouheng.camerax.manager.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
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
import java.util.Arrays;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:52
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Manager extends BaseCameraManager<String> {

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
    private int cameraState;

    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {

        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(@NonNull CaptureResult result) {
            switch (cameraState) {
                case STATE_PREVIEW:
                    break;
                case STATE_WAITING_LOCK:
                    break;
                case STATE_WAITING_PRE_CAPTURE:
                    break;
                case STATE_WAITING_NON_PRE_CAPTURE:
                    break;
                case STATE_PICTURE_TAKEN:
                    break;
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
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
    @SuppressLint("MissingPermission")
    public void openCamera(CameraOpenListener cameraOpenListener) {
        super.openCamera(cameraOpenListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                Logger.d(TAG, "openCamera");
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
        return flashMode;
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
    public Size getSize(@Camera.SizeFor int sizeFor) {
        switch (sizeFor) {
            case Camera.SIZE_FOR_PREVIEW:
                return previewSize;
            case Camera.SIZE_FOR_PICTURE:
                return pictureSize;
            case Camera.SIZE_FOR_VIDEO:
                return videoSize;
        }
        return null;
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
        }
        return null;
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
    }

    @Override
    public void takePicture(CameraPhotoListener cameraPhotoListener) {
        super.takePicture(cameraPhotoListener);
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                    cameraState = STATE_WAITING_LOCK;
                    captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
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
    }

    @Override
    public void closeCamera() {
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
        } else if (mediaType == Media.TYPE_VIDEO && videoSize == null) {
            videoSize = cameraSizeCalculator.getVideoSize(videoSizes, expectAspectRatio, expectSize);
            previewSize = cameraSizeCalculator.getVideoPreviewSize(previewSizes, videoSize);
        }

        imageReader = ImageReader.newInstance(pictureSize.width, pictureSize.height, ImageFormat.JPEG, /*maxImages*/2);
        imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);
    }

    private void setupPreview() {
        try {
            // TODO TEST
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
                                Camera2Manager.this.captureSession = cameraCaptureSession;
                                previewRequest = previewRequestBuilder.build();
                                captureSession = cameraCaptureSession;
                                try {
                                    captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                                } catch (CameraAccessException ex) {
                                    Logger.e(TAG, "SetupPreview error " + ex);
                                }
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Logger.d(TAG, "onConfigureFailed");
                        }
                    }, null);
        } catch (Exception ex) {
            Logger.e(TAG, "SetupPreview error " + ex);
        }
    }

    @IntDef({STATE_PREVIEW, STATE_WAITING_LOCK, STATE_WAITING_PRE_CAPTURE, STATE_WAITING_NON_PRE_CAPTURE, STATE_PICTURE_TAKEN})
    @Retention(RetentionPolicy.SOURCE)
    @interface CameraState {
    }
}
