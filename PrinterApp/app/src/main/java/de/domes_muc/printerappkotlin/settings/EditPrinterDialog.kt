package de.domes_muc.printerappkotlin.settings

import android.app.AlertDialog
import android.app.Dialog
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.database.DeviceInfo
import de.domes_muc.printerappkotlin.devices.discovery.DiscoveryController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.model.ModelProfile
import de.domes_muc.printerappkotlin.octoprint.OctoprintConnection
import de.domes_muc.printerappkotlin.octoprint.OctoprintProfiles
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.util.ArrayList

/**
 * This class will create a dialog to edit and set printer type and settings.
 * It's called from the devices panel when you add a new printer or the settings option.
 *
 *
 * Created by alberto-baeza on 12/4/14.
 */
class EditPrinterDialog//Constructor
    (//Context
    private val mContext: Context, private val mPrinter: ModelPrinter, private val mSettings: JSONObject
) {

    //Adapters and arays
    private var mColorLabelsArray: Array<String>? = null
    private var mColorValuesArray: Array<String>? = null
    private var profileArray: ArrayList<String>? = null

    //UI references
    private var mRootView: View? = null
    private var spinner_printer: Spinner? = null
    private var editText_name: EditText? = null
    private var spinner_color: Spinner? = null
    private var editText_nozzle: EditText? = null
    private var editText_extruders: EditText? = null
    private var editText_width: EditText? = null
    private var editText_depth: EditText? = null
    private var editText_height: EditText? = null
    private var checkBox_circular: CheckBox? = null
    private var checkBox_hot: CheckBox? = null

    private var icon_printer: ImageView? = null
    private var button_edit: ImageButton? = null
    private var button_delete: ImageButton? = null

    private var profile_adapter: ArrayAdapter<String>? = null
    private var type_adapter: ArrayAdapter<String>? = null
    private var color_adapter: ArrayAdapter<String>? = null
    private var spinner_port: Spinner? = null

    init {
        createDialog()
    }

    //Initialize the UI elements
    private fun initElements(v: View) {

        mRootView = v

        //        mColorLabelsArray = new String[]{mContext.getResources().getString(R.string.settings_default_color),"default", "red", "orange", "yellow", "green", "blue", "violet", "black"};
        mColorLabelsArray = mContext.resources.getStringArray(R.array.printer_color_labels)
        mColorValuesArray = mContext.resources.getStringArray(R.array.printer_color_values)

        spinner_printer = v.findViewById(R.id.settings_edit_type_spinner) as Spinner
        editText_name = v.findViewById<View>(R.id.settings_edit_name_edit) as EditText
        spinner_color = v.findViewById<View>(R.id.settings_edit_color_spinner) as Spinner

        //Add default types plus custom types from internal storage
        profileArray = ArrayList()
        for (s in PRINTER_TYPES) {

            profileArray!!.add(s)
        }

        //Add internal storage types
        for (file in mContext.filesDir.listFiles()) {

            //Only files with the .profile extension
            if (file.absolutePath.contains(".profile")) {
                val pos = file.name.lastIndexOf(".")
                val name = if (pos > 0) file.name.substring(0, pos) else file.name

                //Add only the name
                profileArray!!.add(name)
            }

        }

        OctoprintProfiles.mProfiles?.let {
            val objKeys = it.keys()
            objKeys.forEach{
                profileArray!!.add(it)
            }
        }

        //Initialize adapters
        type_adapter = ArrayAdapter(mContext, R.layout.print_panel_spinner_item, profileArray!!)
        type_adapter!!.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item)
        color_adapter = ArrayAdapter(mContext, R.layout.print_panel_spinner_item, mColorLabelsArray!!)
        color_adapter!!.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item)

        //Initial settings and spinners
        editText_name!!.setText(mPrinter.displayName)

        //Select initial profile from the printer type
        spinner_printer!!.adapter = type_adapter

        //If it's a custom profile
        if (mPrinter.profile != null) {

            var pos = 0

            for (s in profileArray!!) {

                if (s == mPrinter.profile) {

                    spinner_printer!!.setSelection(pos)

                }
                pos++

            }

        } else
            spinner_printer!!.setSelection(mPrinter.type - 1) //Default profile

        spinner_color!!.adapter = color_adapter

        spinner_port = v.findViewById(R.id.settings_edit_port_spinner) as Spinner

        //Ports
        try {
            val ports = mSettings.getJSONObject("options").getJSONArray("ports")
            val ports_array = ArrayList<String>()

            for (i in 0 until ports.length()) {
                ports_array.add(ports.get(i).toString())
            }

            val ports_adapter = ArrayAdapter(
                mContext,
                R.layout.print_panel_spinner_item, ports_array
            )
            ports_adapter.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item)
            spinner_port!!.adapter = ports_adapter

        } catch (e: JSONException) {
            e.printStackTrace()
        }

        editText_nozzle = v.findViewById(R.id.settings_edit_nozzle_edit) as EditText
        editText_extruders = v.findViewById(R.id.settings_edit_extruders_edit) as EditText
        editText_width = v.findViewById(R.id.settings_edit_bed_width) as EditText
        editText_height = v.findViewById(R.id.settings_edit_bed_height) as EditText
        editText_depth = v.findViewById(R.id.settings_edit_bed_depth) as EditText

        checkBox_circular = v.findViewById(R.id.settings_edit_circular_check) as CheckBox
        checkBox_hot = v.findViewById(R.id.settings_edit_hot_check) as CheckBox

        icon_printer = v.findViewById<View>(R.id.settings_edit_icon) as ImageView

        //Only enable edit name on button click
        button_edit = v.findViewById(R.id.settings_edit_button) as ImageButton
        button_edit!!.setOnClickListener {
            editText_name!!.isEnabled = true
            editText_name!!.setText("")


            val imm = mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            imm?.showSoftInput(editText_name, 0)
        }

        //Delete custom profiles, only works on created profiles
        button_delete = v.findViewById(R.id.settings_delete_button) as ImageButton
        button_delete!!.setOnClickListener {
            Log.i("OUT", "Delete " + spinner_printer!!.selectedItem)

            deleteProfile(spinner_printer!!.selectedItem.toString())
            //                OctoprintProfiles.deleteProfile(mContext,mPrinter.getAddress(),spinner_printer.getSelectedItem().toString());
        }

        //Change type profile on item selected
        spinner_printer!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {

                var profile: JSONObject? = JSONObject()

                var editable = false
                button_delete!!.visibility = View.GONE

                //Check for default types
                when (i) {

                    0 //witbox (locked)
                    -> {

                        profile =
                                ModelProfile.retrieveProfile(mContext, ModelProfile.WITBOX_PROFILE, ModelProfile.TYPE_P)
                        icon_printer!!.setImageResource(R.drawable.printer_witbox_default)
                    }
                    1 //prusa (locked)
                    -> {

                        profile =
                                ModelProfile.retrieveProfile(mContext, ModelProfile.PRUSA_PROFILE, ModelProfile.TYPE_P)
                        icon_printer!!.setImageResource(R.drawable.printer_prusa_default)
                    }

                    2 //custom (editable)
                    -> {

                        profile = ModelProfile.retrieveProfile(
                            mContext,
                            ModelProfile.DEFAULT_PROFILE,
                            ModelProfile.TYPE_P
                        )
                        icon_printer!!.setImageResource(R.drawable.printer_custom_default)
                        editable = true
                    }

                    else //any other user-defined profile (locked)
                    -> {

                        profile = ModelProfile.retrieveProfile(mContext, profileArray!![i], ModelProfile.TYPE_P)
                        icon_printer!!.setImageResource(R.drawable.printer_custom_default)
                        editable = false
                        button_delete!!.visibility = View.VISIBLE
                    }
                }

                //Load the selected profile
                loadProfile(profile, editable)


            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {

            }
        }

    }

    //Method to create the settings dialog
    fun createDialog() {


        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val editPrinterDialogView = inflater.inflate(R.layout.dialog_edit_printer_info, null)
        initElements(editPrinterDialogView)

        MaterialDialog(mContext)
            .title(R.string.settings_edit_name)
            .customView(view = editPrinterDialogView, scrollable = false)
            .neutralButton(R.string.cancel) {
                it.dismiss()
            }
            .negativeButton(R.string.settings_change_network) {

                DiscoveryController(mContext).changePrinterNetwork(mPrinter)
                it.dismiss()
            }
            .positiveButton(R.string.ok) {
                var newName: String? = editText_name!!.text.toString()
                var newColor: String? = null

                //only edit color if it's not the "keep color" option
                if (spinner_color!!.selectedItemPosition != 0)
                    newColor = mColorValuesArray!![spinner_color!!.selectedItemPosition]

                //Only edit name if it's enabled
                if (newName != null && editText_name!!.isEnabled) {

                    mPrinter.displayName = newName
                    DatabaseController.updateDB(DeviceInfo.FeedEntry.DEVICES_DISPLAY, mPrinter.id, newName)
                } else
                    newName = null

                //if (!editText_name.isEnabled()) newName = null;

                //Set the new name on the server
                OctoprintConnection.setSettings(mPrinter, newName ?: "", newColor ?: "", mContext)

                var auxType: String? = null

                //if it's not a custom editable profile
                if (spinner_printer!!.selectedItemPosition != 2) {

                    mPrinter.setType(
                        spinner_printer!!.selectedItemPosition + 1,
                        spinner_printer!!.selectedItem.toString()
                    )
                    //OctoprintProfiles.selectProfile(mContext,mPrinter.getAddress(),spinner_printer.getSelectedItem().toString());

                    when (spinner_printer!!.selectedItemPosition) {

                        0 -> auxType = "bq_witbox"
                        1 -> auxType = "bq_hephestos"
                        else -> {

                            //Upload profile, connect if successful
                            OctoprintProfiles.uploadProfile(
                                mContext,
                                mPrinter.address,
                                ModelProfile.retrieveProfile(
                                    mContext,
                                    spinner_printer!!.selectedItem.toString(),
                                    ModelProfile.TYPE_P
                                )!!,
                                spinner_port!!.selectedItem.toString()
                            )


                            //auxType = spinner_printer.getSelectedItem().toString();
                        }
                    }

                    //update new profile
                    if (auxType != null) {

                        OctoprintConnection.startConnection(
                            mPrinter.address,
                            mContext,
                            spinner_port!!.selectedItem.toString(),
                            auxType
                        )
                        OctoprintProfiles.updateProfile(mContext, mPrinter.address, auxType)
                    }


                } else { //CUSTOM selected

                    mPrinter.setType(3, null)
                    //Save new profile
                    saveProfile()

                }

                if (!DatabaseController.checkExisting(mPrinter)) {

                    mPrinter.id = DatabaseController.writeDb(
                        mPrinter.name, mPrinter.address, mPrinter.position.toString(), mPrinter.type.toString(),
                        MainActivity.getCurrentNetwork(mContext)
                    )
                    mPrinter.startUpdate(mContext)
                } else {

                    DatabaseController.updateDB(
                        DeviceInfo.FeedEntry.DEVICES_TYPE,
                        mPrinter.id,
                        mPrinter.type.toString()
                    )

                }

                notifyAdapters()
                it.dismiss()
            }
            .cancelable(false)
            .show()
    }

    fun loadProfile(profile: JSONObject?, editable: Boolean) {

        try {

            val extruder = profile!!.getJSONObject("extruder")

            editText_nozzle!!.setText(extruder.getDouble("nozzleDiameter").toString())
            editText_nozzle!!.isEnabled = editable

            editText_extruders!!.setText(extruder.getInt("count").toString())
            editText_extruders!!.isEnabled = editable

            val volume = profile.getJSONObject("volume")

            editText_width!!.setText(volume.getInt("width").toString())
            editText_width!!.isEnabled = editable

            editText_depth!!.setText(volume.getInt("depth").toString())
            editText_depth!!.isEnabled = editable

            editText_height!!.setText(volume.getInt("height").toString())
            editText_height!!.isEnabled = editable

            if (volume.getString("formFactor") == "circular")
                checkBox_circular!!.isChecked = true
            else
                checkBox_circular!!.isChecked = false
            checkBox_circular!!.isEnabled = editable

            checkBox_hot!!.isChecked = profile.getBoolean("heatedBed")
            checkBox_hot!!.isEnabled = editable

            //Enable/disable the tags
            mRootView!!.findViewById<TextView>(R.id.settings_edit_nozzle_tag).setEnabled(editable)
            mRootView!!.findViewById<TextView>(R.id.settings_edit_extruders_tag).setEnabled(editable)
            mRootView!!.findViewById<TextView>(R.id.settings_edit_bed_tag).setEnabled(editable)

        } catch (e: JSONException) {
            e.printStackTrace()
        }


    }

    fun saveProfile() {

        val adb = AlertDialog.Builder(mContext)
        adb.setTitle(R.string.settings_profile_add)

        val name = EditText(mContext)
        adb.setView(name)


        adb.setPositiveButton(R.string.ok) { dialogInterface, i ->
            try {

                val nameFormat = name.text.toString().replace(" ", "_")

                val json = ModelProfile.retrieveProfile(mContext, ModelProfile.DEFAULT_PROFILE, ModelProfile.TYPE_P)

                json!!.put("name", name.text.toString())
                json.put("id", name.text.toString().replace(" ", "_"))
                json.put("model", "ModelPrinter")

                val volume = JSONObject()

                if (checkBox_circular!!.isChecked)
                    volume.put("formFactor", "circular")
                else
                    volume.put("formFactor", "rectangular")

                volume.put("depth", java.lang.Float.parseFloat(editText_depth!!.text.toString()).toDouble())
                volume.put("width", java.lang.Float.parseFloat(editText_width!!.text.toString()).toDouble())
                volume.put("height", java.lang.Float.parseFloat(editText_height!!.text.toString()).toDouble())

                json.put("volume", volume)

                val extruder = JSONObject()

                extruder.put("nozzleDiameter", java.lang.Double.parseDouble(editText_nozzle!!.text.toString()))
                extruder.put("count", Integer.parseInt(editText_extruders!!.text.toString()))

                val s = ArrayList<Float>()
                s.add(java.lang.Float.parseFloat("0.0"))
                s.add(java.lang.Float.parseFloat("0.0"))

                val sa = ArrayList<JSONArray>()
                sa.add(JSONArray(s))

                extruder.put("offsets", JSONArray(sa))

                json.put("extruder", extruder)

                if (checkBox_hot!!.isChecked)
                    json.put("heatedBed", true)
                else
                    json.put("heatedBed", false)

                Log.i("OUT", json.toString())

                if (ModelProfile.saveProfile(mContext, name.text.toString(), json, ModelProfile.TYPE_P)) {

                    mPrinter.setType(3, name.text.toString())
                    DatabaseController.updateDB(DeviceInfo.FeedEntry.DEVICES_PROFILE, mPrinter.id, name.text.toString())

                    //Upload profile, connect if successful
                    OctoprintProfiles.uploadProfile(
                        mContext,
                        mPrinter.address,
                        json,
                        spinner_port!!.selectedItem.toString()
                    )

                    notifyAdapters()

                }


            } catch (e: JSONException) {

                e.printStackTrace()

            }
        }

        adb.show()


    }

    //this method will delete the profile from the system and also from any printer that has it
    fun deleteProfile(name: String) {

        val adb = AlertDialog.Builder(mContext)
        adb.setTitle(R.string.warning)
        adb.setMessage(R.string.settings_profile_delete)
        adb.setPositiveButton("OK") { dialogInterface, i ->
            //Delete profile first
            if (ModelProfile.deleteProfile(mContext, name, ModelProfile.TYPE_P)) {

                profileArray!!.removeAt(spinner_printer!!.selectedItemPosition)
                type_adapter!!.notifyDataSetChanged()

            }

            //Avoid ConcurrentModificationException
            val aux = ArrayList<ModelPrinter>()
            for (p in DevicesListController.list) {

                aux.add(p)

            }

            //Check for profile matches
            for (p in aux) {

                if (p.profile != null)
                    if (p !== mPrinter && p.profile == name) {

                        //Remove from the configured printers list
                        DatabaseController.deleteFromDb(p.id)
                        DevicesListController.list.remove(p)

                        notifyAdapters()

                    }

            }
        }

        adb.show()

    }


    //TODO intent to notify adapters asynchronously
    fun notifyAdapters() {

        val intent = Intent("notify")
        intent.putExtra("message", "Devices")
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)

        val intent2 = Intent("notify")
        intent2.putExtra("message", "Settings")
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent2)

        val intent3 = Intent("notify")
        intent.putExtra("message", "Profile")
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent3)

    }

    companion object {

        //Default printer types
        private val PRINTER_TYPES = arrayOf("bq_witbox", "bq_hephestos", "custom")
    }
}
