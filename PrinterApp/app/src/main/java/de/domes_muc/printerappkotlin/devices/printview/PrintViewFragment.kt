package de.domes_muc.printerappkotlin.devices.printview


import android.app.Dialog
import android.app.DownloadManager
import androidx.fragment.app.Fragment
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.FinishDialog
import de.domes_muc.printerappkotlin.devices.camera.CameraHandler
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.discovery.DiscoveryController
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.model.ModelProfile
import de.domes_muc.printerappkotlin.octoprint.HttpUtils
import de.domes_muc.printerappkotlin.octoprint.OctoprintConnection
import de.domes_muc.printerappkotlin.octoprint.OctoprintControl
import de.domes_muc.printerappkotlin.octoprint.OctoprintFiles
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import de.domes_muc.printerappkotlin.util.ui.ViewHelper
import de.domes_muc.printerappkotlin.viewer.DataStorage
import de.domes_muc.printerappkotlin.viewer.GcodeFile
import de.domes_muc.printerappkotlin.viewer.ViewerMainFragment
import de.domes_muc.printerappkotlin.viewer.ViewerSurfaceView
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.PorterDuff
import android.os.Bundle
//import androidx.appcompat.app.ActionBarActivity;
//import androidx.cardview.widget.CardView;
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.material.widget.PaperButton
import de.domes_muc.printerappkotlin.devices.ExtrudersGridAdapter

import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.util.ArrayList

/**
 * This class will show the PrintView detailed view for every printer
 * Should be able to control printer commands and show video feed.
 *
 * @author alberto-baeza
 */
class PrintViewFragment : Fragment() {
    private var mCamera: CameraHandler? = null
    private var isPrinting = false
    private var isGcodeLoaded = false

    //View references
    private var tv_printer: TextView? = null
    private var tv_file: TextView? = null
    private var tv_temp: TextView? = null
    private var tv_temp_bed: TextView? = null
    private var tv_prog: TextView? = null
    private var tv_profile: TextView? = null

    private var pb_prog: ProgressBar? = null
    private var sb_head: SeekBar? = null

    private var button_pause: PaperButton? = null
    private var button_stop: PaperButton? = null
    private var icon_pause: ImageView? = null
    private var mVideoSurface: SurfaceView? = null

    private var mRootView: View? = null

    private var mDownloadDialog: Dialog? = null

    private var mExtrudersGridAdapter: ExtrudersGridAdapter? = null
    /**
     * Receives the "download complete" event asynchronously
     */
    var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {

            val manager = ctxt.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var filename: String? = null

            //Get the downloaded file name
            val extras = intent.extras
            val q = DownloadManager.Query()
            q.setFilterById(extras!!.getLong(DownloadManager.EXTRA_DOWNLOAD_ID))
            val c = manager.query(q)

            if (c.moveToFirst()) {
                val status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    filename = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE))
                }
            }

            //If we have a stored path
            if (DatabaseController.isPreference(DatabaseController.TAG_REFERENCES, mPrinter!!.name)) {

                val path = DatabaseController.getPreference(DatabaseController.TAG_REFERENCES, mPrinter!!.name)
                val file = File(path)

                if (file.name == filename) {
                    //In case there was a previous cached file with the same path
                    GcodeCache.removeGcodeFromCache(path!!)

                    if (file.exists()) {

                        openGcodePrintView(mRootView!!.context, path, mRootView, R.id.view_gcode)
                        mPrinter!!.jobPath = path

                        //Register receiver
                        mContext!!.unregisterReceiver(this)

                        //DatabaseController.handlePreference(DatabaseController.TAG_REFERENCES, mPrinter.getName(), null, false);

                    } else {

                        Toast.makeText(activity, R.string.printview_download_toast_error, Toast.LENGTH_LONG).show()

                        //Register receiver
                        mContext!!.unregisterReceiver(this)
                    }
                } else {

                }


            }

            if (mDownloadDialog != null) mDownloadDialog!!.dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Reference to View
        mRootView = null

        //If is not new
        if (savedInstanceState == null) {

            //Necessary for gcode tracking
            mContext = activity!!

            //Show custom option menu
            setHasOptionsMenu(true)

            //Get the printer from the list
            mPrinter = arguments?.let {
                DevicesListController.getPrinter(it.getLong("id"))
            }
            //getActivity().getActionBar().setTitle(mPrinter.getAddress().replace("/", ""));

            if (mPrinter == null) {
                activity!!.onBackPressed()
            } else {

                try { //TODO CRASH
                    //Check printing status
                    if (mPrinter!!.status == StateUtils.STATE_PRINTING || mPrinter!!.status == StateUtils.STATE_PAUSED)
                        isPrinting = true
                    else {
                        mActualProgress = 100
                        isPrinting = false
                    }
                } catch (e: NullPointerException) {
                    activity!!.onBackPressed()
                }

                //Update the actionbar to show the up carat/affordance
                if (DatabaseController.count() > 1) {
                    (activity as AppCompatActivity).getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
                }

                //Inflate the fragment
                mRootView = inflater.inflate(
                    R.layout.printview_layout,
                    container, false
                )

                /** */

                //Get video
                mLayoutVideo = mRootView!!.findViewById<View>(R.id.printview_video) as FrameLayout

                //TODO CAMERA DISABLED
//FD20190214: Temporarely disable camera view
//                mPrinter?.let {
//                    if ( it.webcamAddress != null ) {
//                        mCamera = CameraHandler(mContext, it.webcamAddress!!, mLayoutVideo)
//                        mVideoSurface = mCamera?.view
//                        mLayoutVideo?.addView(mVideoSurface)
//                        mCamera?.startVideo()
//                    }
//                }

                val optionTabHost = mRootView!!.findViewById<View>(R.id.printviews_options_tabhost) as TabHost
                optionTabHost.setup()

                //Create temperature tab
                val temperatureTab = optionTabHost.newTabSpec("Temperature")
                temperatureTab.setIndicator(
                    getTabIndicator(
                        mContext.resources.getString(R.string.printview_temperature_text),
                        R.drawable.ic_videocam
                    )
                )
                temperatureTab.setContent(R.id.printview_options_temperature)
                optionTabHost.addTab(temperatureTab)

                //Create motion tab
                val motionTab = optionTabHost.newTabSpec("Motion")
                motionTab.setIndicator(
                    getTabIndicator(
                        mContext.resources.getString(R.string.printview_motion_text),
                        R.drawable.ic_videocam
                    )
                )
                motionTab.setContent(R.id.printview_options_motion)
                optionTabHost.addTab(motionTab)

                //Create motion tab
                val extruderTab = optionTabHost.newTabSpec("Extruder")
                extruderTab.setIndicator(
                    getTabIndicator(
                        mContext.resources.getString(R.string.printview_extruder_text),
                        R.drawable.ic_videocam
                    )
                )
                extruderTab.setContent(R.id.printview_options_extruder)
                optionTabHost.addTab(extruderTab)

//                //Create 3D RENDER tab
//                val featuresTab = tabHost.newTabSpec("3D Render")
//                featuresTab.setIndicator(
//                    getTabIndicator(
//                        mContext.resources.getString(R.string.printview_3d_text),
//                        R.drawable.visual_normal_24dp
//                    )
//                )
//                featuresTab.setContent(R.id.view_gcode)
//                tabHost.addTab(featuresTab)

                //Get tabHost from the xml
                val tabHost = mRootView!!.findViewById<View>(R.id.printviews_tabhost) as TabHost
                tabHost.setup()

                //Create VIDEO tab
                val settingsTab = tabHost.newTabSpec("Video")
                settingsTab.setIndicator(
                    getTabIndicator(
                        mContext.resources.getString(R.string.printview_video_text),
                        R.drawable.ic_videocam
                    )
                )
                settingsTab.setContent(R.id.printview_video)
                tabHost.addTab(settingsTab)

                //Create 3D RENDER tab
                val featuresTab = tabHost.newTabSpec("3D Render")
                featuresTab.setIndicator(
                    getTabIndicator(
                        mContext.resources.getString(R.string.printview_3d_text),
                        R.drawable.visual_normal_24dp
                    )
                )
                featuresTab.setContent(R.id.view_gcode)
                tabHost.addTab(featuresTab)

                tabHost.setOnTabChangedListener { s ->
                    if (s == "Video") {
                        if (mSurface != null)
                            mLayout!!.removeAllViews()
                    } else {    //Redraw the gcode
                        if (!isGcodeLoaded) {
                            //Show gcode tracking if there's a current path in the printer/preferences
                            if (mPrinter!!.job.filename != null) {
                                retrieveGcode()
                            }
                        } else {
                            if (mSurface != null) {
                                drawPrintView()
                            }
                        }
                    }

                    //TODO CAMERA DISABLED
                    mLayoutVideo!!.invalidate()
                }

                /** */


                initUiElements()



                refreshData()

                //Register receiver
                mContext.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

            }


        }
        return mRootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.printview_menu, menu)
    }

    //Switch menu options if it's printing/paused
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
    }

    //Option menu
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {

        when (item.itemId) {
            android.R.id.home -> {
                if (DatabaseController.count() > 1) activity!!.onBackPressed()
                return true
            }

            R.id.printview_add -> {
                DiscoveryController(activity!!).scanDelayDialog()
                return true
            }

            R.id.printview_settings -> {
                //getActivity().onBackPressed();
                MainActivity.showExtraFragment(0, 0)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    //Initialize all UI elements
    private fun initUiElements() {

        //UI references
        tv_printer = mRootView!!.findViewById<View>(R.id.printview_printer_tag) as TextView
        tv_file = mRootView!!.findViewById<View>(R.id.printview_printer_file) as TextView
//        tv_temp = mRootView!!.findViewById<View>(R.id.printview_extruder_temp) as TextView
//        tv_temp_bed = mRootView!!.findViewById<View>(R.id.printview_bed_temp) as TextView
        tv_prog = mRootView!!.findViewById<View>(R.id.printview_printer_progress) as TextView
        tv_profile = mRootView!!.findViewById<View>(R.id.printview_text_profile_text) as TextView
        pb_prog = mRootView!!.findViewById<View>(R.id.printview_progress_bar) as ProgressBar

        button_pause = mRootView!!.findViewById<View>(R.id.printview_pause_button) as PaperButton
        icon_pause = mRootView!!.findViewById<View>(R.id.printview_pause_image) as ImageView
        button_stop = mRootView!!.findViewById<View>(R.id.printview_stop_button) as PaperButton

        icon_pause!!.setColorFilter(
            mContext.resources.getColor(R.color.body_text_2),
            PorterDuff.Mode.MULTIPLY
        )

        button_pause!!.setOnClickListener {
            if (!isPrinting) {

                if (mPrinter!!.job.progress != "null" && mPrinter!!.job.finished) {

                    FinishDialog(mContext, mPrinter!!)

                } else {
                    OctoprintControl.sendCommand(activity!!, mPrinter!!.address, "start")
                }
            } else
                OctoprintControl.sendCommand(activity!!, mPrinter!!.address, "pause")
        }

        //        ((ImageView) mRootView.findViewById(R.id.printview_stop_image)).
        //                setColorFilter(mContext.getResources().getColor(android.R.color.holo_red_dark),
        //                        PorterDuff.Mode.MULTIPLY);
        button_stop!!.setOnClickListener { OctoprintControl.sendCommand(activity!!, mPrinter!!.address, "cancel") }

        mExtrudersGridAdapter = ExtrudersGridAdapter(mContext, 0, mPrinter!!)
        val tempGridView = mRootView!!.findViewById<GridView>(R.id.printview_extruder_tempgrid)
        tempGridView?.setAdapter(mExtrudersGridAdapter)
        tempGridView?.setOnItemClickListener { parent, view, position, id ->

            Toast.makeText(mContext, "Clicked item :"+" "+position,Toast.LENGTH_LONG).show()
        }

        sb_head = mRootView!!.findViewById<View>(R.id.seekbar_head_movement_amount) as SeekBar
        sb_head!!.progress = 2


        mRootView!!.findViewById<View>(R.id.button_xy_down).setOnClickListener {
            OctoprintControl.sendHeadCommand(
                activity!!,
                mPrinter!!.address,
                "jog",
                "y",
                convertProgress(sb_head!!.progress)
            )
        }

        mRootView!!.findViewById<View>(R.id.button_xy_up).setOnClickListener {
            OctoprintControl.sendHeadCommand(
                activity!!,
                mPrinter!!.address,
                "jog",
                "y",
                -convertProgress(sb_head!!.progress)
            )
        }

        mRootView!!.findViewById<View>(R.id.button_xy_left).setOnClickListener {
            OctoprintControl.sendHeadCommand(
                activity!!,
                mPrinter!!.address,
                "jog",
                "x",
                -convertProgress(sb_head!!.progress)
            )
        }

        mRootView!!.findViewById<View>(R.id.button_xy_right).setOnClickListener {
            OctoprintControl.sendHeadCommand(
                activity!!,
                mPrinter!!.address,
                "jog",
                "x",
                convertProgress(sb_head!!.progress)
            )
        }

        mRootView!!.findViewById<View>(R.id.button_z_down).setOnClickListener {
            OctoprintControl.sendHeadCommand(
                activity!!,
                mPrinter!!.address,
                "jog",
                "z",
                -convertProgress(sb_head!!.progress)
            )
        }

        mRootView!!.findViewById<View>(R.id.button_z_up).setOnClickListener {
            OctoprintControl.sendHeadCommand(
                activity!!,
                mPrinter!!.address,
                "jog",
                "z",
                convertProgress(sb_head!!.progress)
            )
        }

        mRootView!!.findViewById<View>(R.id.button_z_home)
            .setOnClickListener { OctoprintControl.sendHeadCommand(activity!!, mPrinter!!.address, "home", "z", 0.0) }

        mRootView!!.findViewById<View>(R.id.button_xy_home)
            .setOnClickListener { OctoprintControl.sendHeadCommand(activity!!, mPrinter!!.address, "home", "xy", 0.0) }

        /**
         * Temperatures
         */

//        val extruder1SeekBar = mRootView!!.findViewById<View>(R.id.printview_extruder_temp_slider) as SeekBar
//        val extruder1Button = mRootView!!.findViewById<View>(R.id.printview_extruder_temp_button) as PaperButton
//        extruder1Button.setText(String.format(resources.getString(R.string.printview_change_temp_button), 0))
//
//        extruder1SeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
//
//                extruder1Button.setText(String.format(resources.getString(R.string.printview_change_temp_button), i))
//
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar) {
//
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar) {
//
//            }
//        })
//
//        extruder1Button.setOnClickListener {
//            OctoprintControl.sendToolCommand(
//                activity!!,
//                mPrinter!!.address,
//                "target",
//                "tool0",
//                extruder1SeekBar.progress.toDouble()
//            )
//        }

//        val bedSeekBar = mRootView!!.findViewById<View>(R.id.printview_bed_temp_slider) as SeekBar
//        val bedButton = mRootView!!.findViewById<View>(R.id.printview_bed_temp_button) as PaperButton
//        bedButton.setText(String.format(resources.getString(R.string.printview_change_temp_button), 0))
//
//        bedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
//
//                bedButton.setText(String.format(resources.getString(R.string.printview_change_temp_button), i))
//
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar) {
//
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar) {
//
//            }
//        })
//
//        bedButton.setOnClickListener {
//            OctoprintControl.sendToolCommand(
//                activity!!,
//                mPrinter!!.address,
//                "target",
//                "bed",
//                bedSeekBar.progress.toDouble()
//            )
//        }

        /*
        Extruder

         */

        val et_am = mRootView!!.findViewById<View>(R.id.et_amount) as EditText

        mRootView!!.findViewById<View>(R.id.printview_retract_button).setOnClickListener {
            OctoprintControl.sendToolCommand(
                activity!!,
                mPrinter!!.address,
                "extrude",
                null,
                java.lang.Double.parseDouble(et_am.text.toString())
            )
        }

        mRootView!!.findViewById<View>(R.id.printview_etrude_button).setOnClickListener {
            OctoprintControl.sendToolCommand(
                activity!!,
                mPrinter!!.address,
                "extrude",
                null,
                -java.lang.Double.parseDouble(et_am.text.toString())
            )
        }

    }

    private fun convertProgress(amount: Int): Double {

        return 0.1 * Math.pow(10.0, Math.abs(amount).toDouble())

    }

    /**
     * Return the custom view of the print view tab
     *
     * @param title Title of the tab
     * @param icon  Icon of the tab
     * @return Custom view of a tab layout
     */
    private fun getTabIndicator(title: String, icon: Int): View {
        val view = LayoutInflater.from(mContext).inflate(R.layout.printview_tab_layout, null)
        val iv = view.findViewById<View>(R.id.tab_icon_imageview) as ImageView
        iv.setImageResource(icon)
        iv.setColorFilter(
            mContext.resources.getColor(R.color.body_text_1),
            PorterDuff.Mode.MULTIPLY
        )
        val tv = view.findViewById<View>(R.id.tab_title_textview) as TextView
        tv.text = title
        return view
    }

    /**
     * Convert progress string to percentage
     *
     * @param p progress string
     * @return converted value
     */
    fun getProgress(p: String): String {

        var value = 0.0

        try {
            value = java.lang.Double.valueOf(p)
        } catch (e: Exception) {
            //e.printStackTrace();
        }

        return value.toInt().toString()
    }

    /**
     * Dinamically update progress bar and text from the main activity
     */
    fun refreshData() {

        //Check around here if files were changed
        tv_printer!!.text = mPrinter!!.displayName + ": " + mPrinter!!.message + " [" + mPrinter!!.port + "]"
        tv_file!!.text = mPrinter!!.job.filename
        //tv_temp!!.text = mPrinter!!.temperature + "ºC / " + mPrinter!!.tempTarget + "ºC"
        //tv_temp_bed!!.text = mPrinter!!.bedTemperature + "ºC / " + mPrinter!!.bedTempTarget + "ºC"

        tv_profile!!.text = " " + mPrinter?.profile ?: "No profile"
        val printer_select_layout = mRootView!!.findViewById<View>(R.id.printer_select_card_view) as CardView

        if (mPrinter!!.status == StateUtils.STATE_PRINTING || mPrinter!!.status == StateUtils.STATE_PAUSED) {

            isPrinting = true

            if (mPrinter!!.status == StateUtils.STATE_PRINTING) {

                button_pause!!.setText(getString(R.string.printview_pause_button))
                icon_pause!!.setImageDrawable(resources.getDrawable(R.drawable.ic_pause))

            } else {

                button_pause!!.setText(getString(R.string.printview_start_button))
                icon_pause!!.setImageDrawable(resources.getDrawable(R.drawable.ic_play))

            }


            tv_prog!!.text =
                    getProgress(mPrinter!!.job.progress) + "% (" +
                    OctoprintConnection.ConvertSecondToHHMMString(mPrinter!!.job.printTimeLeft!!) +
                    " left / " + OctoprintConnection.ConvertSecondToHHMMString(mPrinter!!.job.printTime!!) +
                    " elapsed) - "

            if (mPrinter!!.job.progress != "null") {
                val n = java.lang.Double.valueOf(mPrinter!!.job.progress)
                pb_prog!!.progress = n.toInt()


            }

            if (mDataGcode != null)
                changeProgress(java.lang.Double.valueOf(mPrinter!!.job.progress))

        } else {


            if (!mPrinter!!.loaded) tv_file!!.setText(R.string.devices_upload_waiting)

            if (mPrinter!!.job.progress != "null" && mPrinter!!.job.finished) {

                pb_prog!!.progress = 100
                tv_file!!.setText(R.string.devices_text_completed)
                button_pause!!.setText(getString(R.string.printview_finish_button))
                icon_pause!!.setImageDrawable(resources.getDrawable(R.drawable.ic_action_done))

                mRootView!!.findViewById<View>(R.id.stop_button_container).visibility = View.INVISIBLE

            } else {

                tv_prog!!.text = mPrinter!!.message + " - "
                button_pause!!.setText(getString(R.string.printview_start_button))
                icon_pause!!.setImageDrawable(resources.getDrawable(R.drawable.ic_play))

                mRootView!!.findViewById<View>(R.id.stop_button_container).visibility = View.VISIBLE
            }

            isPrinting = false

        }

        if (mPrinter!!.status == StateUtils.STATE_NONE || mPrinter!!.status == StateUtils.STATE_CLOSED) {

            tv_file!!.visibility = View.INVISIBLE
            tv_prog!!.visibility = View.INVISIBLE
            tv_profile!!.visibility = View.INVISIBLE
            sb_head!!.progress = 0
            mRootView!!.findViewById<View>(R.id.printview_text_profile_tag).visibility = View.INVISIBLE

            ViewHelper.disableEnableAllViews(false, printer_select_layout)

        } else {
            //mRootView.findViewById(R.id.disabled_gray_tint).setVisibility(View.VISIBLE);
            tv_file!!.visibility = View.VISIBLE
            tv_prog!!.visibility = View.VISIBLE
            tv_profile!!.visibility = View.VISIBLE
            sb_head!!.progress = 2
            mRootView!!.findViewById<View>(R.id.printview_text_profile_tag).visibility = View.VISIBLE

            ViewHelper.disableEnableAllViews(true, printer_select_layout)
        }

        activity?.invalidateOptionsMenu()

        notifyAdapter()
    }


    fun stopCameraPlayback() {

        //TODO CAMERA DEISABLE
        mCamera?.view?.let {
            it.stopPlayback()
            it.visibility = View.GONE
        }


    }

    override fun onDestroy() {

        //mContext.unregisterReceiver(onComplete);
        super.onDestroy()
    }

    /**
     * ****************************************************************************************
     *
     *
     * PRINT VIEW PROGRESS HANDLER
     *
     *
     */

    /**
     * Method to check if we own the gcode loaded in the printer to display it or we have to download it.
     */
    fun retrieveGcode() {


        //If we have a jobpath, we've uploaded the file ourselves
        if (mPrinter!!.jobPath != null) {

            Log.i(TAG, "PATH IS " + mPrinter!!.jobPath!!)

            //Get filename
            val currentFile = File(mPrinter!!.jobPath)

            if (currentFile.exists())
            //if it's the same as the server or it's in process of being uploaded
                if (mPrinter!!.job.filename == currentFile.name || !mPrinter!!.loaded) {

                    Log.i(TAG, "Sigh, loading " + mPrinter!!.jobPath!!)

                    if (LibraryController.hasExtension(1, currentFile.name))
                        openGcodePrintView(activity!!, mPrinter!!.jobPath, mRootView, R.id.view_gcode)
                    else
                        Log.i(TAG, "Das not gcode")

                    isGcodeLoaded = true

                    //end process
                    return

                    //Not the same file
                } else
                    Log.i(TAG, "FAIL ;D " + mPrinter!!.jobPath!!)

        }

        if (mPrinter!!.loaded)

            if (mPrinter!!.job.filename != null)
            //The server actually has a job
                if (mPrinter!!.job.filename != "null") {

                    Log.i(TAG, "Either it's not the same or I don't have it, download: " + mPrinter!!.job.filename!!)

                    var download: String? = ""
                    if (DatabaseController.getPreference(DatabaseController.TAG_REFERENCES, mPrinter!!.name) != null) {

                        Log.i(TAG, "NOT NULLO")

                        download = DatabaseController.getPreference(DatabaseController.TAG_REFERENCES, mPrinter!!.name)

                    } else {

                        Log.i(TAG, "Çyesp NULLO")
                        download = LibraryController.parentFolder.toString() + "/temp/" + mPrinter!!.job.filename

                        //Add it to the reference list
                        DatabaseController.handlePreference(
                            DatabaseController.TAG_REFERENCES, mPrinter!!.name,
                            LibraryController.parentFolder.toString() + "/temp/" + mPrinter!!.job.filename, true
                        )
                    }


                    //Check if we've downloaded the same file before
                    //File downloadPath = new File(LibraryController.getParentFolder() + "/temp/", mPrinter.getJob().getFilename());
                    val downloadPath = File(download)

                    if (downloadPath.exists()) {

                        Log.i(TAG, "Wait, I downloaded it once!")
                        openGcodePrintView(activity!!, downloadPath.absolutePath, mRootView, R.id.view_gcode)

                        //File changed, remove jobpath
                        mPrinter!!.jobPath = downloadPath.absolutePath

                        //We have to download it again
                    } else {

                        //Remake temp folder if it's not available
                        if (!downloadPath.parentFile.exists())
                            downloadPath.parentFile.mkdirs()

                        Log.i(
                            TAG,
                            "Downloadinag " + downloadPath.parentFile.absolutePath + " PLUS " + mPrinter!!.job.filename
                        )

                        //Download file
                        OctoprintFiles.downloadFile(
                            mContext!!, mPrinter!!.address + HttpUtils.URL_DOWNLOAD_FILES,
                            downloadPath.parentFile.absolutePath + "/", mPrinter!!.job.filename!!
                        )



                        Log.i(TAG, "Downloading and adding to preferences")

                        //Get progress dialog UI
                        val waitingForServiceDialogView =
                            LayoutInflater.from(mContext).inflate(R.layout.dialog_progress_content_horizontal, null)
                        (waitingForServiceDialogView.findViewById<View>(R.id.progress_dialog_text) as TextView).setText(
                            R.string.printview_download_dialog
                        )

                        //Show progress dialog
                        val connectionDialogBuilder = MaterialDialog(mContext)
                        connectionDialogBuilder.customView(view = waitingForServiceDialogView, scrollable = true)
                            .noAutoDismiss()

                        //Progress dialog to notify command events
                        MaterialDialog(mContext!!)
                            .customView(view = waitingForServiceDialogView, scrollable = true)
                            .noAutoDismiss()
                            .show()

                        //File changed, remove jobpath
                        mPrinter!!.jobPath = null
                    }


                }

        isGcodeLoaded = true
    }


    fun openGcodePrintView(context: Context, filePath: String?, rootView: View?, frameLayoutId: Int) {
        //Context context = getActivity();
        mLayout = rootView!!.findViewById<View>(frameLayoutId) as FrameLayout
        val file = File(filePath)

        val tempData = GcodeCache.retrieveGcodeFromCache(file.absolutePath)

        if (tempData != null) {

            mDataGcode = tempData
            drawPrintView()


        } else {

            mDataGcode = DataStorage()
            mDataGcode?.let {
                GcodeFile.openGcodeFile(context, file, it, ViewerMainFragment.PRINT_PREVIEW)
                GcodeCache.addGcodeToCache(it)
            }
        }


        mDataGcode!!.actualLayer = mActualProgress

    }

    override fun onDestroyView() {

        try {
            mContext!!.unregisterReceiver(onComplete)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, e.message ?: "No Error message")
        }

        super.onDestroyView()
    }

    fun notifyAdapter() {

        try {
            mExtrudersGridAdapter?.notifyDataSetChanged()

            //TODO removed for list video bugs
            //mCameraAdapter.notifyDataSetChanged();
        } catch (e: NullPointerException) {
            //Random adapter crash
            e.printStackTrace()
        }

    }

    companion object {

        private val TAG = "PrintView"

        //Current Printer and status
        private var mPrinter: ModelPrinter? = null

        //File references
        private var mDataGcode: DataStorage? = null
        private var mSurface: ViewerSurfaceView? = null
        private var mLayout: FrameLayout? = null
        private var mLayoutVideo: FrameLayout? = null

        //Context needed for file loading
        private lateinit var mContext: Context
        private var mActualProgress = 0

        fun drawPrintView(): Boolean {
            val gcodeList = ArrayList<DataStorage>()
            mDataGcode?.let { gcodeList.add(it) }

            mSurface = ViewerSurfaceView(
                mContext,
                gcodeList,
                ViewerSurfaceView.LAYERS,
                ViewerMainFragment.PRINT_PREVIEW,
                null
            )

            mLayout!!.removeAllViews()
            mLayout!!.addView(mSurface, 0)

            changeProgress(mActualProgress.toDouble())

            mSurface!!.setZOrderOnTop(true)

            val profile = ModelProfile.retrieveProfile(mContext, mPrinter!!.profile!!, ModelProfile.TYPE_P)
            try {
                val volume = profile!!.getJSONObject("volume")

                mSurface!!.changePlate(
                    intArrayOf(
                        volume.getInt("width") / 2,
                        volume.getInt("depth") / 2,
                        volume.getInt("height")
                    )
                )
                mSurface!!.requestRender()

            } catch (e: JSONException) {
                e.printStackTrace()
            }


            return true
        }

        fun changeProgress(percentage: Double) {
            val maxLines = mDataGcode!!.maxLayer
            val progress = percentage.toInt() * maxLines / 100
            mDataGcode!!.actualLayer = progress
            if (mSurface != null) mSurface!!.requestRender()
        }
    }
}

