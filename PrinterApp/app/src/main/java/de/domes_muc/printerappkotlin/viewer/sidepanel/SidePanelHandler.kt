package de.domes_muc.printerappkotlin.viewer.sidepanel

import android.app.Activity
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.model.ModelProfile
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import de.domes_muc.printerappkotlin.util.ui.CustomPopupWindow
import de.domes_muc.printerappkotlin.util.ui.ViewHelper
import de.domes_muc.printerappkotlin.viewer.SlicingHandler
import de.domes_muc.printerappkotlin.viewer.ViewerMainFragment
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.preference.PreferenceManager
//import androidx.cardview.widget.CardView;
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.material.widget.PaperButton
import com.rengwuxian.materialedittext.MaterialEditText

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.File

/**
 * Class to initialize and handle the side panel in the print panel
 * Created by alberto-baeza on 10/24/14.
 */
class SidePanelHandler//Constructor
    (//Inherited elements
    private val mSlicingHandler: SlicingHandler, private val mActivity: Activity, private val mRootView: View
) {
    private var mCurrentInfill = DEFAULT_INFILL

    //Printer to send the files
    private var mPrinter: ModelPrinter? = null

    //UI elements
    private var printButton: PaperButton? = null
    private var sliceButton: PaperButton? = null
    private var saveButton: PaperButton? = null
    private var restoreButton: PaperButton? = null
    private var deleteButton: PaperButton? = null

    private var s_profile: Spinner? = null
    private var s_type: Spinner? = null
    private var s_adhesion: Spinner? = null
    private var s_support: Spinner? = null

    private var s_infill: RelativeLayout? = null
    private var mInfillOptionsPopupWindow: PopupWindow? = null
    private var infillText: TextView? = null

    var printerAdapter: SidePanelPrinterAdapter? = null
    var profileAdapter: SidePanelProfileAdapter? = null

    private var layerHeight: EditText? = null
    private var shellThickness: EditText? = null
    private var enableRetraction: CheckBox? = null
    private var bottomTopThickness: EditText? = null
    private var printSpeed: EditText? = null
    private var printTemperature: EditText? = null
    private var filamentDiamenter: EditText? = null
    private var filamentFlow: EditText? = null

    private var travelSpeed: EditText? = null
    private var bottomLayerSpeed: EditText? = null
    private var infillSpeed: EditText? = null
    private var outerShellSpeed: EditText? = null
    private var innerShellSpeed: EditText? = null

    private var minimalLayerTime: EditText? = null
    private var enableCoolingFan: CheckBox? = null

    init {
        mPrinter = null

        initUiElements()
        initSidePanel()

    }

    //Initialize UI references
    fun initUiElements() {

        s_type = mRootView.findViewById<View>(R.id.type_spinner) as Spinner
        s_profile = mRootView.findViewById<View>(R.id.profile_spinner) as Spinner
        s_adhesion = mRootView.findViewById<View>(R.id.adhesion_spinner) as Spinner
        s_support = mRootView.findViewById<View>(R.id.support_spinner) as Spinner

        s_infill = mRootView.findViewById<View>(R.id.infill_spinner) as RelativeLayout
        infillText = mRootView.findViewById<View>(R.id.infill_number_view) as TextView

        printButton = mRootView.findViewById<View>(R.id.print_model_button) as PaperButton
        sliceButton = mRootView.findViewById<View>(R.id.slice_model_button) as PaperButton
        saveButton = mRootView.findViewById<View>(R.id.save_settings_button) as PaperButton
        restoreButton = mRootView.findViewById<View>(R.id.restore_settings_button) as PaperButton
        deleteButton = mRootView.findViewById<View>(R.id.delete_settings_button) as PaperButton

        layerHeight = mRootView.findViewById<View>(R.id.layer_height_edittext) as EditText
        shellThickness = mRootView.findViewById<View>(R.id.shell_thickness_edittext) as EditText
        enableRetraction = mRootView.findViewById<View>(R.id.enable_retraction_checkbox) as CheckBox
        bottomTopThickness = mRootView.findViewById<View>(R.id.bottom_top_thickness_edittext) as EditText
        printSpeed = mRootView.findViewById<View>(R.id.print_speed_edittext) as EditText
        printTemperature = mRootView.findViewById<View>(R.id.print_temperature_edittext) as EditText
        filamentDiamenter = mRootView.findViewById<View>(R.id.diameter_edittext) as EditText
        filamentFlow = mRootView.findViewById<View>(R.id.flow_title_edittext) as EditText

        travelSpeed = mRootView.findViewById<View>(R.id.travel_speed_edittext) as EditText
        bottomLayerSpeed = mRootView.findViewById<View>(R.id.bottom_layer_speed_edittext) as EditText
        infillSpeed = mRootView.findViewById<View>(R.id.infill_speed_edittext) as EditText
        outerShellSpeed = mRootView.findViewById<View>(R.id.outher_shell_speed_edittext) as EditText
        innerShellSpeed = mRootView.findViewById<View>(R.id.inner_shell_speed_edittext) as EditText

        minimalLayerTime = mRootView.findViewById<View>(R.id.minimal_layer_time_edittext) as EditText
        enableCoolingFan = mRootView.findViewById<View>(R.id.enable_cooling_fan_checkbox) as CheckBox

        //profileText = (EditText) mRootView.findViewById(R.id.profile_edittext);

        // SCROLL VIEW HACK

        /**
         * Removes focus from the scrollview when notifying the adapter
         */
        val view = mRootView.findViewById<View>(R.id.advanced_options_scroll_view) as ScrollView
        view.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.setOnTouchListener { v, event ->
            v.requestFocusFromTouch()
            false
        }

        mRootView.findViewById<View>(R.id.connect_printer_button).setOnClickListener {
            //Open devices panel to connect a new printer
            MainActivity.performClick(2)
        }

        initTextWatchers()

    }

    fun initTextWatchers() {

        layerHeight!!.addTextChangedListener(GenericTextWatcher("profile.layer_height"))
        shellThickness!!.addTextChangedListener(GenericTextWatcher("profile.wall_thickness"))
        enableRetraction!!.setOnCheckedChangeListener(GenericTextWatcher("profile.retraction_enable"))
        bottomTopThickness!!.addTextChangedListener(GenericTextWatcher("profile.solid_layer_thickness"))
        printSpeed!!.addTextChangedListener(GenericTextWatcher("profile.print_speed"))
        printTemperature!!.addTextChangedListener(GenericTextWatcher("profile.print_temperature"))
        filamentDiamenter!!.addTextChangedListener(GenericTextWatcher("profile.filament_diameter"))
        filamentFlow!!.addTextChangedListener(GenericTextWatcher("profile.filament_flow"))
        travelSpeed!!.addTextChangedListener(GenericTextWatcher("profile.travel_speed"))
        bottomLayerSpeed!!.addTextChangedListener(GenericTextWatcher("profile.bottom_layer_speed"))
        infillSpeed!!.addTextChangedListener(GenericTextWatcher("profile.infill_speed"))
        outerShellSpeed!!.addTextChangedListener(GenericTextWatcher("profile.outer_shell_speed"))
        innerShellSpeed!!.addTextChangedListener(GenericTextWatcher("profile.inner_shell_speed"))
        minimalLayerTime!!.addTextChangedListener(GenericTextWatcher("profile.cool_min_layer_time"))
        enableCoolingFan!!.setOnCheckedChangeListener(GenericTextWatcher("profile.fan_enabled"))

    }

    //Enable/disable profile options depending on the model type
    fun enableProfileSelection(enable: Boolean) {

        s_profile!!.isEnabled = enable
        s_support!!.isEnabled = enable
        s_adhesion!!.isEnabled = enable
        s_infill!!.isEnabled = enable

    }

    //Initializes the side panel with the printer data
    fun initSidePanel() {

        val handler = Handler()

        handler.post {
            try {

                //Initialize item listeners

                /************************* INITIALIZE TYPE SPINNER  */

                /************************* INITIALIZE TYPE SPINNER  */

                s_type!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {

                        when (i) {

                            0 -> ViewerMainFragment.changePlate(ModelProfile.WITBOX_PROFILE)
                            1 -> ViewerMainFragment.changePlate(ModelProfile.PRUSA_PROFILE)
                            else ->

                                //TODO Profiles being removed automatically

                                try {

                                    ViewerMainFragment.changePlate(s_type!!.selectedItem.toString())

                                } catch (e: NullPointerException) {

                                }

                        }


                        mPrinter = DevicesListController.selectAvailablePrinter(i + 1, s_type!!.selectedItem.toString())
                        mPrinter?.let { mSlicingHandler.setPrinter(it) }

                        ViewerMainFragment.slicingCallback()

                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>) {

                    }
                }

                reloadProfileAdapter()

                /******************** INITIALIZE SECONDARY PANEL  */

                /******************** INITIALIZE SECONDARY PANEL  */

                //Set slicing parameters to send to the server

                //The quality adapter is set by the printer spinner
                s_profile!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {

                        parseJson(
                            ModelProfile.retrieveProfile(
                                mActivity,
                                s_profile!!.selectedItem.toString(),
                                ModelProfile.TYPE_Q
                            )
                        )
                        //mSlicingHandler.setExtras("profile", s_profile.getSelectedItem().toString());

                        if (i > 2) {

                            refreshProfileExtras()

                        } else {
                            reloadBasicExtras()
                            mSlicingHandler.setExtras("profile", s_profile!!.selectedItem.toString())

                        }

                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>) {
                        mSlicingHandler.setExtras("profile", "")
                    }
                }

                reloadQualityAdapter()

                //Adhesion type
                s_adhesion!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {

                        mSlicingHandler.setExtras(
                            "profile.platform_adhesion",
                            s_adhesion!!.getItemAtPosition(i).toString().toLowerCase()
                        )

                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>) {
                        mSlicingHandler.setExtras("profile.fill_density", "")
                    }
                }

                //Support
                s_support!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                        mSlicingHandler.setExtras(
                            "profile.support",
                            s_support!!.getItemAtPosition(i).toString().toLowerCase()
                        )

                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>) {
                        mSlicingHandler.setExtras("profile.support", "")
                    }
                }

                val adapter_adhesion = ArrayAdapter(
                    mActivity,
                    R.layout.print_panel_spinner_item, ADHESION_OPTIONS
                )
                adapter_adhesion.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item)
                val adapter_support = ArrayAdapter(
                    mActivity,
                    R.layout.print_panel_spinner_item, SUPPORT_OPTIONS
                )
                adapter_support.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item)

                // s_profile.setAdapter(adapter_profile);
                s_adhesion!!.adapter = adapter_adhesion
                s_support!!.adapter = adapter_support

                infillText!!.text = DEFAULT_INFILL.toString() + "%"
                s_infill!!.setOnClickListener { openInfillPopupWindow() }

                /** */

                /** */

                /******************************** INITIALIZE BUTTONS  */

                /******************************** INITIALIZE BUTTONS  */

                //Send a print command
                printButton!!.setOnClickListener {
                    refreshPrinters()
                    sendToPrint()
                }

                sliceButton!!.setOnClickListener {
                    ViewerMainFragment.slicingCallbackForced()
                    switchSlicingButton(false)
                }

                saveButton!!.setOnClickListener { saveProfile() }

                restoreButton!!.setOnClickListener {
                    //parseJson(s_profile.getSelectedItemPosition());
                    parseJson(
                        ModelProfile.retrieveProfile(
                            mActivity,
                            s_profile!!.selectedItem.toString(),
                            ModelProfile.TYPE_Q
                        )
                    )
                    if (s_profile!!.selectedItemPosition <= 2) reloadBasicExtras()
                }

                deleteButton!!.setOnClickListener {
                    if (s_profile!!.selectedItemPosition > 2)
                        deleteProfile(s_profile!!.selectedItem.toString())
                    else {
                        Toast.makeText(mActivity, "You can't delete this profile", Toast.LENGTH_SHORT).show()
                    }
                }

                /** */

                /** */

            } catch (e: Exception) {
                e.printStackTrace()
            }

            /**
             * Set preferred settings
             */
            /**
             * Set preferred settings
             */
            val prefType = DatabaseController.getPreference(DatabaseController.TAG_PROFILE, "type")
            val prefQuality = DatabaseController.getPreference(DatabaseController.TAG_PROFILE, "quality")
            //String prefPrinter = DatabaseController.getPreference(DatabaseController.TAG_PROFILE,"type");
            if (prefType != null) s_type!!.setSelection(Integer.parseInt(prefType))
            if (prefQuality != null) s_profile!!.setSelection(Integer.parseInt(prefQuality))
            //if (prefPrinter!=null) s_printer.setSelection(Integer.parseInt(prefPrinter));

            refreshPrinters()
        }

    }

    /**
     * Send a gcode file to the selected printer
     */
    private fun sendToPrint() {

        if (mPrinter != null) {

            //If printer is available
            if (mPrinter!!.status == StateUtils.STATE_OPERATIONAL) {

                //Retrieve the current file
                val mFile = ViewerMainFragment.file

                if (mFile != null) {

                    Log.i("Slicer", "Current file: " + mFile!!.getAbsolutePath())

                    var actualFile: File? = null
                    if (mSlicingHandler.originalProject != null)
                        actualFile = File(mSlicingHandler.originalProject)

                    var finalFile: File? = null

                    Log.i("Slicer", "Current project: " + mSlicingHandler.originalProject!!)

                    if (actualFile != null)
                        if (LibraryController.isProject(actualFile)) {

                            //It's the last file
                            if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last") != null) {

                                /*mSlicingHandler.setExtras("print",true);
                            mPrinter.setJobPath(mSlicingHandler.getLastReference());
                           /mPrinter.setLoaded(false);
                            ItemListFragment.performClick(0);
                            ItemListActivity.showExtraFragment(1, mPrinter.getId());*/

                                //Add it to the reference list
                                DatabaseController.handlePreference(
                                    DatabaseController.TAG_REFERENCES, mPrinter!!.name,
                                    mSlicingHandler.originalProject!! + "/_tmp/temp.gco", true
                                )

                                mPrinter!!.jobPath = null

                                DevicesListController.selectPrinter(mActivity, actualFile, mSlicingHandler)

                            } else {

                                //Check for temporary gcode
                                val tempFile = File(LibraryController.parentFolder.toString() + "/temp/temp.gco")

                                //If we have a gcode which is temporary it's either a stl or sliced gcode
                                if (tempFile.exists()) {

                                    //Get original project
                                    //final File actualFile = new File(mSlicingHandler.getOriginalProject());

                                    //File renameFile = new File(tempFile.getParentFile().getAbsolutePath() + "/" + (new File(mSlicingHandler.getOriginalProject()).getName() + ".gco"));

                                    val tempFolder = File(mSlicingHandler.originalProject!! + "/_tmp/")
                                    if (!tempFolder.exists()) {
                                        if (tempFolder.mkdir()) {

                                            Log.i("Slicer", "Creating temp " + tempFolder.absolutePath)

                                        }
                                        Log.i("Slicer", "Creating temp NOPE " + tempFolder.absolutePath)
                                    }

                                    finalFile = File(
                                        tempFolder.toString() + "/" + actualFile.name.replace(
                                            " ",
                                            "_"
                                        ) + "_tmp.gcode"
                                    )



                                    Log.i("Slicer", "Creating new file in " + finalFile.absolutePath)

                                    Log.i("Slicer", "Final file is: STL or Sliced STL")


                                    tempFile.renameTo(finalFile)

                                    //if we don't have a temporary gcode, means we are currently watching an original gcode from a project
                                } else {

                                    if (LibraryController.hasExtension(1, mFile!!.getName())) {

                                        Log.i("Slicer", "Final file is: Project GCODE")

                                        finalFile = mFile

                                    } else {

                                        Log.i("Slicer", "Mada mada")

                                    }

                                }

                            }

                            //Not a project
                        } else {

                            //Check for temporary gcode
                            val tempFile = File(LibraryController.parentFolder.toString() + "/temp/temp.gco")

                            //If we have a gcode which is temporary it's a sliced gcode
                            if (tempFile.exists()) {

                                Log.i("Slicer", "Final file is: Random STL or Random Sliced STL")
                                finalFile = tempFile

                                //It's a random gcode
                            } else {

                                Log.i("Slicer", "Final file is: Random GCODE")
                                finalFile = mFile
                            }

                        }


                    if (finalFile != null)

                    //either case if the file exists, we send it to the printer
                        if (finalFile.exists()) {

                            DevicesListController.selectPrinter(mActivity, finalFile, null)
                            mPrinter!!.jobPath = finalFile.absolutePath

                        } else {

                            Toast.makeText(mActivity, R.string.viewer_slice_error, Toast.LENGTH_LONG).show()

                        }

                } else {
                    Toast.makeText(mActivity, R.string.devices_toast_no_gcode, Toast.LENGTH_LONG).show()
                }

            } else
                Toast.makeText(mActivity, R.string.viewer_printer_unavailable, Toast.LENGTH_LONG).show()

        } else
            Toast.makeText(mActivity, R.string.viewer_printer_unavailable, Toast.LENGTH_LONG).show()

        /**
         * Save the printer profile settings
         */
        DatabaseController.handlePreference(
            DatabaseController.TAG_PROFILE,
            "type",
            s_type!!.selectedItemPosition.toString(),
            true
        )
        DatabaseController.handlePreference(
            DatabaseController.TAG_PROFILE,
            "quality",
            s_profile!!.selectedItemPosition.toString(),
            true
        )

    }

    /**
     * Parses a JSON profile to the side panel
     *
     * @i printer index in the list
     */
    fun parseJson(profile: JSONObject?) {

        //Parse the JSON element
        try {

            //JSONObject data = mPrinter.getProfiles().get(i).getJSONObject("data");
            val data = profile!!.getJSONObject("data")
            layerHeight!!.setText(data.getString("layer_height"))
            shellThickness!!.setText(data.getString("wall_thickness"))
            bottomTopThickness!!.setText(data.getString("solid_layer_thickness"))
            printSpeed!!.setText(data.getString("print_speed"))
            printTemperature!!.setText(data.getJSONArray("print_temperature").get(0).toString())
            filamentDiamenter!!.setText(data.getJSONArray("filament_diameter").get(0).toString())
            filamentFlow!!.setText(data.getString("filament_flow"))
            travelSpeed!!.setText(data.getString("travel_speed"))
            bottomLayerSpeed!!.setText(data.getString("bottom_layer_speed"))
            infillSpeed!!.setText(data.getString("infill_speed"))
            outerShellSpeed!!.setText(data.getString("outer_shell_speed"))
            innerShellSpeed!!.setText(data.getString("inner_shell_speed"))

            minimalLayerTime!!.setText(data.getString("cool_min_layer_time"))

            if (data.has("retraction_enable"))
                if (data.getString("retraction_enable") == "true") {
                    enableRetraction!!.isChecked = true
                    Log.i("OUT", "Checked true")
                } else {
                    enableRetraction!!.isChecked = false
                    Log.i("OUT", "Checked false")
                }

            if (data.getBoolean("fan_enabled")) {
                enableCoolingFan!!.isChecked = true
                Log.i("OUT", "Checked true")
            } else {
                enableCoolingFan!!.isChecked = false
                Log.i("OUT", "Checked false")
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: NullPointerException) { //If invalid values
            e.printStackTrace()
        }

    }

    /**
     * Parse float to a variable to avoid accuracy error
     *
     * @param s
     * @return
     */
    @Throws(NumberFormatException::class)
    fun getFloatValue(s: String): Float? {

        return java.lang.Float.parseFloat(s)
    }

    /**
     * Open a pop up window with the infill options
     */
    fun openInfillPopupWindow() {

        //        if (mInfillOptionsPopupWindow == null) {
        //Get the content view of the pop up window
        val popupLayout = mActivity.layoutInflater
            .inflate(R.layout.print_panel_infill_dropdown_menu, null) as LinearLayout
        popupLayout.measure(0, 0)

        val gridResource = BitmapFactory.decodeResource(mActivity.resources, R.drawable.fill_grid)

        //Set the behavior of the infill seek bar
        val infillSeekBar = popupLayout.findViewById<View>(R.id.seekBar_infill) as SeekBar
        val infillPercent = popupLayout.findViewById<View>(R.id.infill_number_view) as TextView
        val infillGrid = popupLayout.findViewById<View>(R.id.infill_grid_view) as ImageView
        infillSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                infillPercent.text = progress.toString() + "%"
                infillText!!.text = progress.toString() + "%"

                if (progress == 0) {
                    infillGrid.setImageDrawable(mActivity.resources.getDrawable(R.drawable.grid_empty))
                }
                if (progress > 0 && progress <= 25) {
                    infillGrid.setImageDrawable(mActivity.resources.getDrawable(R.drawable.grid_0))
                }
                if (progress > 26 && progress <= 50) {
                    infillGrid.setImageDrawable(mActivity.resources.getDrawable(R.drawable.grid_25))
                }
                if (progress > 51 && progress <= 75) {
                    infillGrid.setImageDrawable(mActivity.resources.getDrawable(R.drawable.grid_50))
                }
                if (progress > 76 && progress < 100) {
                    infillGrid.setImageDrawable(mActivity.resources.getDrawable(R.drawable.grid_75))
                }
                if (progress == 100) {
                    infillGrid.setImageDrawable(mActivity.resources.getDrawable(R.drawable.grid_full))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mCurrentInfill = infillSeekBar.progress
                mSlicingHandler.setExtras("profile.fill_density", mCurrentInfill)

            }
        })

        infillSeekBar.progress = mCurrentInfill
        infillPercent.text = mCurrentInfill.toString() + " %"

        //Show the pop up window in the correct position
        val infillSpinnerCoordinates = IntArray(2)
        s_infill!!.getLocationOnScreen(infillSpinnerCoordinates)
        val popupLayoutPadding = mActivity.resources.getDimensionPixelSize(R.dimen.content_padding_normal)
        val popupLayoutWidth = 360 //FIXED WIDTH
        val popupLayoutHeight = popupLayout.measuredHeight
        val popupLayoutX = infillSpinnerCoordinates[0] - 2 //Remove the background padding
        val popupLayoutY = infillSpinnerCoordinates[1]

        mInfillOptionsPopupWindow = CustomPopupWindow(
            popupLayout, popupLayoutWidth,
            popupLayoutHeight, R.style.PopupMenuAnimation, true
        ).popupWindow

        mInfillOptionsPopupWindow!!.showAtLocation(
            s_infill, Gravity.NO_GRAVITY,
            popupLayoutX, popupLayoutY
        )
        //        }
    }

    /**
     * Save a slicing profile by adding every individual element to a JSON
     */
    fun saveProfile() {

        val inflater = mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val getModelsDialogView = inflater.inflate(R.layout.dialog_create_profile, null)
        val nameEditText = getModelsDialogView.findViewById(R.id.new_profile_name_edittext) as MaterialEditText

        val createFolderDialog = MaterialDialog(mActivity)
        createFolderDialog.title(R.string.dialog_create_profile_title)
            .customView(view = getModelsDialogView, scrollable = true)
            .positiveButton(R.string.create) {
                val name = nameEditText.text.toString().trim { it <= ' ' }
                if (name.isNullOrBlank() ) {
                    nameEditText.error = mActivity.getString(R.string.library_create_folder_name_error)
                } else {
                    //Init UI elements

                    var profile: JSONObject? = null

                    //Parse the JSON element
                    try {

                        //Profile info
                        profile = JSONObject()

                        profile.put("displayName", nameEditText.text.toString())
                        profile.put("description", "Test profile created from App") //TODO
                        profile.put("key", nameEditText.text.toString().replace(" ", "_").toLowerCase())

                        //Data info
                        val data = JSONObject()

                        data.put("layer_height", getFloatValue(layerHeight!!.text.toString()))
                        data.put("wall_thickness", getFloatValue(shellThickness!!.text.toString()))
                        data.put("solid_layer_thickness", getFloatValue(bottomTopThickness!!.text.toString()))

                        data.put("print_speed", getFloatValue(printSpeed!!.text.toString()))
                        data.put(
                            "print_temperature",
                            JSONArray().put(getFloatValue(printTemperature!!.text.toString()))
                        )
                        data.put(
                            "filament_diameter",
                            JSONArray().put(getFloatValue(filamentDiamenter!!.text.toString()))
                        )
                        data.put("filament_flow", getFloatValue(filamentFlow!!.text.toString()))
                        data.put("retraction_enable", enableRetraction!!.isChecked)

                        data.put("travel_speed", getFloatValue(travelSpeed!!.text.toString()))
                        data.put("bottom_layer_speed", getFloatValue(bottomLayerSpeed!!.text.toString()))
                        data.put("infill_speed", getFloatValue(infillSpeed!!.text.toString()))
                        data.put("outer_shell_speed", getFloatValue(outerShellSpeed!!.text.toString()))
                        data.put("inner_shell_speed", getFloatValue(innerShellSpeed!!.text.toString()))

                        data.put("cool_min_layer_time", getFloatValue(minimalLayerTime!!.text.toString()))
                        data.put("fan_enabled", enableCoolingFan!!.isChecked)

                        profile.put("data", data)


                        Log.i("OUT", profile.toString())

                    } catch (e: JSONException) {
                        e.printStackTrace()

                    } catch (e: NumberFormatException) {

                        //Check if there was an invalid number
                        e.printStackTrace()
                        Toast.makeText(mActivity, e.message, Toast.LENGTH_LONG).show()
                        profile = null

                    }

                    if (profile != null) {

                        //check if name already exists to avoid overwriting
                        for (s in ModelProfile.qualityList!!) {

                            try {
                                if (profile.get("displayName") == s) {

                                    Toast.makeText(
                                        mActivity,
                                        mActivity.getString(R.string.printview_profiles_overwrite) + ": " + s,
                                        Toast.LENGTH_LONG
                                    ).show()

                                }

                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }

                        }

                        if (ModelProfile.saveProfile(
                                mActivity,
                                nameEditText.text.toString(),
                                profile,
                                ModelProfile.TYPE_Q
                            )
                        ) {

                            reloadQualityAdapter()

                            for (i in 0 until s_profile!!.count) {

                                if (s_profile!!.getItemAtPosition(i).toString() == nameEditText.text.toString()) {
                                    s_profile!!.setSelection(i)
                                    break
                                }

                            }

                        }

                    }

                }

                it.dismiss()
            }
            .negativeButton(R.string.cancel) {
                it.dismiss()
            }
            .noAutoDismiss()
            .show()
    }

    fun deleteProfile(name: String) {

        //Delete profile first
        if (ModelProfile.deleteProfile(mActivity, name, ModelProfile.TYPE_Q)) {

            reloadQualityAdapter()

        }
    }

    /*******************************************
     * ADAPTERS
     */

    fun switchSlicingButton(enable: Boolean) {

        if (mPrinter != null) {
            sliceButton!!.isClickable = enable
            sliceButton!!.refreshTextColor(enable)
        } else {
            sliceButton!!.isClickable = false
            sliceButton!!.refreshTextColor(false)
        }

    }

    fun reloadProfileAdapter() {

        ModelProfile.reloadList(mActivity)

        val mProfileAdapter = ArrayAdapter<String>(
            mActivity,
            R.layout.print_panel_spinner_item, ModelProfile.profileList!!
        )
        mProfileAdapter.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item)
        s_type!!.adapter = mProfileAdapter

        if (mProfileAdapter != null) {
            mProfileAdapter.notifyDataSetChanged()
            s_type!!.postInvalidate()
        }

    }

    fun reloadQualityAdapter() {

        ModelProfile.reloadQualityList(mActivity)

        val mProfileAdapter = ArrayAdapter<String>(
            mActivity,
            R.layout.print_panel_spinner_item, ModelProfile.qualityList!!
        )
        mProfileAdapter.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item)
        s_profile!!.adapter = mProfileAdapter

        if (mProfileAdapter != null) {
            mProfileAdapter.notifyDataSetChanged()
            s_profile!!.postInvalidate()
        }

    }

    /** */

    /**
     * Only works for "extra" profiles, add a new value per field since we can't upload them yet
     */
    //TODO Temporary
    fun refreshProfileExtras() {

        if (s_profile!!.selectedItemPosition > 2) {

            mSlicingHandler.setExtras("profile.layer_height", getFloatValue(layerHeight!!.text.toString()) ?: 0.0f)
            mSlicingHandler.setExtras("profile.wall_thickness", getFloatValue(shellThickness!!.text.toString()) ?: 0.0f)
            mSlicingHandler.setExtras(
                "profile.solid_layer_thickness",
                getFloatValue(bottomTopThickness!!.text.toString())  ?: 0.0f
            )

            mSlicingHandler.setExtras("profile.print_speed", getFloatValue(printSpeed!!.text.toString()) ?: 0.0f)
            mSlicingHandler.setExtras(
                "profile.print_temperature",
                JSONArray().put(getFloatValue(printTemperature!!.text.toString()) ?: 0.0f)
            )
            mSlicingHandler.setExtras(
                "profile.filament_diameter",
                JSONArray().put(getFloatValue(filamentDiamenter!!.text.toString()) ?: 0.0f)
            )
            mSlicingHandler.setExtras("profile.filament_flow", getFloatValue(filamentFlow!!.text.toString()) ?: 0.0f)
            mSlicingHandler.setExtras("profile.retraction_enable", enableRetraction!!.isChecked)

            mSlicingHandler.setExtras("profile.travel_speed", getFloatValue(travelSpeed!!.text.toString()) ?: 0.0f)
            mSlicingHandler.setExtras("profile.bottom_layer_speed", getFloatValue(bottomLayerSpeed!!.text.toString()) ?: 0.0f)
            mSlicingHandler.setExtras("profile.infill_speed", getFloatValue(infillSpeed!!.text.toString()) ?: 0.0f)
            mSlicingHandler.setExtras("profile.outer_shell_speed", getFloatValue(outerShellSpeed!!.text.toString()) ?: 0.0f)
            mSlicingHandler.setExtras("profile.inner_shell_speed", getFloatValue(innerShellSpeed!!.text.toString()) ?: 0.0f)

            mSlicingHandler.setExtras("profile.cool_min_layer_time", getFloatValue(minimalLayerTime!!.text.toString()) ?: 0.0f)
            mSlicingHandler.setExtras("profile.fan_enabled", enableCoolingFan!!.isChecked)

        }

    }

    fun refreshPrinters() {

        val advanced_layout = mRootView.findViewById(R.id.advanced_options_card_view) as CardView
        val simple_layout = mRootView.findViewById<View>(R.id.simple_settings_layout) as LinearLayout
        val buttons_layout = mRootView.findViewById<View>(R.id.advanced_settings_buttons_container) as LinearLayout
        val print_button = mRootView.findViewById<View>(R.id.print_button_container) as LinearLayout

        if (DatabaseController.count() < 1) {
            mRootView.findViewById<View>(R.id.viewer_select_printer_layout).visibility = View.GONE
            mRootView.findViewById<View>(R.id.viewer_no_printer_layout).visibility = View.VISIBLE


            ViewHelper.disableEnableAllViews(false, advanced_layout)
            ViewHelper.disableEnableAllViews(false, simple_layout)
            ViewHelper.disableEnableAllViews(false, buttons_layout)
            ViewHelper.disableEnableAllViews(false, print_button)

        } else {
            mRootView.findViewById<View>(R.id.viewer_select_printer_layout).visibility = View.VISIBLE
            mRootView.findViewById<View>(R.id.viewer_no_printer_layout).visibility = View.GONE

            ViewHelper.disableEnableAllViews(true, advanced_layout)
            ViewHelper.disableEnableAllViews(true, simple_layout)
            ViewHelper.disableEnableAllViews(true, buttons_layout)
            ViewHelper.disableEnableAllViews(true, print_button)
            mPrinter = DevicesListController.selectAvailablePrinter(
                s_type!!.selectedItemPosition + 1,
                s_type!!.selectedItem.toString()
            )
            mPrinter?.let { mSlicingHandler.setPrinter(it) }
            if (mPrinter != null)
                ViewHelper.disableEnableAllViews(true, print_button)
            else
                ViewHelper.disableEnableAllViews(false, print_button)
        }

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(mActivity)
        if (sharedPref.getBoolean(mActivity.resources.getString(R.string.shared_preferences_autoslice), false))
            sliceButton!!.visibility = View.INVISIBLE
        else
            sliceButton!!.visibility = View.VISIBLE

        mRootView.invalidate()

    }

    /**
     * Clear the extra parameter list and reload basic parameters
     */
    fun reloadBasicExtras() {

        mSlicingHandler.clearExtras()
        mSlicingHandler.setExtras("profile.fill_density", mCurrentInfill)
        mSlicingHandler.setExtras("profile.support", s_support!!.selectedItem)
        mSlicingHandler.setExtras("profile", s_profile!!.selectedItem.toString())

    }

    /**
     * Generic text watcher to add new printing parameters
     */
    private inner class GenericTextWatcher constructor(private val mValue: String) : TextWatcher,
        CompoundButton.OnCheckedChangeListener {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {

        }

        override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {

        }

        override fun afterTextChanged(editable: Editable) {

            try {
                mSlicingHandler.setExtras(mValue, getFloatValue(editable.toString()) ?: 0.0f)

            } catch (e: NumberFormatException) {

                Log.i("Slicer", "Invalid value " + editable.toString())

            }

        }

        override fun onCheckedChanged(compoundButton: CompoundButton, b: Boolean) {

            mSlicingHandler.setExtras(mValue, b)

        }
    }

    companion object {

        //static parameters
        private val SUPPORT_OPTIONS = arrayOf("None", "Buildplate", "Everywhere") //support options
        private val ADHESION_OPTIONS = arrayOf("None", "Brim", "Raft") //adhesion options
        private val PRINTER_TYPE = arrayOf("Witbox", "Hephestos")
        private val PREDEFINED_PROFILES = arrayOf("bq") //filter for profile deletion

        private val DEFAULT_INFILL = 20
    }

}
