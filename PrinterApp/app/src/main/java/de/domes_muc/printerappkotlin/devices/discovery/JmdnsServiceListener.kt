package de.domes_muc.printerappkotlin.devices.discovery

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Handler

import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException


import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener


/**
 * Service listener class that will handle new device discovery and add them
 * to a permanent list
 * @author alberto-baeza
 */
class JmdnsServiceListener(private val mContext: DiscoveryController) : ServiceListener {

    //New thread to handle both listeners.
    private val mHandler: Handler

    init {

        //Create new Handler after 1s to setup the service listener
        mHandler = Handler()
        mHandler.postDelayed({ setUp() }, 1000)

    }

    /**
     * Setup our Listener to browse services on the local network
     */

    private fun setUp() {

        try {
            //We need to get our device IP address to bind it when creating JmDNS since we want to address it to a specific network interface
            val wifi = mContext.activity.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val deviceIpAddress = getDeviceIpAddress(wifi)

            //Get multicast lock because we need to listen to multicast packages
            mMulticastLock = wifi.createMulticastLock(javaClass.name)
            mMulticastLock!!.setReferenceCounted(true)
            mMulticastLock!!.acquire()

            Log.i("Model", "Starting JmDNS Service Listener...." + deviceIpAddress!!.toString())

            mJmdns = JmDNS.create(deviceIpAddress, null) //Creating an instance of JmDNS
            //Search for an specific service type
            mJmdns!!.addServiceListener(OCTOPRINT_SERVICE, this)

        } catch (e: IOException) {
            e.printStackTrace()
        } catch (n: NullPointerException) {
            n.printStackTrace()
        }


    }

    /****************************************
     * Handlers for ServiceListener
     */

    override fun serviceAdded(event: ServiceEvent) {

        //When a service is found, we request it to be resolved
        mJmdns!!.requestServiceInfo(event.type, event.name, 1)

    }

    override fun serviceRemoved(event: ServiceEvent) {

    }

    override fun serviceResolved(event: ServiceEvent) {

        //Creates a service with info
        Log.i(
            "Discovery",
            "Service resolved: " + event.name + "@" + event.info.qualifiedName + " port:" + event.info.port
        )
        val service = mJmdns!!.getServiceInfo(event.type, event.name)

        if (service.inetAddresses[0].toString() != "/10.250.250.1") {

            Log.i("Discovery", "Added to list")
            mContext.addElement(
                ModelPrinter(
                    service.name,
                    service.inetAddresses[0].toString() /*+ HttpUtils.CUSTOM_PORT*/ + ":" + event.info.port,
                    StateUtils.STATE_NEW
                )
            )

        }


    }

    private fun checkNetworkId() {


    }

    /**
     * Reload the service discovery by registering the service again in case it's not detected automatically
     */
    fun reloadListening() {

        if (mJmdns != null) {
            mJmdns!!.unregisterAllServices()
            mMulticastLock!!.release()

            mMulticastLock!!.acquire()
            //Search for an specific service type
            mJmdns!!.addServiceListener("_ipp3._tcp.local.", this)
        }


    }

    fun unregister() {
        if (mJmdns != null) {
            mJmdns!!.unregisterAllServices()
            mMulticastLock!!.release()
        }
    }

    companion object {

        //Allows this application to receive Multicast packets, can cause a noticable battery drain.
        private var mMulticastLock: WifiManager.MulticastLock? = null

        private val OCTOPRINT_SERVICE = "_octoprint._tcp.local."

        //JmDNS manager which will create both listeners
        private var mJmdns: JmDNS? = null


        //This method was obtained externally, basically it gets our IP Address, or return Android localhost by default.
        fun getDeviceIpAddress(wifi: WifiManager): InetAddress? {

            var result: InetAddress? = null
            try {
                //Default to Android localhost
                result = InetAddress.getByName("10.0.0.2")

                //Figure out our wifi address, otherwise bail
                val wifiinfo = wifi.connectionInfo

                val intaddr = wifiinfo.ipAddress
                val byteaddr = byteArrayOf(
                    (intaddr and 0xff).toByte(),
                    (intaddr shr 8 and 0xff).toByte(),
                    (intaddr shr 16 and 0xff).toByte(),
                    (intaddr shr 24 and 0xff).toByte()
                )
                result = InetAddress.getByAddress(byteaddr)

            } catch (ex: UnknownHostException) {
                Log.i("Controller", String.format("getDeviceIpAddress Error: %s", ex.message))
            }

            return result
        }
    }

}
