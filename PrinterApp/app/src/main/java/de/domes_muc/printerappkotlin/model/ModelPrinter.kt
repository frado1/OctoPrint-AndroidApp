package de.domes_muc.printerappkotlin.model

import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.camera.CameraHandler
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.database.DeviceInfo
import de.domes_muc.printerappkotlin.octoprint.OctoprintConnection
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.Context
import de.domes_muc.printerappkotlin.octoprint.OctoprintLogin

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.util.ArrayList

class ModelPrinter(//Service info
    /*********
     * Gets
     */

    val name: String, val address: String, type: Int
) {

    //Id for database interaction
    var id: Long = 0

    var type: Int = 0
        private set
    var profile: String? = null
        private set
    var displayName: String? = null
    var displayColor = 0
    var status = StateUtils.STATE_NONE
        private set
    var port: String? = null
    var network: String? = null
    var webcamAddress: String? = null

    var userName: String? = name
    var userSession: String? = null

    //TODO hardcoded string
    var message = "Offline"
        private set
    var temperature: String? = null
        private set
    var tempTarget: String? = null
        private set
    var bedTemperature: String? = null
        private set
    var bedTempTarget: String? = null
        private set

    var extruderTemp: Array<String?> = arrayOfNulls<String>(4)
        private set
    var extruderTempTarget: Array<String?> = arrayOfNulls<String>(4)
        private set

    val files: ArrayList<File>

    //Pending job
    val job: ModelJob
    var loaded: Boolean = false

    //Job path in case it's a local file, else null
    var jobPath: String? = null

    //Camera
    private val mCam: CameraHandler? = null

    //Position in grid
    private var mPosition: Int = 0

    //TODO temporary profile handling
    val profiles: ArrayList<JSONObject>

    //change position
    var position: Int
        get() = mPosition
        set(pos) {

            if (Integer.valueOf(pos) == null)
                mPosition = DevicesListController.searchAvailablePosition()
            else
                mPosition = pos

            DatabaseController.updateDB(DeviceInfo.FeedEntry.DEVICES_POSITION, id, mPosition.toString())
        }

    init {
        displayName = name
        job = ModelJob()
        files = ArrayList()
        profiles = ArrayList()
        loaded = true

        //TODO: Load with db
        jobPath = null

        //Set new position according to the position in the DB, or the first available
        //if ((Integer.valueOf(position)==null)) mPosition = DevicesListController.searchAvailablePosition();
        //else mPosition = position;

        mPosition = DevicesListController.searchAvailablePosition()

        //TODO predefine network types

        when (type) {

            StateUtils.STATE_ADHOC, StateUtils.STATE_NEW -> status = type

            else -> status = StateUtils.STATE_NONE
        }

        this.type = type
    }

    /**********
     * Sets
     */

    fun updatePrinter(message: String, stateCode: Int, status: JSONObject?) {

        this.status = stateCode
        this.message = message


        if (status != null) {

            job.updateJob(status)

            try {
                //Avoid having empty temperatures
                val temperature = status.getJSONArray("temps")
                if (temperature.length() > 0) {
                    val iter = temperature.getJSONObject(0).keys()
                    while (iter.hasNext()) {
                        val key = iter.next()
                        if (key.startsWith("tool")) {
                            val toolNumber = key.substringAfter("tool").toInt()
                            try {
                                extruderTemp[toolNumber] = temperature.getJSONObject(0).getJSONObject(key).getString("actual")
                                extruderTempTarget[toolNumber] = temperature.getJSONObject(0).getJSONObject(key).getString("target")
                            } catch (e: JSONException) {
                                // Something went wrong!
                                e.printStackTrace()
                            }
                        }

                    }
                    this.temperature = temperature.getJSONObject(0).getJSONObject("tool0").getString("actual")
                    tempTarget = temperature.getJSONObject(0).getJSONObject("tool0").getString("target")

                    bedTemperature = temperature.getJSONObject(0).getJSONObject("bed").getString("actual")
                    bedTempTarget = temperature.getJSONObject(0).getJSONObject("bed").getString("target")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
    }

    fun updateFiles(m: File) {
        files.add(m)
    }

    fun startUpdate(context: Context) {
        //Initialize web socket connection
        //OctoprintConnection.getNewConnection(context, this, false);
        status = StateUtils.STATE_NONE
        OctoprintLogin.postLogin(context, this) { context: Context, p: ModelPrinter ->
            OctoprintConnection.openSocket(p, context)
        }
    }

    fun setConnecting() {
        status = StateUtils.STATE_NONE
    }

    fun setType(type: Int, profile: String?) {
        this.type = type
        this.profile = profile
    }

}
