package de.domes_muc.printerappkotlin.octoprint

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.discovery.AuthenticationUtils
import de.domes_muc.printerappkotlin.devices.discovery.PrintNetworkManager
import de.domes_muc.printerappkotlin.devices.discovery.PrintNetworkReceiver
import de.domes_muc.printerappkotlin.model.ModelPrinter
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

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException

import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

object OctoprintNetwork {

    /**
     * Obtain the network list available to the server to configure one
     * TODO MOST STUPID METHOD EVER
     * @param controller
     */
    fun getNetworkList(controller: PrintNetworkManager, p: ModelPrinter) {


        HttpClientHandler.sync_get(p.address + HttpUtils.URL_AUTHENTICATION, RequestParams(), object : JsonHttpResponseHandler() {

            fun onProgress(bytesWritten: Int, totalSize: Int) {}

            override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                super.onSuccess(statusCode, headers, response)


                Log.i("OUT", "Posting auth")

                val `object` = JSONObject()
                var entity: StringEntity? = null
                try {

                    `object`.put("appid", "com.bq.octoprint.android")
                    `object`.put("key", response.getString("unverifiedKey"))
                    `object`.put(
                        "_sig",
                        AuthenticationUtils.signStuff(controller.context, response.getString("unverifiedKey"))
                    )
                    entity = StringEntity(`object`.toString(), "UTF-8")

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

                HttpClientHandler.post(controller.context, p.address + HttpUtils.URL_AUTHENTICATION,
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
                                //Log.i("Connection","Adding API key " + response.getString("key") + " for ID: " + PrintNetworkManager.getNetworkId(p.getName()));

                                HttpClientHandler.get(
                                    p.address + HttpUtils.URL_NETWORK,
                                    RequestParams(),
                                    object : JsonHttpResponseHandler() {

                                        fun onProgress(bytesWritten: Int, totalSize: Int) {

                                        }

                                        override fun onSuccess(
                                            statusCode: Int, headers: Array<Header>,
                                            response: JSONObject
                                        ) {
                                            super.onSuccess(statusCode, headers, response)

                                            //Send the network list to the Network manager
                                            controller.selectNetworkPrinter(response, p.address)

                                        }


                                        override fun onFailure(
                                            statusCode: Int, headers: Array<Header>,
                                            throwable: Throwable, errorResponse: JSONObject
                                        ) {

                                            super.onFailure(statusCode, headers, throwable, errorResponse)

                                            Log.i("Connection", "Failure while connecting $statusCode")
                                        }

                                    })


                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }

                        }


                    })

            }
        })


    }

    /*******************
     *
     * @param context
     * @param ssid
     * @param psk
     * @param url
     */
    fun configureNetwork(pr: PrintNetworkReceiver, context: Context, ssid: String, psk: String?, url: String) {

        val `object` = JSONObject()
        var entity: StringEntity? = null

        Log.i("Manager", "Configure Network for: $ssid")

        try {
            `object`.put("command", "configure_wifi")
            `object`.put("ssid", ssid)

            if (psk != null)
                `object`.put("psk", psk)
            else
                `object`.put("psk", "")

            entity = StringEntity(`object`.toString(), "UTF-8")

        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        HttpClientHandler.post(context, url + HttpUtils.URL_NETWORK,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

            })
    }

}
