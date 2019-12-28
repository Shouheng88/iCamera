package me.shouheng.sample.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.widget.PopupMenu
import android.view.Gravity
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import me.shouheng.mvvm.base.CommonActivity
import me.shouheng.mvvm.base.anno.ActivityConfiguration
import me.shouheng.mvvm.comn.EmptyViewModel
import me.shouheng.sample.BuildConfig
import me.shouheng.sample.R
import me.shouheng.sample.databinding.ActivityCameraBinding
import me.shouheng.utils.app.ResUtils
import me.shouheng.utils.stability.L
import me.shouheng.utils.store.FileUtils
import me.shouheng.utils.store.IOUtils
import me.shouheng.utils.store.PathUtils
import me.shouheng.utils.ui.BarUtils
import me.shouheng.xcamera.config.ConfigurationProvider
import me.shouheng.xcamera.config.size.Size
import me.shouheng.xcamera.config.size.SizeMap
import me.shouheng.xcamera.enums.CameraFace
import me.shouheng.xcamera.enums.CameraSizeFor
import me.shouheng.xcamera.enums.FlashMode
import me.shouheng.xcamera.enums.MediaType
import me.shouheng.xcamera.listener.CameraOpenListener
import me.shouheng.xcamera.listener.CameraPhotoListener
import me.shouheng.xcamera.listener.CameraSizeListener
import me.shouheng.xcamera.listener.CameraVideoListener
import java.io.File
import java.io.FileNotFoundException

/**
 * Camera preview activity
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 22:22
 */
@ActivityConfiguration(layoutResId = R.layout.activity_camera)
class CameraActivity : CommonActivity<ActivityCameraBinding, EmptyViewModel>() {

    /**
     * Is currently capturing picture.
     * Be sure to change this value if you used app:mediaType="picture" property in layout.
     */
    private var isCapturePicture = true

    /**
     * Is currently recording video.
     */
    private var isCameraRecording = false

    override fun doCreateView(savedInstanceState: Bundle?) {
        BarUtils.setStatusBarLightMode(window, false)
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
        binding.scFlash.isChecked = ConfigurationProvider.get().defaultFlashMode == FlashMode.FLASH_ON
        binding.scFlash.setOnCheckedChangeListener { _, isChecked ->
            binding.cv.flashMode = if (isChecked) FlashMode.FLASH_ON else FlashMode.FLASH_OFF
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
            showPopDialog(it, binding.cv.getSizes(CameraSizeFor.SIZE_FOR_PREVIEW))
        }
        binding.tvPictureSizes.setOnClickListener {
            showPopDialog(it, binding.cv.getSizes(CameraSizeFor.SIZE_FOR_PICTURE))
        }
        binding.tvVideoSizes.setOnClickListener {
            showPopDialog(it, binding.cv.getSizes(CameraSizeFor.SIZE_FOR_VIDEO))
        }
        binding.tvSwitchCamera.setOnClickListener {
            binding.cv.switchCamera(if (binding.cv.cameraFace == CameraFace.FACE_FRONT) CameraFace.FACE_REAR else CameraFace.FACE_FRONT)
        }
    }

    private fun configMain() {
        binding.sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // empty
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // empty
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val room = 1 + (binding.cv.maxZoom - 1) * (1.0f * progress / seekBar!!.max)
                binding.cv.zoom = room
            }
        })
        binding.cv.setOnMoveListener {
            toast(if (it) "LEFT" else "RIGHT")
        }
        binding.cv.addCameraSizeListener(object : CameraSizeListener {

            private var previewSize: Size? = null
            private var videoSize: Size? = null
            private var pictureSize: Size? = null

            override fun onPreviewSizeUpdated(previewSize: Size?) {
                this.previewSize = previewSize
                displaySizeInfo()
                L.d("onPreviewSizeUpdated : $previewSize")
            }

            override fun onVideoSizeUpdated(videoSize: Size?) {
                this.videoSize = videoSize
                displaySizeInfo()
                L.d("onVideoSizeUpdated : $videoSize")
            }

            override fun onPictureSizeUpdated(pictureSize: Size?) {
                this.pictureSize = pictureSize
                displaySizeInfo()
                L.d("onPictureSizeUpdated : $pictureSize")
            }

            @SuppressLint("SetTextI18n")
            private fun displaySizeInfo() {
                binding.tvInfo.text = "Camera Info:\n" +
                        "1.Preview Size: ${previewSize?.toString()}\n" +
                        "2.Picture Size: ${pictureSize?.toString()}\n" +
                        "3.Video Size: ${videoSize?.toString()}"
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
        pop
    }

    override fun onResume() {
        super.onResume()
        if (!binding.cv.isCameraOpened) {
            binding.cv.openCamera(object : CameraOpenListener {
                override fun onCameraOpened(cameraFace: Int) {
                    L.d("onCameraOpened")
                }

                override fun onCameraOpenError(throwable: Throwable?) {
                    L.e("error : $throwable")
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        binding.cv.closeCamera {
            L.d("closeCamera : $it")
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
        binding.cv.setMediaType(MediaType.TYPE_PICTURE)
    }

    fun video(v: View) {
        isCapturePicture = false
        (v as TextView).setTextColor(Color.WHITE)
        binding.tvPicture.setTextColor(Color.LTGRAY)
        binding.cv.setMediaType(MediaType.TYPE_VIDEO)
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
                L.d("onCaptureFailed : $throwable")
            }

            override fun onPictureTaken(data: ByteArray?) {
                val fileToSave = getSavedFile("jpg")
                IOUtils.writeFileFromBytesByStream(fileToSave, data)
                saveImageToGallery(fileToSave, fileToSave.name)
                toast("Saved to $fileToSave")
                binding.cv.resumePreview()
            }
        })
    }

    private fun takeVideo() {
        if (!isCameraRecording) {
            val seconds: Int = try {
                Integer.parseInt(binding.etVideoDuration.text.toString())
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

                override fun onVideoRecordStop(file: File?) {
                    isCameraRecording = false
                    saveImageToGallery(fileToSave, fileToSave.name)
                    toast("Saved to $file")
                }

                override fun onVideoRecordError(throwable: Throwable?) {
                    isCameraRecording = false
                    toast("$throwable")
                }
            })
        } else {
            binding.cv.stopVideoRecord()
        }
    }

    private fun saveImageToGallery(file: File, fileName: String) {
        try {
            MediaStore.Images.Media.insertImage(contentResolver, file.absolutePath, fileName, null)
        } catch (e: FileNotFoundException) {
            L.d("saveImageToGallery: FileNotFoundException MediaStore")
            e.printStackTrace()
        }
        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, getUriFromFile(file)))
    }

    private fun getSavedFile(appendix: String): File {
        val appDir = File(PathUtils.getExternalPicturesPath(), ResUtils.getString(R.string.app_name))
        FileUtils.createOrExistsDir(appDir.path)
        val fileName = "${System.currentTimeMillis()}.${appendix}"
        return File(appDir, fileName)
    }

    private fun getUriFromFile(file: File): Uri {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            Uri.fromFile(file)
        }
    }
}