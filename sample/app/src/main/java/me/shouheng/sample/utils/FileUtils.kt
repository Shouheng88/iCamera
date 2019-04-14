package me.shouheng.sample.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 14:54
 */
object FileUtils {

    fun writeData(file: File, data: ByteArray?) {
        val os = FileOutputStream(file)
        os.write(data)
        os.flush()
        os.close()
    }

    fun getTempFile(ctx: Context, ext: String) : File {
        return File(ctx.getExternalFilesDir(null), "${System.currentTimeMillis()}.$ext")
    }
}
