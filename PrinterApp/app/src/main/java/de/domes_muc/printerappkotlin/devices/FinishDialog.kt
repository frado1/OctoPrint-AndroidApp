package de.domes_muc.printerappkotlin.devices

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.printview.GcodeCache
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.OctoprintFiles
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
//import com.afollestad.materialdialogs.MaterialDialogCompat;

import java.io.File

/**
 * Created by alberto-baeza on 2/12/15.
 */
class FinishDialog(private val mContext: Context, private val mPrinter: ModelPrinter) {

    init {

        createDialog()

    }

    fun createDialog() {

        //Inflate the view
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(R.layout.dialog_finish_printing, null, false)

        //Constructor
        val adb = MaterialDialog(mContext)
            .title(text = mContext.getString(R.string.finish_dialog_title) + " " + mPrinter.job.filename)
            .customView(view = v)
            .positiveButton(R.string.confirm) {
                if (mPrinter.jobPath != null) {
                    val file = File(mPrinter.jobPath)

                    if (file.parentFile.absolutePath.contains(STRING_TEMP)) {

                        //Auto-save
                        val sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext)
                        if (sharedPref.getBoolean(
                                mContext.resources.getString(R.string.shared_preferences_save),
                                true
                            )
                        ) {

                            //Select the same file again to reset progress
                            OctoprintFiles.fileCommand(
                                mContext,
                                mPrinter.address,
                                mPrinter.job.filename!!,
                                "/local/",
                                false,
                                false
                            )

                            val to = File(file.parentFile.parentFile.absolutePath + "/_gcode/" + file.name)

                            DatabaseController.updateHistoryPath(file.absolutePath, to.absolutePath)

                            file.renameTo(to)

                            LibraryController.deleteFiles(file.parentFile)

                        } else {
                            createFinishDialogSave(mPrinter, file)

                        }

                    } else {
                        OctoprintFiles.fileCommand(
                            mContext,
                            mPrinter.address,
                            mPrinter.job.filename!!,
                            "/local/",
                            true,
                            false
                        )
                    }

                    GcodeCache.removeGcodeFromCache(mPrinter.jobPath!!)
                    mPrinter.jobPath = null
                    DatabaseController.handlePreference(DatabaseController.TAG_REFERENCES, mPrinter.name, null, false)
                } else {

                    OctoprintFiles.fileCommand(
                        mContext,
                        mPrinter.address,
                        mPrinter.job.filename!!,
                        "/local/",
                        true,
                        false
                    )

                }
            }
            .show()

    }

    fun createFinishDialogSave(m: ModelPrinter, file: File) {

        //Constructor
        val adb = MaterialDialog(mContext)
        adb.setTitle(m.displayName + " (100%) - " + file.name)

        //Inflate the view
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(R.layout.print_finished_dialog, null, false)

        val cb_server = v.findViewById<View>(R.id.checkbox_keep_server) as CheckBox
        val cb_local = v.findViewById<View>(R.id.checkbox_keep_local) as CheckBox
        val et_name = v.findViewById<View>(R.id.et_name_model) as EditText

        et_name.setText(file.name)

        adb.setContentView(v)
        adb.positiveButton(R.string.ok) {
            if (cb_server.isChecked) {

                //Select the same file again to reset progress
                OctoprintFiles.fileCommand(mContext, m.address, m.job.filename!!, "/local/", false, false)

            } else {

                //Remove file from server
                OctoprintFiles.fileCommand(mContext, m.address, m.job.filename!!, "/local/", true, false)

            }

            if (cb_local.isChecked) {

                val to = File(file.parentFile.parentFile.absolutePath + "/_gcode/" + et_name.text.toString())


                DatabaseController.updateHistoryPath(file.absolutePath, to.absolutePath)
                file.renameTo(to)

                LibraryController.initializeHistoryList()

            } else {

                try {
                    //Delete file locally
                    if (file.delete()) {

                        Log.i("OUT", "File deleted!")

                    }

                } catch (e: NullPointerException) {

                    Log.i("OUT", "Error deleting the file")

                }

            }

            LibraryController.deleteFiles(file.parentFile)
        }

        adb.negativeButton(R.string.cancel)

        adb.show()
    }

    companion object {

        private val STRING_TEMP = "/_tmp"
    }
}
