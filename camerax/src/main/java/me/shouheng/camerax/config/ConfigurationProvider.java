package me.shouheng.camerax.config;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.SparseArray;
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
    private SparseArray<List<Size>> sizeMap;
    private SparseArray<List<Float>> ratioMap;

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

    public List<Size> getSizes(android.hardware.Camera camera, @Camera.Face int cameraFace, @Camera.SizeFor int sizeFor) {
        int hash = cameraFace | sizeFor | Camera.TYPE_CAMERA1;
        if (useCacheValues) {
            List<Size> sizes = sizeMap.get(hash);
            if (sizes != null) {
                return sizes;
            }
        }
        List<Size> sizes;
        switch (sizeFor) {
            case Camera.SIZE_FOR_PICTURE:
                sizes = Size.fromList(camera.getParameters().getSupportedPictureSizes());
                break;
            case Camera.SIZE_FOR_PREVIEW:
                sizes = Size.fromList(camera.getParameters().getSupportedPreviewSizes());
                break;
            case Camera.SIZE_FOR_VIDEO:
                sizes = Size.fromList(camera.getParameters().getSupportedVideoSizes());
                break;
            default:
                throw new IllegalArgumentException("Unsupported size for " + sizeFor);
        }
        if (useCacheValues) {
            sizeMap.put(hash, sizes);
        }
        return sizes;
    }

    public List<Float> getZoomRatios(android.hardware.Camera camera, @Camera.Face int cameraFace) {
        int hash = cameraFace | Camera.TYPE_CAMERA1;
        if (useCacheValues) {
            List<Float> zoomRatios = ratioMap.get(hash);
            if (zoomRatios != null) {
                return zoomRatios;
            }
        }
        List<Integer> ratios = camera.getParameters().getZoomRatios();
        List<Float> result = new ArrayList<>(ratios.size());
        for (Integer ratio : ratios) {
            result.add(ratio * 0.01f);
        }
        if (useCacheValues) {
            ratioMap.put(hash, result);
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public List<Size> getSizes(StreamConfigurationMap configurationMap, @Camera.Face int cameraFace, @Camera.SizeFor int sizeFor) {
        int hash = cameraFace | sizeFor | Camera.TYPE_CAMERA2;
        if (useCacheValues) {
            List<Size> sizes = sizeMap.get(hash);
            if (sizes != null) {
                return sizes;
            }
        }
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
        if (useCacheValues) {
            sizeMap.put(hash, sizes);
        }
        return sizes;
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
    }

}
