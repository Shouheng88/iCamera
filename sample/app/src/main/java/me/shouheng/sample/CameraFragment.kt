package me.shouheng.sample

import android.os.Bundle
import com.google.android.cameraview.CameraView
import me.shouheng.sample.databinding.FragmentCameraBinding

/**
 * Created on 2019/2/1.
 */
class CameraFragment : CommonFragment<FragmentCameraBinding>() {

    override fun getLayoutResId() = R.layout.fragment_camera

    override fun doCreateView(savedInstanceState: Bundle?) {
        binding.cv.addCallback(callback)
        binding.cv.setOnMoveListener(onMoveListener)
        binding.cv.setOpenVoice(true)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            binding.cv.resumePreview()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isHidden) {
            return
        }
        binding.cv.start()
    }

    override fun onPause() {
        super.onPause()
        binding.cv.stop()
    }

    private val onMoveListener = CameraView.OnMoveListener {
        println("------ onMove " + (if (it) "Left" else "Right"))
    }

    private val callback = object : CameraView.Callback() {
        override fun onCameraOpened(cameraView: CameraView?) {
            super.onCameraOpened(cameraView)
        }

        override fun onCameraClosed(cameraView: CameraView?) {
            super.onCameraClosed(cameraView)
        }

        override fun onPictureTaken(cameraView: CameraView?, data: ByteArray?) {
            super.onPictureTaken(cameraView, data)
        }

        override fun notPermission() {
            super.notPermission()
        }

        override fun onPreviewFrame(cameraView: CameraView?, data: ByteArray?, width: Int, height: Int, format: Int) {
            super.onPreviewFrame(cameraView, data, width, height, format)
        }

        override fun onTouchMove(left: Boolean) {
            super.onTouchMove(left)
        }
    }

}