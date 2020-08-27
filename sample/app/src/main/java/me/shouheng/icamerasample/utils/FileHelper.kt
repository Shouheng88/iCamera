package me.shouheng.icamerasample.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import me.shouheng.icamerasample.BuildConfig
import me.shouheng.icamerasample.R
import me.shouheng.utils.app.ResUtils
import me.shouheng.utils.stability.L
import me.shouheng.utils.store.FileUtils
import me.shouheng.utils.store.PathUtils
import java.io.File
import java.io.FileNotFoundException

/**
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2020-08-25 16:21
 */
object FileHelper {

    fun saveImageToGallery(context: Context, file: File, fileName: String) {
        try {
            MediaStore.Images.Media.insertImage(context.contentResolver, file.absolutePath, fileName, null)
        } catch (e: FileNotFoundException) {
            L.d("saveImageToGallery: FileNotFoundException MediaStore")
            e.printStackTrace()
        }
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, getUriFromFile(context, file)))
    }

    fun saveVideoToGallery(context: Context, file: File, fileName: String) {
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, getUriFromFile(context, file)))
    }

    fun getSavedFile(appendix: String): File {
        val appDir = File(PathUtils.getExternalPicturesPath(), ResUtils.getString(R.string.app_name))
        FileUtils.createOrExistsDir(appDir.path)
        val fileName = "${System.currentTimeMillis()}.${appendix}"
        return File(appDir, fileName)
    }

    fun getUriFromFile(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            Uri.fromFile(file)
        }
    }
}