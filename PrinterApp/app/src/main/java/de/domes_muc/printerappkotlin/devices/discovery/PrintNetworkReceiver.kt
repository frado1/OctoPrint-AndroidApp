package de.domes_muc.printerappkotlin.devices.discovery

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Parcelable
import android.widget.ArrayAdapter

import java.util.ArrayList

/**
 * This class will handle new ad-hoc printers, searching for them on the local network and connecting to them to configure.
 * @author alberto-baeza
 */
class PrintNetworkReceiver//Constructor
    (private val mController: PrintNetworkManager) : BroadcastReceiver() {
    private val mContext: Context
    private val mFilter: IntentFilter
    private val cm: ConnectivityManager

    private var mRetries = MAX_RETRIES


    init {

        this.mContext = mController.context
        mWifiManager = mContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        //Network list
        //TODO: Don't make it permanent
        networkList = ArrayAdapter(mContext, android.R.layout.select_dialog_singlechoice)



        cm = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        mFilter = IntentFilter()
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

        register()
    }

    /**
     * Search for a pre-defined network name and treat them as a new offline printer.
     */
    override fun onReceive(context: Context, intent: Intent) {


        /**
         * New method to check an established connection with a network (Reliable)
         */
        if (intent.action === ConnectivityManager.CONNECTIVITY_ACTION) {
            val activeNetwork = cm.activeNetworkInfo

            if (activeNetwork != null) {

                Log.i("NETWORK", "Network connectivity change " + activeNetwork.state)
                mController.dismissNetworkDialog()

            }
        }

        if (intent.action === WifiManager.SUPPLICANT_STATE_CHANGED_ACTION) {

            val state = intent.getParcelableExtra<Parcelable>(WifiManager.EXTRA_NEW_STATE) as SupplicantState
            Log.i("NETWORK", state.toString())

            when (state) {

                SupplicantState.DISCONNECTED -> {

                    mRetries--

                    Log.i("NETWORK", "Retries $mRetries")

                    if (mRetries == 0) {
                        mController.errorNetworkDialog()
                        mRetries = MAX_RETRIES
                    }
                }

                SupplicantState.COMPLETED ->

                    mRetries = MAX_RETRIES
            }

        }

        //Search for the Network with the desired name
        if (intent.action === WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {

            val result = mWifiManager.scanResults

            setNetworkList(result)

            //Put the results on a list and search for the one(s) we want
            for (s in result) {

                //Log.i("out",s.SSID);

                //TODO: This should search for multiple Networks son we can't unregister the receiver.
                //Log.i("Network",s.toString());
                if (s.SSID.contains(NETWORK_NAME)) {

                    Log.i("Network", "New printer found! " + s.SSID)
                    //unregister();

                    val m = ModelPrinter(s.SSID, "/10.250.250.1", StateUtils.STATE_ADHOC)


                    //mController.checkNetworkId(s.SSID,false);
                    mController.addElementController(m)


                }
            }

        }

    }

    //Start scanning for Networks.
    fun startScan() {

        mRetries = MAX_RETRIES
        mWifiManager.startScan()
        mWifiManager.scanResults

    }

    fun register() {
        mContext.registerReceiver(this, mFilter)
        startScan()

    }

    fun unregister() {
        try {
            mContext.unregisterReceiver(this)

        } catch (e: IllegalArgumentException) {


        }

    }

    /**
     * Fill the list with every available Network, it's updated "automatically" with every
     * scan request which can also be triggered with "Refresh"
     * @param networks
     */
    fun setNetworkList(networks: List<ScanResult>) {

        networkList.clear()
        val a = ArrayList<String>()

        for (s in networks) {
            if (!a.contains(s.SSID)) {
                a.add(s.SSID)
            } else {//Log.i("OUT","Duplicate network " + s.SSID);
            }
        }


        for (s in a) {
            networkList.add(s)

        }


    }

    companion object {

        //TODO: Hardcoded Network name for testing
        //Filter to search for when scanning networks
        private val NETWORK_NAME = "OctoPi"
        private val MAX_RETRIES = 5

        private lateinit var mWifiManager: WifiManager
        lateinit var networkList: ArrayAdapter<String>
    }


}
