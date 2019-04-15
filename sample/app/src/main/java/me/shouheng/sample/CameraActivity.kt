package me.shouheng.sample

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import me.shouheng.camerax.config.ConfigurationProvider
import me.shouheng.camerax.enums.Flash
import me.shouheng.camerax.enums.Media
import me.shouheng.camerax.listener.CameraPhotoListener
import me.shouheng.camerax.listener.CameraVideoListener
import me.shouheng.sample.base.CommonActivity
import me.shouheng.sample.databinding.ActivityCameraBinding
import me.shouheng.sample.utils.FileUtils
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
        configDrawer()
        configMain()
    }

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
    }

    override fun onResume() {
        super.onResume()
        binding.cv.openCamera()
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