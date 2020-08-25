package me.shouheng.icamerasample

import android.app.Application
import me.shouheng.vmlib.VMLib

/**
 * App
 *
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2019-12-28 15:47
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // initialize the vmlib
        VMLib.onCreate(this)
    }
}