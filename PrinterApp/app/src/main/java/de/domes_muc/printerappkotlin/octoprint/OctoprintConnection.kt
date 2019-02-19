package de.domes_muc.printerappkotlin.octoprint

import android.app.Dialog
import de.domes_muc.printerappkotlin.ListContent
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.database.DeviceInfo
import de.domes_muc.printerappkotlin.devices.discovery.DiscoveryController
import de.domes_muc.printerappkotlin.devices.discovery.PrintNetworkManager
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.model.ModelProfile
import de.domes_muc.printerappkotlin.settings.EditPrinterDialog
import de.domes_muc.printerappkotlin.viewer.ViewerMainFragment
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.preference.PreferenceManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.loopj.android.http.JsonHttpResponseHandler

import cz.msebera.android.httpclient.HttpEntity
import com.loopj.android.http.*;
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.client.ClientProtocolException
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse
import cz.msebera.android.httpclient.client.methods.HttpPatch
import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory
import cz.msebera.android.httpclient.entity.StringEntity
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder
import cz.msebera.android.httpclient.ssl.SSLContexts

import org.json.JSONException
import org.json.JSONObject

import java.io.UnsupportedEncodingException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import de.tavendo.autobahn.WebSocketConnection
import de.tavendo.autobahn.WebSocketException
import de.tavendo.autobahn.WebSocketHandler
import org.json.JSONArray

/**
 * Class for Connection handling with Octoprint's API. Since the API is still on developement
 * we need more than one method, and they need interaction between them, this may change.
 * @author alberto-baeza
 */
object OctoprintConnection {

    private val TAG = "OctoprintConnection"
    private val SOCKET_TIMEOUT = 10000
    val DEFAULT_PORT = "/dev/ttyUSB0"
    private val DEFAULT_PROFILE = "_default"
    private val API_DISABLED_MSG = "API disabled"
    private val API_INVALID_MSG = "Invalid API key"

    /**
     *
     * Post parameters to handle connection. JSON for the new API is made
     * but never used.
     *
     */
    fun startConnection(url: String, context: Context, port: String, profile: String) {

        val payloadObj = JSONObject()
        var entity: StringEntity? = null
        try {
            payloadObj.put("command", "connect")
            payloadObj.put("port", port)
            payloadObj.put("printerProfile", profile)
            payloadObj.put("save", true)
            payloadObj.put("autoconnect", "true")
            entity = StringEntity(payloadObj.toString(), "UTF-8")
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        Log.i(TAG, "Start connection on $profile")

        HttpClientHandler.post(context, url + HttpUtils.URL_CONNECTION,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

                override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONArray?) {
                    super.onSuccess(statusCode, headers, response)

                }
                override fun onFailure(
                    statusCode: Int, headers: Array<Header>,
                    responseString: String, throwable: Throwable
                ) {
                    Log.i(TAG, "Failure because: $responseString")
                    super.onFailure(statusCode, headers, responseString, throwable)
                }

            })
    }

    fun disconnect(context: Context, url: String) {

        val payloadObj = JSONObject()
        var entity: StringEntity? = null
        try {
            payloadObj.put("command", "disconnect")
            entity = StringEntity(payloadObj.toString(), "UTF-8")
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        HttpClientHandler.post(context, url + HttpUtils.URL_CONNECTION,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

                override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONArray?) {
                    super.onSuccess(statusCode, headers, response)

                }
                override fun onFailure(
                    statusCode: Int, headers: Array<Header>,
                    responseString: String, throwable: Throwable
                ) {
                    super.onFailure(statusCode, headers, responseString, throwable)
                }

            })
    }

    fun getLinkedConnection(context: Context, p: ModelPrinter) {

        HttpClientHandler.get(p.address + HttpUtils.URL_CONNECTION, RequestParams(), object : JsonHttpResponseHandler() {

            override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                super.onSuccess(statusCode, headers, response)

                var current: JSONObject? = null
                try {
                    current = response.getJSONObject("current")
                    p.port = current!!.getString("port")
                    convertType(p, current.getString("printerProfile"))

                    //retrieve settings
                    //getUpdatedSettings(p,current.getString("printerProfile"));
                    getSettings(p)

                    val intent = Intent("notify")
                    intent.putExtra("message", "Devices")
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

                    Log.i(TAG, "Printer already connected to " + p.port!!)
                    //p.startUpdate(context);
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<Header>,
                responseString: String,
                throwable: Throwable
            ) {
                super.onFailure(statusCode, headers, responseString, throwable)

                if (statusCode == 401 && responseString == API_DISABLED_MSG) {
                    Log.i(TAG, responseString)
                } else {
                    OctoprintAuthentication.getAuth(context, p, false)
                }


            }
        })

    }

    /**
     * Obtains the current state of the machine and issues new connection commands
     * @param p printer
     */
    fun getNewConnection(context: Context, p: ModelPrinter) {

        //Get progress dialog UI
        val configurePrinterDialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_progress_content_horizontal, null)
        (configurePrinterDialogView.findViewById<View>(R.id.progress_dialog_text) as TextView).setText(R.string.devices_discovery_connect)


        //        try{
        //Show progress dialog
        val progressDialog = MaterialDialog(context)
        progressDialog.title(R.string.devices_discovery_title)
            .customView(view = configurePrinterDialogView, scrollable = true)
            .cancelable(true)
            .negativeButton(R.string.cancel) {
                it.setOnDismissListener(null)
                it.dismiss()
            }
            .noAutoDismiss()
        //Progress dialog to notify command events
        progressDialog.show()
        //        } catch (WindowManager.BadTokenException e){
        //            e.printStackTrace();
        //        }


        //Get connection status
        HttpClientHandler.get(p.address + HttpUtils.URL_CONNECTION, RequestParams(), object : JsonHttpResponseHandler() {

            override fun onSuccess(
                statusCode: Int, headers: Array<Header>,
                response: JSONObject
            ) {
                super.onSuccess(statusCode, headers, response)


                //TODO Random crash
                try {
                    if (progressDialog.isShowing())
                        progressDialog.dismiss()
                    else
                        return
                } catch (e: ArrayIndexOutOfBoundsException) {

                    e.printStackTrace()

                } catch (e: NullPointerException) {

                    e.printStackTrace()

                    return

                }


                //Check for current status
                var current: JSONObject? = null

                try {
                    current = response.getJSONObject("current")

                    Log.i(TAG, "State: " + current!!.getString("state"))

                    //if closed or error
                    if (current!!.getString("state").contains("Closed")
                        || current.getString("state").contains("Error")
                        || current.getString("printerProfile") == DEFAULT_PROFILE
                    ) {

                        //configure new printer
                        EditPrinterDialog(context, p, response)

                    } else {


                        //already connected
                        if (p.status == StateUtils.STATE_NEW) {

                            //load information
                            p.port = current.getString("port")
                            convertType(p, current.getString("printerProfile"))
                            //getUpdatedSettings(p,current.getString("printerProfile"));
                            getSettings(p)
                            Log.i(TAG, "Printer already connected to " + p.port!!)

                            val network = MainActivity.getCurrentNetwork(context)
                            p.network = network

                            p.id = DatabaseController.writeDb(
                                p.name,
                                p.address,
                                p.position.toString(),
                                p.type.toString(),
                                network
                            )

                            p.startUpdate(context)

                        }


                    }


                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }

            override fun onFailure(
                statusCode: Int, headers: Array<Header>,
                responseString: String, throwable: Throwable
            ) {
                super.onFailure(statusCode, headers, responseString, throwable)


                Log.i(TAG, "Failure while connecting $statusCode == $responseString")

                if (statusCode == 401 && responseString == API_DISABLED_MSG) {
                    showApiDisabledDialog(context)
                } else {
                    OctoprintAuthentication.getAuth(context, p, true)
                }
                progressDialog.dismiss()

            }

        })


    }

    fun showApiDisabledDialog(context: Context) {

        MaterialDialog(context)
            .title(R.string.error)
            .message(R.string.connection_error_api_disabled)
            .positiveButton(R.string.ok)  {
                DiscoveryController(context).optionAddPrinter("","","")
            }
            .negativeButton(R.string.cancel)
            .show()


    }

    private fun convertType(p: ModelPrinter, type: String) {

        Log.i(TAG, "Converting type $type")

        if (type == ModelProfile.WITBOX_PROFILE)
            p.setType(1, ModelProfile.WITBOX_PROFILE)
        else if (type == ModelProfile.PRUSA_PROFILE)
            p.setType(2, ModelProfile.PRUSA_PROFILE)
        else if (p.profile == null) {

            Log.i(TAG, "Setting type ")

            p.setType(3, type)
        } else if (p.profile != "_default") {
            Log.i(TAG, "Setting type default")

            p.setType(3, type)

        } else
            Log.i(TAG, "Basura " + p.profile!!)

        Log.i(TAG, "Get type " + p.profile!!)


    }

    /*************************************************
     * SETTINGS
     */

    fun convertColor(color: String): Int {

        if (color == "default") return Color.TRANSPARENT
        if (color == "red") return Color.RED
        if (color == "orange") return Color.rgb(255, 165, 0)
        if (color == "yellow") return Color.YELLOW
        if (color == "green") return Color.GREEN
        if (color == "blue") return Color.BLUE
        return if (color == "violet") Color.rgb(138, 43, 226) else Color.BLACK

    }


    fun getUpdatedSettings(p: ModelPrinter, profile: String) {

        HttpClientHandler.get(
            p.address + HttpUtils.URL_PROFILES + "/" + profile,
            RequestParams(),
            object : JsonHttpResponseHandler() {


                fun onProgress(bytesWritten: Int, totalSize: Int) {

                }

                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                    super.onSuccess(statusCode, headers, response)


                    Log.i(TAG, response.toString())

                    try {
                        val name = response.getString("name")
                        val color = response.getString("color")

                        if (name != "") {

                            p.displayName = name
                            DatabaseController.updateDB(DeviceInfo.FeedEntry.DEVICES_DISPLAY, p.id, name)
                        }

                        p.displayColor = convertColor(color)


                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }


                }

                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header>,
                    responseString: String,
                    throwable: Throwable
                ) {
                    super.onFailure(statusCode, headers, responseString, throwable)
                }
            })


    }

    /**
     * Function to get the settings from the server
     * @param p
     */
    fun getSettings(p: ModelPrinter) {

        val PREFIX = "http:/"

        HttpClientHandler.get(p.address + HttpUtils.URL_SETTINGS, RequestParams(), object : JsonHttpResponseHandler() {

            override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                super.onSuccess(statusCode, headers, response)

                try {
                    val appearance = response.getJSONObject("appearance")

                    Log.i(TAG, appearance.toString())

                    val newName = appearance.getString("name")
                    if (newName != "") {

                        p.displayName = newName
                        DatabaseController.updateDB(DeviceInfo.FeedEntry.DEVICES_DISPLAY, p.id, newName)
                    }

                    p.displayColor = convertColor(appearance.getString("color"))

                } catch (e: JSONException) {
                    e.printStackTrace()
                }



                try {

                    val webcam = response.getJSONObject("webcam")

                    if (webcam.has("streamUrl")) {
                        if (webcam.getString("streamUrl").startsWith("/")) {
                            p.webcamAddress = PREFIX + p.address + webcam.getString("streamUrl")
                        } else {
                            p.webcamAddress = webcam.getString("streamUrl")
                        }
                    }


                } catch (e: JSONException) {
                    e.printStackTrace()
                }


            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<Header>,
                responseString: String,
                throwable: Throwable
            ) {
                super.onFailure(statusCode, headers, responseString, throwable)

                Log.i(TAG, "Settings failure: $responseString")
                DatabaseController.handlePreference(
                    DatabaseController.TAG_KEYS,
                    PrintNetworkManager.getNetworkId(p.address),
                    null,
                    false
                )
                MainActivity.showDialog(responseString)
            }
        })

    }

    /**
     * Function to set the settings to the server
     */
    fun setSettings(p: ModelPrinter, newName: String, newColor: String, context: Context) {

        val payloadObj = JSONObject()
        val appearance = JSONObject()
        var entity: StringEntity? = null
        try {
            appearance.put("name", newName)
            appearance.put("color", newColor)
            payloadObj.put("appearance", appearance)
            entity = StringEntity(payloadObj.toString(), "UTF-8")
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        HttpClientHandler.post(context, p.address + HttpUtils.URL_SETTINGS,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

                //Override onProgress because it's faulty
                fun onProgress(bytesWritten: Int, totalSize: Int) {}

                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                    super.onSuccess(statusCode, headers, response)

                    //if (newColor!=null) p.setDisplayColor(convertColor(newColor));
                }

                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header>,
                    responseString: String,
                    throwable: Throwable
                ) {
                    super.onFailure(statusCode, headers, responseString, throwable)

                    Log.i(TAG, "Settings failure: $responseString")
                }
            })

    }


    /**
     *
     * Obtains the state of the machine that will be on the Connection API, that's why this is here.
     * Works in conjunction with GET /api/connection on the NEW API.
     *
     * New client implementation uses Websockets to receive status updates from the server
     * so we only need to open a new connection and parse the payload.
     */
    fun openSocket(p: ModelPrinter, context: Context) {

        p.setConnecting()


        //Web socket URI
        val wsuri = "ws:/" + p.address + HttpUtils.URL_SOCKET

        try {
            val mConnection = WebSocketConnection()

            //mConnection is a new websocket connection
            mConnection.connect(wsuri, object : WebSocketHandler() {

                //When the websocket opens
                override fun onOpen() {
                    //TODO unify this method
                    Log.i(TAG, "Status: Connected to $wsuri")
                    Log.i(TAG, "Connection from: SOCKET")

                    //"auth": "someuser:LGZ0trf8By"
                    val authParam = JSONObject().put("auth", p.userName + ":" + p.userSession).toString()
                    mConnection.sendTextMessage(authParam)

                    doConnection(context, p)
                }

                //On message received
                override fun onTextMessage(payload: String?) {
                    //Log.i("SOCK", "Got echo [" + p.getAddress() + "]: " + payload);

                    try {
                        val payloadObj = JSONObject(payload)

                        //Get the json string for "current" status
                        if (payloadObj.has("current")) {

                            val response = JSONObject(payload).getJSONObject("current")

                            //Update job with current status
                            //We'll add every single parameter
                            p.updatePrinter(
                                response.getJSONObject("state").getString("text"),
                                createStatus(response.getJSONObject("state").getJSONObject("flags")),
                                response
                            )

                            //SEND NOTIFICATION
                            val intent = Intent("notify")
                            intent.putExtra("message", "Devices")
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

                            if (response.getJSONObject("progress").getString("completion") != "null") {
                                val d =
                                    java.lang.Double.parseDouble(response.getJSONObject("progress").getString("completion"))

                                if (d > 0 && p.status == StateUtils.STATE_PRINTING) {
                                    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                                    if (sharedPref.getBoolean(
                                            context.resources.getString(R.string.shared_preferences_print),
                                            true
                                        )
                                    ) {
                                        val intentN = Intent()
                                        intentN.action = "de.domes_muc.printerappkotlin.NotificationReceiver"
                                        intentN.putExtra("printer", p.id)
                                        intentN.putExtra("progress", d.toInt())
                                        intentN.putExtra("type", "print")
                                        context.sendBroadcast(intentN)
                                    }
                                }
                            }
                        }

                        //Check for events in the server
                        if (payloadObj.has("event")) {
                            val response = JSONObject(payload).getJSONObject("event")

                            //Slicing finished should be handled in another method
                            if (response.getString("type") == "SlicingDone") {
                                val slicingPayload = response.getJSONObject("payload")
                                sliceHandling(context, slicingPayload, p.address)
                            }

                            //A file was uploaded
                            if (response.getString("type") == "Upload") {
                                //p.setLoaded(true);
                                if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last") != null)
                                    if (DatabaseController.getPreference(
                                            "Slicing",
                                            "Last"
                                        ) == response.getJSONObject("payload").getString("file")
                                    ) {

                                        //Log.i("Slicer","LETS SLICE " + response.getJSONObject("payload").getString("file"));
                                    }

                            }
                            if (response.getString("type") == "PrintStarted") {
                                p.loaded = true
                            }

                            if (response.getString("type") == "Connected") {
                                p.port = response.getJSONObject("payload").getString("port")
                                Log.i(TAG, "UPDATED PORT " + p.port!!)
                            }

                            if (response.getString("type") == "PrintDone") {

                                //SEND NOTIFICATION

                                Log.i(TAG, "PRINT FINISHED! " + response.toString())

                                if (p.jobPath != null) addToHistory(p, response.getJSONObject("payload"))

                                val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                                if (sharedPref.getBoolean(
                                        context.resources.getString(R.string.shared_preferences_print),
                                        true
                                    )
                                ) {

                                    val intentN = Intent()
                                    intentN.action = "de.domes_muc.printerappkotlin.NotificationReceiver"
                                    intentN.putExtra("printer", p.id)
                                    intentN.putExtra("progress", 100)
                                    intentN.putExtra("type", "finish")
                                    context.sendBroadcast(intentN)
                                }


                            }

                            if (response.getString("type") == "SettingsUpdated") {


                                getLinkedConnection(context, p)


                            }

                        }


                        //update slicing progress in the print panel fragment
                        if (payloadObj.has("slicingProgress")) {

                            val response = JSONObject(payload).getJSONObject("slicingProgress")

                            //TODO random crash because not yet created
                            try {
                                //Check if it's our file
                                if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last") != null)
                                    if (DatabaseController.getPreference(
                                            DatabaseController.TAG_SLICING,
                                            "Last"
                                        ) == response.getString("source_path")
                                    ) {

                                        //Log.i("Slicer","Progress received for " + response.getString("source_path"));

                                        val progress = response.getInt("progress")


                                        //TODO
                                        ViewerMainFragment.showProgressBar(StateUtils.SLICER_SLICE, progress)

                                        //SEND NOTIFICATION

                                        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                                        if (sharedPref.getBoolean(
                                                context.resources.getString(R.string.shared_preferences_slice),
                                                true
                                            )
                                        ) {

                                            val intent = Intent()
                                            intent.action = "de.domes_muc.printerappkotlin.NotificationReceiver"
                                            intent.putExtra("printer", p.id)
                                            intent.putExtra("progress", progress)
                                            intent.putExtra("type", "slice")
                                            context.sendBroadcast(intent)
                                        }
                                    }
                            } catch (e: NullPointerException) {

                                //e.printStackTrace();
                                //Log.i(TAG,"Null slicing");
                            }


                        }


                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.i(TAG, "Invalid JSON")

                    }


                }

                override fun onClose(code: Int, reason: String?) {
                    Log.i(TAG, "Connection lost at $code because $reason")

                    mConnection.disconnect()

                    //Timeout for reconnection
                    val handler = Handler()
                    handler.postDelayed({
                        Log.i(TAG, "Timeout expired, reconnecting to " + p.address)

                        p.startUpdate(context)
                    }, SOCKET_TIMEOUT.toLong())


                }
            })
        } catch (e: WebSocketException) {

            Log.i(TAG, e.toString())
        }

    }

    //TODO
    //Method to invoke connection handling
    fun doConnection(context: Context, p: ModelPrinter) {


        getLinkedConnection(context, p)

        //Get printer settings

        //getSettings(p);

        //Get a new set of files
        OctoprintFiles.getFiles(context, p, null)

        //Get a new set of profiles
        //OctoprintSlicing.retrieveProfiles(context,p); //Don't retrieve profiles yet


    }

    fun createStatus(flags: JSONObject): Int {

        try {
            if (flags.getBoolean("paused")) return StateUtils.STATE_PAUSED
            if (flags.getBoolean("printing")) return StateUtils.STATE_PRINTING
            if (flags.getBoolean("operational")) return StateUtils.STATE_OPERATIONAL
            if (flags.getBoolean("error")) return StateUtils.STATE_ERROR
            if (flags.getBoolean("paused")) return StateUtils.STATE_PAUSED
            if (flags.getBoolean("closedOrError")) return StateUtils.STATE_CLOSED

        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return StateUtils.STATE_NONE

    }

    /**
     * This method will create a dialog to handle the sliced file from the server.
     * @param context
     * @param payload sliced file data from the server
     * @param url server address
     */
    private fun sliceHandling(context: Context, payload: JSONObject, url: String) {

        try {

            Log.i(TAG, "Slice done received for " + payload.getString("stl"))

            //Search for files waiting for slice
            if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last") != null)
                if (DatabaseController.getPreference(
                        DatabaseController.TAG_SLICING,
                        "Last"
                    ) == payload.getString("stl")
                ) {

                    Log.i(TAG, "Changed PREFERENCE [Last]: " + payload.getString("gcode"))
                    DatabaseController.handlePreference(
                        DatabaseController.TAG_SLICING,
                        "Last",
                        payload.getString("gcode"),
                        true
                    )

                    ViewerMainFragment.showProgressBar(StateUtils.SLICER_DOWNLOAD, 0)

                    OctoprintSlicing.getMetadata(url, payload.getString("gcode"))
                    OctoprintFiles.downloadFile(
                        context, url + HttpUtils.URL_DOWNLOAD_FILES,
                        LibraryController.parentFolder.toString() + "/temp/", payload.getString("gcode")
                    )
                    OctoprintFiles.deleteFile(context, url, payload.getString("stl"), "/local/")

                } else {

                    Log.i(TAG, "Slicing NOPE for me!")

                }

        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    fun addToHistory(p: ModelPrinter, history: JSONObject) {


        try {
            val name = history.getString("filename")
            val path = p.jobPath
            val time = ConvertSecondToHHMMString(history.getString("time"))
            val type = p.profile

            val sdf = SimpleDateFormat("dd/MM/yyyy")
            val date = sdf.format(Date())

            if (type != null && path != null && !path.contains("/temp/")) {
                    LibraryController.addToHistory(ListContent.DrawerListItem(type, name, time, date, path))
                    DatabaseController.writeDBHistory(name, path, time, type, date)
                }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }

    //External method to convert seconds to HHmmss
    fun ConvertSecondToHHMMString(secondtTime: String): String {
        var time = "--:--:--"

        if (secondtTime != "null") {

            val value = java.lang.Float.parseFloat(secondtTime).toInt()

            val tz = TimeZone.getTimeZone("UTC")
            val df = SimpleDateFormat("HH:mm:ss", Locale.US)
            df.timeZone = tz
            time = df.format(Date(value * 1000L))
        }


        return time

    }

}
