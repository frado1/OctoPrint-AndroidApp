package de.domes_muc.printerappkotlin.library

import android.app.AlertDialog
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView

import java.io.File
import java.util.ArrayList

/**
 * This class will search for files inside the External directory to add them as projects.
 *
 *
 * Created by alberto-baeza on 1/15/15.
 */
class FileScanner(path: String, context: Context) {

    private val mFileList = ArrayList<File>()

    init {

        Log.i("Scanner", "Starting scanner!")


        //Create search dialog
        val adb = AlertDialog.Builder(context)
        val alert: AlertDialog

        adb.setTitle(R.string.search)

        //TODO create a proper view
        val pb = ProgressBar(context)
        pb.isIndeterminate = true
        adb.setView(pb)

        alert = adb.create()
        alert.show()

        startScan(path)

        alert.dismiss() //Dismiss dialog

        Log.i("Scanner", "Found " + mFileList.size + " elements!")

        addDialog(context)

    }

    //Scan recursively for files in the external directory
    private fun startScan(path: String) {

        val pathFile = File(path)

        val files = pathFile.listFiles()

        if (files != null)
            for (file in files) {

                //If folder
                if (file.isDirectory) {


                    //exclude files from the application folder
                    if (!file.absolutePath.contains(LibraryController.parentFolder.getAbsolutePath())) {

                        startScan(file.absolutePath)

                    }
                } else {

                    //Add stl/gcodes to the search list
                    if (LibraryController.hasExtension(0, file.name) || LibraryController.hasExtension(1, file.name)) {

                        Log.i("Scanner", "File found! " + file.name)

                        if (!LibraryController.fileExists(file.name)) {

                            mFileList.add(file)
                        }


                    }


                }


            }

    }

    //Creates a dialog to add the files
    private fun addDialog(context: Context) {

        val fileNames = arrayOfNulls<String>(mFileList.size)
        val checkedItems = BooleanArray(mFileList.size)

        var i = 0

        for (f in mFileList) {

            fileNames[i] = f.name
            checkedItems[i] = false
            i++

        }

        val li = LayoutInflater.from(context)
        val view = li.inflate(R.layout.dialog_list, null)

        val listView = view.findViewById(R.id.dialog_list_listview) as uk.co.androidalliance.edgeeffectoverride.ListView
        listView.selector = context.resources.getDrawable(R.drawable.list_selector)
        val emptyText = view.findViewById<View>(R.id.dialog_list_emptyview) as TextView
        listView.emptyView = emptyText

        val ad = ArrayAdapter<String>(context, R.layout.list_item_add_models_dialog, R.id.text1, fileNames)
        listView.adapter = ad
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.divider = null

        MaterialDialog(context)
            .title(R.string.library_scan_dialog_title)
            .customView(view = view, scrollable = false)
            .negativeButton(R.string.cancel)
            .positiveButton(R.string.dialog_continue) {
                val ids = listView.checkedItemPositions

                val mCheckedFiles = ArrayList<File>()
                for (i in 0 until ids.size()) {

                    if (ids.valueAt(i)) {

                        val file = mFileList[ids.keyAt(i)]
                        mCheckedFiles.add(file)
                        Log.i("Scanner", "Adding: " + file.name)

                    }
                }

                if (mCheckedFiles.size > 0)
                    LibraryModelCreation.enqueueJobs(context, mCheckedFiles) //enqueue checked files

            }
            .show()

    }


}
