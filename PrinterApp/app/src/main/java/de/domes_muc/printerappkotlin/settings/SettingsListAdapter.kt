package de.domes_muc.printerappkotlin.settings

import android.app.AlertDialog
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.database.DeviceInfo
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.OctoprintConnection
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

/**
 * This is the adapter for the printer list on the settings fragment
 * It's going to hold the same device list as the Devices fragment
 *
 * @author alberto-baeza
 */
class SettingsListAdapter(
    context: Context, resource: Int,
    objects: List<ModelPrinter>
) : ArrayAdapter<ModelPrinter>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var v = convertView
        val m = getItem(position)

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        if (!DatabaseController.checkExisting(m!!)) {

            v = inflater.inflate(R.layout.null_item, null, false)

        } else {

            if (v == null) {

                v = inflater.inflate(R.layout.list_item_settings_device, null, false)

            } else {
                if (!DatabaseController.checkExisting(m))
                    v = inflater.inflate(R.layout.null_item, null, false)
                else
                    v = inflater.inflate(R.layout.list_item_settings_device, null, false)
                //v = convertView;
            }

            val deviceNameTextView = v!!.findViewById<View>(R.id.device_name_textview) as TextView
            deviceNameTextView.text = m.displayName + " [" + m.address.replace("/", "") + "]"

            try {
                val deviceNetworkTextView = v.findViewById<View>(R.id.device_network_textview) as TextView
                deviceNetworkTextView.text = "(" + m.network!!.replace("\"", "") + ")"
            } catch (e: NullPointerException) {

                e.printStackTrace() //No network
            }


            val iv = v.findViewById<View>(R.id.device_icon_imageview) as ImageView

            val connectionButton = v.findViewById<View>(R.id.device_connection_button) as ImageButton

            when (m.status) {

                StateUtils.STATE_CLOSED, StateUtils.STATE_ERROR -> connectionButton.setImageResource(R.drawable.ic_settings_disconnect)
                else -> connectionButton.setImageResource(R.drawable.ic_settings_connect)
            }

            when (m.type) {

                StateUtils.TYPE_WITBOX -> iv.setImageResource(R.drawable.printer_witbox_default)
                StateUtils.TYPE_PRUSA -> iv.setImageResource(R.drawable.printer_prusa_default)
                StateUtils.TYPE_CUSTOM -> iv.setImageResource(R.drawable.printer_custom_default)
            }

            v.findViewById<View>(R.id.device_delete_button).setOnClickListener {
                DatabaseController.deleteFromDb(m.id)

                //TODO change to remove method
                DevicesListController.list.remove(m)
                //ItemListActivity.notifyAdapters();
                notifyDataSetChanged()
            }

            v.findViewById<View>(R.id.device_edit_button).setOnClickListener { appearanceEditDialog(m) }


            //TODO notify adapter instead of changing icons
            connectionButton.setOnClickListener {
                Toast.makeText(context, "Status: " + m.status, Toast.LENGTH_SHORT).show()

                if (m.status == StateUtils.STATE_OPERATIONAL) {
                    OctoprintConnection.disconnect(context, m.address)
                    connectionButton.setImageResource(R.drawable.ic_settings_disconnect)
                } else {
                    OctoprintConnection.getNewConnection(context, m)
                    connectionButton.setImageResource(R.drawable.ic_settings_connect)
                }
            }

        }

        return v
    }

    //Edit printer name by changing its display name and write it into the Database
    fun appearanceEditDialog(m: ModelPrinter?) {

        val colorArray = arrayOf(
            context.resources.getString(R.string.settings_default_color),
            "default",
            "red",
            "orange",
            "yellow",
            "green",
            "blue",
            "violet",
            "black"
        )

        val adb = AlertDialog.Builder(context)
        adb.setTitle(R.string.settings_edit_name)

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val v = inflater.inflate(R.layout.settings_edit_mini_layout, null)

        val et = v.findViewById<View>(R.id.settings_edit_name_edit) as EditText
        et.setText(m!!.displayName)

        val spinner = v.findViewById<View>(R.id.settings_edit_color_spinner) as Spinner
        spinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, colorArray)

        adb.setView(v)

        adb.setPositiveButton(R.string.ok) { dialog, which ->
            val newName = et.text.toString()
            var newColor: String? = null
            if (spinner.selectedItemPosition != 0)
                newColor = colorArray[spinner.selectedItemPosition]

            if (newName != "") m.displayName = newName
            DatabaseController.updateDB(DeviceInfo.FeedEntry.DEVICES_DISPLAY, m.id, newName)

            notifyDataSetChanged()

            //Set the new name on the server
            OctoprintConnection.setSettings(m, newName, newColor ?: "", context)
        }

        adb.setNegativeButton(R.string.cancel, null)

        adb.show()


    }


}
