package me.shouheng.sample

import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.PopupMenu
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import me.shouheng.camerax.config.ConfigurationProvider
import me.shouheng.camerax.config.sizes.Size
import me.shouheng.camerax.config.sizes.SizeMap
import me.shouheng.camerax.enums.Camera
import me.shouheng.camerax.enums.Flash
import me.shouheng.camerax.enums.Media
import me.shouheng.camerax.listener.CameraOpenListener
import me.shouheng.camerax.listener.CameraPhotoListener
import me.shouheng.camerax.listener.CameraSizeListener
import me.shouheng.camerax.listener.CameraVideoListener
import me.shouheng.camerax.util.Logger
import me.shouheng.sample.base.CommonActivity
import me.shouheng.sample.databinding.ActivityCameraBinding
import me.shouheng.sample.utils.FileUtils
import me.shouheng.sample.utils.ThemeUtils
import java.io.File

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:22
 */
class CameraActivity : CommonActivity<ActivityCameraBinding>() {

    private var isCapturePicture = true

    private var isCameraRecording = false

    companion object {
        const val TAG = "CameraActivity"
    }

    override fun getLayoutResId() = R.layout.activity_camera

    override fun doCreateView(savedInstanceState: Bundle?) {
        ThemeUtils.setStatusBarLightMode(window, false)
        configDrawer()
        configMain()
    }

    override fun useTransparentStatusBarForLollipop(): Boolean = true

    private fun configDrawer() {
        binding.scVoice.isChecked = ConfigurationProvider.get().isVoiceEnable
        binding.scVoice.setOnCheckedChangeListener { _, isChecked ->
            binding.cv.isVoiceEnable = isChecked
        }
        binding.scFocus.isChecked = ConfigurationProvider.get().isAutoFocus
        binding.scFocus.setOnCheckedChangeListener { _, isChecked ->
            binding.cv.isAutoFocus = isChecked
        }
        binding.scFlash.isChecked = ConfigurationProvider.get().defaultFlashMode == Flash.FLASH_ON
        binding.scFlash.setOnCheckedChangeListener { _, isChecked ->
            binding.cv.flashMode = if (isChecked) Flash.FLASH_ON else Flash.FLASH_OFF
        }
        binding.scTouchFocus.isChecked = true
        binding.scTouchFocus.setOnCheckedChangeListener { _, isChecked ->
            binding.cv.setUseTouchFocus(isChecked)
        }
        binding.scTouchZoom.isChecked = true
        binding.scTouchZoom.setOnCheckedChangeListener { _, isChecked ->
            binding.cv.setTouchZoomEnable(isChecked)
        }
        binding.tvPreviewSizes.setOnClickListener {
            showPopDialog(it, binding.cv.getSizes(Camera.SIZE_FOR_PREVIEW))
        }
        binding.tvPictureSizes.setOnClickListener {
            showPopDialog(it, binding.cv.getSizes(Camera.SIZE_FOR_PICTURE))
        }
        binding.tvVideoSizes.setOnClickListener {
            showPopDialog(it, binding.cv.getSizes(Camera.SIZE_FOR_VIDEO))
        }
        binding.tvSwitchCamera.setOnClickListener {
            binding.cv.switchCamera(if (binding.cv.cameraFace == Camera.FACE_FRONT) Camera.FACE_REAR else Camera.FACE_FRONT)
        }
    }

    private fun configMain() {
        binding.sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val room = 1 + (binding.cv.maxZoom - 1) * (1.0f * progress / seekBar!!.max)
                binding.cv.zoom = room
            }
        })
        binding.cv.setOnMoveListener {
            Toast.makeText(this@CameraActivity, if (it) "LEFT" else "RIGHT", Toast.LENGTH_SHORT).show()
        }
        binding.cv.addCameraSizeListener(object : CameraSizeListener {
            override fun onPreviewSizeUpdated(previewSize: Size?) {
                Logger.d(TAG, "onPreviewSizeUpdated : $previewSize")
            }

            override fun onVideoSizeUpdated(videoSize: Size?) {
                Logger.d(TAG, "onVideoSizeUpdated : $videoSize")
            }

            override fun onPictureSizeUpdated(pictureSize: Size?) {
                Logger.d(TAG, "onPictureSizeUpdated : $pictureSize")
            }
        })
    }

    private fun showPopDialog(view: View, sizes: SizeMap) {
        val pop = PopupMenu(this, view)
        val list = mutableListOf<Size>()
        sizes.values.forEach {
            list.addAll(it)
        }
        list.forEach {
            pop.menu.add(it.toString())
        }
        pop.gravity = Gravity.END
        pop.setOnMenuItemClickListener {
            val txt = it.title.substring(1, it.title.length-1)
            val arr = txt.split(",")
            binding.cv.setExpectSize(Size.of(arr[0].trim().toInt(), arr[1].trim().toInt()))
            return@setOnMenuItemClickListener true
        }
        pop.show()
    }

    override fun onResume() {
        super.onResume()
        if (!binding.cv.isCameraOpened) {
            binding.cv.openCamera(object : CameraOpenListener {
                override fun onCameraOpened(cameraFace: Int) {
                    Logger.d(TAG, "onCameraOpened")
                }

                override fun onCameraOpenError(throwable: Throwable?) {
                    Logger.d(TAG, "error : $throwable")
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        binding.cv.closeCamera {
            Logger.d(TAG, "closeCamera : $it")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.cv.releaseCamera()
    }

    fun picture(v: View) {
        isCapturePicture = true
        (v as TextView).setTextColor(Color.WHITE)
        binding.tvVideo.setTextColor(Color.LTGRAY)
        binding.cv.setMediaType(Media.TYPE_PICTURE)
    }

    fun video(v: View) {
        isCapturePicture = false
        (v as TextView).setTextColor(Color.WHITE)
        binding.tvPicture.setTextColor(Color.LTGRAY)
        binding.cv.setMediaType(Media.TYPE_VIDEO)
    }

    fun shot(v: View) {
        if (isCapturePicture) {
            takePicture()
        } else {
            takeVideo()
        }
    }

    private fun takePicture() {
        binding.cv.takePicture(object : CameraPhotoListener {
            override fun onCaptureFailed(throwable: Throwable?) {
                Log.d(TAG, "onCaptureFailed : $throwable")
            }

            override fun onPictureTaken(data: ByteArray?) {
                val file = FileUtils.getTempFile(this@CameraActivity, "jpg")
                FileUtils.writeData(file, data)
                Toast.makeText(this@CameraActivity, "Saved to $file", Toast.LENGTH_SHORT).show()
                binding.cv.resumePreview()
            }
        })
    }

    private fun takeVideo() {
        if (!isCameraRecording) {
            binding.cv.startVideoRecord(FileUtils.getTempFile(this, "mp4"), object : CameraVideoListener {
                override fun onVideoRecordStart() {
                    Toast.makeText(this@CameraActivity, "Video record START!", Toast.LENGTH_SHORT).show()
                    isCameraRecording = true
                }

                override fun onVideoRecordStop(file: File?) {
                    isCameraRecording = false
                    Toast.makeText(this@CameraActivity, "Saved to $file", Toast.LENGTH_SHORT).show()
                }

                override fun onVideoRecordError(throwable: Throwable?) {
                    isCameraRecording = false
                    Toast.makeText(this@CameraActivity, "$throwable", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            binding.cv.stopVideoRecord()
        }
    }

}