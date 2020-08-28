package me.shouheng.icamera.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import me.shouheng.icamera.CameraView;
import me.shouheng.icamera.R;
import me.shouheng.icamera.listener.OnMoveListener;
import me.shouheng.icamera.util.XLog;
import me.shouheng.icamera.util.XUtils;

/**
 * Touch focus maker layout for camera preview.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/15 8:07
 */
public class FocusMarkerLayout extends FrameLayout implements View.OnTouchListener {

    private static final String TAG = "FocusMarkerLayout";

    public static final int DEFAULT_SCALE_RATE = 5;

    private FrameLayout focusMarkerContainer;
    private AppCompatImageView ivFill;

    private boolean useTouchFocus = true;
    private boolean touchZoomEnable = true;
    private int touchAngle = 0;
    private int scaleRate = DEFAULT_SCALE_RATE;
    private OnMoveListener onMoveListener;
    private CameraView cameraView;

    private boolean multiTouch;
    private float fingerSpacing;
    private int mDownViewX;
    private int mDownViewY;
    private int dx;
    private int dy;

    public FocusMarkerLayout(@NonNull Context context) {
        this(context, null);
    }

    public FocusMarkerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocusMarkerLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int dp55 = XUtils.dp2Px(context, 55);
        focusMarkerContainer = new FrameLayout(context);
        focusMarkerContainer.setLayoutParams(new FrameLayout.LayoutParams(dp55, dp55));
        focusMarkerContainer.setAlpha(0);
        addView(focusMarkerContainer);

        ivFill = new AppCompatImageView(context);
        ivFill.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivFill.setBackgroundResource(R.drawable.ic_focus_marker_fill);
        focusMarkerContainer.addView(ivFill);

        AppCompatImageView ivOutline = new AppCompatImageView(context);
        ivOutline.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivOutline.setBackgroundResource(R.drawable.ic_focus_marker_outline);
        focusMarkerContainer.addView(ivOutline);

        setOnTouchListener(this);
    }

    // todo the camera should focus in the point of focusing
    private void focus(float mx, float my) {
        int x = (int) (mx - focusMarkerContainer.getWidth() / 2);
        int y = (int) (my - focusMarkerContainer.getWidth() / 2);

        focusMarkerContainer.setTranslationX(x);
        focusMarkerContainer.setTranslationY(y);

        focusMarkerContainer.animate().setListener(null).cancel();
        ivFill.animate().setListener(null).cancel();

        ivFill.setScaleX(0);
        ivFill.setScaleY(0);
        ivFill.setAlpha(1f);

        focusMarkerContainer.setScaleX(1.36f);
        focusMarkerContainer.setScaleY(1.36f);
        focusMarkerContainer.setAlpha(1f);

        focusMarkerContainer.animate().scaleX(1).scaleY(1).setStartDelay(0).setDuration(330)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        focusMarkerContainer.animate().alpha(0).setStartDelay(50).setDuration(100).setListener(null).start();
                    }
                }).start();

        ivFill.animate().scaleX(1).scaleY(1).setDuration(330)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ivFill.animate().alpha(0).setDuration(100).setListener(null).start();
                    }
                }).start();

    }

    /**
     * To listener the move event on the maker
     *
     * @param onMoveListener the move listener
     */
    public void setOnMoveListener(OnMoveListener onMoveListener) {
        this.onMoveListener = onMoveListener;
    }

    public void setCameraView(CameraView cameraView) {
        this.cameraView = cameraView;
    }

    public void setTouchAngle(int touchAngle) {
        this.touchAngle = touchAngle;
    }

    /**
     * The scale rate every time when zoom was invoked
     *
     * @param scaleRate scale rate
     */
    public void setScaleRate(int scaleRate) {
        this.scaleRate = scaleRate;
    }

    /**
     * Enable multi-touch to control zoom of camera
     *
     * @param touchZoomEnable touch zoom enabled or not, true to use
     */
    public void setTouchZoomEnable(boolean touchZoomEnable) {
        this.touchZoomEnable = touchZoomEnable;
    }

    /**
     * Was touch focus enabled
     *
     * @param useTouchFocus use touch focus or not, true to use
     */
    public void setUseTouchFocus(boolean useTouchFocus) {
        this.useTouchFocus = useTouchFocus;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (event.getPointerCount() > 1) {
            multiTouch = true;
            if (!touchZoomEnable) {
                return true;
            }
            float currentFingerSpacing = getFingerSpacing(event);
            if (fingerSpacing != 0) {
                try {
                    if (cameraView != null) {
                        int maxZoom = (int) (cameraView.getMaxZoom() * 100);
                        int zoom = (int) (cameraView.getZoom() * 100);
                        if (fingerSpacing < currentFingerSpacing && zoom < maxZoom) {
                            zoom += scaleRate;
                        } else if (currentFingerSpacing < fingerSpacing && zoom > 0) {
                            zoom -= scaleRate;
                        }
                        cameraView.setZoom(zoom * 0.01f);
                    }
                } catch (Exception e) {
                    XLog.e(TAG, "onTouch error : " + e);
                }
            }
            fingerSpacing = currentFingerSpacing;
        } else {
            if (event.getPointerCount() == 1) {
                int x = (int) event.getRawX();
                int y = (int) event.getRawY();
                if (action == MotionEvent.ACTION_DOWN) {
                    mDownViewX = x;
                    mDownViewY = y;
                }
                if (action == MotionEvent.ACTION_MOVE) {
                    dx = x - mDownViewX;
                    dy = y - mDownViewY;
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (multiTouch) {
                        multiTouch = false;
                    } else {
                        if (Math.abs(dx) > 100 && touchAngle == 0) {
                            notifyMove(dx < -100);
                            return true;
                        }
                        if (Math.abs(dy) > 100 && touchAngle == 90){
                            notifyMove(dx >= -100);
                            return true;
                        }
                        if (Math.abs(dy) > 100 && touchAngle == -90){
                            notifyMove(dx <= -100);
                            return true;
                        }
                        if (useTouchFocus) {
                            focus(event.getX(), event.getY());
                        }
                    }
                }
            }
        }
        return true;
    }

    private void notifyMove(boolean left) {
        if (onMoveListener != null) {
            onMoveListener.onMove(left);
        }
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

}
