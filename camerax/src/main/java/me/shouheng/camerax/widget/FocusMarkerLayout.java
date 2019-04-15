package me.shouheng.camerax.widget;

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
import me.shouheng.camerax.R;
import me.shouheng.camerax.listener.OnMoveListener;
import me.shouheng.camerax.util.Utils;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/15 8:07
 */
public class FocusMarkerLayout extends FrameLayout implements View.OnTouchListener {

    private FrameLayout focusMarkerContainer;
    private AppCompatImageView ivFill;

    private boolean multiTouch;
    private int mDownViewX, mDownViewY, dx, dy;
    private int touchAngle = 0;
    private TouchEventDispatcher touchEventDispatcher;
    private OnMoveListener onMoveListener;

    public FocusMarkerLayout(@NonNull Context context) {
        this(context, null);
    }

    public FocusMarkerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocusMarkerLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int dp55 = Utils.dp2Px(context, 55);
        focusMarkerContainer = new FrameLayout(context);
        focusMarkerContainer.setLayoutParams(new FrameLayout.LayoutParams(dp55, dp55));
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
                        super.onAnimationEnd(animation);
                        focusMarkerContainer.animate().alpha(0).setStartDelay(50).setDuration(100).setListener(null).start();
                    }
                }).start();

        ivFill.animate().scaleX(1).scaleY(1).setDuration(330)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        ivFill.animate().alpha(0).setDuration(100).setListener(null).start();
                    }
                }).start();

    }

    public void setTouchEventDispatcher(TouchEventDispatcher touchEventDispatcher) {
        this.touchEventDispatcher = touchEventDispatcher;
    }

    public void setOnMoveListener(OnMoveListener onMoveListener) {
        this.onMoveListener = onMoveListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (this.touchEventDispatcher != null) {
            touchEventDispatcher.onTouch(event);
        }
        int action = event.getAction();
        if (event.getPointerCount() > 1) {
            multiTouch = true;
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
                            if (onMoveListener != null) {
                                onMoveListener.onMove(dx < -100);
                            }
                            return true;
                        }

                        if (Math.abs(dy) > 100 && touchAngle == 90){
                            if (onMoveListener != null) {
                                onMoveListener.onMove(dx >= -100);
                            }
                            return true;
                        }

                        if (Math.abs(dy) > 100 && touchAngle == -90){
                            if (onMoveListener != null) {
                                onMoveListener.onMove(dx <= -100);
                            }
                            return true;
                        }

                        focus(event.getX(), event.getY());
                    }
                }
            }
        }
        return true;
    }

    public interface TouchEventDispatcher {
        void onTouch(MotionEvent event);
    }

}
