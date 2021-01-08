package me.shouheng.icamera.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.FrameLayout
import me.shouheng.icamera.CameraView
import me.shouheng.icamera.R
import me.shouheng.icamera.listener.OnMoveListener
import me.shouheng.icamera.util.XLog
import me.shouheng.icamera.util.XUtils.dp2Px
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Touch focus maker layout for camera preview.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/15 8:07
 */
class FocusMarkerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), OnTouchListener {
    private val focusMarkerContainer: FrameLayout
    private val ivFill: AppCompatImageView
    private var useTouchFocus = true
    private var touchZoomEnable = true
    private var touchAngle = 0
    private var scaleRate = DEFAULT_SCALE_RATE
    private var onMoveListener: OnMoveListener? = null
    private var cameraView: CameraView? = null
    private var multiTouch = false
    private var fingerSpacing = 0f
    private var mDownViewX = 0
    private var mDownViewY = 0
    private var dx = 0
    private var dy = 0

    // todo the camera should focus in the point of focusing
    private fun focus(mx: Float, my: Float) {
        val x = (mx - focusMarkerContainer.width / 2).toInt()
        val y = (my - focusMarkerContainer.width / 2).toInt()
        focusMarkerContainer.translationX = x.toFloat()
        focusMarkerContainer.translationY = y.toFloat()
        focusMarkerContainer.animate().setListener(null).cancel()
        ivFill.animate().setListener(null).cancel()
        ivFill.scaleX = 0f
        ivFill.scaleY = 0f
        ivFill.alpha = 1f
        focusMarkerContainer.scaleX = 1.36f
        focusMarkerContainer.scaleY = 1.36f
        focusMarkerContainer.alpha = 1f
        focusMarkerContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(0)
            .setDuration(330)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    focusMarkerContainer.animate()
                        .alpha(0f)
                        .setStartDelay(50)
                        .setDuration(100)
                        .setListener(null)
                        .start()
                }
            }).start()
        ivFill.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(330)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ivFill.animate().alpha(0f).setDuration(100).setListener(null).start()
                }
            }).start()
    }

    /** To listener the move event on the maker */
    fun setOnMoveListener(onMoveListener: OnMoveListener?) {
        this.onMoveListener = onMoveListener
    }

    fun setCameraView(cameraView: CameraView?) {
        this.cameraView = cameraView
    }

    fun setTouchAngle(touchAngle: Int) {
        this.touchAngle = touchAngle
    }

    /** The scale rate every time when zoom was invoked */
    fun setScaleRate(scaleRate: Int) {
        this.scaleRate = scaleRate
    }

    /** Enable multi-touch to control zoom of camera */
    fun setTouchZoomEnable(touchZoomEnable: Boolean) {
        this.touchZoomEnable = touchZoomEnable
    }

    /** Was touch focus enabled */
    fun setUseTouchFocus(useTouchFocus: Boolean) {
        this.useTouchFocus = useTouchFocus
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val action = event.action
        if (event.pointerCount > 1) {
            multiTouch = true
            if (!touchZoomEnable) {
                return true
            }
            val currentFingerSpacing = getFingerSpacing(event)
            if (fingerSpacing != 0f) {
                try {
                    if (cameraView != null) {
                        val maxZoom = (cameraView!!.maxZoom * 100).toInt()
                        var zoom = (cameraView!!.zoom * 100).toInt()
                        if (fingerSpacing < currentFingerSpacing && zoom < maxZoom) {
                            zoom += scaleRate
                        } else if (currentFingerSpacing < fingerSpacing && zoom > 0) {
                            zoom -= scaleRate
                        }
                        cameraView!!.zoom = zoom * 0.01f
                    }
                } catch (e: Exception) {
                    XLog.e(TAG, "onTouch error : $e")
                }
            }
            fingerSpacing = currentFingerSpacing
        } else {
            if (event.pointerCount == 1) {
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                if (action == MotionEvent.ACTION_DOWN) {
                    mDownViewX = x
                    mDownViewY = y
                }
                if (action == MotionEvent.ACTION_MOVE) {
                    dx = x - mDownViewX
                    dy = y - mDownViewY
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (multiTouch) {
                        multiTouch = false
                    } else {
                        if (abs(dx) > 100 && touchAngle == 0) {
                            notifyMove(dx < -100)
                            return true
                        }
                        if (abs(dy) > 100 && touchAngle == 90) {
                            notifyMove(dx >= -100)
                            return true
                        }
                        if (abs(dy) > 100 && touchAngle == -90) {
                            notifyMove(dx <= -100)
                            return true
                        }
                        if (useTouchFocus) {
                            focus(event.x, event.y)
                        }
                    }
                }
            }
        }
        return true
    }

    private fun notifyMove(left: Boolean) {
        onMoveListener?.onMove(left)
    }

    private fun getFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y.toDouble()).toFloat()
    }

    companion object {
        private const val TAG = "FocusMarkerLayout"
        const val DEFAULT_SCALE_RATE = 5
    }

    init {
        val dp55 = dp2Px(context, 55f)
        focusMarkerContainer = FrameLayout(context)
        focusMarkerContainer.layoutParams = LayoutParams(dp55, dp55)
        focusMarkerContainer.alpha = 0f
        addView(focusMarkerContainer)
        ivFill = AppCompatImageView(context)
        ivFill.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        ivFill.setBackgroundResource(R.drawable.ic_focus_marker_fill)
        focusMarkerContainer.addView(ivFill)
        val ivOutline = AppCompatImageView(context)
        ivOutline.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        ivOutline.setBackgroundResource(R.drawable.ic_focus_marker_outline)
        focusMarkerContainer.addView(ivOutline)
        setOnTouchListener(this)
    }
}