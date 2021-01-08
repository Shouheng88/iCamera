package me.shouheng.icamera.listener

import android.content.Context
import android.util.SparseIntArray
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/17 22:26
 */
abstract class DisplayOrientationDetector protected constructor(context: Context?) {
    private val orientationEventListener: OrientationEventListener

    companion object {
        private val DISPLAY_ORIENTATIONS = SparseIntArray()

        init {
            DISPLAY_ORIENTATIONS.put(Surface.ROTATION_0, 0)
            DISPLAY_ORIENTATIONS.put(Surface.ROTATION_90, 90)
            DISPLAY_ORIENTATIONS.put(Surface.ROTATION_180, 180)
            DISPLAY_ORIENTATIONS.put(Surface.ROTATION_270, 270)
        }
    }

    private var display: Display? = null

    var lastKnownDisplayOrientation = 0
        private set

    fun enable(display: Display) {
        this.display = display
        orientationEventListener.enable()
        // Immediately dispatch the first callback
        dispatchOnDisplayOrientationChanged(DISPLAY_ORIENTATIONS[display.rotation])
    }

    fun disable() {
        orientationEventListener.disable()
        display = null
    }

    private fun dispatchOnDisplayOrientationChanged(displayOrientation: Int) {
        lastKnownDisplayOrientation = displayOrientation
        onDisplayOrientationChanged(displayOrientation)
    }

    /**
     * Called when display orientation is changed.
     *
     * @param displayOrientation One of 0, 90, 180, and 270.
     */
    abstract fun onDisplayOrientationChanged(displayOrientation: Int)

    init {
        orientationEventListener = object : OrientationEventListener(context) {
            private var mLastKnownRotation = -1
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN || display == null) {
                    return
                }
                val rotation = display!!.rotation
                if (mLastKnownRotation != rotation) {
                    mLastKnownRotation = rotation
                    dispatchOnDisplayOrientationChanged(DISPLAY_ORIENTATIONS[rotation])
                }
            }
        }
    }
}