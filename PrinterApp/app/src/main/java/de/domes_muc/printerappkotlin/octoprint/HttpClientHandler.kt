package de.domes_muc.printerappkotlin.octoprint

import de.domes_muc.printerappkotlin.Log
import android.content.Context


import cz.msebera.android.httpclient.HttpEntity
import com.loopj.android.http.*;
import cz.msebera.android.httpclient.client.ClientProtocolException
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse
import cz.msebera.android.httpclient.client.methods.HttpPatch
import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder
import cz.msebera.android.httpclient.ssl.SSLContexts

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

import javax.net.ssl.SSLContext

/**
 * Static class to handle Http requests with the old API or the new one (with API_KEY)
 * @author alberto-baeza
 */
object HttpClientHandler {

    //Base URL to handle http requests, only needs one slash because services come with another one
    private val BASE_URL = "http:/"
    private val DEFAULT_TIMEOUT = 30000
    private val BIG_TIMEOUT = 70000


    //GET method for both APIs
    operator fun get(url: String, params: RequestParams, responseHandler: AsyncHttpResponseHandler) {
        generateAsyncHttpClient(url).get(getAbsoluteUrl(url), params, responseHandler)

    }

    //GET method for synchronous calls
    fun sync_get(url: String, params: RequestParams, responseHandler: ResponseHandlerInterface) {

        val sync_client = SyncHttpClient()
        sync_client.addHeader("X-Api-Key", HttpUtils.getApiKey(url))
        sync_client.get(getAbsoluteUrl(url), params, responseHandler)
    }

    //POST method for multipart forms
    fun post(url: String, params: RequestParams, responseHandler: AsyncHttpResponseHandler) {
        val client = AsyncHttpClient()
        client.addHeader("X-Api-Key", HttpUtils.getApiKey(url))
        client.connectTimeout = BIG_TIMEOUT
        client.responseTimeout = BIG_TIMEOUT
        client.post(getAbsoluteUrl(url), params, responseHandler)
    }

    //POST method for the new API
    fun post(
        context: Context,
        url: String,
        entity: HttpEntity,
        contentType: String,
        responseHandler: AsyncHttpResponseHandler
    ) {
        generateAsyncHttpClient(url).post(context, getAbsoluteUrl(url), entity, contentType, responseHandler)
    }

    //POST method for synchronous calls
    fun sync_post(
        context: Context,
        url: String,
        entity: HttpEntity,
        contentType: String,
        responseHandler: ResponseHandlerInterface
    ) {
        // sync_client.post(context, getAbsoluteUrl(url), entity, contentType, responseHandler);
    }

    //PUT method for the new API
    fun put(
        context: Context,
        url: String,
        entity: HttpEntity,
        contentType: String,
        responseHandler: AsyncHttpResponseHandler
    ) {
        generateAsyncHttpClient(url).put(context, getAbsoluteUrl(url), entity, contentType, responseHandler)
    }

    //TODO Temporal patch method until it's implemented on the AsyncHttpClient library
    //PUT method for the new API
    fun patch(
        context: Context,
        url: String,
        entity: HttpEntity,
        contentType: String,
        responseHandler: AsyncHttpResponseHandler
    ) {


        var response: CloseableHttpResponse? = null
        try {
            val sslContext = SSLContexts.createSystemDefault()
            val sslsf = SSLConnectionSocketFactory(
                sslContext,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER
            )
            val httpClient = HttpClientBuilder.create()
                .setSSLSocketFactory(sslsf)
                .build()

            //CloseableHttpClient httpClient = HttpClients.custom().build();
            val httpPatch = HttpPatch(URI(getAbsoluteUrl(url)))
            httpPatch.addHeader("Content-Type", "application/json")
            httpPatch.addHeader("X-Api-Key", HttpUtils.getApiKey(url))
            httpPatch.setEntity(entity)
            response = httpClient.execute(httpPatch)

        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: ClientProtocolException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {

            e.printStackTrace()
        } finally {
            try {
                response!!.close()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }


        //client.post(context, getAbsoluteUrl(url), entity, contentType, responseHandler);
    }

    //DELETE method
    @Throws(IllegalArgumentException::class)
    fun delete(context: Context, url: String, responseHandler: AsyncHttpResponseHandler) {
        generateAsyncHttpClient(url).delete(context, getAbsoluteUrl(url), responseHandler)
    }


    private fun getAbsoluteUrl(relativeUrl: String): String {


        Log.i("Connection", BASE_URL + relativeUrl + "?apikey=" + HttpUtils.getApiKey(relativeUrl))
        return BASE_URL + relativeUrl
    }

    /**
     * Generate a client for this session
     * @param relativeUrl
     * @return
     */

    private fun generateAsyncHttpClient(relativeUrl: String): AsyncHttpClient {

        val client = AsyncHttpClient()
        client.addHeader("Content-Type", "application/json")

        if (!relativeUrl.contains(HttpUtils.URL_AUTHENTICATION)) {
            client.addHeader("X-Api-Key", HttpUtils.getApiKey(relativeUrl))
        }

        client.connectTimeout = DEFAULT_TIMEOUT
        client.responseTimeout = DEFAULT_TIMEOUT

        return client

    }


}
