package de.domes_muc.printerappkotlin.viewer

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import androidx.fragment.app.Fragment
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.model.ModelProfile
import de.domes_muc.printerappkotlin.octoprint.OctoprintConnection
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import de.domes_muc.printerappkotlin.util.ui.CustomEditableSlider
import de.domes_muc.printerappkotlin.util.ui.CustomPopupWindow
import de.domes_muc.printerappkotlin.util.ui.ListIconPopupWindowAdapter
import de.domes_muc.printerappkotlin.viewer.sidepanel.SidePanelHandler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.devsmart.android.ui.HorizontalListView

import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.ArrayList
import java.util.Locale


//private static Geometry.Point mPreviousOffset;

//Empty constructor
class ViewerMainFragment : Fragment() {

    //Advanced settings expandable panel
    private var mSettingsPanelMinHeight: Int = 0
    private var isKeyboardShown = false

    /**
     * Receives the "download complete" event asynchronously
     */
    private var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {

            if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last") != null)
                if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last") == "temp.gco") {

                    DatabaseController.handlePreference(DatabaseController.TAG_SLICING, "Last", null, false)


                    showProgressBar(StateUtils.SLICER_HIDE, 0)
                } else {

                }


        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Retain instance to keep the Fragment from destroying itself
        retainInstance = true

        mSlicingHandler = SlicingHandler(activity!!)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Reference to View
        mRootView = null

        super.onCreateView(inflater, container, savedInstanceState)

        //If is not new
        if (savedInstanceState == null) {

            //Show custom option menu
            setHasOptionsMenu(true)

            //Inflate the fragment
            mRootView = inflater.inflate(
                R.layout.print_panel_main,
                container, false
            )

            mContext = activity


            //Register receiver
            mContext!!.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

            initUIElements()

            //            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

            //Init slicing elements
            mSidePanelHandler = SidePanelHandler(mSlicingHandler!!, activity!!, mRootView!!)
            currentType = WitboxFaces.TYPE_WITBOX
            currentPlate = intArrayOf(WitboxFaces.WITBOX_LONG, WitboxFaces.WITBOX_WITDH, WitboxFaces.WITBOX_HEIGHT)

            mSurface = ViewerSurfaceView(mContext!!, mDataList, NORMAL, DONT_SNAPSHOT, mSlicingHandler)
            draw()

            //Hide the action bar when editing the scale of the model
            mRootView!!.viewTreeObserver.addOnGlobalLayoutListener {
                val r = Rect()
                mRootView!!.getWindowVisibleDisplayFrame(r)

                if (mSurface!!.editionMode == ViewerSurfaceView.SCALED_EDITION_MODE) {

                    val location = IntArray(2)
                    val heightDiff = mRootView!!.rootView.height - (r.bottom - r.top)

                    if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...

                        if (!isKeyboardShown) {
                            isKeyboardShown = true
                            mActionModePopupWindow!!.contentView.getLocationInWindow(location)

                            if (Build.VERSION.SDK_INT >= 19)
                                mActionModePopupWindow!!.update(location[0], location[1] - MENU_HIDE_OFFSET_SMALL)
                            else
                                mActionModePopupWindow!!.update(location[0], location[1] + MENU_HIDE_OFFSET_BIG)
                        }
                    } else {
                        if (isKeyboardShown) {
                            isKeyboardShown = false
                            mActionModePopupWindow!!.contentView.getLocationInWindow(location)

                            if (Build.VERSION.SDK_INT >= 19)
                                mActionModePopupWindow!!.update(location[0], location[1] + MENU_HIDE_OFFSET_SMALL)
                            else
                                mActionModePopupWindow!!.update(location[0], location[1] - MENU_HIDE_OFFSET_BIG)

                        }

                    }
                }
            }
        }

        return mRootView

    }

    /**
     * ********************** UI ELEMENTS *******************************
     */

    private fun initUIElements() {

        //Set behavior of the expandable panel
        val expandablePanel = mRootView!!.findViewById<View>(R.id.advanced_options_expandable_panel) as FrameLayout
        expandablePanel.post() //Get the initial height of the panel after onCreate is executed
        {
            mSettingsPanelMinHeight = expandablePanel.measuredHeight
        }
        /*final CheckBox expandPanelButton = (CheckBox) mRootView.findViewById(R.id.expand_button_checkbox);
        expandPanelButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Expand/collapse the expandable panel
                if (isChecked) ExpandCollapseAnimation.collapse(expandablePanel, mSettingsPanelMinHeight);
                else ExpandCollapseAnimation.expand(expandablePanel);
            }
        });*/

        //Set elements to handle the model
        mSeekBar = mRootView!!.findViewById<View>(R.id.barLayer) as SeekBar
        mSeekBar!!.thumb.mutate().alpha = 0
        mSeekBar!!.visibility = View.INVISIBLE

        //Undo button bar
        mUndoButtonBar = mRootView!!.findViewById<View>(R.id.model_button_undo_bar_linearlayout) as LinearLayout

        mLayout = mRootView!!.findViewById<View>(R.id.viewer_container_framelayout) as FrameLayout

        mVisibilityModeButton = mRootView!!.findViewById<View>(R.id.visibility_button) as ImageButton
        mVisibilityModeButton!!.setOnClickListener { showVisibilityPopUpMenu() }

        mSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mDataList[0].actualLayer = progress
                mSurface!!.requestRender()
            }
        })


        /*****************************
         * EXTRA
         */
        mProgress = mRootView!!.findViewById<View>(R.id.progress_bar) as ProgressBar
        mProgress!!.visibility = View.GONE
        mSizeText = mRootView!!.findViewById<View>(R.id.axis_info_layout) as LinearLayout
        mActionImage = mRootView!!.findViewById<View>(R.id.print_panel_bar_action_image) as ImageView


        mRotationSlider = mRootView!!.findViewById<View>(R.id.print_panel_slider) as CustomEditableSlider
        mRotationSlider!!.setValue(12)
        mRotationSlider!!.shownValue = 0
        mRotationSlider!!.max = 24
        mRotationSlider!!.isShowNumberIndicator = true
        mRotationSlider!!.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {

                MotionEvent.ACTION_DOWN -> {
                }

                MotionEvent.ACTION_UP -> {
                    mSettingsPanelMinHeight


                    if (mSurface!!.editionMode == ViewerSurfaceView.ROTATION_EDITION_MODE)
                        mSurface!!.refreshRotatedObject()
                    slicingCallback()
                }
            }


            false
        })
        mRotationSlider!!.onValueChangedListener = object : CustomEditableSlider.OnValueChangedListener {

            var lock = false

            override fun onValueChanged(value: Int) {

                //Calculation on a 12 point seekbar
                val newAngle = (value - 12) * POSITIVE_ANGLE

                mRotationSlider!!.shownValue = newAngle.toInt()

                try {


                    if (!lock) {

                        when (mCurrentAxis) {

                            0 -> mSurface!!.rotateAngleAxisX(newAngle)
                            1 -> mSurface!!.rotateAngleAxisY(newAngle)
                            2 -> mSurface!!.rotateAngleAxisZ(newAngle)
                            else -> return
                        }

                    }

                    mSurface!!.requestRender()


                } catch (e: ArrayIndexOutOfBoundsException) {

                    e.printStackTrace()
                }


            }
        }

        mStatusBottomBar = mRootView!!.findViewById<View>(R.id.model_status_bottom_bar) as LinearLayout
        mRotationLayout = mRootView!!.findViewById<View>(R.id.model_button_rotate_bar_linearlayout) as LinearLayout
        mScaleLayout = mRootView!!.findViewById<View>(R.id.model_button_scale_bar_linearlayout) as LinearLayout

        mTextWatcherX = ScaleChangeListener(0)
        mTextWatcherY = ScaleChangeListener(1)
        mTextWatcherZ = ScaleChangeListener(2)

        mScaleEditX = mScaleLayout!!.findViewById<View>(R.id.scale_bar_x_edittext) as EditText
        mScaleEditY = mScaleLayout!!.findViewById<View>(R.id.scale_bar_y_edittext) as EditText
        mScaleEditZ = mScaleLayout!!.findViewById<View>(R.id.scale_bar_z_edittext) as EditText
        mUniformScale = mScaleLayout!!.findViewById<View>(R.id.scale_uniform_button) as ImageButton
        mUniformScale!!.setOnClickListener {
            if (mUniformScale!!.isSelected) {
                mUniformScale!!.isSelected = false
            } else {
                mUniformScale!!.isSelected = true
            }
        }
        mUniformScale!!.isSelected = true

        mScaleEditX!!.addTextChangedListener(mTextWatcherX)
        mScaleEditY!!.addTextChangedListener(mTextWatcherY)
        mScaleEditZ!!.addTextChangedListener(mTextWatcherZ)

        mStatusBottomBar!!.visibility = View.VISIBLE
        mBottomBar = mRootView!!.findViewById<View>(R.id.bottom_bar) as FrameLayout
        mBottomBar!!.visibility = View.INVISIBLE
        mCurrentAxis = -1

    }


    /**
     * ********************** OPTIONS MENU *******************************
     */
    //Create option menu and inflate viewer menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.print_panel_menu, menu)

    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {

        when (item.itemId) {

            R.id.viewer_open -> {
                FileBrowser.openFileBrowser(
                    activity!!,
                    FileBrowser.VIEWER,
                    getString(R.string.choose_file),
                    ".stl",
                    ".gcode"
                )
                return true
            }

            R.id.viewer_save -> {
                saveNewProject()
                return true
            }

            R.id.viewer_restore -> {
                optionRestoreView()
                return true
            }

            R.id.viewer_clean -> {

                optionClean()

                return true
            }

            R.id.library_settings -> {
                hideActionModePopUpWindow()
                hideCurrentActionPopUpWindow()
                MainActivity.showExtraFragment(0, 0)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }


    /**
     * ********************** FILE MANAGEMENT *******************************
     */


    /**
     * Restore the original view and discard the modifications by clearing the data list
     */
    fun optionRestoreView() {


        if (mDataList.size > 0) {
            val pathStl = mDataList[0].pathFile
            mDataList.clear()

            if ( pathStl != null )
                openFile(pathStl)
        }


    }

    private fun changeStlViews(state: Int) {

        //Handle the special mode: LAYER
        if (state == LAYER) {
            val tempFile = File(LibraryController.parentFolder.toString() + "/temp/temp.gco")
            if (tempFile.exists()) {

                //It's the last file
                if (DatabaseController.getPreference("Slicing", "Last") == null) {

                    //Open desired file
                    openFile(tempFile.getAbsolutePath())
                    mCurrentViewMode = state

                } else {
                    Toast.makeText(activity, R.string.viewer_slice_wait, Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(activity, R.string.viewer_slice_wait, Toast.LENGTH_SHORT).show()

            }
        } else {
            if (file != null) {
                if (!file!!.path.endsWith(".stl") && !file!!.path.endsWith(".STL")) {


                    if (openStlFile()) {

                        mCurrentViewMode = state


                    }
                } else {

                    mSurface!!.configViewMode(state)
                    mCurrentViewMode = state
                }


            } else {
                Toast.makeText(activity, R.string.viewer_toast_not_available_2, Toast.LENGTH_SHORT).show()
            }
        }//Handle TRANSPARENT, NORMAL and OVERHANG modes
    }

    private fun openStlFile(): Boolean {

        //Name didn't work with new gcode creation so new stuff!
        //String name = mFile.getName().substring(0, mFile.getName().lastIndexOf('.'));

        val pathStl: String?

        if (mSlicingHandler!!.lastReference != null) {

            pathStl = mSlicingHandler!!.lastReference
            if ( pathStl != null )
                openFile(pathStl)

            return true

        } else {

            //Here's the new stuff!
            pathStl = //LibraryController.getParentFolder().getAbsolutePath() + "/Files/" + name + "/_stl/";
                    file!!.parentFile.parent + "/_stl/"
            val f = File(pathStl)

            //Only when it's a project
            if (f.isDirectory && f.list().size > 0) {
                openFile(pathStl + f.list()[0])

                return true

            } else {
                Toast.makeText(activity, R.string.devices_toast_no_stl, Toast.LENGTH_SHORT).show()

                return false
            }
        }


    }

    /**
     * ********************** SAVE FILE *******************************
     */
    private fun saveNewProject() {
        val createProjectDialog = LayoutInflater.from(mContext).inflate(R.layout.dialog_save_model, null)
        val proyectNameText = createProjectDialog.findViewById<EditText>(R.id.model_name_textview)

        val radioGroup = createProjectDialog.findViewById<View>(R.id.save_mode_radiogroup) as RadioGroup

        proyectNameText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                proyectNameText.error = null
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //do nothing
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                //do nothing
            }
        })

        val dialogTitle: String

        if (file != null)
            dialogTitle = getString(R.string.save) + " - " + file!!.name
        else
            dialogTitle = getString(R.string.save)

        val createFolderDialog = MaterialDialog(activity!!)
            .title(text = dialogTitle)
            .customView(view = createProjectDialog, scrollable = true)
            .positiveButton(R.string.save) {

                val selected = radioGroup.checkedRadioButtonId

                when (selected) {

                    R.id.save_model_stl_checkbox ->
                        if (file != null) {
                            if (LibraryController.hasExtension(0, file!!.name)) {
                                if (StlFile.checkIfNameExists(proyectNameText.text.toString()))
                                    proyectNameText.error =
                                            mContext!!.getString(R.string.proyect_name_not_available)
                                else {
                                    if (StlFile.saveModel(mDataList, proyectNameText.text.toString(), null))
                                        it.dismiss()
                                    else {
                                        Toast.makeText(
                                            mContext,
                                            R.string.error_saving_invalid_model,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        it.dismiss()
                                    }
                                }
                            } else {
                                Toast.makeText(mContext, R.string.devices_toast_no_stl, Toast.LENGTH_SHORT).show()
                                it.dismiss()
                            }
                        } else {

                            Toast.makeText(mContext, R.string.error_saving_invalid_model, Toast.LENGTH_SHORT).show()
                            it.dismiss()
                        }

                    R.id.save_model_gcode_checkbox -> {
                        val fileFrom = File(LibraryController.parentFolder.toString() + "/temp/temp.gco")

                        //if there is a temporary sliced gcode
                        if (fileFrom.exists()) {

                            //Get original project
                            val actualFile = File(mSlicingHandler!!.originalProject)

                            //Save gcode
                            val fileTo = File(
                                actualFile.toString() + "/_gcode/" + proyectNameText.text.toString().replace(
                                    " ",
                                    "_"
                                ) + ".gcode"
                            )

                            //Delete file if success
                            try {
                                fileCopy(fileFrom, fileTo)

                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            if (file!!.name == fileFrom.getName())
                                openFile(fileTo.absolutePath)

                            /**
                             * Use an intent because it's an asynchronous static method without any reference (yet)
                             */
                            val intent = Intent("notify")
                            intent.putExtra("message", "Files")
                            LocalBroadcastManager.getInstance(mContext!!).sendBroadcast(intent)
                        } else {
                            Toast.makeText(activity, R.string.viewer_slice_wait, Toast.LENGTH_SHORT).show()
                        }
                        it.dismiss()
                    }

                    R.id.save_model_overwrite_checkbox -> {
                        Toast.makeText(activity, R.string.option_unavailable, Toast.LENGTH_SHORT).show()
                        it.dismiss()
                    }
                    else -> it.dismiss()
                }
            }
            .negativeButton(R.string.discard) {
                it.cancel()
                it.dismiss()
            }
            .noAutoDismiss()
            .show()
    }

    //Copy a file to another location
    @Throws(IOException::class)
    fun fileCopy(src: File, dst: File) {
        src.copyTo(dst, true)
    }

    /**
     * ********************** SURFACE CONTROL *******************************
     */
    //This method will set the visibility of the surfaceview so it doesn't overlap
    //with the video grid view
    fun setSurfaceVisibility(i: Int) {

        if (mSurface != null) {
            when (i) {
                0 -> mSurface!!.visibility = View.GONE
                1 -> mSurface!!.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Show a pop up window with the visibility options: Normal, overhang, transparent and layers.
     */
    fun showVisibilityPopUpMenu() {

        //Hide action mode pop up window to show the new menu
        hideActionModePopUpWindow()


        //Show a menu with the visibility options
        if (mCurrentActionPopupWindow == null) {
            val actionButtonsValues = mContext!!.resources.getStringArray(R.array.models_visibility_values)
            val actionButtonsIcons = mContext!!.resources.obtainTypedArray(R.array.models_visibility_icons)
            showHorizontalMenuPopUpWindow(mVisibilityModeButton, actionButtonsValues, actionButtonsIcons,
                Integer.toString(mCurrentViewMode), AdapterView.OnItemClickListener { parent, view, position, id ->
                    //Change the view mode of the model
                    changeStlViews(Integer.parseInt(actionButtonsValues[position]))
                    hideCurrentActionPopUpWindow()
                })
        } else {
            hideCurrentActionPopUpWindow()
        }

    }

    /**
     * Notify the side panel adapters, check for null if they're not available yet (rare case)
     */
    fun notifyAdapter() {

        try {
            if (mSidePanelHandler!!.profileAdapter != null)
                mSidePanelHandler!!.profileAdapter!!.notifyDataSetChanged()

            mSidePanelHandler!!.reloadProfileAdapter()

        } catch (e: NullPointerException) {

            e.printStackTrace()
        }


    }

    //Refresh printers when the fragmetn is shown
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        mSidePanelHandler!!.refreshPrinters()
    }

    internal class SliceTask : AsyncTask<Any, Any, Any>() {


        protected override fun doInBackground(objects: Array<Any>): Any? {

            Log.i("Slicer", "Starting background slicing task")

            val newList = ArrayList(mDataList)

            //Code to update the UI─aÇ >
            //Check if the file is not yet loaded
            for (i in newList.indices) {

                if (newList[i].vertexArray == null) {
                    return null
                }

            }

            if (mSlicingHandler != null && file != null) {

                if (LibraryController.hasExtension(0, file!!.name)) {
                    // StlFile.saveModel(newList, null, mSlicingHandler);
                    mSlicingHandler!!.sendTimer(newList)
                }

            }

            return null
        }

        protected override fun onPostExecute(o: Any) {
            super.onPostExecute(o)
        }
    }

    /********************************* RESTORE PANEL  */

    /**
     * check if there is a reference to restore the last panel and open it
     */
    private fun restoreLastPanel() {

        if (mSlicingHandler!!.lastReference == null)
        //Only if there is no last reference
            if (DatabaseController.getPreference(DatabaseController.TAG_RESTORE, "Last") != null) {

                val file = DatabaseController.getPreference(DatabaseController.TAG_RESTORE, "Last")

                val adb = AlertDialog.Builder(mContext)
                adb.setTitle(R.string.viewer_restore_session)

                adb.setPositiveButton(R.string.ok) { dialogInterface, i ->
                    openFileDialog(file)
                    mSlicingHandler!!.lastReference = file
                }

                adb.setNegativeButton(R.string.cancel) { dialogInterface, i ->
                    DatabaseController.handlePreference(
                        DatabaseController.TAG_RESTORE,
                        "Last",
                        null,
                        false
                    )
                }

                adb.show()

            } else {

                Toast.makeText(mContext, "No last session", Toast.LENGTH_SHORT).show()

            }

    }


    private inner class ScaleChangeListener constructor(internal var mAxis: Int) : TextWatcher {


        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {

            mScaleEditX!!.error = null
            mScaleEditY!!.error = null
            mScaleEditZ!!.error = null

        }

        override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {

        }

        override fun afterTextChanged(editable: Editable) {

            var valid = true


            //Check decimals
            if (editable.toString().endsWith(".")) {
                valid = false

            }


            if (valid)
                try {
                    when (mAxis) {

                        0 -> mSurface!!.doScale(
                            java.lang.Float.parseFloat(editable.toString()),
                            0f,
                            0f,
                            mUniformScale!!.isSelected
                        )

                        1 -> mSurface!!.doScale(
                            0f,
                            java.lang.Float.parseFloat(editable.toString()),
                            0f,
                            mUniformScale!!.isSelected
                        )

                        2 -> mSurface!!.doScale(
                            0f,
                            0f,
                            java.lang.Float.parseFloat(editable.toString()),
                            mUniformScale!!.isSelected
                        )
                    }
                } catch (e: NumberFormatException) {

                    e.printStackTrace()

                }


        }
    }

    companion object {
        //Tabs
        private val NORMAL = 0
        private val OVERHANG = 1
        private val TRANSPARENT = 2
        private val XRAY = 3
        private val LAYER = 4

        private var mCurrentViewMode = 0

        //Constants
        val DO_SNAPSHOT = 0
        val DONT_SNAPSHOT = 1
        val PRINT_PREVIEW = 3
        val STL = true
        val GCODE = false

        private val POSITIVE_ANGLE = 15f
        private val NEGATIVE_ANGLE = -15f

        private val MENU_HIDE_OFFSET_SMALL = 20
        private val MENU_HIDE_OFFSET_BIG = 1000

        //Variables
        /**
         * *********************************  SIDE PANEL *******************************************************
         */

        var file: File? = null
            private set

        private var mSurface: ViewerSurfaceView? = null
        private var mLayout: FrameLayout? = null

        //Buttons
        private var mVisibilityModeButton: ImageButton? = null

        private var mSeekBar: SeekBar? = null

        private val mDataList = ArrayList<DataStorage>()

        //Undo button bar
        private var mUndoButtonBar: LinearLayout? = null

        //Edition menu variables
        private var mProgress: ProgressBar? = null

        private var mContext: Context? = null
        private var mRootView: View? = null

        private var mStatusBottomBar: LinearLayout? = null
        private var mBottomBar: FrameLayout? = null
        private var mRotationLayout: LinearLayout? = null
        private var mScaleLayout: LinearLayout? = null
        private var mRotationSlider: CustomEditableSlider? = null
        private var mActionImage: ImageView? = null

        private var mScaleEditX: EditText? = null
        private var mScaleEditY: EditText? = null
        private var mScaleEditZ: EditText? = null
        private var mUniformScale: ImageButton? = null

        private var mTextWatcherX: ScaleChangeListener? = null
        private var mTextWatcherY: ScaleChangeListener? = null
        private var mTextWatcherZ: ScaleChangeListener? = null

        /**
         * ****************************************************************************
         */
        private var mSlicingHandler: SlicingHandler? = null
        private var mSidePanelHandler: SidePanelHandler? = null

        var currentType = WitboxFaces.TYPE_WITBOX
            private set
        var currentPlate = intArrayOf(WitboxFaces.WITBOX_LONG, WitboxFaces.WITBOX_WITDH, WitboxFaces.WITBOX_HEIGHT)
            private set

        private var mSizeText: LinearLayout? = null
        private var mCurrentAxis: Int = 0

        fun resetWhenCancel() {


            //Crashes on printview
            try {
                mDataList.removeAt(mDataList.size - 1)
                mSurface!!.requestRender()

                mCurrentViewMode = NORMAL
                mSurface!!.configViewMode(mCurrentViewMode)
                file = File(mSlicingHandler!!.lastReference)

            } catch (e: Exception) {

                e.printStackTrace()

            }


        }

        /**
         * Change the current rotation axis and update the text accordingly
         *
         *
         * Alberto
         */
        fun changeCurrentAxis(currentAxis: Int) {

            mCurrentAxis = currentAxis

            val currentAngle = 12f

            when (mCurrentAxis) {

                0 -> mRotationSlider!!.setBackgroundColor(Color.GREEN)

                1 -> mRotationSlider!!.setBackgroundColor(Color.RED)
                2 -> mRotationSlider!!.setBackgroundColor(Color.BLUE)
                else -> mRotationSlider!!.setBackgroundColor(Color.TRANSPARENT)
            }

            mSurface!!.setRendererAxis(mCurrentAxis)

            mRotationSlider!!.setValue(currentAngle.toInt())

        }


        /**
         * *************************************************************************
         */

        fun initSeekBar(max: Int) {
            mSeekBar!!.max = max
            mSeekBar!!.progress = max
        }

        fun configureProgressState(v: Int) {
            if (v == View.GONE)
                mSurface!!.requestRender()
            else if (v == View.VISIBLE) mProgress!!.bringToFront()

            mProgress!!.visibility = v
        }

        /**
         * Clean the print panel and delete all references
         */
        fun optionClean() {

            //Delete slicing reference
            //DatabaseController.handlePreference("Slicing", "Last", null, false);

            mDataList.clear()
            file = null

            if (mSlicingHandler != null) {

                mSlicingHandler!!.originalProject = null
                mSlicingHandler!!.lastReference = null
                mSeekBar!!.visibility = View.INVISIBLE
                mSurface!!.requestRender()
                showProgressBar(0, 0)
            }


        }

        /**
         * Open a dialog if it's a GCODE to warn the user about unsaved data loss
         *
         * @param filePath
         */
        fun openFileDialog(filePath: String?) {

            if (LibraryController.hasExtension(0, filePath!!)) {

                if (!StlFile.checkFileSize(File(filePath), mContext!!)) {
                    MaterialDialog(mContext!!)
                        .title(R.string.warning)
                        .message(R.string.viewer_file_size)
                        .negativeButton(R.string.cancel)
                        .positiveButton(R.string.ok) {
                            openFile(filePath)
                        }
                        .show()

                } else {
                    openFile(filePath)
                }
            } else if (LibraryController.hasExtension(1, filePath)) {
                MaterialDialog(mContext!!)
                    .title(R.string.warning)
                    .message(R.string.viewer_open_gcode_dialog)
                    .negativeButton(R.string.cancel)
                    .positiveButton(R.string.ok) {
                        openFile(filePath)
                    }
                    .show()
            }
        }

        //Select the last object added
        fun doPress() {
            mSurface!!.doPress(mDataList.size - 1)
        }


        fun openFile(filePath: String) {
            var data: DataStorage? = null
            //Open the file
            if (LibraryController.hasExtension(0, filePath)) {

                data = DataStorage()

                mVisibilityModeButton!!.visibility = View.VISIBLE
                file = File(filePath)
                StlFile.openStlFile(mContext!!, file!!, data, DONT_SNAPSHOT)
                mSidePanelHandler!!.enableProfileSelection(true)
                mCurrentViewMode = NORMAL

            } else if (LibraryController.hasExtension(1, filePath!!)) {

                data = DataStorage()
                if (!filePath!!.contains("/temp")) {
                    mVisibilityModeButton!!.visibility = View.GONE
                    optionClean()
                }
                file = File(filePath)
                GcodeFile.openGcodeFile(mContext!!, file!!, data, DONT_SNAPSHOT)
                mSidePanelHandler!!.enableProfileSelection(false)
                mCurrentViewMode = LAYER

            }

            data?.let { mDataList.add(it) }


            //Adding original project //TODO elsewhere?
            if (mSlicingHandler != null)
                if (mSlicingHandler!!.originalProject == null) {
                    mSlicingHandler!!.originalProject = file!!.parentFile.parent
                } else {
                    if (!file!!.absolutePath.contains("/temp")) {
                        mSlicingHandler!!.originalProject = file!!.parentFile.parent
                    }
                }


        }

        fun draw() {
            //Once the file has been opened, we need to refresh the data list. If we are opening a .gcode file, we need to ic_action_delete the previous files (.stl and .gcode)
            //If we are opening a .stl file, we need to ic_action_delete the previous file only if it was a .gcode file.
            //We have to do this here because user can cancel the opening of the file and the Print Panel would appear empty if we clear the data list.

            var filePath = ""
            if (file != null) filePath = file!!.absolutePath

            if (LibraryController.hasExtension(0, filePath)) {
                if (mDataList.size > 1) {
                    if (LibraryController.hasExtension(1, mDataList[mDataList.size - 2].pathFile!!)) {
                        mDataList.removeAt(mDataList.size - 2)
                    }
                }
                Geometry.relocateIfOverlaps(mDataList)
                mSeekBar!!.visibility = View.INVISIBLE

            } else if (LibraryController.hasExtension(1, filePath)) {
                if (mDataList.size > 1)
                    while (mDataList.size > 1) {
                        mDataList.removeAt(0)
                    }
                mSeekBar!!.visibility = View.VISIBLE
            }

            //Add the view
            mLayout!!.removeAllViews()
            mLayout!!.addView(mSurface, 0)
            mLayout!!.addView(mSeekBar, 1)
            mLayout!!.addView(mSizeText, 2)

            //      mLayout.addView(mUndoButtonBar, 3);
            //      mLayout.addView(mEditionLayout, 2);
        }

        private var mActionModePopupWindow: PopupWindow? = null
        private var mCurrentActionPopupWindow: PopupWindow? = null

        /**
         * ********************** ACTION MODE *******************************
         */

        /**
         * Show a pop up window with the available actions of the item
         */
        fun showActionModePopUpWindow() {

            hideCurrentActionPopUpWindow()

            mSizeText!!.visibility = View.VISIBLE

            if (mActionModePopupWindow == null) {

                //Get the content view of the pop up window
                val popupLayout = (mContext as Activity).layoutInflater
                    .inflate(R.layout.item_edit_popup_menu, null) as LinearLayout
                popupLayout.measure(0, 0)

                //Set the behavior of the action buttons
                var imageButtonHeight = 0
                for (i in 0 until popupLayout.childCount) {
                    val v = popupLayout.getChildAt(i)
                    if (v is ImageButton) {
                        imageButtonHeight = v.measuredHeight
                        v.setOnClickListener { view -> onActionItemSelected(view as ImageButton) }
                    }
                }

                //Show the pop up window in the correct position
                val viewerContainerCoordinates = IntArray(2)
                mLayout!!.getLocationOnScreen(viewerContainerCoordinates)
                val popupLayoutPadding = mContext!!.resources.getDimensionPixelSize(R.dimen.content_padding_normal)
                val popupLayoutWidth = popupLayout.measuredWidth
                val popupLayoutHeight = popupLayout.measuredHeight
                val popupLayoutX = viewerContainerCoordinates[0] + mLayout!!.width - popupLayoutWidth
                val popupLayoutY = viewerContainerCoordinates[1] + imageButtonHeight + popupLayoutPadding

                mActionModePopupWindow = CustomPopupWindow(
                    popupLayout, popupLayoutWidth,
                    popupLayoutHeight, R.style.SlideRightAnimation
                ).popupWindow

                mActionModePopupWindow!!.showAtLocation(
                    mSurface, Gravity.NO_GRAVITY,
                    popupLayoutX, popupLayoutY
                )

            }
        }

        /**
         * Hide the action mode pop up window
         */
        fun hideActionModePopUpWindow() {
            if (mActionModePopupWindow != null) {
                mActionModePopupWindow!!.dismiss()
                mSurface!!.exitEditionMode()
                mRotationLayout!!.visibility = View.GONE
                mScaleLayout!!.visibility = View.GONE
                mStatusBottomBar!!.visibility = View.VISIBLE
                mBottomBar!!.visibility = View.INVISIBLE
                mActionModePopupWindow = null
                mSurface!!.setRendererAxis(-1)
            }

            //Hide size text
            if (mSizeText != null)
                if (mSizeText!!.visibility == View.VISIBLE) mSizeText!!.visibility = View.INVISIBLE

            //hideCurrentActionPopUpWindow();
        }

        /**
         * Hide the current action pop up window if it is showing
         */
        fun hideCurrentActionPopUpWindow() {
            if (mCurrentActionPopupWindow != null) {
                mCurrentActionPopupWindow!!.dismiss()
                mCurrentActionPopupWindow = null
            }
            hideSoftKeyboard()
        }

        fun hideSoftKeyboard() {
            try {
                val inputMethodManager =
                    mContext!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow((mContext as Activity).currentFocus!!.windowToken, 0)
            } catch (e: NullPointerException) {

            }

        }

        /**
         * Perform the required action depending on the pressed button
         *
         * @param item Action button that has been pressed
         */
        fun onActionItemSelected(item: ImageButton) {

            mStatusBottomBar!!.visibility = View.VISIBLE
            mSurface!!.setRendererAxis(-1)
            mRotationLayout!!.visibility = View.GONE
            mScaleLayout!!.visibility = View.GONE
            mBottomBar!!.visibility = View.INVISIBLE
            mSizeText!!.visibility = View.VISIBLE

            selectActionButton(item.id)

            when (item.id) {
                R.id.move_item_button -> {
                    hideCurrentActionPopUpWindow()
                    mSurface!!.editionMode = ViewerSurfaceView.MOVE_EDITION_MODE
                }
                R.id.rotate_item_button ->

                    if (mCurrentActionPopupWindow == null) {
                        val actionButtonsValues = mContext!!.resources.getStringArray(R.array.rotate_model_values)
                        val actionButtonsIcons = mContext!!.resources.obtainTypedArray(R.array.rotate_model_icons)
                        showHorizontalMenuPopUpWindow(
                            item,
                            actionButtonsValues,
                            actionButtonsIcons,
                            null,
                            AdapterView.OnItemClickListener { parent, view, position, id ->
                                changeCurrentAxis(Integer.parseInt(actionButtonsValues[position]))
                                mBottomBar!!.visibility = View.VISIBLE
                                mRotationLayout!!.visibility = View.VISIBLE
                                mSurface!!.editionMode = ViewerSurfaceView.ROTATION_EDITION_MODE
                                hideCurrentActionPopUpWindow()
                                item.setImageResource(actionButtonsIcons.getResourceId(position, -1))
                                mActionImage!!.setImageDrawable(
                                    mContext!!.resources.getDrawable(
                                        actionButtonsIcons.getResourceId(
                                            position,
                                            -1
                                        )
                                    )
                                )
                            })
                    } else {
                        hideCurrentActionPopUpWindow()
                    }
                R.id.scale_item_button -> {
                    hideCurrentActionPopUpWindow()
                    mBottomBar!!.visibility = View.VISIBLE
                    mScaleLayout!!.visibility = View.VISIBLE
                    mSurface!!.editionMode = ViewerSurfaceView.SCALED_EDITION_MODE
                    mActionImage!!.setImageDrawable(mContext!!.resources.getDrawable(R.drawable.ic_action_scale))
                    displayModelSize(mSurface!!.objectPresed)
                }
                /*case R.id.mirror:
                    mSurface.setEditionMode(ViewerSurfaceView.MIRROR_EDITION_MODE);
                    mSurface.doMirror();

                    slicingCallback();
                    break;*/
                R.id.multiply_item_button -> {
                    hideCurrentActionPopUpWindow()
                    showMultiplyDialog()
                }
                R.id.delete_item_button -> {
                    hideCurrentActionPopUpWindow()
                    mSurface!!.deleteObject()
                    hideActionModePopUpWindow()
                }
            }

        }


        /**
         * Set the state of the selected action button
         *
         * @param selectedId Id of the action button that has been pressed
         */
        fun selectActionButton(selectedId: Int) {

            if (mActionModePopupWindow != null) {
                //Get the content view of the pop up window
                val popupLayout = mActionModePopupWindow!!.contentView as LinearLayout

                //Set the behavior of the action buttons
                for (i in 0 until popupLayout.childCount) {
                    val v = popupLayout.getChildAt(i)
                    if (v is ImageButton) {
                        if (v.id == selectedId)
                            v.setBackgroundDrawable(mContext!!.resources.getDrawable(R.drawable.oval_background_green))
                        else
                            v.setBackgroundDrawable(mContext!!.resources.getDrawable(R.drawable.action_button_selector_dark))
                    }
                }
            }
        }

        /**
         * Show a pop up window with a horizontal list view as a content view
         */
        fun showHorizontalMenuPopUpWindow(
            currentView: View?, actionButtonsValues: Array<String>,
            actionButtonsIcons: TypedArray,
            selectedOption: String?,
            onItemClickListener: AdapterView.OnItemClickListener
        ) {

            val landscapeList = HorizontalListView(mContext, null)
            val listAdapter =
                ListIconPopupWindowAdapter(mContext!!, actionButtonsValues, actionButtonsIcons, selectedOption!!)
            landscapeList.setOnItemClickListener(onItemClickListener)
            landscapeList.setAdapter(listAdapter)

            landscapeList.measure(0, 0)

            var popupLayoutHeight = 0
            var popupLayoutWidth = 0
            for (i in 0 until listAdapter.getCount()) {
                val mView = listAdapter.getView(i, null, landscapeList)
                mView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                popupLayoutHeight = mView.getMeasuredHeight()
                popupLayoutWidth += mView.getMeasuredWidth()
            }

            //Show the pop up window in the correct position
            val actionButtonCoordinates = IntArray(2)
            currentView!!.getLocationOnScreen(actionButtonCoordinates)
            val popupLayoutPadding = mContext!!.resources.getDimensionPixelSize(R.dimen.content_padding_normal)
            val popupLayoutX = actionButtonCoordinates[0] - popupLayoutWidth - popupLayoutPadding / 2
            val popupLayoutY = actionButtonCoordinates[1]

            mCurrentActionPopupWindow = CustomPopupWindow(
                landscapeList, popupLayoutWidth,
                popupLayoutHeight + popupLayoutPadding, R.style.SlideRightAnimation
            ).popupWindow

            mCurrentActionPopupWindow!!.showAtLocation(mSurface, Gravity.NO_GRAVITY, popupLayoutX, popupLayoutY)
        }

        /**
         * ********************** MULTIPLY ELEMENTS *******************************
         */

        fun showMultiplyDialog() {
            val multiplyModelDialog = LayoutInflater.from(mContext).inflate(R.layout.dialog_multiply_model, null)
            val numPicker = multiplyModelDialog.findViewById<View>(R.id.number_copies_numberpicker) as NumberPicker
            numPicker.maxValue = 10
            numPicker.minValue = 0

            val count = numPicker.childCount
            for (i in 0 until count) {
                val child = numPicker.getChildAt(i)
                if (child is EditText) {
                    try {
                        val selectorWheelPaintField = numPicker.javaClass
                            .getDeclaredField("mSelectorWheelPaint")
                        selectorWheelPaintField.isAccessible = true
                        (selectorWheelPaintField.get(numPicker) as Paint).color =
                                mContext!!.resources.getColor(R.color.theme_primary_dark)
                        child.setTextColor(mContext!!.resources.getColor(R.color.theme_primary_dark))

                        val pickerFields = NumberPicker::class.java.declaredFields
                        for (pf in pickerFields) {
                            if (pf.name == "mSelectionDivider") {
                                pf.isAccessible = true
                                try {
                                    pf.set(
                                        numPicker,
                                        mContext!!.resources.getDrawable(R.drawable.separation_line_horizontal)
                                    )
                                } catch (e: IllegalArgumentException) {
                                    e.printStackTrace()
                                } catch (e: Resources.NotFoundException) {
                                    e.printStackTrace()
                                } catch (e: IllegalAccessException) {
                                    e.printStackTrace()
                                }

                                break
                            }
                        }

                        numPicker.invalidate()
                    } catch (e: NoSuchFieldException) {
                        Log.w("setNumberPickerTextColor", e.toString())
                    } catch (e: IllegalAccessException) {
                        Log.w("setNumberPickerTextColor", e.toString())
                    } catch (e: IllegalArgumentException) {
                        Log.w("setNumberPickerTextColor", e.toString())
                    }

                }
            }

            //Remove soft-input from number picker
            numPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            MaterialDialog(mContext!!)
                .title(R.string.viewer_menu_multiply_title)
                .customView(view = multiplyModelDialog, scrollable = true)
                .positiveButton(R.string.dialog_continue) {
                    drawCopies(numPicker.value)
                    slicingCallback()
                }
                .negativeButton(R.string.cancel)
                .noAutoDismiss()
                .show()

        }

        private fun drawCopies(numCopies: Int) {
            val model = mSurface!!.objectPresed
            var num = 0

            while (num < numCopies) {
                val newData = DataStorage()
                newData.copyData(mDataList[model])
                mDataList.add(newData)

                /**
                 * Check if the piece is out of the plate and stop multiplying
                 */
                if (!Geometry.relocateIfOverlaps(mDataList)) {

                    Toast.makeText(mContext, R.string.viewer_multiply_error, Toast.LENGTH_LONG).show()
                    mDataList.remove(newData)
                    break

                }

                num++
            }

            draw()
        }

        /**
         * **************************** PROGRESS BAR FOR SLICING ******************************************
         */

        /**
         * Static method to show the progress bar by sending an integer when receiving data from the socket
         *
         * @param i either -1 to hide the progress bar, 0 to show an indefinite bar, or a normal integer
         */
        fun showProgressBar(status: Int, i: Int) {


            if (mRootView != null) {


                val pb = mRootView!!.findViewById<View>(R.id.progress_slice) as ProgressBar
                val tv = mRootView!!.findViewById<View>(R.id.viewer_text_progress_slice) as TextView
                val tve = mRootView!!.findViewById<View>(R.id.viewer_text_estimated_time) as TextView
                val tve_title = mRootView!!.findViewById<View>(R.id.viewer_estimated_time_textview) as TextView

                if (mSlicingHandler!!.lastReference != null) {

                    tve_title.visibility = View.VISIBLE
                    pb.visibility = View.VISIBLE

                    when (status) {

                        StateUtils.SLICER_HIDE -> {

                            if (i < 0) {

                                tv.setText(R.string.error)

                            } else {
                                tv.setText(R.string.viewer_text_downloaded)
                            }

                            pb.visibility = View.INVISIBLE
                        }

                        StateUtils.SLICER_UPLOAD -> {

                            var uploadText = mContext!!.getString(R.string.viewer_text_uploading)


                            if (i == 0)
                                pb.isIndeterminate = true
                            else {

                                pb.progress = i
                                pb.isIndeterminate = false

                                uploadText += " ($i%)"

                            }

                            tv.text = uploadText
                            tve.text = null
                        }

                        StateUtils.SLICER_SLICE -> {

                            var slicingText = mContext!!.getString(R.string.viewer_text_slicing)


                            if (i == 0) {
                                pb.isIndeterminate = true

                            } else if (i == 100) {

                                pb.isIndeterminate = false
                                pb.progress = 100

                                slicingText += "  " + mContext!!.getString(R.string.viewer_text_done)

                            } else {

                                pb.progress = i
                                pb.isIndeterminate = false

                                slicingText += "  ($i%)"

                            }

                            tv.text = slicingText
                            tve.text = null

                            mRootView!!.invalidate()
                        }

                        StateUtils.SLICER_DOWNLOAD -> {


                            if (i > 0) {
                                tve.text = OctoprintConnection.ConvertSecondToHHMMString(i.toString())
                            }
                            tv.setText(R.string.viewer_text_downloading)
                            pb.isIndeterminate = true
                        }

                        else -> {
                        }
                    }

                } else {

                    pb.visibility = View.INVISIBLE
                    tve_title.visibility = View.INVISIBLE
                    tv.text = null
                    tve.text = null
                    mRootView!!.invalidate()


                }
            }


        }

        /**
         * Display model width, depth and height when touched
         */
        fun displayModelSize(position: Int) {
            try {
                //TODO RANDOM CRASH ArrayIndexOutOfBoundsException
                val data = mDataList[position]

                //Set point instead of comma
                val otherSymbols = DecimalFormatSymbols(Locale.ENGLISH)
                otherSymbols.decimalSeparator = '.'
                otherSymbols.groupingSeparator = ','

                //Define new decimal format to display only 2 decimals
                val df = DecimalFormat("##.##", otherSymbols)

                val width = df.format((data.maxX - data.minX).toDouble())
                val depth = df.format((data.maxY - data.minY).toDouble())
                val height = df.format((data.maxZ - data.minZ).toDouble())

                //Display size of the model
                //mSizeText.setText("W = " + width + " mm / D = " + depth + " mm / H = " + height + " mm");
                //mSizeText.setText(String.format(mContext.getResources().getString(R.string.viewer_axis_info), Double.parseDouble(width), Double.parseDouble(depth), Double.parseDouble(height)));

                Log.i("Scale", "Vamos a petar $width")
                (mSizeText!!.findViewById<View>(R.id.print_panel_x_size) as TextView).text = width
                (mSizeText!!.findViewById<View>(R.id.print_panel_y_size) as TextView).text = depth
                (mSizeText!!.findViewById<View>(R.id.print_panel_z_size) as TextView).text = height

                if (mScaleLayout!!.visibility == View.VISIBLE) {

                    mScaleEditX!!.removeTextChangedListener(mTextWatcherX)
                    mScaleEditY!!.removeTextChangedListener(mTextWatcherY)
                    mScaleEditZ!!.removeTextChangedListener(mTextWatcherZ)

                    mScaleEditX!!.setText(width)
                    mScaleEditX!!.setSelection(mScaleEditX!!.text.length)
                    mScaleEditY!!.setText(depth)
                    mScaleEditY!!.setSelection(mScaleEditY!!.text.length)
                    mScaleEditZ!!.setText(height)
                    mScaleEditZ!!.setSelection(mScaleEditZ!!.text.length)

                    mScaleEditX!!.addTextChangedListener(mTextWatcherX)
                    mScaleEditY!!.addTextChangedListener(mTextWatcherY)
                    mScaleEditZ!!.addTextChangedListener(mTextWatcherZ)
                }

            } catch (e: ArrayIndexOutOfBoundsException) {

                e.printStackTrace()
            }


        }


        fun slicingCallback() {

            val sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext)
            if (sharedPref.getBoolean(mContext!!.resources.getString(R.string.shared_preferences_autoslice), true)) {

                val task = SliceTask()
                mSidePanelHandler!!.refreshPrinters()
                task.execute()
            } else {

                mSidePanelHandler!!.refreshPrinters()
                mSidePanelHandler!!.switchSlicingButton(true)
            }


        }

        fun slicingCallbackForced() {

            //        SliceTask task = new SliceTask();
            mSidePanelHandler!!.refreshPrinters()
            //        task.execute();

            val slicingHandler = Handler()
            slicingHandler.post(mSliceRunnable)
        }

        internal var mSliceRunnable: Runnable = Runnable {
            val newList = ArrayList(mDataList)

            //Code to update the UI─aÇ >
            //Check if the file is not yet loaded
            for (i in newList.indices) {

                if (newList[i].vertexArray == null) {
                    return@Runnable
                }

            }

            if (mSlicingHandler != null && file != null) {

                if (LibraryController.hasExtension(0, file!!.name)) {
                    // StlFile.saveModel(newList, null, mSlicingHandler);
                    mSlicingHandler!!.sendTimer(newList)
                }

            }
        }

        @Throws(NullPointerException::class)
        fun changePlate(resource: String) {

            val profile = ModelProfile.retrieveProfile(mContext!!, resource, ModelProfile.TYPE_P)

            try {
                val volume = profile!!.getJSONObject("volume")
                currentPlate =
                        intArrayOf(volume.getInt("width") / 2, volume.getInt("depth") / 2, volume.getInt("height"))

            } catch (e: JSONException) {
                e.printStackTrace()
            }

            mSurface!!.changePlate(currentPlate)
            mSurface!!.requestRender()
        }

        fun setSlicingPosition(x: Float, y: Float) {

            val position = JSONObject()
            try {

                //mPreviousOffset = new Geometry.Point(x,y,0);

                position.put("x", x.toInt() + currentPlate[0])
                position.put("y", y.toInt() + currentPlate[1])

                mSlicingHandler!!.setExtras("position", position)
            } catch (e: JSONException) {
                e.printStackTrace()
            }


        }

        fun isOutsidePlate(x: Float, y: Float): Boolean {

            return if (x < currentPlate[1] || y < currentPlate[2]) {
                true
            } else {
                false
            }

        }

        fun displayErrorInAxis(axis: Int) {

            if (mScaleLayout!!.visibility == View.VISIBLE) {
                when (axis) {

                    0 -> mScaleEditX!!.error =
                            mContext!!.resources.getString(R.string.viewer_error_bigger_plate, (currentPlate[0] * 2).toString())

                    1 -> mScaleEditY!!.error =
                            mContext!!.resources.getString(R.string.viewer_error_bigger_plate, (currentPlate[1] * 2).toString())
                }
            }


        }
    }


}
