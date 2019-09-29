package me.shouheng.shining.manager;

import android.content.Context;
import me.shouheng.shining.config.sizes.AspectRatio;
import me.shouheng.shining.config.sizes.Size;
import me.shouheng.shining.config.sizes.SizeMap;
import me.shouheng.shining.enums.Camera;
import me.shouheng.shining.enums.Flash;
import me.shouheng.shining.enums.Media;
import me.shouheng.shining.listener.*;

import java.io.File;

/**
 * The camera manager interface.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:49
 */
public interface CameraManager {

    /**
     * Initialize camera manager.
     *
     * @param context the context
     */
    void initialize(Context context);

    /**
     * Open camera.
     *
     * @param cameraOpenListener camera open callback
     */
    void openCamera(CameraOpenListener cameraOpenListener);

    /**
     * Whether camera is opened.
     *
     * @return whether camera is opened
     */
    boolean isCameraOpened();

    /**
     * Get current camera face.
     *
     * @return camera face
     */
    @Camera.Face int getCameraFace();

    /**
     * Switch camera into given face.
     *
     * @param cameraFace camera face
     */
    void switchCamera(@Camera.Face int cameraFace);

    /**
     * Set current media type.
     *
     * @param mediaType the media type
     */
    void setMediaType(@Media.Type int mediaType);

    /**
     * Set whether the voice enabled
     *
     * @param voiceEnable voice enabled
     */
    void setVoiceEnable(boolean voiceEnable);

    /**
     * Whether the voice is enabled.
     *
     * @return whether voice enabled.
     */
    boolean isVoiceEnable();

    /**
     * Set whether auto focus enabled.
     *
     * @param autoFocus auto focus
     */
    void setAutoFocus(boolean autoFocus);

    /**
     * Whether auto focus
     *
     * @return whether auto focus.
     */
    boolean isAutoFocus();

    /**
     * Set flash mode
     *
     * @param flashMode flash mode
     */
    void setFlashMode(@Flash.FlashMode int flashMode);

    /**
     * Get current flash mode
     *
     * @return current flash mode
     */
    @Flash.FlashMode int getFlashMode();

    /**
     * Set zoom
     *
     * @param zoom zoom
     */
    void setZoom(float zoom);

    /**
     * Get zoom
     *
     * @return zoom
     */
    float getZoom();

    /**
     * Get max zoom supported by camera
     *
     * @return max zoom supported
     */
    float getMaxZoom();

    /**
     * Set desired size
     *
     * @param expectSize the size
     */
    void setExpectSize(Size expectSize);

    /**
     * Sed desired aspect ratio.
     *
     * @param expectAspectRatio the desired aspect ratio
     */
    void setExpectAspectRatio(AspectRatio expectAspectRatio);

    /**
     * Get current size for usage.
     *
     * @param sizeFor the size for
     * @return        the size
     */
    Size getSize(@Camera.SizeFor int sizeFor);

    /**
     * Get sizes map from aspect ratio to sizes.
     *
     * @param sizeFor sizes for
     * @return        size map
     */
    SizeMap getSizes(@Camera.SizeFor int sizeFor);

    /**
     * Get current aspect ratio.
     *
     * @return aspect ratio
     */
    AspectRatio getAspectRatio();

    /**
     * Set display orientation
     *
     * @param displayOrientation display orientation
     */
    void setDisplayOrientation(int displayOrientation);

    /**
     * Add camera size change callback
     *
     * @param cameraSizeListener camera size callback
     */
    void addCameraSizeListener(CameraSizeListener cameraSizeListener);

    /**
     * Take a picture
     *
     * @param cameraPhotoListener camera capture callback
     */
    void takePicture(CameraPhotoListener cameraPhotoListener);

    /**
     * Set maximum video size in bytes.
     *
     * @param videoFileSize video file size
     */
    void setVideoFileSize(long videoFileSize);

    /**
     * Set maximum video duration in ms.
     *
     * @param videoDuration video duration in ms
     */
    void setVideoDuration(int videoDuration);

    /**
     * Start video record
     *
     * @param file                video file to save
     * @param cameraVideoListener video record callback
     */
    void startVideoRecord(File file, CameraVideoListener cameraVideoListener);

    /**
     * Stop video record
     */
    void stopVideoRecord();

    /**
     * Resume preview
     */
    void resumePreview();

    /**
     * Close camera
     *
     * @param cameraCloseListener camera close callback
     */
    void closeCamera(CameraCloseListener cameraCloseListener);

    /**
     * Release camera, destroy thread etc.
     */
    void releaseCamera();
}
