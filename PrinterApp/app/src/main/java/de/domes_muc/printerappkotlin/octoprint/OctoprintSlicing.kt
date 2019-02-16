package de.domes_muc.printerappkotlin.octoprint

import android.app.ProgressDialog
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.viewer.ViewerMainFragment
import android.content.Context

import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestParams

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

import java.io.File
import java.io.FileNotFoundException
import java.io.UnsupportedEncodingException

object OctoprintSlicing {

    /**
     * Upload a profile to the server with custom parameters
     * @param context
     * @param p
     * @param profile
     */
    fun sendProfile(context: Context, p: ModelPrinter, profile: JSONObject) {


        var entity: StringEntity? = null

        var key: String? = null
        //
        try {
            entity = StringEntity(profile.toString(), "UTF-8")
            key = profile.getString("key")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        //Progress dialog to notify command events
/*
        val pd = ProgressDialog(context)
        pd.setMessage(context.getString(R.string.devices_command_waiting))
        pd.show()
*/

        HttpClientHandler.put(context, p.address + HttpUtils.URL_SLICING + "/" + key,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

                fun onProgress(bytesWritten: Int, totalSize: Int) {}

                override fun onSuccess(
                    statusCode: Int, headers: Array<Header>,
                    response: JSONObject
                ) {
                    super.onSuccess(statusCode, headers, response)

                    Log.i("OUT", response.toString())
                    //Dismiss progress dialog
                    //pd.dismiss()

                    //Reload profiles
                    OctoprintSlicing.retrieveProfiles(context, p)


                }

                override fun onFailure(
                    statusCode: Int, headers: Array<Header>,
                    responseString: String, throwable: Throwable
                ) {

                    super.onFailure(statusCode, headers, responseString, throwable)
                    Log.i("OUT", responseString)
                    //Dismiss progress dialog
                    //pd.dismiss()
                    MainActivity.showDialog(responseString)
                }
            })

    }

    /**
     * Delete the profile selected by the profile parameter
     * @param context
     * @param profile
     */
    fun deleteProfile(context: Context, p: ModelPrinter, profile: String) {

        HttpClientHandler.delete(
            context,
            p.address + HttpUtils.URL_SLICING + "/" + profile,
            object : JsonHttpResponseHandler() {

                fun onProgress(bytesWritten: Int, totalSize: Int) {}

                override fun onSuccess(
                    statusCode: Int, headers: Array<Header>,
                    response: JSONObject
                ) {
                    super.onSuccess(statusCode, headers, response)

                    //Reload profiles
                    retrieveProfiles(context, p)
                }


            })

    }


    /**
     * Method to retrieve slice profiles before sending the file to the actual printer
     *
     */
    fun retrieveProfiles(context: Context, p: ModelPrinter) {

        HttpClientHandler.get(p.address + HttpUtils.URL_SLICING, RequestParams(), object : JsonHttpResponseHandler() {

            override fun onSuccess(
                statusCode: Int, headers: Array<Header>,
                response: JSONObject
            ) {
                super.onSuccess(statusCode, headers, response)

                p.profiles.clear()

                val keys = response.keys()

                while (keys.hasNext()) {

                    val current = keys.next()

                    try {

                        if (response.getJSONObject(current).getBoolean("default")) {
                            Log.i("OUT", "Selected item is " + response.getJSONObject(current).getString("key"))
                        }

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    HttpClientHandler.get(
                        p.address + HttpUtils.URL_SLICING + "/" + current,
                        RequestParams(),
                        object : JsonHttpResponseHandler() {


                            override fun onSuccess(
                                statusCode: Int, headers: Array<Header>,
                                response: JSONObject
                            ) {
                                super.onSuccess(statusCode, headers, response)


                                /**
                                 * Check if the profile is already added because auto-refresh
                                 */
                                for (o in p.profiles) {

                                    try {
                                        if (o.getString("key") == response.getString("key")) return
                                    } catch (e: JSONException) {
                                        e.printStackTrace()
                                    }

                                }


                                if (!p.profiles.contains(response)) {

                                    p.profiles.add(response)

                                    Log.i("OUT", "Adding profile")
                                }


                            }


                        })

                }


            }

            override fun onFailure(
                statusCode: Int, headers: Array<Header>,
                responseString: String, throwable: Throwable
            ) {

                super.onFailure(statusCode, headers, responseString, throwable)
                Log.i("OUT", responseString)
            }
        })

    }

    fun getMetadata(url: String, filename: String) {


        HttpClientHandler.get(
            url + HttpUtils.URL_FILES + "/local/" + filename,
            RequestParams(),
            object : JsonHttpResponseHandler() {

                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                    super.onSuccess(statusCode, headers, response)

                    Log.i("Metadata", response.toString())

                    try {

                        val estimated = response.getJSONObject("gcodeAnalysis").getString("estimatedPrintTime")
                        ViewerMainFragment.showProgressBar(StateUtils.SLICER_DOWNLOAD, Integer.parseInt(estimated))


                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }


                }
            })


    }


    /**
     * Send a slice command by uploading the file first and then send the command, the result
     * will be handled in the socket payload response
     * @param context
     * @param url
     * @param file
     */
    fun sliceCommand(context: Context, url: String, file: File?, extras: JSONObject) {

        val params = RequestParams()
        try {
            params.put("file", file)

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        //Log.i("Slicer","Upaload " + file.getName());
        if (file != null)
            HttpClientHandler.post(url + HttpUtils.URL_FILES + "/local",
                params, object : JsonHttpResponseHandler() {

                    //Override onProgress because it's faulty
                    fun onProgress(bytesWritten: Int, totalSize: Int) {
                        val progress = bytesWritten * 100 / totalSize

                        if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last") != null)
                            if (DatabaseController.getPreference("Slicing", "Last") == file.name) {
                                ViewerMainFragment.showProgressBar(StateUtils.SLICER_UPLOAD, progress)
                            } //else sendFailureMessage(0, null, null, null);


                    }


                    //If success, the file was uploaded correctly
                    override fun onSuccess(
                        statusCode: Int, headers: Array<Header>,
                        response: JSONObject
                    ) {
                        super.onSuccess(statusCode, headers, response)


                        Log.i("Slicer", "Upload successful") //TODO

                        var entity: StringEntity? = null

                        try {
                            extras.put("command", "slice")
                            extras.put("slicer", "cura")

                            //TODO select profile

                            //object.put("profile", profile);
                            extras.put("gcode", "temp.gco")
                            entity = StringEntity(extras.toString(), "UTF-8")

                            Log.i("OUT", "Uploading " + extras.toString())

                        } catch (e: JSONException) {
                            e.printStackTrace()
                        } catch (e: UnsupportedEncodingException) {
                            e.printStackTrace()
                        }



                        Log.i("Slicer", "Send slice command for " + file.name)

                        if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last") != null)
                            if (DatabaseController.getPreference("Slicing", "Last") == file.name)
                                HttpClientHandler.post(context, url + HttpUtils.URL_FILES + "/local/" + file.name,
                                    entity!!, "application/json", object : JsonHttpResponseHandler() {

                                        fun onProgress(
                                            bytesWritten: Int,
                                            totalSize: Int
                                        ) {
                                        }

                                        override fun onSuccess(
                                            statusCode: Int,
                                            headers: Array<Header>, response: JSONObject
                                        ) {
                                            super.onSuccess(statusCode, headers, response)


                                            ViewerMainFragment.showProgressBar(StateUtils.SLICER_SLICE, 0)
                                            Log.i("Slicer", "Slicing started")


                                        }


                                        override fun onFailure(
                                            statusCode: Int, headers: Array<Header>,
                                            responseString: String, throwable: Throwable
                                        ) {

                                            super.onFailure(statusCode, headers, responseString, throwable)
                                            Log.i("OUT", responseString)

                                            ViewerMainFragment.showProgressBar(StateUtils.SLICER_HIDE, -1)
                                        }
                                    })

                    }

                    override fun onFailure(
                        statusCode: Int,
                        headers: Array<Header>,
                        responseString: String,
                        throwable: Throwable
                    ) {
                        super.onFailure(statusCode, headers, responseString, throwable)

                        Log.i("Slicer", "FAILURESLICING")
                        ViewerMainFragment.showProgressBar(StateUtils.SLICER_HIDE, -1)
                    }
                })


    }


}

