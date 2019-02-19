package de.domes_muc.printerappkotlin.devices.camera

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.view.View.OnClickListener
import android.widget.FrameLayout
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient

/*
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
*/

import java.io.IOException
import java.net.URI

/**
 * This class will handle the Camera connection.
 * The device camera will stream on MJPEG.
 * @author alberto-baeza
 */
class CameraHandler(
    private val mContext: Context, //sample public cam
    private val URL: String, private val mRootView: FrameLayout?
) {

    //UI reference to the video view
    var view: MjpegView? = null

    //Boolean to check if the stream was already started
    var isRunning = false

    init {
        view = MjpegView(mContext)


        /**
         * This method handles the stream connection / reconnection.
         * We need to check if the stream alredy started to reconnect in case
         * of a bad initial conection (when we upgrade a service).
         * If it was running, we only need to restart the stream by setting
         * the source again.
         */
        view!!.setOnClickListener {
            if (!view!!.isRunning) {

                if (!isRunning) {
                    DoRead().execute(URL)
                    isRunning = true
                } else
                    view!!.restartPlayback(mContext)


            }
        }

        //Read stream
        Log.i("CAMERA", "Executing $URL")

    }//Create URL
    //        URL = "http:/" + address.substring(0,address.lastIndexOf(':')) + STREAM_PORT;

    fun startVideo() {

        if (!view!!.isRunning) {

            if (!isRunning) {
                DoRead().execute(URL)
                isRunning = true
            }

        }
    }

    /**
     * This class will send a http get request to the server's stream
     * @author alberto-baeza
     */
    internal inner class DoRead : AsyncTask<String, Void, MjpegInputStream>() {

        override fun doInBackground(vararg url: String): MjpegInputStream? {


            var res: HttpResponse? = null
            val httpclient = DefaultHttpClient()

            try {
                res = httpclient.execute(HttpGet(URI.create(url[0])))

                return if (res!!.getStatusLine().getStatusCode() === 401) {
                    null
                } else MjpegInputStream(res!!.getEntity().getContent())

            } catch (e: Exception) {
                //Error connecting to camera
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(result: MjpegInputStream?) {

            if (result != null) {
                //Returns an input stream
                view!!.setSource(result)

                //Display options
                view!!.setDisplayMode(MjpegView.SIZE_BEST_FIT)

                view!!.showFps(true)

            } else {
                mRootView?.findViewById<View>(R.id.videocam_off_layout)?.bringToFront()
            }


        }
    }

    companion object {

        private val STREAM_PORT = ":8080/?action=stream"
    }
}
