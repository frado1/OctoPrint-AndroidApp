package de.domes_muc.printerappkotlin.octoprint

import android.app.DownloadManager
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast

import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestParams

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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.FileNotFoundException
import java.io.UnsupportedEncodingException
import java.lang.Exception

object OctoprintFiles {

    private fun processJsonFile(context: Context, p: ModelPrinter, json: JSONArray, path: File) {
        for (i in 0 until json.length()) {
            //Retrieve every file
            val jsonObj = json.getJSONObject(i)

            //TODO check pending files

            var m: OctoprintFile? = null

            //If it has an origin we need to set it for the printer
            if (jsonObj.getString("origin") == "sdcard") {

                //Set the storage to sd
                m = OctoprintFile("sd/" + jsonObj.getString("name"), false)
                if (m.parent != "sd") m = null
            } else {
                if (jsonObj.getString("type") == "folder") {
                    val children = jsonObj.getJSONArray("children")
                    if (children.length() > 0) {
                        val folderName = jsonObj.getString("name")
                        val folderPath = OctoprintFile(path, folderName, true)
                        processJsonFile(context, p, children, folderPath)
                        m = OctoprintFile(File(path, folderName), true)
                    }
                } else {
                    if (LibraryController.hasExtension(0, jsonObj.getString("name"))) {

                        if (DatabaseController.getPreference(
                                DatabaseController.TAG_SLICING,
                                "Last"
                            ) != null
                        ) {

                            if (DatabaseController.getPreference(
                                    DatabaseController.TAG_SLICING,
                                    "Last"
                                ) == jsonObj.getString("name")
                            ) {

                                if (jsonObj.has("links")) {
                                    DatabaseController.handlePreference(
                                        DatabaseController.TAG_SLICING,
                                        "Last",
                                        "temp.gco",
                                        true
                                    )

                                    OctoprintFiles.downloadFile(
                                        context, p.address + HttpUtils.URL_DOWNLOAD_FILES,
                                        LibraryController.parentFolder.toString() + "/temp/", "temp.gco"
                                    )
                                    OctoprintFiles.deleteFile(
                                        context,
                                        p.address,
                                        jsonObj.getString("name"),
                                        "/local/"
                                    )

                                } else {

                                }

                            }

                        }

                    } else if (LibraryController.hasExtension(1, jsonObj.getString("name"))) {

                        //Set the storage to Witbox
                        //m = File(path, jsonObj.getString("name"))
                        m = OctoprintFile(File(path, jsonObj.getString("name")), false)
                    }
                }

            }

            //Add to storage file list
            if (m != null) p.updateFiles(m)
        }

    }

    /**
     * Get the whole filelist from the server.
     */
    fun getFiles(context: Context, p: ModelPrinter, fileUpload: File?) {

        if (fileUpload != null) { //TODO emulating fileUpload

            p.loaded = false

            //if it's a local file
            p.jobPath = fileUpload.absolutePath
            DatabaseController.handlePreference(DatabaseController.TAG_REFERENCES, p.name, p.jobPath, true)

        }

        val params = RequestParams()

        try {
            //TODO fix
            //params.put("select", true);
            params.put("recursive", "true")

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }


        HttpClientHandler.get(p.address + HttpUtils.URL_FILES, params, object : JsonHttpResponseHandler() {

            override fun onSuccess(
                statusCode: Int, headers: Array<Header>,
                response: JSONObject
            ) {
                super.onSuccess(statusCode, headers, response)

                p.files.clear()
                try {
                    val json = response.getJSONArray("files")
                    if (json != null) {
                        if (fileUpload == null) {
                            processJsonFile(context, p, json, File("/printer/${p.id}/local/"))
/*
                            for (i in 0 until json.length()) {
                                //Retrieve every file
                                val jsonObj = json.getJSONObject(i)

                                //TODO check pending files


                                var m: File? = null


                                //If it has an origin we need to set it for the printer
                                if (jsonObj.getString("origin") == "sdcard") {

                                    //Set the storage to sd
                                    m = File("sd/" + jsonObj.getString("name"))
                                    if (m.parent != "sd") m = null
                                } else {

                                    if (LibraryController.hasExtension(0, jsonObj.getString("name"))) {

                                        if (DatabaseController.getPreference(
                                                DatabaseController.TAG_SLICING,
                                                "Last"
                                            ) != null
                                        ) {

                                            if (DatabaseController.getPreference(
                                                    DatabaseController.TAG_SLICING,
                                                    "Last"
                                                ) == jsonObj.getString("name")
                                            ) {

                                                if (jsonObj.has("links")) {
                                                    DatabaseController.handlePreference(
                                                        DatabaseController.TAG_SLICING,
                                                        "Last",
                                                        "temp.gco",
                                                        true
                                                    )

                                                    OctoprintFiles.downloadFile(
                                                        context, p.address + HttpUtils.URL_DOWNLOAD_FILES,
                                                        LibraryController.parentFolder.toString() + "/temp/", "temp.gco"
                                                    )
                                                    OctoprintFiles.deleteFile(
                                                        context,
                                                        p.address,
                                                        jsonObj.getString("name"),
                                                        "/local/"
                                                    )

                                                } else {

                                                }

                                            }

                                        }


                                    } else if (LibraryController.hasExtension(1, jsonObj.getString("name"))) {

                                        //Set the storage to Witbox
                                        m = File("local/" + jsonObj.getString("name"))
                                    }


                                }

                                //Add to storage file list
                                if (m != null) p.updateFiles(m)


                            }
*/

                        } else {

                            val hash = LibraryController.calculateHash(fileUpload)
                            var found = -1

                            for (i in 0 until json.length()) {

                                //Retrieve every file
                                val jsonObj = json.getJSONObject(i)

                                if (jsonObj.getString("origin") == "local")
                                    if (jsonObj.getString("hash") == hash) {

                                        Log.i("Slicer", "File found with hash " + jsonObj.getString("hash"))
                                        found = i

                                    }

                            }

                            if (found != -1)
                                fileCommand(
                                    context,
                                    p.address,
                                    json.getJSONObject(found).getString("name"),
                                    "/local/",
                                    false,
                                    true
                                )
                            else
                                uploadFile(context, fileUpload, p)
                        }

                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }

            override fun onFailure(
                statusCode: Int, headers: Array<Header>,
                responseString: String, throwable: Throwable
            ) {

                super.onFailure(statusCode, headers, responseString, throwable)
            }

        })

    }

    /**
     * This method will send a select command to the server to load the file into the printer
     * If a select command is sent when the file is 100% printed, the progress will reset
     * If a delete command is also issued, the file will be unselected and then deleted from the server
     *
     * @param context
     * @param url
     * @param filename
     * @param target
     * @param delete
     */
    fun fileCommand(context: Context, url: String, filename: String, target: String, delete: Boolean, print: Boolean) {

        val jsonObj = JSONObject()
        var entity: StringEntity? = null

        try {
            jsonObj.put("command", "select")
            if (print) jsonObj.put("print", "true")
            entity = StringEntity(jsonObj.toString(), "UTF-8")

        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }


        HttpClientHandler.post(context, url + HttpUtils.URL_FILES + target + filename,
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
                    Log.i("OUT", "Command successful")

                    if (delete) {

                        deleteFile(context, url, filename, target)

                    }
                }

                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header>,
                    responseString: String,
                    throwable: Throwable
                ) {
                    super.onFailure(statusCode, headers, responseString, throwable)

                    Log.i("OUT", "Not found on local, trying sd")

                    val jsonObj = JSONObject()
                    var entity: StringEntity? = null

                    try {
                        jsonObj.put("command", "select")
                        if (print) jsonObj.put("print", "true")
                        entity = StringEntity(jsonObj.toString(), "UTF-8")

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }


                    HttpClientHandler.post(context, url + HttpUtils.URL_FILES + "/sdcard/" + filename,
                        entity!!, "application/json", object : JsonHttpResponseHandler() {

                            override fun onSuccess(
                                statusCode: Int,
                                headers: Array<Header>, response: JSONObject
                            ) {
                                super.onSuccess(statusCode, headers, response)
                                Log.i("OUT", "Command successful")

                            }

                        })
                }
            })

    }

    /**
     * Upload a new file to the server using the new API.
     *
     * Right now it uses two requests, the first to upload the file and another one to load it in the printer.
     * @param file
     */
    fun uploadFile(context: Context, file: File, p: ModelPrinter) {

        val params = RequestParams()

        //if it's a local file
        p.jobPath = file.absolutePath

        try {
            //TODO fix
            //params.put("select", true);
            params.put("file", file)

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }


        Toast.makeText(
            context,
            p.displayName + ": " + context.getString(R.string.devices_text_loading) + " " + file.name,
            Toast.LENGTH_LONG
        ).show()
        p.loaded = false

        DatabaseController.handlePreference(DatabaseController.TAG_REFERENCES, p.name, p.jobPath, true)

        HttpClientHandler.post(p.address + HttpUtils.URL_FILES + "/local",
            params, object : JsonHttpResponseHandler() {

                //Override onProgress because it's faulty
                fun onProgress(bytesWritten: Int, totalSize: Int) {}

                //If success, the file was uploaded correctly
                override fun onSuccess(
                    statusCode: Int, headers: Array<Header>,
                    response: JSONObject
                ) {
                    super.onSuccess(statusCode, headers, response)


                    Log.i("SUCCESS", response.toString())


                    try {
                        //p.setLoaded(true);
                        fileCommand(context, p.address, file.name, "/local/", false, true)

                        Toast.makeText(
                            context,
                            p.displayName + ": " + context.getString(R.string.devices_toast_upload_1) + file.name,
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: IllegalArgumentException) {

                        e.printStackTrace()
                        p.loaded = true
                    }

                }

                override fun onFailure(
                    statusCode: Int, headers: Array<Header>,
                    responseString: String, throwable: Throwable
                ) {
                    super.onFailure(statusCode, headers, responseString, throwable)
                    p.loaded = true
                    Log.i("RESPONSEFAIL", responseString)

                    Toast.makeText(
                        context,
                        p.displayName + ": " + context.getString(R.string.devices_toast_upload_2) + file.name,
                        Toast.LENGTH_LONG
                    ).show()

                }

            })

    }

    /**
     * Method to delete a file on the server remotely after it was printed.
     * @param context
     * @param url
     * @param filename
     * @param target
     */
    fun deleteFile(context: Context, url: String, filename: String, target: String) {

        HttpClientHandler.delete(context, url + HttpUtils.URL_FILES + "/local/" + filename,
            object : JsonHttpResponseHandler() {

                //Override onProgress because it's faulty
                fun onProgress(bytesWritten: Int, totalSize: Int) {}

                //If success, the file was uploaded correctly
                override fun onSuccess(
                    statusCode: Int, headers: Array<Header>,
                    response: JSONObject
                ) {
                    super.onSuccess(statusCode, headers, response)

                }

                override fun onFailure(
                    statusCode: Int, headers: Array<Header>,
                    responseString: String, throwable: Throwable
                ) {
                    // TODO Auto-generated method stub
                    super.onFailure(statusCode, headers, responseString, throwable)

                }

            })

    }

    /**
     * TODO CHANGE TO BACKGROUND DOWNLOAD
     * This method will create a Download Manager to retrieve gcode files from the server.
     * Files will be saved in the gcode folder for the current project.
     *
     * @param context
     * @param url download reference
     * @param path local folder to store the file
     * @param filename
     */
    fun downloadFile(context: Context, url: String, path: String, filename: String) {

        Log.i("Slicer", "Downloading $filename")

        val request = DownloadManager.Request(Uri.parse("http:/$url$filename"))

        //hide notifications
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)

        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner()
            //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }

        //Delete duplicated files
        val extra = File(path + filename)
        if (extra.exists()) {
            extra.delete()
        }

        request.setDestinationUri(Uri.parse("file://$path$filename"))

        // get download service and enqueue file
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)

    }

    fun getPrinterIdFromPath(path: String): Long? {
        var printerId: Long? = null

        if (path.startsWith("/printer")) {
            // get printer out of path
            try {
                printerId = java.lang.Long.parseLong(path.split("/")[2])
            } catch (e: Exception) {
                printerId = null
            }
        }
        return printerId
    }
}
