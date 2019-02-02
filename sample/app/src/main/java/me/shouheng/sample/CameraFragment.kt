package me.shouheng.sample

import android.os.Bundle
import android.widget.Toast
import com.google.android.cameraview.CameraView
import me.shouheng.camerax.listeners.StateListener
import me.shouheng.sample.databinding.FragmentCameraBinding
import java.io.File
import java.io.FileOutputStream

/**
 * Created on 2019/2/1.
 */
class CameraFragment : CommonFragment<FragmentCameraBinding>() {

    override fun getLayoutResId() = R.layout.fragment_camera

    override fun doCreateView(savedInstanceState: Bundle?) {
        testCameraX()
    }

    fun testCameraView() {
        // 添加监听方法
        binding.cv.addCallback(callback)
        binding.cv.setOnMoveListener(onMoveListener)
        binding.cv.setOpenVoice(true)

        binding.btnShot.setOnClickListener({ binding.cv.takePicture() })
    }

    fun testCameraX() {
        // 添加监听方法
        binding.cvx.addStateListener(object : StateListener {
            override fun onVideoToken() {
                // empty
            }

            override fun onPictureTaken(cameraView: me.shouheng.camerax.CameraView?, data: ByteArray?) {
                // empty
            }

            override fun onTouch() {
                // empty
            }

            override fun onPreviewFrame(
                cameraView: me.shouheng.camerax.CameraView?,
                data: ByteArray?,
                width: Int,
                height: Int,
                format: Int
            ) {
                // empty
            }

            override fun onPermissionDenied(
                cameraView: me.shouheng.camerax.CameraView?,
                permissions: Array<out String>?
            ) {
                // empty
            }

            override fun onCameraOpened(cameraView: me.shouheng.camerax.CameraView?) {

            }

            override fun onCameraClosed(cameraView: me.shouheng.camerax.CameraView?) {
                // empty
            }
        })

//        binding.btnShot.setOnClickListener({ binding.cv.takePicture() })
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            // 恢复预览
            binding.cv.resumePreview()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isHidden) {
            return
        }
        // 开启相机
//        binding.cv.start()
        binding.cvx.start()
    }

    override fun onPause() {
        super.onPause()
        // 关闭相机
//        binding.cv.stop()
    }

    private val onMoveListener = CameraView.OnMoveListener {
        println("------ onMove " + (if (it) "Left" else "Right"))
    }

    private val callback = object : CameraView.Callback() {
        override fun onCameraOpened(cameraView: CameraView?) {
            /* TODO 这个参数是传入进去之后做了什么处理？ */
            binding.cv.setWidthHeightRatio(3, 4)
            binding.cv.setPictureSize(1000, 1333)
        }

        override fun onCameraClosed(cameraView: CameraView?) {
            super.onCameraClosed(cameraView)
        }

        override fun onPictureTaken(cameraView: CameraView?, data: ByteArray?) {
            binding.cv.resumePreview()
            val path = System.currentTimeMillis().toString() + ".png"
            val file = File(context!!.getExternalFilesDir(null), path)
            val out = FileOutputStream(file)
            out.write(data)
            out.close()
            Toast.makeText(context, "Wrote to $path", Toast.LENGTH_SHORT).show()
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