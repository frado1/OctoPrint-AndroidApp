package de.domes_muc.printerappkotlin.devices.discovery

import android.app.AlertDialog
import android.app.Dialog
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.OctoprintConnection
import de.domes_muc.printerappkotlin.octoprint.OctoprintNetwork
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView

import java.util.ArrayList
import java.util.ConcurrentModificationException

/**
 * Created by alberto-baeza on 2/5/15.
 */
class DiscoveryController(val activity: Context) {


    private val mServiceList: ArrayList<ModelPrinter>
    private var mAdapter: DiscoveryDevicesGridAdapter? = null
    private var mPrintersGridView: GridView? = null
    private var mNetworkManager: PrintNetworkManager? = null
    private var mServiceListener: JmdnsServiceListener? = null

    private var mWaitProgressDialog: Dialog? = null

    private var mFinalPrinter: ModelPrinter? = null

    init {
        mServiceList = ArrayList()

        //scanDelayDialog();

    }

    fun scanDelayDialog() {

        mServiceList.clear()

        try {
            //Clear list for unadded printers
            for (p in DevicesListController.list) {

                if (p.status == StateUtils.STATE_NEW || p.status == StateUtils.STATE_ADHOC)
                    DevicesListController.list.remove(p)

            }

            if (mNetworkManager == null)
                mNetworkManager = PrintNetworkManager(this@DiscoveryController)
            else
                mNetworkManager!!.reloadNetworks()
            if (mServiceListener == null)
                mServiceListener = JmdnsServiceListener(this@DiscoveryController)
            else
                mServiceListener!!.reloadListening()

            //Get progress dialog UI
            val scanDelayDialogView =
                LayoutInflater.from(activity).inflate(R.layout.dialog_progress_content_vertical, null)
            (scanDelayDialogView.findViewById<View>(R.id.progress_dialog_text) as TextView).setText(R.string.printview_searching_networks_dialog_content)

            val handler = Handler()

            //Build progress dialog
            val scanDelayDialog = MaterialDialog(activity)
            scanDelayDialog.title(R.string.printview_searching_networks_dialog_title)
                .customView(view = scanDelayDialogView, scrollable = true)
                .cancelable(true)
                .positiveButton(R.string.dialog_printer_manual_add)  {
                    optionAddPrinter("","","")
                    it.setOnDismissListener(null)
                    it.dismiss()

                }
                .negativeButton(R.string.cancel) {
                    it.setOnDismissListener(null)
                    it.dismiss()

                }
                .noAutoDismiss()

            scanDelayDialog.onDismiss { scanNetwork() }

            //Show dialog
            scanDelayDialog.show()

            handler.postDelayed({ scanDelayDialog.dismiss() }, 3000)
        } catch (e: ConcurrentModificationException) {
            e.printStackTrace()
        }


    }

    fun changePrinterNetwork(p: ModelPrinter) {

        if (mNetworkManager == null) mNetworkManager = PrintNetworkManager(this@DiscoveryController)
        OctoprintNetwork.getNetworkList(mNetworkManager!!, p)


    }


    private fun scanNetwork() {

        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val discoveryPrintersDialogView = inflater.inflate(R.layout.dialog_discovery_printers, null)

        val mServiceNames = arrayOfNulls<String>(mServiceList.size)
        for (i in mServiceList.indices) {

            val p = mServiceList[i]

            mServiceNames[i] = p.name + " " + p.address

        }

        val dialog: Dialog

        dialog = MaterialDialog(activity)
            .title(R.string.printview_searching_networks_dialog_title)
            .customView(view = discoveryPrintersDialogView, scrollable = false)
            .positiveButton(R.string.dialog_printer_manual_add) {
                //scanDelayDialog();
                optionAddPrinter("","","")
                it.setOnDismissListener(null)
                it.dismiss()
            }
            .negativeButton(R.string.cancel)

        if (mServiceList.size == 0) {
            discoveryPrintersDialogView.setOnClickListener {
                dialog.setOnDismissListener(null)
                dialog.dismiss()
                scanDelayDialog()
            }
        }


        mAdapter = DiscoveryDevicesGridAdapter(activity, mServiceList)

        mPrintersGridView = discoveryPrintersDialogView.findViewById<View>(R.id.discovery_gridview) as GridView
        mPrintersGridView!!.adapter = mAdapter
        mPrintersGridView!!.emptyView = discoveryPrintersDialogView.findViewById(R.id.wifi_networks_noresults_container)
        mPrintersGridView!!.setSelector(R.drawable.selectable_rect_background_green)
        mPrintersGridView!!.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            for (p in mServiceList) {

                Log.i("Discovery", "Searching " + p.name)


                if (mServiceNames[i] == p.name + " " + p.address) {

                    DevicesListController.addToList(p)

                    if (p.status == StateUtils.STATE_NEW) {


//                        val t=p.address.replace("/".toRegex(), "")
//                        val access = t.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//
//                        optionAddPrinter(access[0],access[1] ?: "80","")
                        OctoprintConnection.getNewConnection(activity, p)


                    } else if (p.status == StateUtils.STATE_ADHOC) {

                        DevicesListController.addToList(p)
                        mNetworkManager!!.setupNetwork(p, p.position)
                    }
                }
            }

            dialog.dismiss()
            //mServiceListener.unregister();
        }

        dialog.show()

    }


    fun waitServiceDialog() {

        //Get progress dialog UI
        val waitingForServiceDialogView =
            LayoutInflater.from(activity).inflate(R.layout.dialog_progress_content_horizontal, null)
        (waitingForServiceDialogView.findViewById<View>(R.id.progress_dialog_text) as TextView).setText(R.string.devices_configure_waiting)

        //Show progress dialog
        mWaitProgressDialog = MaterialDialog(activity)
            .title(R.string.devices_configure_wifi_title)
            .customView(view = waitingForServiceDialogView, scrollable = true)
            .cancelable(false)
            .noAutoDismiss()

        //Progress dialog to notify command events
        mWaitProgressDialog!!.setOnDismissListener {
            if (mFinalPrinter != null) {

                OctoprintConnection.getNewConnection(activity, mFinalPrinter!!)
                mFinalPrinter = null
                mWaitProgressDialog = null

            }
        }
        mWaitProgressDialog!!.show()

        if (mServiceListener != null) mServiceListener!!.reloadListening()

        val timeOut = Handler()
        timeOut.postDelayed({
            if (mWaitProgressDialog != null) {
                mWaitProgressDialog!!.dismiss()
                errorDialog()
                mWaitProgressDialog = null
            }
        }, 30000)

    }

    private fun errorDialog() {

        MaterialDialog(activity)
            .title(R.string.error)
            .message(R.string.devices_configure_wifi_error)
            .positiveButton(R.string.retry) {
                scanDelayDialog()
            }
            .negativeButton(R.string.cancel)
            .show()
    }

    fun checkExisting(m: ModelPrinter): Boolean {

        var exists = false

        for (p in mServiceList) {

            if (m.name == p.name || m.name.contains(PrintNetworkManager.getNetworkId(p.name))) {

                exists = true

            }

        }

        return exists

    }

    fun addElement(printer: ModelPrinter) {

        if (mWaitProgressDialog != null) {

            if (printer.status == StateUtils.STATE_NEW)
                if (PrintNetworkManager.checkNetworkId(printer.name, true)) {

                    mServiceList.add(printer)

                    DevicesListController.addToList(printer)

                    mServiceListener!!.unregister()

                    mFinalPrinter = printer

                    mWaitProgressDialog!!.dismiss()

                }


        } else {

            if (!DevicesListController.checkExisting(printer.address))
                if (!checkExisting(printer)) {

                    mServiceList.add(printer)
                    if (mAdapter != null)
                        mAdapter!!.notifyDataSetChanged()
                }
        }

    }

    /**
     * Add a new printer to the database by IP instead of service discovery
     */
    fun optionAddPrinter(adress: String, port: String, apiKey: String) {

        //Inflate the view
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(R.layout.settings_add_printer_dialog, null, false)

        val et_address = v.findViewById(R.id.et_address) as EditText
        val et_port = v.findViewById(R.id.et_port) as EditText
        val et_key = v.findViewById(R.id.et_apikey) as EditText

        et_address.setText(adress)
        et_port.setText(port)
        et_key.setText(apiKey)

        MaterialDialog(activity)
            .title(R.string.settings_add_title)
            .customView(view = v, scrollable = false)
            .positiveButton(R.string.add)  {

                if (et_address.text.toString() == "") {
                    et_address.error = activity.getString(R.string.manual_add_error_address)
                } else {
                    var port = et_port.text.toString()
                    if (port == "") port = "80"

                    val p = ModelPrinter(
                        activity.getString(R.string.app_name),
                        "/" + et_address.text.toString() + ":" + port,
                        StateUtils.STATE_NEW
                    )
                    DatabaseController.handlePreference(
                        DatabaseController.TAG_KEYS,
                        PrintNetworkManager.getNetworkId(p.address),
                        et_key.text.toString(),
                        true
                    )

                    if (!DevicesListController.checkExisting(p.address)) {
                        DevicesListController.addToList(p)

                        OctoprintConnection.getNewConnection(activity, p)
                    } else {
                        Toast.makeText(activity, "That printer already exists", Toast.LENGTH_SHORT).show()
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
}
