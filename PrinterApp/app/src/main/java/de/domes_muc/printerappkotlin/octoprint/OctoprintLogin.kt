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
object OctoprintLogin {

    private val API_INVALID_MSG = "Invalid app"
    private val TAG = "OctoprintLogin"

    private var mLoginResponse: JSONObject? = null

    val session: String
    get() {
        return mLoginResponse?.getString("session") ?: ""
    }

    /**
     * Add the verified API key to the server
     * @param context
     * @param key
     * @param p
     * @param retry
     */
    fun postLogin(context: Context, p: ModelPrinter, callback: ((Context, ModelPrinter) -> (Unit))?) {

        Log.i(TAG, "Posting Login")

        val payloadObj = JSONObject()
        var entity: HttpEntity? = null
        try {
            payloadObj.put("passive", "passive")
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

        HttpClientHandler.post(context, p.address + HttpUtils.URL_LOGIN,
            entity!!, "application/json", object : JsonHttpResponseHandler() {

                //Override onProgress because it's faulty
                fun onProgress(bytesWritten: Int, totalSize: Int) {}

                override fun onSuccess(statusCode: Int, headers: Array<Header>, response: JSONObject) {
                    super.onSuccess(statusCode, headers, response)

                    try {
                        mLoginResponse = response
                        p.userName = response.getString("name")
                        p.userSession = response.getString("session")

                        Log.i(TAG,"Login: ${p.userName}:${p.userSession}")
                        //OctoprintConnection.doConnection(context,p);

                        if ( callback != null )
                            callback(context, p)


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
