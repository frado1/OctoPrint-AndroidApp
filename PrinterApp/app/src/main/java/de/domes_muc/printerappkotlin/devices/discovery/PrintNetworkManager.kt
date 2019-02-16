package de.domes_muc.printerappkotlin.devices.discovery

import android.app.Dialog
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.OctoprintNetwork
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Handler
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.math.BigInteger
import java.net.InetAddress
import java.util.ArrayList

/**
 * This class will connect to the server AP and select a wifi from the list to connect the server to
 * TODO As it is right now, the server part is unstable and doesn't always creates the AP
 *
 * @author alberto-baeza
 */
class PrintNetworkManager//Constructor
    (context: DiscoveryController) {

    //Check if the network is currently being configured
    private var isOffline = false

    private val mReceiver: PrintNetworkReceiver

    //position for the current printer being selected
    private var mPosition = -1
    private var mPrinter: ModelPrinter? = null

    //original network to configure if an error happens
    private var mOriginalNetwork: Int = 0

    /**
     * Get Device context
     *
     * @return
     */
    val context: Context
        get() = mController.activity


    init {

        mController = context

        //Create a new Network Receiver
        mReceiver = PrintNetworkReceiver(this)

    }

    /**
     * Method to connect to the AP
     *
     * @param position
     */
    fun setupNetwork(p: ModelPrinter, position: Int) {


        //Get connection parameters
        val conf = WifiConfiguration()
        conf.SSID = "\"" + p.name + "\""
        conf.preSharedKey = "\"" + PASS + "\""

        mPrinter = p
        mPosition = position

        //Add the new network
        mManager = mController.activity.getSystemService(Context.WIFI_SERVICE) as WifiManager

        mOriginalNetwork = mManager!!.connectionInfo.networkId

        val nId = mManager!!.addNetwork(conf)

        //Configure network
        isOffline = true


        connectSpecificNetwork(nId)

        createNetworkDialog(context.getString(R.string.devices_discovery_connect))

    }


    /*******************************************************************
     * NETWORK HANDLING
     */

    /**
     * Get the network list from the server and select one to connect
     *
     * @param response list with the networks
     */
    fun selectNetworkPrinter(response: JSONObject, url: String) {


        try {

            mReceiver.unregister()

            val wifis = response.getJSONArray("wifis")
            val wifiList = ArrayList<String>()
            val wifiQualityList = ArrayList<String>()
            for (i in 0 until wifis.length()) {
                wifiList.add(wifis.getJSONObject(i).getString("ssid"))
                wifiQualityList.add(wifis.getJSONObject(i).getString("quality"))

            }

            val networkListDialogAdapter = DiscoveryWifiNetworksListAdapter(context, wifiList, wifiQualityList)

            //Get the dialog UI
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val selectWifiNetworkDialogView = inflater.inflate(R.layout.dialog_select_wifi_network, null)

            val selectNetworkDialog: Dialog

            selectNetworkDialog = MaterialDialog(context)
                .title(R.string.devices_configure_wifi_title)
                .customView(view = selectWifiNetworkDialogView, scrollable = false)
                .negativeButton(R.string.cancel)

            val wifiNetworksListView = selectWifiNetworkDialogView.findViewById(R.id.wifi_networks_listview) as ListView
            wifiNetworksListView.adapter = networkListDialogAdapter
            wifiNetworksListView.emptyView =
                    selectWifiNetworkDialogView.findViewById(R.id.wifi_networks_noresults_container)
            wifiNetworksListView.setSelector(R.drawable.selectable_rect_background_green)
            wifiNetworksListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                //Inflate network layout
                val inflater = mController.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val wifiPasswordDialogView = inflater.inflate(R.layout.dialog_wifi_network_info, null)

                val wifiPasswordEditText = wifiPasswordDialogView.findViewById(R.id.wifi_password_edittext) as EditText


                //Add check box to show/hide the password
                val showPasswordCheckbox =
                    wifiPasswordDialogView.findViewById<View>(R.id.show_password_checkbox) as CheckBox
                showPasswordCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    // Use this for store the current cursor mPosition of the edit text
                    val start = wifiPasswordEditText.selectionStart
                    val end = wifiPasswordEditText.selectionEnd

                    if (isChecked)
                        wifiPasswordEditText.transformationMethod = null
                    else
                        wifiPasswordEditText.transformationMethod = PasswordTransformationMethod()

                    wifiPasswordEditText.setSelection(start, end)
                }


                wifiPasswordEditText.requestFocus()

                try {
                    if (wifis.getJSONObject(position).getBoolean("encrypted")) {

                        MaterialDialog(context)
                            .title(text = wifiList[position])
                            .customView(view = wifiPasswordDialogView, scrollable = false)
                            .positiveButton(R.string.ok) {
                                val ssid = wifiList[position]
                                val psk = wifiPasswordEditText.text.toString().trim { it <= ' ' }

                                if (psk == "") {
                                    wifiPasswordEditText.error = context.getString(R.string.empty_password_error)
                                } else {
                                    configureSelectedNetwork(ssid, psk, url)
                                    mReceiver.unregister()
                                    it.dismiss()
                                    selectNetworkDialog.dismiss()
                                    wifiPasswordEditText.clearFocus()
                                }
                            }
                            .negativeButton(R.string.cancel) {
                                it.dismiss()
                            }
                            .noAutoDismiss()
                            .show()
                    } else {

                        configureSelectedNetwork(wifiList[position], null, url)
                        mReceiver.unregister()
                        selectNetworkDialog.dismiss()

                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            selectNetworkDialog.show()

        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    fun configureSelectedNetwork(ssid: String, pass: String?, url: String) {

        /**
         * SAVE PARAMETERS
         */

        //Target network for both the client and the device

        val target = WifiConfiguration()
        //Set the parameters for the target network
        target.SSID = "\"" + ssid + "\""

        if (pass != null)
            target.preSharedKey = "\"" + pass + "\""
        else
            target.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)


        createNetworkDialog(context.getString(R.string.devices_discovery_config))

        //Send a network configuration request to the server
        OctoprintNetwork.configureNetwork(mReceiver, context, ssid, pass, url)


        //From this point on we need a delay to the configuration to ensure a clear connection

        /**
         * TODO Need a handler for this?
         * Remove AP from the network list and connect to the Target network
         */
        val handler = Handler()

        handler.postDelayed({
            //Configuring network
            isOffline = true

            if (mManager == null) mManager = mController.activity.getSystemService(Context.WIFI_SERVICE) as WifiManager
            mManager!!.disconnect()

            val origin = getNetworkId(mManager!!.connectionInfo.ssid)

            mManager!!.disableNetwork(mManager!!.connectionInfo.networkId)
            mManager!!.removeNetwork(mManager!!.connectionInfo.networkId)

            //Clear existing networks
            //clearNetwork(target.SSID);
            connectSpecificNetwork(searchNetwork(target))


            DevicesListController.removeElement(mPosition)

            val postHandler = Handler()
            postHandler.postDelayed({
                Log.i("MANAGER", "Registering again with " + target.SSID + "!")

                DevicesListController.removeElement(mPosition)


                DatabaseController.handlePreference(DatabaseController.TAG_NETWORK, "Last", origin, true)

                //Remove ad-hoc network
                clearNetwork("OctoPi-Dev")
                mPosition = -1
                mPrinter = null

                //mController.notifyAdapter();
                // mReceiver.register();
                dismissNetworkDialog()
            }, 10000)
        }, 5000)


    }

    fun connectSpecificNetwork(nId: Int) {

        Log.i("Manager", "Enabling $nId")
        //Disconnect to the current network and reconnect to the new
        mManager!!.disconnect()
        mManager!!.enableNetwork(nId, true)
        mManager!!.reconnect()

    }


    /*********************************************************************
     * DIALOG HANDLING
     */

    /**
     * Create Network Dialog while connecting to the Printer
     *
     * @param message
     */
    fun createNetworkDialog(message: String) {

        //Get progress dialog UI
        val configurePrinterDialogView =
            LayoutInflater.from(mController.activity).inflate(R.layout.dialog_progress_content_horizontal, null)
        (configurePrinterDialogView.findViewById<View>(R.id.progress_dialog_text) as TextView).text = message

        //Show progress dialog
        mDialog = MaterialDialog(mController.activity)
            .title(R.string.devices_discovery_title)
            .customView(view = configurePrinterDialogView, scrollable = true)
            .cancelable(false)
            .negativeButton(R.string.cancel) {
                it.setOnDismissListener(null)
                it.dismiss()

            }
            .noAutoDismiss()
        mDialog!!.show()
    }

    /**
     * Called when the Network is established, should open a Dialog with the network list from the server
     * only will be called if there's a Network available (Dialog won't be null)
     */
    fun dismissNetworkDialog() {


        if (isOffline) {
            isOffline = false
            mDialog!!.dismiss()
            if (mManager != null) {
                val ipAddress = BigInteger.valueOf(mManager!!.dhcpInfo.gateway.toLong()).toByteArray()
                reverse(ipAddress)
            }

            val myaddr: InetAddress

            // myaddr = InetAddress.getByAddress(ipAddress);


            //TODO HARDCODED ACCESS POINT
            //String hostaddr = "10.250.250.1";//myaddr.getHostAddress();

            if (mPrinter != null) {
                OctoprintNetwork.getNetworkList(this, mPrinter!!)
            } else {

                mController.waitServiceDialog()

            }

        }


    }

    fun errorNetworkDialog() {

        if (mDialog != null) {

            mDialog!!.dismiss()
            connectSpecificNetwork(mOriginalNetwork)
            createNetworkDialog(context.getString(R.string.dialog_add_printer_timeout))

        }


    }


    /**
     * Add a new Printer calling to the Controller
     *
     * @param p
     */
    fun addElementController(p: ModelPrinter) {

        mController.addElement(p)
        //p.setNotConfigured();

    }


    fun reloadNetworks() {

        mReceiver.unregister()
        mReceiver.register()

    }

    companion object {

        //This should contain the static generic password to the ad-hoc network
        //TODO hardcoded password
        private val PASS = "OctoPrint"

        //Reference to main Controller
        private lateinit var mController: DiscoveryController

        //Wifi network manager
        private var mManager: WifiManager? = null

        //Dialog handler
        private var mDialog: Dialog? = null


        /**
         * Reverse an array of bytes to get the actual IP address
         *
         * @param array
         */
        fun reverse(array: ByteArray?) {
            if (array == null) {
                return
            }
            var i = 0
            var j = array.size - 1
            var tmp: Byte
            while (j > i) {
                tmp = array[j]
                array[j] = array[i]
                array[i] = tmp
                j--
                i++
            }
        }


        /**
         * This method will clear the existing Networks with the same name as the new inserted
         *
         * @param ssid
         */
        private fun clearNetwork(ssid: String) {

            val configs = mManager!!.configuredNetworks
            for (c in configs) {
                if (c.SSID.contains(ssid)) {
                    mManager!!.removeNetwork(c.networkId)
                }
            }
        }

        private fun searchNetwork(ssid: WifiConfiguration): Int {

            val configs = mManager!!.configuredNetworks
            for (c in configs) {
                if (c.SSID == ssid.SSID) {
                    return c.networkId
                }
            }

            return mManager!!.addNetwork(ssid)
        }


        /**
         * EXCLUSIVE TO THIS IMPLEMENTATION
         *
         *
         * Parse the network ID to search in the preference list
         *
         * @param s ssid to get the number
         * @return the parsed number
         */
        fun getNetworkId(s: String): String {

            val ssid = s.replace("[^A-Za-z0-9]".toRegex(), "")

            return if (ssid.length >= 4)
                ssid.substring(ssid.length - 4, ssid.length)
            else
                "0000"

        }


        /**
         * Check if the network was on the preference list to link it to the service
         *
         * @param ssid
         * @param result
         */
        fun checkNetworkId(ssid: String, result: Boolean): Boolean {

            Log.i("Discovery", "Checking ID")

            val message: Int
            var exists = false

            if (result)
                message = R.string.devices_discovery_toast_success
            else
                message = R.string.devices_discovery_toast_error

            /**
             * Check for pending networks in the preference list
             */
            if (DatabaseController.isPreference(DatabaseController.TAG_NETWORK, "Last")) {

                if (DatabaseController.getPreference(DatabaseController.TAG_NETWORK, "Last") == getNetworkId(ssid)) {

                    exists = true

                    DatabaseController.handlePreference(DatabaseController.TAG_NETWORK, "Last", null, false)

                    //Toast.makeText(mController.getActivity(), message, Toast.LENGTH_LONG).show();


                }
            }

            return exists
        }

        val currentNetwork: String
            get() {

                val manager = mController.activity.getSystemService(Context.WIFI_SERVICE) as WifiManager

                return manager.connectionInfo.ssid

            }
    }
}
