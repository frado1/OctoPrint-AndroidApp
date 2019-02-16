package de.domes_muc.printerappkotlin.octoprint

import android.app.Dialog
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import android.content.Context
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
import cz.msebera.android.httpclient.message.BasicHeader
import cz.msebera.android.httpclient.protocol.HTTP
import cz.msebera.android.httpclient.ssl.SSLContexts

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.UnsupportedEncodingException
import java.util.ArrayList

/**
 * This class will issue commands to the server. Mainly control commands like Start, Pause and Cancel.
 * Also will control print head jog and extruder.
 * @author alberto-baeza
 */
object OctoprintControl {


    /**
     * Send a command to the server to start/pause/stop a job
     * @param context
     * @param url
     * @param command
     */
    fun sendCommand(context: Context, url: String, command: String) {

        val jsonObj = JSONObject()
        var entity: StringEntity? = null

        try {
            jsonObj.put("command", command)
            entity = StringEntity(jsonObj.toString(), "UTF-8")
            entity.setContentType(BasicHeader(HTTP.CONTENT_TYPE, "application/json"))

        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        //Get progress dialog UI
        val waitingForServiceDialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_progress_content_horizontal, null)
        (waitingForServiceDialogView.findViewById<View>(R.id.progress_dialog_text) as TextView).setText(R.string.devices_configure_waiting)

        //Show progress dialog
        val connectionDialog = MaterialDialog(context)
        connectionDialog.customView(view = waitingForServiceDialogView, scrollable = true)
            .noAutoDismiss()
        connectionDialog.show()


        HttpClientHandler.post(context, url + HttpUtils.URL_CONTROL,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

                fun onProgress(bytesWritten: Int, totalSize: Int) {}

                override fun onSuccess(
                    statusCode: Int, headers: Array<Header>,
                    response: JSONObject
                ) {
                    super.onSuccess(statusCode, headers, response)

                    //Dismiss progress dialog
                    connectionDialog.dismiss()
                }

                override fun onFailure(
                    statusCode: Int, headers: Array<Header>,
                    responseString: String, throwable: Throwable
                ) {

                    super.onFailure(statusCode, headers, responseString, throwable)

                    //Dismiss progress dialog
                    connectionDialog.dismiss()
                    MainActivity.showDialog(responseString)
                }
            })


    }

    /**
     * Send a printer head command to jog or move home
     * @param context
     * @param url
     * @param command
     * @param axis
     * @param amount
     */
    fun sendHeadCommand(context: Context, url: String, command: String, axis: String, amount: Double) {

        val jsonObj = JSONObject()
        var entity: StringEntity? = null

        try {

            jsonObj.put("command", command)
            if (command == "home") {

                //Must be array list to be able to convert a JSONArray in API < 19
                val s = ArrayList<String>()
                if (axis == "xy") {
                    s.add("x")
                    s.add("y")
                }

                if (axis == "z") s.add("z")


                jsonObj.put("axes", JSONArray(s))

            } else
                jsonObj.put(axis, amount)

            entity = StringEntity(jsonObj.toString(), "UTF-8")

        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }


        HttpClientHandler.post(context, url + HttpUtils.URL_PRINTHEAD,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

                fun onProgress(bytesWritten: Int, totalSize: Int) {}

                override fun onSuccess(
                    statusCode: Int, headers: Array<Header>,
                    response: JSONObject
                ) {
                    super.onSuccess(statusCode, headers, response)

                }

                override fun onFailure(
                    statusCode: Int, headers: Array<Header>,
                    responseString: String, throwable: Throwable
                ) {

                    super.onFailure(statusCode, headers, responseString, throwable)
                    MainActivity.showDialog(responseString)
                }
            })

    }

    fun sendToolCommand(context: Context, url: String, command: String, tool: String?, amount: Double) {

        val jsonObj = JSONObject()
        var entity: StringEntity? = null
        var destinationUrl = HttpUtils.URL_TOOL

        try {

            jsonObj.put("command", command)

            val json = JSONObject()


            if (tool != null) {

                if (tool == "bed") {

                    destinationUrl = HttpUtils.URL_BED
                    jsonObj.put("target", amount)

                } else {
                    json.put(tool, amount)
                    jsonObj.put("targets", json)
                }

            } else {

                jsonObj.put("amount", amount)

            }

            entity = StringEntity(jsonObj.toString(), "UTF-8")

        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }


        Log.i("TOOL", "Sending: " + jsonObj.toString())
        HttpClientHandler.post(context, url + destinationUrl,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

                override fun onSuccess(
                    statusCode: Int, headers: Array<Header>,
                    response: JSONObject
                ) {
                    super.onSuccess(statusCode, headers, response)

                }

                override fun onFailure(
                    statusCode: Int, headers: Array<Header>,
                    responseString: String, throwable: Throwable
                ) {

                    super.onFailure(statusCode, headers, responseString, throwable)
                    MainActivity.showDialog(responseString)
                }
            })

    }

}
