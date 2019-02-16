package de.domes_muc.printerappkotlin.viewer

import android.app.Activity
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.OctoprintFiles
import de.domes_muc.printerappkotlin.octoprint.OctoprintSlicing
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.os.AsyncTask
import android.os.Handler
import android.widget.Toast

import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.FileOutputStream
import java.util.Random
import java.util.Timer
import java.util.TimerTask

/**
 * Created by alberto-baeza on 10/7/14.
 */
class SlicingHandler(private val mActivity: Activity) {

    //Data array to send to the server
    private var mData: ByteArray? = null
    private var mDataList: List<DataStorage>? = null
    //private String mProfile = null;

    private var mExtras = JSONObject()


    //timer to upload files
    private var mTimer: Timer? = null

    //Check if there is a pending timer
    private var isRunning: Boolean = false

    //Last reference to the temp file
    //returns last .stl reference
    var lastReference: String? = null
    var originalProject: String? = null
        set(path) {

            field = path
            Log.i("OUT", "Workspace: $path")

        }

    //Default URL to slice models
    private var mPrinter: ModelPrinter? = null
    private val mSaveRunnable = Runnable {
        val mFile = createTempFile()

        Log.i("Slicer", "Sending slice command")
        OctoprintSlicing.sliceCommand(mActivity, mPrinter!!.address, mFile, mExtras)
        //if (mExtras.has("print")) mExtras.remove("print");

        Log.i("Slicer", "Showing progress bar")
        ViewerMainFragment.showProgressBar(StateUtils.SLICER_UPLOAD, 0)
    }

    init {
        isRunning = false

        //TODO Clear temp folder?
        cleanTempFolder()
    }


    fun setData(data: ByteArray) {

        mData = data

    }

    fun clearExtras() {

        mExtras = JSONObject()
    }

    fun setExtras(tag: String, value: Any) {

        //mProfile = profile;
        try {

            if (mExtras.has(tag))
                if (mExtras.get(tag) == value) {
                    return
                }
            mExtras.put(tag, value)


            // Log.i("Slicer","Added extra " + tag + ":" + value + " [" + mExtras.toString()+"]");
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        ViewerMainFragment.slicingCallback()
    }

    //Set the printer dynamically to send the files
    fun setPrinter(p: ModelPrinter) {

        mPrinter = p

    }


    //Creates a temporary file and save it into the parent folder
    //TODO create temp folder
    fun createTempFile(): File? {

        var tempFile: File? = null


        //Create temporary folder
        val tempPath = File(LibraryController.parentFolder.getAbsolutePath() + "/temp")

        if (tempPath.mkdir()) {

            Log.i("Slicer", "Creating temporary file $tempPath")

        } else
            Log.i("Slicer", "Directory exists $tempPath")

        try {

            //add an extra random id
            val randomInt = Random().nextInt(100000)

            tempFile = File.createTempFile("tmp", randomInt.toString() + ".stl", tempPath)
            tempFile!!.deleteOnExit()

            //delete previous file
            try {


                var lastFile: File? = null
                if (lastReference != null) {
                    lastFile = File(lastReference)
                    lastFile.delete()
                }


                Log.i("Slicer", "Deleted " + lastReference!!)
            } catch (e: NullPointerException) {

                e.printStackTrace()
            }

            if (tempFile.exists()) {

                lastReference = tempFile.absolutePath

                DatabaseController.handlePreference(DatabaseController.TAG_RESTORE, "Last", lastReference, true)

                DatabaseController.handlePreference(DatabaseController.TAG_SLICING, "Last", tempFile.name, true)


                StlFile.saveModel(mDataList!!, null ?: "", this@SlicingHandler)

                val fos = FileOutputStream(tempFile)
                fos.write(mData)
                fos.fd.sync()
                fos.close()

            } else {

            }


        } catch (e: Exception) {

            e.printStackTrace()
        }

        /*  if (tempFile != null )Log.i("OUT", "FIle created nasdijalskdjldaj as fucking name " + tempFile.getName());
        else Log.i("OUT","ERROR CREATING TEMP FILASIDÑLAISDÑ  ");*/

        return tempFile

    }

    //TODO implementation with timers, should change to ScheduledThreadPoolExecutor maybe
    fun sendTimer(data: List<DataStorage>) {

        //Reset timer in case it was on progress
        if (isRunning) {

            Log.i("Slicer", "Cancelling previous timer")
            mTimer!!.cancel()
            mTimer!!.purge()
            isRunning = false
        }

        //Reschedule task
        mTimer = Timer()
        mDataList = data
        mTimer!!.schedule(SliceTask(), DELAY.toLong())
        isRunning = true

    }

    /**
     * Task to start the uploading and slicing process from a timer
     */
    private inner class SliceTask : TimerTask() {

        override fun run() {


            mActivity.runOnUiThread {
                Log.i("Slicer", "Timer ended, Starting task")

                if (mPrinter != null) {

                    if (mPrinter!!.status == StateUtils.STATE_OPERATIONAL) {

                        DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last")?.let {
                            OctoprintFiles.deleteFile(
                                mActivity,
                                mPrinter!!.address,
                                it,
                                "/local/"
                            )
                        }
                        val saveHandler = Handler()
                        saveHandler.post(mSaveRunnable)

                            //                            new SaveTask().execute();
                    } else {

                        Log.i("Slicer", "No printer available")

                        Toast.makeText(mActivity, R.string.viewer_printer_selected, Toast.LENGTH_LONG).show()

                    }


                } else {

                    if (DatabaseController.count() > 1) {

                    }
                    Toast.makeText(mActivity, R.string.viewer_printer_unavailable, Toast.LENGTH_LONG).show()

                }
            }


            //Timer stopped
            isRunning = false


        }
    }

    /**
     * Task to save the actual file ia background process and then upload it to the server
     */
    private inner class SaveTask : AsyncTask<Any, Any, Any>() {

        internal var mFile: File? = null


        protected override fun doInBackground(objects: Array<Any>): Any? {

            mFile = createTempFile()

            return null
        }

        protected override fun onPostExecute(o: Any) {

            Log.i("Slicer", "Sending slice command")
            OctoprintSlicing.sliceCommand(mActivity, mPrinter!!.address, mFile, mExtras)
            //if (mExtras.has("print")) mExtras.remove("print");

            Log.i("Slicer", "Showing progress bar")
            ViewerMainFragment.showProgressBar(StateUtils.SLICER_UPLOAD, 0)

        }
    }


    //delete temp folder
    private fun cleanTempFolder() {

        val file = File(LibraryController.parentFolder.toString() + "/temp/")

        LibraryController.deleteFiles(file)
    }

    companion object {

        private val DELAY = 3000 //timer delay just in case
    }

}
