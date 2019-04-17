package me.shouheng.camerax.config;

import me.shouheng.camerax.config.calculator.CameraSizeCalculator;
import me.shouheng.camerax.config.calculator.impl.CameraSizeCalculatorImpl;
import me.shouheng.camerax.config.creator.CameraManagerCreator;
import me.shouheng.camerax.config.creator.CameraPreviewCreator;
import me.shouheng.camerax.config.creator.impl.CameraManagerCreatorImpl;
import me.shouheng.camerax.config.creator.impl.CameraPreviewCreatorImpl;
import me.shouheng.camerax.config.sizes.AspectRatio;
import me.shouheng.camerax.config.sizes.Size;
import me.shouheng.camerax.enums.Camera;
import me.shouheng.camerax.enums.Flash;
import me.shouheng.camerax.enums.Media;

import java.util.ArrayList;
import java.util.List;

/**
 * Global configuration provider.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:44
 */
public class ConfigurationProvider {

    private static volatile ConfigurationProvider configurationProvider;

    private CameraManagerCreator cameraManagerCreator;
    private CameraPreviewCreator cameraPreviewCreator;
    private CameraSizeCalculator cameraSizeCalculator;

    private boolean useCacheValues;
    private List<Size> previewSizes;
    private List<Size> pictureSizes;
    private List<Size> videoSizes;
    private List<Float> zoomRatios;

    @Camera.Face
    private int defaultCameraFace;
    @Media.Type
    private int defaultMediaType;
    @Media.Quality
    private int defaultMediaQuality;
    private AspectRatio defaultAspectRatio;
    private boolean isVoiceEnable;
    private boolean isAutoFocus;
    @Flash.FlashMode
    private int defaultFlashMode;

    private boolean isDebug;

    private ConfigurationProvider() {
        if (configurationProvider != null) {
            throw new UnsupportedOperationException("U can't initialize me!");
        }
        initWithDefaultValues();
    }

    private void initWithDefaultValues() {
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

    public void clearCacchedValues() {
        previewSizes = null;
        pictureSizes = null;
        videoSizes = null;
        zoomRatios = null;
    }

    public List<Size> getPreviewSizes(android.hardware.Camera camera) {
        if (useCacheValues && previewSizes != null) {
            return previewSizes;
        }
        List<Size> sizes = Size.fromList(camera.getParameters().getSupportedPreviewSizes());
        if (useCacheValues) {
            previewSizes = sizes;
        }
        return sizes;
    }

    public List<Size> getPictureSizes(android.hardware.Camera camera) {
        if (useCacheValues && pictureSizes != null) {
            return pictureSizes;
        }
        List<Size> sizes = Size.fromList(camera.getParameters().getSupportedPictureSizes());
        if (useCacheValues) {
            pictureSizes = sizes;
        }
        return sizes;
    }

    public List<Size> getVideoSizes(android.hardware.Camera camera) {
        if (useCacheValues && videoSizes != null) {
            return videoSizes;
        }
        List<Size> sizes = Size.fromList(camera.getParameters().getSupportedVideoSizes());
        if (useCacheValues) {
            videoSizes = sizes;
        }
        return sizes;
    }

    public List<Float> getZoomRatios(android.hardware.Camera camera) {
        if (useCacheValues && zoomRatios != null) {
            return zoomRatios;
        }
        List<Integer> ratios = camera.getParameters().getZoomRatios();
        List<Float> result = new ArrayList<>(ratios.size());
        for (Integer ratio : ratios) {
            result.add(ratio * 0.01f);
        }
        if (useCacheValues) {
            zoomRatios = result;
        }
        return result;
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

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

}
