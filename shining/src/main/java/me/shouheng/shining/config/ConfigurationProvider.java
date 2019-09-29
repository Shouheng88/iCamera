package me.shouheng.shining.config;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.SparseArray;
import me.shouheng.shining.config.calculator.CameraSizeCalculator;
import me.shouheng.shining.config.calculator.impl.CameraSizeCalculatorImpl;
import me.shouheng.shining.config.creator.CameraManagerCreator;
import me.shouheng.shining.config.creator.CameraPreviewCreator;
import me.shouheng.shining.config.creator.impl.CameraManagerCreatorImpl;
import me.shouheng.shining.config.creator.impl.CameraPreviewCreatorImpl;
import me.shouheng.shining.config.sizes.AspectRatio;
import me.shouheng.shining.config.sizes.Size;
import me.shouheng.shining.enums.Camera;
import me.shouheng.shining.enums.Flash;
import me.shouheng.shining.enums.Media;
import me.shouheng.shining.util.SLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Global configuration provider for Shining camera.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:44
 */
public class ConfigurationProvider {

    // singleton
    private static volatile ConfigurationProvider configurationProvider;

    /**
     * The creator for {@link me.shouheng.shining.manager.CameraManager}.
     */
    private CameraManagerCreator cameraManagerCreator;

    /**
     * The creator for {@link me.shouheng.shining.preview.CameraPreview}.
     */
    private CameraPreviewCreator cameraPreviewCreator;

    /**
     * The calculator for camera size.
     */
    private CameraSizeCalculator cameraSizeCalculator;

    /**
     * Whether use memory cache in library. default is true.
     */
    private boolean useCacheValues;

    /**
     * The sizes map from a int value, which was calculated from:
     * hash = {@link Camera.Face} | {@link Camera.SizeFor} | {@link Camera.Type}
     */
    private SparseArray<List<Size>> sizeMap;

    /**
     * The room ratios map from a int value, which was calculated from:
     * hash = {@link Camera.Face} | {@link Camera.Type}
     */
    private SparseArray<List<Float>> ratioMap;

    @Camera.Face private int defaultCameraFace;
    @Media.Type private int defaultMediaType;
    @Media.Quality private int defaultMediaQuality;
    private AspectRatio defaultAspectRatio;
    private boolean isVoiceEnable;
    private boolean isAutoFocus;
    @Flash.FlashMode private int defaultFlashMode;
    private long defaultVideoFileSize = -1;
    private int defaultVideoDuration = -1;

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
        defaultCameraFace = Camera.FACE_REAR;
        defaultMediaType = Media.TYPE_PICTURE;
        defaultMediaQuality = Media.QUALITY_HIGH;
        defaultAspectRatio = AspectRatio.of(3, 4);
        isVoiceEnable = true;
        isAutoFocus = true;
        defaultFlashMode = Flash.FLASH_AUTO;
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

    /**
     * Get standard supported sizes for camera1 of given condition.
     *
     * @param camera     camera
     * @param cameraFace camera face
     * @param sizeFor    camera size for
     * @return           the size
     */
    public List<Size> getSizes(android.hardware.Camera camera, @Camera.Face int cameraFace, @Camera.SizeFor int sizeFor) {
        // calculate hash of map
        int hash = cameraFace | sizeFor | Camera.TYPE_CAMERA1;
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
            case Camera.SIZE_FOR_PICTURE:
                sizes = Size.fromList(parameters.getSupportedPictureSizes());
                break;
            case Camera.SIZE_FOR_PREVIEW:
                sizes = Size.fromList(parameters.getSupportedPreviewSizes());
                break;
            case Camera.SIZE_FOR_VIDEO:
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
    public List<Float> getZoomRatios(android.hardware.Camera camera, @Camera.Face int cameraFace) {
        // calculate hash of map
        int hash = cameraFace | Camera.TYPE_CAMERA1;
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
    public List<Size> getSizes(StreamConfigurationMap configurationMap, @Camera.Face int cameraFace, @Camera.SizeFor int sizeFor) {
        // calculate hash
        int hash = cameraFace | sizeFor | Camera.TYPE_CAMERA2;
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
            case Camera.SIZE_FOR_PICTURE:
                sizes = Size.fromList(configurationMap.getOutputSizes(ImageFormat.JPEG));
                break;
            case Camera.SIZE_FOR_PREVIEW:
                sizes = Size.fromList(configurationMap.getOutputSizes(SurfaceTexture.class));
                break;
            case Camera.SIZE_FOR_VIDEO:
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

    @Camera.Face
    public int getDefaultCameraFace() {
        return defaultCameraFace;
    }

    public void setDefaultCameraFace(@Camera.Face int defaultCameraFace) {
        this.defaultCameraFace = defaultCameraFace;
    }

    @Media.Type
    public int getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(@Media.Type int defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

    @Media.Quality
    public int getDefaultMediaQuality() {
        return defaultMediaQuality;
    }

    public void setDefaultMediaQuality(@Media.Quality int defaultMediaQuality) {
        this.defaultMediaQuality = defaultMediaQuality;
    }

    public AspectRatio getDefaultAspectRatio() {
        return defaultAspectRatio;
    }

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

    @Flash.FlashMode
    public int getDefaultFlashMode() {
        return defaultFlashMode;
    }

    public void setDefaultFlashMode(@Flash.FlashMode int defaultFlashMode) {
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
        SLogger.setDebug(debug);
    }

}
