package de.domes_muc.printerappkotlin.octoprint

import de.domes_muc.printerappkotlin.Log
import android.content.Context

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

/**
 * Created by alberto-baeza on 12/9/14.
 */
object OctoprintProfiles {


    /**
     * Uploads a profile and then connects to the server with that profile on the specified port
     * @param context
     * @param url server address
     * @param profile printer profile selected
     * @param port preferred port to connect
     */
    fun uploadProfile(context: Context, url: String, profile: JSONObject, port: String) {

        var entity: StringEntity? = null
        var id: String? = null

        try {

            val finalProfile = JSONObject()

            finalProfile.put("profile", profile)

            entity = StringEntity(finalProfile.toString(), "UTF-8")
            Log.i("OUT", "Profile to add:" + finalProfile.toString())

            id = profile.getString("id")

        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }


        val finalId = id
        HttpClientHandler.post(context, url + HttpUtils.URL_PROFILES,
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
                    Log.i("OUT", "Profile Upload successful")

                    OctoprintConnection.startConnection(url, context, port, finalId!!)


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
     * Delete profiles from the server
     * @param context
     * @param url
     * @param profile
     */
    fun deleteProfile(context: Context, url: String, profile: String) {

        HttpClientHandler.delete(
            context,
            url + HttpUtils.URL_PROFILES + "/" + profile,
            object : JsonHttpResponseHandler() {


                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                    super.onSuccess(statusCode, headers, response)


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
     * Delete profiles from the server
     * @param context
     * @param url
     * @param profile
     */
    fun updateProfile(context: Context, url: String, profile: String) {

        var entity: StringEntity? = null
        try {

            val finalProfile = JSONObject()
            val settings = JSONObject()
            settings.put("default", true)

            finalProfile.put("profile", settings)

            entity = StringEntity(finalProfile.toString(), "UTF-8")
            Log.i("OUT", "Profile to add:" + finalProfile.toString())

        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        HttpClientHandler.patch(context, url + HttpUtils.URL_PROFILES + "/" + profile,
            entity!!, "application/json", object : JsonHttpResponseHandler() {


                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                    super.onSuccess(statusCode, headers, response)



                    Log.i("OUT", "Profile PATCH  successful")


                }

                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header>,
                    responseString: String,
                    throwable: Throwable
                ) {
                    super.onFailure(statusCode, headers, responseString, throwable)

                    Log.i("OUT", "Profile PATCH  FOESNT EXIST")
                }
            })


    }

}
