package de.domes_muc.printerappkotlin.model

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import android.content.Context

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList

/**
 * This class will define the profile type that will be used by the printers / quality types
 * Created by alberto-baeza on 12/4/14.
 */
object ModelProfile {

    //Printer profiles
    val WITBOX_PROFILE = "bq_witbox"
    val PRUSA_PROFILE = "bq_hephestos"
    val DEFAULT_PROFILE = "CUSTOM"

    val TYPE_P = ".profile"
    val TYPE_Q = ".quality"

    //Quality profiles
    val LOW_PROFILE = "low_bq"
    val MEDIUM_PROFILE = "medium_bq"
    val HIGH_PROFILE = "high_bq"

    private val PRINTER_TYPE = arrayOf("Witbox", "Hephestos")
    private val PROFILE_OPTIONS = arrayOf(HIGH_PROFILE, MEDIUM_PROFILE, LOW_PROFILE)


    var profileList: ArrayList<String>? = null
        private set
    var qualityList: ArrayList<String>? = null
        private set

    //Retrieve a profile in JSON format
    fun retrieveProfile(context: Context, resource: String, type: String): JSONObject? {

        var id = 0

        //Select a predefined profile
        if (resource == WITBOX_PROFILE) id = R.raw.witbox
        if (resource == PRUSA_PROFILE) id = R.raw.prusa
        if (resource == DEFAULT_PROFILE) id = R.raw.defaultprinter
        if (resource == LOW_PROFILE) id = R.raw.low
        if (resource == MEDIUM_PROFILE) id = R.raw.medium
        if (resource == HIGH_PROFILE) id = R.raw.high

        var fis: InputStream? = null

        if (id != 0)
            fis = context.resources.openRawResource(id)
        else { //Custom profile

            try {
                Log.i("PROFILE", "Looking for $resource")
                fis = context.openFileInput(resource + type)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }


        }
        //fis = context.getResources().openRawResource(id);
        var json: JSONObject? = null
        if (fis != null) {

            val reader = BufferedReader(InputStreamReader(fis))
            val sb = StringBuilder()
            var line: String? = null



            try {
                while (reader.readLine()?.let {sb.append(it)} != null);

                reader.close()


                json = JSONObject(sb.toString())

                Log.i("json", json.toString())


            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }



        return json

    }


    //Save a new custom profile
    fun saveProfile(context: Context, name: String, json: JSONObject, type: String): Boolean {

        val filename = name + type
        val outputStream: FileOutputStream

        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
            outputStream.write(json.toString().toByteArray())
            outputStream.close()

            Log.i("OUT", "Written $filename")

        } catch (e: Exception) {
            e.printStackTrace()

            return false
        }

        return true

    }

    //Delete profile file from internal storage
    fun deleteProfile(context: Context, name: String, type: String): Boolean {

        val file = File(context.filesDir, name + type)
        return if (file.delete()) true else false

    }

    fun reloadQualityList(context: Context) {

        //Add default types plus custom types from internal storage
        qualityList = ArrayList()
        qualityList!!.clear()
        for (s in PROFILE_OPTIONS) {

            qualityList!!.add(s)
        }

        //Add internal storage types
        for (file in context.applicationContext.filesDir.listFiles()) {


            //Only files with the .profile extension
            if (file.absolutePath.contains(TYPE_Q)) {


                val pos = file.name.lastIndexOf(".")
                val name = if (pos > 0) file.name.substring(0, pos) else file.name

                //Add only the name
                qualityList!!.add(name)
            }

        }


    }

    fun reloadList(context: Context) {

        //Add default types plus custom types from internal storage
        profileList = ArrayList()
        profileList!!.clear()
        for (s in PRINTER_TYPE) {

            profileList!!.add(s)
        }

        //Add internal storage types
        for (file in context.applicationContext.filesDir.listFiles()) {

            //Only files with the .profile extension
            if (file.absolutePath.contains(TYPE_P)) {

                val pos = file.name.lastIndexOf(".")
                val name = if (pos > 0) file.name.substring(0, pos) else file.name

                //Add only the name
                profileList!!.add(name)
            }

        }

    }

}
