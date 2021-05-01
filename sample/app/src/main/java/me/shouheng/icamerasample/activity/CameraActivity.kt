package me.shouheng.icamerasample.activity

import android.animation.Animator
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.PopupMenu
import android.view.Gravity
import android.view.View
import android.widget.SeekBar
import me.shouheng.icamera.config.ConfigurationProvider
import me.shouheng.icamera.config.size.Size
import me.shouheng.icamera.config.size.SizeMap
import me.shouheng.icamera.enums.CameraFace
import me.shouheng.icamera.enums.CameraSizeFor
import me.shouheng.icamera.enums.FlashMode
import me.shouheng.icamera.enums.MediaType
import me.shouheng.icamera.listener.*
import me.shouheng.icamera.util.CameraHelper
import me.shouheng.icamera.util.ImageHelper
import me.shouheng.icamerasample.R
import me.shouheng.icamerasample.databinding.ActivityCameraBinding
import me.shouheng.icamerasample.utils.FileHelper.getSavedFile
import me.shouheng.icamerasample.utils.FileHelper.saveImageToGallery
import me.shouheng.icamerasample.utils.FileHelper.saveVideoToGallery
import me.shouheng.utils.ktx.dp2px
import me.shouheng.utils.ktx.invisible
import me.shouheng.utils.ktx.onDebouncedClick
import me.shouheng.utils.ktx.stringOf
import me.shouheng.utils.stability.L
import me.shouheng.utils.ui.BarUtils
import me.shouheng.vmlib.base.CommonActivity
import me.shouheng.vmlib.comn.EmptyViewModel
import java.io.File

/**
 * Camera preview activity
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:22
 */
class CameraActivity : CommonActivity<EmptyViewModel, ActivityCameraBinding>() {

    override fun getLayoutResId(): Int = R.layout.activity_camera

    /** Is currently recording video. */
    private var isCameraRecording = false

    override fun doCreateView(savedInstanceState: Bundle?) {
        L.d("doCreateView")
        BarUtils.setStatusBarLightMode(window, false)
        configDrawer()
        configMain()
    }

    private fun configDrawer() {
        binding.scVoice.isChecked = ConfigurationProvider.get().isVoiceEnable
        binding.scVoice.setOnCheckedChangeListener { _, isChecked -> binding.cv.isVoiceEnable = isChecked }
        binding.scFocus.isChecked = ConfigurationProvider.get().isAutoFocus
        binding.scFocus.setOnCheckedChangeListener { _, isChecked -> binding.cv.isAutoFocus = isChecked }
        binding.scTouchFocus.isChecked = true
        binding.scTouchFocus.setOnCheckedChangeListener { _, isChecked -> binding.cv.setUseTouchFocus(isChecked) }
        binding.scTouchZoom.isChecked = true
        binding.scTouchZoom.setOnCheckedChangeListener { _, isChecked -> binding.cv.setTouchZoomEnable(isChecked) }

        binding.tvPreviewSizes.onDebouncedClick {
            showPopDialog(it, binding.cv.getSizes(CameraSizeFor.SIZE_FOR_PREVIEW))
        }
        binding.tvPictureSizes.onDebouncedClick {
            showPopDialog(it, binding.cv.getSizes(CameraSizeFor.SIZE_FOR_PICTURE))
        }
        binding.tvVideoSizes.onDebouncedClick {
            showPopDialog(it, binding.cv.getSizes(CameraSizeFor.SIZE_FOR_VIDEO))
        }
    }

    private fun configMain() {
        binding.ivSetting.onDebouncedClick { binding.drawer.openDrawer(Gravity.END) }
        binding.ivFlash.setImageResource(
            when(ConfigurationProvider.get().defaultFlashMode) {
                FlashMode.FLASH_AUTO -> R.drawable.ic_flash_auto_white_24dp
                FlashMode.FLASH_OFF -> R.drawable.ic_flash_off_white_24dp
                FlashMode.FLASH_ON -> R.drawable.ic_flash_on_white_24dp
                else -> R.drawable.ic_flash_auto_white_24dp
            }
        )
        binding.ivFlash.onDebouncedClick {
            val mode = when(binding.cv.flashMode) {
                FlashMode.FLASH_AUTO -> FlashMode.FLASH_ON
                FlashMode.FLASH_OFF -> FlashMode.FLASH_AUTO
                FlashMode.FLASH_ON -> FlashMode.FLASH_OFF
                else -> FlashMode.FLASH_AUTO
            }
            binding.cv.flashMode = mode
            binding.ivFlash.setImageResource(
                when(mode) {
                    FlashMode.FLASH_AUTO -> R.drawable.ic_flash_auto_white_24dp
                    FlashMode.FLASH_OFF -> R.drawable.ic_flash_off_white_24dp
                    FlashMode.FLASH_ON -> R.drawable.ic_flash_on_white_24dp
                    else -> R.drawable.ic_flash_auto_white_24dp
                }
            )
        }

        binding.sb.animate()
            .translationX(130f.dp2px().toFloat())
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) { /*noop*/ }

                override fun onAnimationEnd(animation: Animator?) { binding.sb.invisible() }

                override fun onAnimationCancel(animation: Animator?) { /*noop*/ }

                override fun onAnimationStart(animation: Animator?) { /*noop*/ }
            })
        binding.sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) { /*noop*/  }

            override fun onStopTrackingTouch(seekBar: SeekBar?) { /*noop*/ }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // set camera zoom
                val room = 1 + (binding.cv.maxZoom - 1) * (1.0f * progress / seekBar!!.max)
                binding.cv.zoom = room
                displayCameraInfo()
            }
        })

        binding.ivSwitch.onDebouncedClick {
            // switch camera between front and end camera
            binding.cv.switchCamera(if (binding.cv.cameraFace == CameraFace.FACE_FRONT)
                CameraFace.FACE_REAR else CameraFace.FACE_FRONT)
        }
        binding.ivTypeSwitch.onDebouncedClick {
            // switch camera between video and picture mode
            if (binding.cv.mediaType == MediaType.TYPE_PICTURE) {
                binding.ivTypeSwitch.setImageResource(R.drawable.ic_videocam_white_24dp)
                binding.cv.mediaType = MediaType.TYPE_VIDEO
            } else {
                binding.ivTypeSwitch.setImageResource(R.drawable.ic_photo_camera_white_24dp)
                binding.cv.mediaType = MediaType.TYPE_PICTURE
            }
            displayCameraInfo()
        }

        binding.cv.setOnMoveListener(object : OnMoveListener {
            override fun onMove(left: Boolean) {
                toast(if (left) "LEFT" else "RIGHT")
            }
        })
        binding.cv.addCameraSizeListener(object : CameraSizeListener {
            override fun onPreviewSizeUpdated(previewSize: Size) {
                L.d("onPreviewSizeUpdated : $previewSize")
                displayCameraInfo()
            }

            override fun onVideoSizeUpdated(videoSize: Size) {
                L.d("onVideoSizeUpdated : $videoSize")
                displayCameraInfo()
            }

            override fun onPictureSizeUpdated(pictureSize: Size) {
                L.d("onPictureSizeUpdated : $pictureSize")
                displayCameraInfo()
            }
        })

        binding.ivShot.onDebouncedClick {
            if (binding.cv.mediaType == MediaType.TYPE_PICTURE) { takePicture()
            } else { takeVideo() }
        }

        binding.cv.addOrientationChangedListener(object : OnOrientationChangedListener {
            override fun onOrientationChanged(degree: Int) {
                ViewCompat.setRotation(binding.ivFlash, degree.toFloat())
                ViewCompat.setRotation(binding.ivSwitch, degree.toFloat())
                ViewCompat.setRotation(binding.ivTypeSwitch, degree.toFloat())
                ViewCompat.setRotation(binding.ivSetting, degree.toFloat())
            }
        })
        binding.cv.setCameraPreviewListener(object : CameraPreviewListener {
            /*used to slow the calculation*/
            private var frame: Int = 0
            override fun onPreviewFrame(data: ByteArray, size: Size, format: Int) {
                if (frame % 25 == 0) {
                    frame = 1
                    try {
                        val light = ImageHelper.convertYUV420_NV21toARGB8888(data, size.width, size.height)
                        binding.tvLightTip.text = if (light <= 30) stringOf(R.string.camera_main_light_tip) else ""
                        binding.ivPreview.setImageBitmap(ImageHelper.convertNV21ToBitmap(data, size.width, size.height))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                frame++
            }
        })

        displayCameraInfo()
    }

    private fun showPopDialog(view: View, sizes: SizeMap): PopupMenu {
        val pop = PopupMenu(this, view)
        val list = mutableListOf<Size>()
        sizes.values.forEach { list.addAll(it) }
        list.forEach { pop.menu.add(it.toString()) }
        pop.gravity = Gravity.END
        pop.setOnMenuItemClickListener {
            val txt = it.title.substring(1, it.title.length-1)
            val arr = txt.split(",")
            binding.cv.setExpectSize(Size.of(arr[0].trim().toInt(), arr[1].trim().toInt()))
            return@setOnMenuItemClickListener true
        }
        pop.show()
        return pop
    }

    override fun onResume() {
        super.onResume()
        if (!binding.cv.isCameraOpened) {
            binding.cv.openCamera(object : CameraOpenListener {
                override fun onCameraOpened(cameraFace: Int) { L.d("onCameraOpened") }

                override fun onCameraOpenError(throwable: Throwable) {
                    L.e(throwable)
                    toast("Camera open error : $throwable")
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        binding.cv.closeCamera(object : CameraCloseListener {
            override fun onCameraClosed(cameraFace: Int) {
                L.d("closeCamera : $cameraFace")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.cv.releaseCamera()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        L.d("CameraActivity", "onConfigurationChanged")
    }

    private fun takePicture() {
        val fileToSave = getSavedFile("jpg")
        binding.cv.takePicture(fileToSave, object : CameraPhotoListener {
            override fun onCaptureFailed(throwable: Throwable) {
                L.e(throwable)
                toast("onCaptureFailed : $throwable")
            }

            override fun onPictureTaken(data: ByteArray, picture: File) {
//                IOUtils.writeFileFromBytesByStream(fileToSave, data)
                saveImageToGallery(context, fileToSave, fileToSave.name)
                toast("Saved to $fileToSave")
                binding.cv.resumePreview()
            }
        })
    }

    private fun takeVideo() {
        if (!isCameraRecording) {
            val seconds: Int = try {
                binding.etVideoDuration.text.toString().toInt()
            } catch (ex: Exception) {
                0
            }
            binding.cv.setVideoDuration(seconds * 1000)
            val fileToSave = getSavedFile("mp4")
            binding.cv.startVideoRecord(fileToSave, object : CameraVideoListener {
                override fun onVideoRecordStart() {
                    toast("Video record START!")
                    isCameraRecording = true
                }

                override fun onVideoRecordStop(file: File) {
                    isCameraRecording = false
                    saveVideoToGallery(context, fileToSave, fileToSave.name)
                    toast("Saved to $file")
                }

                override fun onVideoRecordError(throwable: Throwable) {
                    isCameraRecording = false
                    toast("$throwable")
                    L.e(throwable)
                }
            })
        } else {
            binding.cv.stopVideoRecord()
        }
    }

    private fun displayCameraInfo() {
        val info = "Camera info:\n" +
                "1.Preview size: ${binding.cv.getSize(CameraSizeFor.SIZE_FOR_PREVIEW)}\n" +
                "2.Picture size: ${binding.cv.getSize(CameraSizeFor.SIZE_FOR_PICTURE)}\n" +
                "3.Video size: ${binding.cv.getSize(CameraSizeFor.SIZE_FOR_VIDEO)}\n" +
                "4.Media type: ${if (binding.cv.mediaType == MediaType.TYPE_PICTURE) "picture" else "video" }\n" +
                "5.Zoom: ${binding.cv.zoom}\n" +
                "6.Max zoom: ${binding.cv.maxZoom}\n" +
                "7.Camera face: ${if (binding.cv.cameraFace == CameraFace.FACE_REAR) "rear" else "front"}\n" +
                "8.Has camera: ${CameraHelper.hasCamera(context)}\n" +
                "9.Camera2: ${if (CameraHelper.hasCamera2(context)) "support" else "not support" }"
        binding.tvInfo.text = info
    }
}
