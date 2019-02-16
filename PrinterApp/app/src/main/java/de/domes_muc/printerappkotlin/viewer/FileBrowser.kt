package de.domes_muc.printerappkotlin.viewer

import android.app.Activity
import android.app.Dialog
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.library.LibraryModelCreation
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView

import java.io.File
import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator
import java.util.Locale

/**
 *
 * @author Marina Gonzalez
 */
class FileBrowser : Activity() {

    /**
     *
     * @return selected file name
     */
    val selectedFileName: String
        get() {
            var ret = ""
            if (mSelectedIndex >= 0) {
                ret = mDialogFileList!![mSelectedIndex].name
            }
            return ret
        }

    interface OnFileListDialogListener {
        fun onClickFileList(file: File?)
    }

    companion object {
        private var mContext: Context? = null
        private var mCurrentPath: File? = null
        private var mDialogFileList: Array<File>? = null
        private var mSelectedIndex = -1
        private var mFileListListener: OnFileListDialogListener? = null
        private var mClickListener: OnClickListener? = null
        private var mKeyListener: DialogInterface.OnKeyListener? = null

        private var mTitle: String? = null
        private var mExtStl: String? = null
        private var mExtGcode: String? = null

        val VIEWER = 0
        val LIBRARY = 1

        fun openFileBrowser(context: Context, mode: Int, title: String, extStl: String, extGcode: String) {
            mTitle = title
            mExtStl = extStl
            mExtGcode = extGcode
            mContext = context

            setOnFileListDialogListener(context)
            setOnClickListener(context)
            setOnKeyListener(context)

            when (mode) {
                VIEWER -> setOnFileListDialogListenerToOpenFiles(context)
                LIBRARY -> setOnFileListDialogListener(context)
            }

            show(LibraryController.parentFolder.getAbsolutePath())
        }

        /**
         * Display the file chooser dialog
         *
         * @param path
         */
        fun show(path: String) {
            try {
                mCurrentPath = File(path)
                mDialogFileList = File(path).listFiles()
                if (mDialogFileList == null) {
                    // NG
                    if (mFileListListener != null) {
                        mFileListListener!!.onClickFileList(null)
                    }
                } else {
                    val list = ArrayList<String>()
                    val fileList = ArrayList<File>()
                    // create file list
                    Arrays.sort(mDialogFileList) { object1, object2 ->
                        object1.name.toLowerCase(Locale.US).compareTo(object2.name.toLowerCase(Locale.US))
                    }
                    for (file in mDialogFileList!!) {
                        if (!file.canRead()) {
                            continue
                        }
                        var name: String? = null
                        if (file.isDirectory) {
                            if (!file.name.startsWith(".")) {
                                name = file.name + File.separator
                            }
                        } else {
                            if (LibraryController.hasExtension(0, file.name)) {
                                name = file.name
                            }

                            if (LibraryController.hasExtension(1, file.name)) {
                                name = file.name
                            }
                        }

                        if (name != null) {

                            //Filter by directory, stl or gcode extension
                            if (LibraryController.hasExtension(0, name) || LibraryController.hasExtension(1, name)
                                || file.isDirectory
                            ) {
                                list.add(name)
                                fileList.add(file)
                            }

                        }
                    }

//                    val dialog: Dialog

                    val li = LayoutInflater.from(mContext)
                    val view = li.inflate(R.layout.dialog_list, null)

                    val listView =
                        view.findViewById(R.id.dialog_list_listview) as uk.co.androidalliance.edgeeffectoverride.ListView
                    listView.selector = mContext!!.resources.getDrawable(R.drawable.list_selector)
                    val emptyText = view.findViewById<View>(R.id.dialog_list_emptyview) as TextView
                    listView.emptyView = emptyText

                    val fileBrowserAdapter = FileBrowserAdapter(mContext!!, list, fileList)
                    listView.adapter = fileBrowserAdapter
                    listView.divider = null

                    mDialogFileList = fileList.toTypedArray()

                    val dialog = MaterialDialog(mContext!!)
                        .title(text = mTitle)
                        .customView(view = view, scrollable = false)
                        .negativeButton(R.string.cancel) {
                                mFileListListener!!.onClickFileList(null)
                                it.dismiss()
                            }

                     //   .keyListener(mKeyListener)

                    dialog.show()

                    listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                        // save current position
                        mSelectedIndex = position
                        if (mDialogFileList == null || mFileListListener == null) {
                        } else {
                            val file = mDialogFileList!![position]

                            if (file.isDirectory) {
                                // is a directory: display file list-up again.
                                show(file.absolutePath)
                            } else {
                                // file selected. call the event mFileListListener
                                mFileListListener!!.onClickFileList(file)
                            }
                        }
                        dialog.dismiss()
                    }
                }
            } catch (se: SecurityException) {
                se.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        fun setOnKeyListener(context: Context) {
            mKeyListener = DialogInterface.OnKeyListener { dialog, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                    val fileParent = mCurrentPath!!.parentFile
                    if (fileParent != null) {
                        show(fileParent.absolutePath)
                        dialog.dismiss()
                    } else {
                        // Already the root directory: finish dialog.
                        mFileListListener!!.onClickFileList(null)
                        dialog.dismiss()
                    }

                    return@OnKeyListener true
                }
                false
            }
        }

        fun setOnFileListDialogListener(context: Context) {
            mFileListListener = object : OnFileListDialogListener {
                override fun onClickFileList(file: File?) {
                    LibraryModelCreation.createFolderStructure(context, file)
                }
            }
        }

        fun setOnFileListDialogListenerToOpenFiles(context: Context) {
            mFileListListener = object : OnFileListDialogListener {
                override fun onClickFileList(file: File?) {
                    if (file != null) ViewerMainFragment.openFileDialog(file.path)
                }
            }
        }

        fun setOnClickListener(context: Context) {
            mClickListener = OnClickListener { dialog, which ->
                // save current position
                mSelectedIndex = which
                if (mDialogFileList == null || mFileListListener == null) {
                } else {
                    val file = mDialogFileList!![which]

                    if (file.isDirectory) {
                        // is a directory: display file list-up again.
                        show(file.absolutePath)
                    } else {
                        // file selected. call the event mFileListListener
                        mFileListListener!!.onClickFileList(file)
                    }
                }
            }
        }
    }

}