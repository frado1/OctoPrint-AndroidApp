package de.domes_muc.printerappkotlin.octoprint

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.discovery.AuthenticationUtils
import de.domes_muc.printerappkotlin.devices.discovery.PrintNetworkManager
import de.domes_muc.printerappkotlin.model.ModelPrinter
import android.content.Context

import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.HttpEntity

import cz.msebera.android.httpclient.client.ClientProtocolException
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse
import cz.msebera.android.httpclient.client.methods.HttpPatch
import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory
import cz.msebera.android.httpclient.entity.StringEntity
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder
import cz.msebera.android.httpclient.ssl.SSLContexts

import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException

import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

/**
 * Created by alberto-baeza on 11/21/14.
 */
object OctoprintAuthentication {

    private val API_INVALID_MSG = "Invalid app"


    /**
     * Send an authentication petition to retrieve an unverified api key
     * @param context
     * @param p target printer
     * @param retry whether we should re-display the dialog or not
     */

    fun getAuth(context: Context, p: ModelPrinter, retry: Boolean) {

        HttpClientHandler.get(p.address + HttpUtils.URL_AUTHENTICATION, RequestParams(), object : JsonHttpResponseHandler() {

            fun onProgress(bytesWritten: Int, totalSize: Int) {}

            override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                super.onSuccess(statusCode, headers, response)

                Log.i("Connection", "Success! " + response.toString())

                try {
                    postAuth(context, response.getString("unverifiedKey"), p, retry)
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

                Log.i("Connection", "Failure! $responseString")
            }
        })

    }

    /**
     * Add the verified API key to the server
     * @param context
     * @param key
     * @param p
     * @param retry
     */
    fun postAuth(context: Context, key: String, p: ModelPrinter, retry: Boolean) {


        Log.i("OUT", "Posting auth")

        val payloadObj = JSONObject()
        var entity: HttpEntity? = null
        try {

            payloadObj.put("appid", "com.bq.octoprint.android")
            payloadObj.put("key", key)
            payloadObj.put("_sig", AuthenticationUtils.signStuff(context, key))
            entity = StringEntity(payloadObj.toString(), "UTF-8")

        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: SignatureException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        HttpClientHandler.post(context, p.address + HttpUtils.URL_AUTHENTICATION,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                    super.onSuccess(statusCode, headers, response)

                    try {
                        DatabaseController.handlePreference(
                            DatabaseController.TAG_KEYS,
                            PrintNetworkManager.getNetworkId(p.name),
                            response.getString("key"),
                            true
                        )
                        Log.i(
                            "Connection",
                            "Adding API key " + response.getString("key") + " for ID: " + PrintNetworkManager.getNetworkId(
                                p.name
                            )
                        )
                        //OctoprintConnection.doConnection(context,p);


                        if (!retry) {

                            Log.i("CONNECTION", "Connection from: AUTH")
                            OctoprintConnection.doConnection(context, p)

                        } else
                            OctoprintConnection.getNewConnection(context, p)

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
                    Log.i("Connection", responseString + " for " + p.address)

                    if (statusCode == 401 && responseString.contains(API_INVALID_MSG)) {

                        //Remove element and show dialog to add manually
                        DevicesListController.removeElement(p.position)
                        OctoprintConnection.showApiDisabledDialog(context)
                    }


                }
            })

    }
}
