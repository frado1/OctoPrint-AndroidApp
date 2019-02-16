package de.domes_muc.printerappkotlin.model

import org.json.JSONException
import org.json.JSONObject

/**
 * This class defines a new Printing Job as a Status listener. Basically it's a reference to the current
 * status of the printer. If there is currently an ongoing job, it'll show the printing status, if there's nothing,
 * it won't show anything.
 * @author alberto-baeza
 */
class ModelJob {

    //Printer status
    /*************
     * GETS
     */

    var filename: String? = null
        private set
    var filament: String? = null
        private set
    var size: String? = null
        private set
    val estimated: String? = null
    private val mTimelapse: String? = null
    private val mHeight: String? = null
    var printTime: String? = null
        private set
    var printTimeLeft: String? = null
        private set
    var printed: String? = null
        private set
    var progress = "0"
        private set

    /**
     * Finish job
     */

    var finished = false
        private set


    /***************
     * SETS
     */

    fun updateJob(status: JSONObject) {
        val job: JSONObject
        val progress: JSONObject
        try {

            //Current job status filesize/filament/estimated print time
            job = status.getJSONObject("job")

            filename = job.getJSONObject("file").getString("name")
            filament = job.getString("filament")
            size = job.getJSONObject("file").getString("size")

            //Progress time/timelapse
            progress = status.getJSONObject("progress")

            printed = progress.getString("filepos")
            printTime = progress.getString("printTime")
            printTimeLeft = progress.getString("printTimeLeft")
            this.progress = progress.getString("completion")

            if (this.progress != "null") {
                val n = java.lang.Double.parseDouble(this.progress)
                if (n.toInt() == 100)
                    finished = true
                else
                    finished = false
            } else
                finished = false

            //Log.i("MODEL", "Timelapse: " + mTimelapse + " Height: " + mHeight + " Print time: " + mPrintTime +
            //" Print time left: " + mPrintTimeLeft);


        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    fun setFinished() {

        finished = true

    }

}
