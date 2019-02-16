package de.domes_muc.printerappkotlin

import android.app.Activity
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.library.LibraryController
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager

import java.util.Timer
import java.util.TimerTask

/**
 * Splash screen activity that shows the logo of the app during a time interval
 * of 3 seconds. Then, the main activity is charged and showed.
 *
 * @author sara-perez
 */
class SplashScreenActivity : Activity() {

    internal lateinit var mContext: Context

    internal var splashDelay: TimerTask = object : TimerTask() {
        override fun run() {

            Log.d(TAG, "[START PRINTERAPP]")

            val mainIntent = Intent().setClass(
                this@SplashScreenActivity, MainActivity::class.java
            )
            startActivity(mainIntent)

            //Close the activity so the user won't able to go back this
            //activity pressing Back button
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash_screen)

        mContext = this

        //Initialize db and lists
        DatabaseController(this)
        DevicesListController.loadList(this)
        LibraryController.initializeHistoryList()

        //Initialize default settings
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)


        if (isTaskRoot) {

            //Simulate a long loading process on application startup
            val timer = Timer()
            timer.schedule(splashDelay, SPLASH_SCREEN_DELAY)

        } else
            finish()

    }

    companion object {

        private val TAG = "SplashScreenActivity"

        //Set the duration of the splash screen
        private val SPLASH_SCREEN_DELAY: Long = 10000
    }

}
