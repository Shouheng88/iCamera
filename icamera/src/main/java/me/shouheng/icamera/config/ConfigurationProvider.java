package me.shouheng.icamera.config;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import me.shouheng.icamera.config.calculator.CameraSizeCalculator;
import me.shouheng.icamera.config.calculator.impl.CameraSizeCalculatorImpl;
import me.shouheng.icamera.config.creator.CameraManagerCreator;
import me.shouheng.icamera.config.creator.CameraPreviewCreator;
import me.shouheng.icamera.config.creator.impl.CameraManagerCreatorImpl;
import me.shouheng.icamera.config.creator.impl.CameraPreviewCreatorImpl;
import me.shouheng.icamera.config.size.AspectRatio;
import me.shouheng.icamera.config.size.Size;
import me.shouheng.icamera.enums.CameraFace;
import me.shouheng.icamera.enums.CameraSizeFor;
import me.shouheng.icamera.enums.CameraType;
import me.shouheng.icamera.enums.DeviceDefaultOrientation;
import me.shouheng.icamera.enums.FlashMode;
import me.shouheng.icamera.enums.MediaQuality;
import me.shouheng.icamera.enums.MediaType;
import me.shouheng.icamera.enums.SensorPosition;
import me.shouheng.icamera.util.XLog;

/**
 * Global configuration provider for camera library. Singleton, also used to
 * cache some values of the camera to avoid multiple calculations.
 * You can call methods of this instance to make set some default values of
 * camera before it finally launched, for example {@link #setDefaultFlashMode(int)},
 * {@link #setDefaultAspectRatio(AspectRatio)} etc.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:44
 */
public final class ConfigurationProvider {
    private static final String TAG = "ConfigurationProvider";

    /** The singleton */
    private static volatile ConfigurationProvider configurationProvider;

    /** The creator for {@link me.shouheng.icamera.manager.CameraManager}. */
    private CameraManagerCreator cameraManagerCreator;

    /** The creator for {@link me.shouheng.icamera.preview.CameraPreview}. */
    private CameraPreviewCreator cameraPreviewCreator;

    /** The calculator for camera size. */
    private CameraSizeCalculator cameraSizeCalculator;

    /** Whether use memory cache in library. default is true. */
    private boolean useCacheValues;

    /**
     * The sizes map from a int value, which was calculated from:
     * hash = {@link CameraFace} | {@link CameraSizeFor} | {@link CameraType} */
    private SparseArray<List<Size>> sizeMap;

    /**
     * The room ratios map from a int value, which was calculated from:
     * hash = {@link CameraFace} | {@link CameraType} */
    private SparseArray<List<Float>> ratioMap;

    @CameraFace private int defaultCameraFace;
    @MediaType private int defaultMediaType;
    @MediaQuality private int defaultMediaQuality;
    private AspectRatio defaultAspectRatio;
    private boolean isVoiceEnable;
    private boolean isAutoFocus;
    @FlashMode private int defaultFlashMode;
    private long defaultVideoFileSize = -1;
    private int defaultVideoDuration = -1;

    /** Device default orientation */
    @DeviceDefaultOrientation private int deviceDefaultOrientation;
    /** The sensor position */
    @SensorPosition private int sensorPosition;
    private int degrees = -1;

    private int numberOfCameras;
    private AtomicBoolean camera2Prepared = new AtomicBoolean();
    private SparseArray<String> cameraIdCamera2 = new SparseArray<>();
    private SparseArray<CameraCharacteristics> cameraCharacteristics = new SparseArray<>();
    private SparseIntArray cameraOrientations = new SparseIntArray();
    private SparseArray<StreamConfigurationMap> streamConfigurationMaps = new SparseArray<>();

    private boolean isDebug;

    private ConfigurationProvider() {
        if (configurationProvider != null) {
            throw new UnsupportedOperationException("U can't initialize me!");
        }
        initWithDefaultValues();
    }

    private void initWithDefaultValues() {
        sizeMap = new SparseArray<>();
        ratioMap = new SparseArray<>();
        cameraManagerCreator = new CameraManagerCreatorImpl();
        cameraPreviewCreator = new CameraPreviewCreatorImpl();
        cameraSizeCalculator = new CameraSizeCalculatorImpl();
        useCacheValues = true;
        defaultCameraFace = CameraFace.FACE_REAR;
        defaultMediaType = MediaType.TYPE_PICTURE;
        defaultMediaQuality = MediaQuality.QUALITY_HIGH;
        defaultAspectRatio = AspectRatio.of(4, 3);
        isVoiceEnable = true;
        isAutoFocus = true;
        defaultFlashMode = FlashMode.FLASH_AUTO;
    }

    public static ConfigurationProvider get() {
        if (configurationProvider == null) {
            synchronized (ConfigurationProvider.class) {
                if (configurationProvider == null) {
                    configurationProvider = new ConfigurationProvider();
                }
            }
        }
        return configurationProvider;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConfigurationProvider prepareCamera2(Context context) {
        if (!camera2Prepared.get()) {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            try {
                assert cameraManager != null;
                final String[] ids = cameraManager.getCameraIdList();
                numberOfCameras = ids.length;
                for (String id : ids) {
                    final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraIdCamera2.put(CameraFace.FACE_FRONT, id);
                        Integer iFrontCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        cameraOrientations.put(CameraFace.FACE_FRONT, iFrontCameraOrientation == null ? 0 : iFrontCameraOrientation);
                        cameraCharacteristics.put(CameraFace.FACE_FRONT, characteristics);
                        streamConfigurationMaps.put(CameraFace.FACE_FRONT, characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP));
                    } else if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK){
                        cameraIdCamera2.put(CameraFace.FACE_REAR, id);
                        Integer iRearCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        cameraOrientations.put(CameraFace.FACE_REAR, iRearCameraOrientation == null ? 0 : iRearCameraOrientation);
                        cameraCharacteristics.put(CameraFace.FACE_REAR, characteristics);
                        streamConfigurationMaps.put(CameraFace.FACE_REAR, characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP));
                    }
                }
                camera2Prepared.set(true);
            } catch (Exception e) {
                XLog.e(TAG, "initCameraInfo error " + e);
            }
        }
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int getNumberOfCameras(Context context) {
        return prepareCamera2(context).numberOfCameras;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public String getCameraId(Context context, @CameraFace int cameraFace) {
        return prepareCamera2(context).cameraIdCamera2.get(cameraFace);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CameraCharacteristics getCameraCharacteristics(Context context, @CameraFace int cameraFace) {
        return prepareCamera2(context).cameraCharacteristics.get(cameraFace);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int getCameraOrientation(Context context, @CameraFace int cameraFace) {
        return prepareCamera2(context).cameraOrientations.get(cameraFace);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public StreamConfigurationMap getStreamConfigurationMap(Context context, @CameraFace int cameraFace) {
        return prepareCamera2(context).streamConfigurationMaps.get(cameraFace);
    }

    /**
     * Get standard supported sizes for camera1 of given condition.
     *
     * @param camera     camera
     * @param cameraFace camera face
     * @param sizeFor    camera size for
     * @return           the size
     */
    public List<Size> getSizes(android.hardware.Camera camera,
                               @CameraFace int cameraFace,
                               @CameraSizeFor int sizeFor) {
        // calculate hash of map
        int hash = cameraFace | sizeFor | CameraType.TYPE_CAMERA1;
        XLog.d(TAG, "getSizes hash : " + Integer.toHexString(hash));
        // try to get sizes from cache first.
        if (useCacheValues) {
            List<Size> sizes = sizeMap.get(hash);
            if (sizes != null) {
                return sizes;
            }
        }
        // get sizes from parameters
        android.hardware.Camera.Parameters parameters = camera.getParameters();
        List<Size> sizes;
        switch (sizeFor) {
            case CameraSizeFor.SIZE_FOR_PICTURE:
                sizes = Size.fromList(parameters.getSupportedPictureSizes());
                break;
            case CameraSizeFor.SIZE_FOR_PREVIEW:
                sizes = Size.fromList(parameters.getSupportedPreviewSizes());
                break;
            case CameraSizeFor.SIZE_FOR_VIDEO:
                sizes = Size.fromList(parameters.getSupportedVideoSizes());
                break;
            default:
                throw new IllegalArgumentException("Unsupported size for " + sizeFor);
        }
        // cache the sizes in memory
        if (useCacheValues) {
            sizeMap.put(hash, sizes);
        }
        return sizes;
    }

    /**
     * Get standard supported room ratios for camera1.
     *
     * @param camera     camera
     * @param cameraFace camera face
     * @return           supported zoom ratios
     */
    public List<Float> getZoomRatios(android.hardware.Camera camera,
                                     @CameraFace int cameraFace) {
        // calculate hash of map
        int hash = cameraFace | CameraType.TYPE_CAMERA1;
        XLog.d(TAG, "getZoomRatios hash : " + Integer.toHexString(hash));
        // try to get ratios from cache first.
        if (useCacheValues) {
            List<Float> zoomRatios = ratioMap.get(hash);
            if (zoomRatios != null) {
                return zoomRatios;
            }
        }
        // calculate room ratios
        List<Integer> ratios = camera.getParameters().getZoomRatios();
        List<Float> result = new ArrayList<>(ratios.size());
        for (Integer ratio : ratios) {
            result.add(ratio * 0.01f);
        }
        // cache room ratios
        if (useCacheValues) {
            ratioMap.put(hash, result);
        }
        return result;
    }

    /**
     * Get standard support sizes for camera2 of given condition.
     *
     * @param configurationMap the configuration map
     * @param cameraFace       the camera face
     * @param sizeFor          the camera size for
     * @return                 the supported sizes
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public List<Size> getSizes(StreamConfigurationMap configurationMap,
                               @CameraFace int cameraFace,
                               @CameraSizeFor int sizeFor) {
        // calculate hash
        int hash = cameraFace | sizeFor | CameraType.TYPE_CAMERA2;
        XLog.d(TAG, "getSizes hash : " + Integer.toHexString(hash));
        // try to get sizes from cache
        if (useCacheValues) {
            List<Size> sizes = sizeMap.get(hash);
            if (sizes != null) {
                return sizes;
            }
        }
        // calculate camera sizes
        List<Size> sizes;
        switch (sizeFor) {
            case CameraSizeFor.SIZE_FOR_PICTURE:
                sizes = Size.fromList(configurationMap.getOutputSizes(ImageFormat.JPEG));
                break;
            case CameraSizeFor.SIZE_FOR_PREVIEW:
                sizes = Size.fromList(configurationMap.getOutputSizes(SurfaceTexture.class));
                break;
            case CameraSizeFor.SIZE_FOR_VIDEO:
                sizes = Size.fromList(configurationMap.getOutputSizes(MediaRecorder.class));
                break;
            default:
                throw new IllegalArgumentException("Unsupported size for " + sizeFor);
        }
        // cache sizes
        if (useCacheValues) {
            sizeMap.put(hash, sizes);
        }
        return sizes;
    }

    /*--------------------------------------- Setters and Getters Region ---------------------------------------*/

    public CameraManagerCreator getCameraManagerCreator() {
        return cameraManagerCreator;
    }

    public void setCameraManagerCreator(CameraManagerCreator cameraManagerCreator) {
        this.cameraManagerCreator = cameraManagerCreator;
    }

    public CameraPreviewCreator getCameraPreviewCreator() {
        return cameraPreviewCreator;
    }

    public void setCameraPreviewCreator(CameraPreviewCreator cameraPreviewCreator) {
        this.cameraPreviewCreator = cameraPreviewCreator;
    }

    public CameraSizeCalculator getCameraSizeCalculator() {
        return cameraSizeCalculator;
    }

    public void setCameraSizeCalculator(CameraSizeCalculator cameraSizeCalculator) {
        this.cameraSizeCalculator = cameraSizeCalculator;
    }

    public boolean isUseCacheValues() {
        return useCacheValues;
    }

    public void setUseCacheValues(boolean useCacheValues) {
        this.useCacheValues = useCacheValues;
    }

    @CameraFace
    public int getDefaultCameraFace() {
        return defaultCameraFace;
    }

    public void setDefaultCameraFace(@CameraFace int defaultCameraFace) {
        this.defaultCameraFace = defaultCameraFace;
    }

    @MediaType
    public int getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(@MediaType int defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

    @MediaQuality
    public int getDefaultMediaQuality() {
        return defaultMediaQuality;
    }

    public void setDefaultMediaQuality(@MediaQuality int defaultMediaQuality) {
        this.defaultMediaQuality = defaultMediaQuality;
    }

    public AspectRatio getDefaultAspectRatio() {
        return defaultAspectRatio;
    }

    /**
     * Set default camera aspect ratio
     *
     * @param defaultAspectRatio the camera aspect ratio
     */
    public void setDefaultAspectRatio(AspectRatio defaultAspectRatio) {
        this.defaultAspectRatio = defaultAspectRatio;
    }

    public boolean isVoiceEnable() {
        return isVoiceEnable;
    }

    public void setVoiceEnable(boolean voiceEnable) {
        this.isVoiceEnable = voiceEnable;
    }

    public boolean isAutoFocus() {
        return isAutoFocus;
    }

    public void setAutoFocus(boolean autoFocus) {
        isAutoFocus = autoFocus;
    }

    @FlashMode
    public int getDefaultFlashMode() {
        return defaultFlashMode;
    }

    public void setDefaultFlashMode(@FlashMode int defaultFlashMode) {
        this.defaultFlashMode = defaultFlashMode;
    }

    public long getDefaultVideoFileSize() {
        return defaultVideoFileSize;
    }

    public void setDefaultVideoFileSize(long defaultVideoFileSize) {
        this.defaultVideoFileSize = defaultVideoFileSize;
    }

    public int getDefaultVideoDuration() {
        return defaultVideoDuration;
    }

    public void setDefaultVideoDuration(int defaultVideoDuration) {
        this.defaultVideoDuration = defaultVideoDuration;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
        XLog.setDebug(debug);
    }

    @DeviceDefaultOrientation
    public int getDeviceDefaultOrientation() {
        return deviceDefaultOrientation;
    }

    public void setDeviceDefaultOrientation(@DeviceDefaultOrientation int deviceDefaultOrientation) {
        this.deviceDefaultOrientation = deviceDefaultOrientation;
    }

    @SensorPosition
    public int getSensorPosition() {
        return sensorPosition;
    }

    public void setSensorPosition(@SensorPosition int sensorPosition) {
        this.sensorPosition = sensorPosition;
    }

    public int getDegrees() {
        return degrees;
    }

    public void setDegrees(int degrees) {
        this.degrees = degrees;
    }
}
