package me.shouheng.icamerasample

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import com.squareup.leakcanary.LeakCanary
import me.shouheng.icamera.config.ConfigurationProvider
import me.shouheng.icamera.util.LogLevel
import me.shouheng.icamera.util.LogWatcher
import me.shouheng.icamerasample.activity.MainActivity
import me.shouheng.uix.common.bean.TextStyleBean
import me.shouheng.uix.pages.CrashReportActivity
import me.shouheng.utils.app.ResUtils
import me.shouheng.utils.permission.Permission
import me.shouheng.utils.permission.PermissionUtils
import me.shouheng.utils.stability.CrashHelper
import me.shouheng.utils.stability.L
import me.shouheng.utils.store.PathUtils
import me.shouheng.vmlib.VMLib
import java.io.File

/**
 * App
 *
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2019-12-28 15:47
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
        // initialize the vmlib
        VMLib.onCreate(this)
        // log
        L.getConfig().setLogSwitch(BuildConfig.DEBUG)
        configCrashHelper(application)
        // set iCamera log switch
        ConfigurationProvider.get().isDebug = BuildConfig.DEBUG
        // leak canary used to detect memory leak of camera
        LeakCanary.install(this)
        ConfigurationProvider.get().addLogWatcher(object : LogWatcher {
            override fun onLog(level: LogLevel, tag: String, msg: String?) {
                when(level) {
                    LogLevel.V -> { Log.v("__I_CAMERA_$tag", msg) }
                    LogLevel.I -> { Log.i("__I_CAMERA_$tag", msg) }
                    LogLevel.D -> { Log.d("__I_CAMERA_$tag", msg)  }
                    LogLevel.W -> { Log.w("__I_CAMERA_$tag", msg)  }
                    LogLevel.E -> { Log.e("__I_CAMERA_$tag", msg)  }
                }
            }
        })
    }

    companion object {
        private lateinit var application: Application
        fun app(): Application = application
        @SuppressLint("MissingPermission")
        fun configCrashHelper(application: Application) {
            // crash detect tools, the crash log was saved to : data/data/package_name/files/crash
            if (PermissionUtils.hasPermissions(Permission.STORAGE)) {
                CrashHelper.init(application,
                    File(PathUtils.getExternalAppFilesPath(), "crash")
                ) { crashInfo, _ ->
                    CrashReportActivity.Companion.Builder(application)
                        .setRestartActivity(MainActivity::class.java)
                        .setMessage(crashInfo)
                        .setImage(R.drawable.uix_crash_error_image)
                        .setTitle(ResUtils.getString(R.string.main_common_crash_happened))
                        .setButtonStyle(TextStyleBean().apply {
                            textSize = 18f
                        })
                        .launch()
                }
            }
        }
    }
}
