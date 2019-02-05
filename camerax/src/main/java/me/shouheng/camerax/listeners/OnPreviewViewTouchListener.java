package me.shouheng.camerax.listeners;

import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import me.shouheng.camerax.utils.CameraHelper;
import me.shouheng.camerax.utils.Constants;
import me.shouheng.camerax.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version $Id: OnPreviewViewTouchListener, v 0.1 2019/2/5 22:09 shouh Exp$
 */
public class OnPreviewViewTouchListener implements View.OnTouchListener {

    private static final String TAG = "PreviewViewTouchListener";

    private Camera camera;
    private Camera.Parameters parameters;
    private Camera.AutoFocusCallback autoFocusCallback;

    private Handler handler = new Handler();
    private float fingerSpacing = 0;
    private float curZoom = 1.0f;
    private int maxZoom;

    public OnPreviewViewTouchListener(Camera camera, Camera.Parameters parameters, Camera.AutoFocusCallback autoFocusCallback) {
        this.camera = camera;
        this.parameters = parameters;
        this.autoFocusCallback = autoFocusCallback;
        this.maxZoom = parameters.getMaxZoom();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getPointerCount() > 1) {
            float curSpacing = CameraHelper.getFingerSpacing(event);
            if (fingerSpacing != 0) {
                try {
                    if (camera != null) {
                        if (parameters.isZoomSupported()) {
                            int zoom = parameters.getZoom();
                            if (fingerSpacing < curSpacing && zoom < maxZoom) {
                                zoom++;
                            } else if (curSpacing < fingerSpacing && zoom > 0) {
                                zoom--;
                            }
                            curZoom = zoom;
                            parameters.setZoom(zoom);
                            camera.setParameters(parameters);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtils.e(TAG, e.toString());
                }
            }
            fingerSpacing = curSpacing;
        } else {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                try {
                    if (camera != null) {
                        String focusMode = parameters.getFocusMode();
                        Rect rect = CameraHelper.calculateFocusArea(v, event.getX(), event.getY());
                        List<Camera.Area> meteringAreas = new ArrayList<>();
                        meteringAreas.add(new Camera.Area(rect, Constants.FOCUS_METERING_AREA_WEIGHT_DEFAULT));
                        if (parameters.getMaxNumFocusAreas() != 0
                                && focusMode != null
                                && (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) ||
                                focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ||
                                focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ||
                                focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                            parameters.setFocusAreas(meteringAreas);
                            if (parameters.getMaxNumMeteringAreas() > 0) {
                                parameters.setMeteringAreas(meteringAreas);
                            }
                            if (!parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                                return false; //cannot autoFocus
                            }

                            try {
                                camera.setParameters(parameters);
                                camera.autoFocus(new Camera.AutoFocusCallback() {
                                    @Override
                                    public void onAutoFocus(boolean success, Camera camera) {
                                        resetFocus(success, camera);
                                    }
                                });
                            } catch (Exception error) {
                                // ignore this exception
                                error.printStackTrace();
                            }
                        } else if (parameters.getMaxNumMeteringAreas() > 0) {
                            if (!parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                                return false; // cannot autoFocus
                            }
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                            parameters.setFocusAreas(meteringAreas);
                            parameters.setMeteringAreas(meteringAreas);

                            camera.setParameters(parameters);
                            camera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    resetFocus(success, camera);
                                }
                            });
                        } else {
                            camera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    if (autoFocusCallback != null) {
                                        autoFocusCallback.onAutoFocus(success, camera);
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtils.e(TAG, e.toString());
                }
            }
        }
        return true;
    }

    private void resetFocus(final boolean success, final Camera camera) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.cancelAutoFocus();
                    try {
                        // TODO Check the auto focus logic
                        Camera.Parameters params = camera.getParameters();
                        if (params != null && !params.getFocusMode().equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                params.setFocusAreas(null);
                                params.setMeteringAreas(null);
                                camera.setParameters(params);
                            }
                        }
                    } catch (Exception error) {
                        //ignore this exception
                    }
                    if (autoFocusCallback != null) {
                        autoFocusCallback.onAutoFocus(success, camera);
                    }
                }
            }
        }, Constants.DELAY_MILLIS_BEFORE_RESETTING_FOCUS);
    }
}
