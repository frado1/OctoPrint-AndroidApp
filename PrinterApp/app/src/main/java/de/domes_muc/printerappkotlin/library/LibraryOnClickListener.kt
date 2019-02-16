package de.domes_muc.printerappkotlin.library

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import de.domes_muc.printerappkotlin.ListContent
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.library.detail.DetailViewFragment
import de.domes_muc.printerappkotlin.model.ModelFile
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.OctoprintFiles
import android.content.Context
import android.os.Bundle
//import androidx.appcompat.app.ActionBarActivity;
import androidx.appcompat.view.ActionMode
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import de.domes_muc.printerappkotlin.library.LibraryController.Companion.currentPath
import de.domes_muc.printerappkotlin.octoprint.OctoprintFile

import java.io.File

/**
 * This class will handle the click events for the library elements
 *
 * @author alberto-baeza
 */
class LibraryOnClickListener(private val mContext: LibraryFragment, private val mListView: ListView?) :
    OnItemClickListener, OnItemLongClickListener {
    private var mActionMode: ActionMode? = null

    /**
     * Action mode
     */

    private val mActionModeCallback = object : ActionMode.Callback {

        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.delete_menu, menu)
            return true
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {

                R.id.library_menu_delete -> {

                    val ids = mListView!!.checkedItemPositions
                    createDeleteDialog(ids)
                }
            }

            return false
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: ActionMode) {
            mListView!!.clearChoices()

            //Removed because was causing issues with checked items size
            /*for (int i = 0; i < mListView.getCount(); i++){
               mListView.setItemChecked(i, false);
            }*/


            mListView.post {
                mActionMode = null
                mListView.setSelector(R.drawable.list_selector)
                mListView.choiceMode = AbsListView.CHOICE_MODE_NONE
                mContext.notifyAdapter()
                val listAdapter = mListView.adapter as LibraryAdapter
                listAdapter.setSelectionMode(false)
            }


        }
    }

    //On long click we'll display the gcodes
    fun onOverflowButtonClick(view: View, position: Int) {

        //Avoid to click in the header
        //        position--;

        Log.d("LibraryOnClickListener", "onOverflowButtonClick")

        val f = LibraryController.fileList.get(position)

        //If it's not IN the printer
        if (!f.getParent().contains("printer") &&
            !f.getParent().contains("sd") &&
            !f.getParent().contains("local")
        ) {

            showOptionPopUp(view, position)
        }
    }

    @SuppressLint("SdCardPath")
    override fun onItemClick(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {

        //Avoid to click in the header
        //        arg2--;

        if (mListView!!.choiceMode == ListView.CHOICE_MODE_MULTIPLE) {

            val checked = mListView.isItemChecked(arg2)

            mListView.setItemChecked(arg2, checked)

            val listAdapter = mListView.adapter as LibraryAdapter
            listAdapter.setItemChecked(arg2, checked)

            if (mListView.checkedItemCount < 1) {

                mActionMode!!.finish()
            }

            mContext.notifyAdapter()


        } else {

            Log.d("LibraryOnClickListener", "onItemClick")

            //Logic for getting file type
            val f = LibraryController.fileList.get(arg2)

            //If it's folder open it
            if (f.isDirectory()) {


                //If it's project folder, send stl
                if (LibraryController.isProject(f)) {
                    //Show detail view as a fragment
                    showRightPanel(arg2)

                } else {

                    //Not a project, open folder
                    val folderName = f.name
                    if ( f.parent.startsWith("/printer")) {
                        // get printer out of path

                        val printerId = OctoprintFiles.getPrinterIdFromPath(f.path)
                        LibraryController.retrievePrinterFiles(printerId, File(f.path))
                    } else {
                        LibraryController.reloadFiles(f.getAbsolutePath())
                    }

                    mContext.showListHeader(File(f.parent).name)

                    mContext.sortAdapter()


                }

                //If it's not a folder, just send the file
            } else {

                //it's a printer file
                if (f.path.matches(Regex("^/printer/[0-9]+$"))) {
                    val directory = "${f.path}/local"

                    LibraryController.retrievePrinterFiles(OctoprintFiles.getPrinterIdFromPath(directory), OctoprintFile(directory , true))
                    mContext.sortAdapter()
                    mContext.notifyAdapter()
                } else {

                    try {
                        MaterialDialog(mContext.activity!!)
                            .title(R.string.library_select_printer_title)
                            .message(text = f.name)
                            .positiveButton(R.string.confirm) {
                                val printerId = OctoprintFiles.getPrinterIdFromPath(LibraryController.currentPath?.path ?: "") ?: -1
                                val p =
                                    DevicesListController.getPrinter(printerId)?.let {

                                        //it's a printer folder because there's a printer with the same name
                                        Log.i("File", "Clicking " + f.getAbsolutePath())
                                        //either sd or internal (must check for folders inside sd
                                        if (f.getAbsolutePath().startsWith("/sd")) {

                                            val finalName = f.getAbsolutePath().substring(4, f.getAbsolutePath().length)
                                            Log.i("File", "Loading $finalName")

                                            OctoprintFiles.fileCommand(
                                                mContext.activity!!,
                                                it.address,
                                                finalName,
                                                "/sdcard/",
                                                false,
                                                true
                                            )
                                            //OctoprintSlicing.sliceCommand(mContext.getActivity(), p.getAddress(), f, "/local/");
                                        } else {
                                            OctoprintFiles.fileCommand(
                                                mContext.activity!!,
                                                it.address,
                                                f.path.removePrefix("/printer/$printerId/local/"), //f.name,
                                                "/local/",
                                                false,
                                                true
                                            )
                                        }
                                        Toast.makeText(
                                            mContext.activity,
                                            "Loading " + f.getName() + " in " + it.displayName,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                            .negativeButton(R.string.cancel)
                            .show()


                    } catch (e: NumberFormatException) {

                        e.printStackTrace()

                    }

                }
            }
        }


    }

    private fun showRightPanel(index: Int) {

        val fragmentTransaction = mContext.activity!!.supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(R.animator.fragment_slide_in_right, R.animator.fragment_slide_out_right)

        //New DetailView with the file as an index
        val detail = DetailViewFragment()
        val args = Bundle()
        args.putInt("index", index)
        detail.arguments = args

        fragmentTransaction.replace(R.id.right_panel_container, detail as Fragment, ListContent.ID_DETAIL).commit()
    }

    //Show dialog for handling files
    private fun showOptionPopUp(view: View, index: Int) {

        //Creating the instance of PopupMenu
        val popup = PopupMenu(mContext.activity, view)

        //Logic for getting file type
        val f = LibraryController.fileList.get(index)

        //Different pop ups for different type of files
        if (f.getParent() == "sd" || f.getParent() == "local") {
            popup.menuInflater.inflate(R.menu.library_model_menu_local, popup.menu)
        } else {

            popup.menuInflater.inflate(R.menu.library_model_menu, popup.menu)

            if (f.isDirectory()) {

                if (!LibraryController.isProject(f)) {
                    popup.menu.findItem(R.id.library_model_print).isVisible = false
                    popup.menu.findItem(R.id.library_model_edit).isVisible = false

                } else {

                    if (mContext.currentTab == LibraryController.TAB_FAVORITES) {

                        popup.menu.findItem(R.id.library_model_delete).isVisible = false
                        popup.menu.findItem(R.id.library_model_move).isVisible = false

                    }

                }

            } else {


            }

        }


        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.library_model_print //Print / Multiprint
                -> if (f.isDirectory()) {
                    if (LibraryController.isProject(f)) {
                        //Show detail view as a fragment
                        showRightPanel(index)
                    }
                } else {
                    MainActivity.requestOpenFile(f.getAbsolutePath())

                }
                R.id.library_model_edit //Edit
                -> {
                    //TODO Doesn't work when empty gcodes comeon
                    popup.dismiss()
                    if (f.isDirectory()) {
                        if (LibraryController.isProject(f)) {

                            if ((f as ModelFile).stl == null) {
                                MainActivity.requestOpenFile((f as ModelFile).gcodeList)
                                //DevicesListController.selectPrinter(mContext.getActivity(), new File (((ModelFile)f).getGcodeList()) , 0);

                            } else {
                                MainActivity.requestOpenFile((f as ModelFile).stl)

                            }
                        }
                    } else {
                        //Check if the gcode is empty, won't work if file is actually corrupted
                        if (f.getAbsoluteFile().length() > 0) {
                            MainActivity.requestOpenFile(f.getAbsolutePath())
                        } else {
                            Toast.makeText(mContext.activity, R.string.storage_toast_corrupted, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                R.id.library_model_move //Move
                -> {
                    mContext.setMoveFile(f)
                    Toast.makeText(mContext.activity, R.string.library_paste_toast, Toast.LENGTH_SHORT).show()
                }
                R.id.library_model_delete //Delete
                -> {

                    val ids = SparseBooleanArray()
                    ids.append(index, true)
                    createDeleteDialog(ids)
                }
            }
            true
        }

        popup.show()//showing popup menu

    }


    override fun onItemLongClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long): Boolean {

        if (mContext.currentTab == LibraryController.TAB_PRINTER || mContext.currentTab == LibraryController.TAB_FAVORITES) return false

        mListView!!.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        mListView.selector = mContext.resources.getDrawable(R.drawable.list_selector)

        val listAdapter = mListView.adapter as LibraryAdapter
        listAdapter.setSelectionMode(true)

        if (mActionMode != null) {
            return false
        }

        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = (mContext.activity as AppCompatActivity).startSupportActionMode(mActionModeCallback)
        //        view.setSelected(true);

        return false
    }

    fun hideActionBar() {
        if (mActionMode != null) mActionMode!!.finish()
    }

    private fun createDeleteDialog(ids: SparseBooleanArray) {

        val inflater = mContext.activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val deleteDialogView = inflater.inflate(R.layout.dialog_delete_models, null)
        (deleteDialogView.findViewById<View>(R.id.new_folder_name_textview) as TextView).text =
                mContext.resources.getQuantityString(R.plurals.library_models_delete, ids.size())
        (deleteDialogView.findViewById<View>(R.id.delete_files_icon) as ImageView).setColorFilter(
            mContext.resources.getColor(
                R.color.body_text_2
            )
        )

        //TODO Set images
        if (mListView != null) {
            if (mListView.checkedItemCount == 1) {

                (deleteDialogView.findViewById<View>(R.id.files_num_textview) as TextView).setText(
                    LibraryController.fileList.get(
                        ids.keyAt(0)
                    ).getName()
                )
            } else {
                (deleteDialogView.findViewById<View>(R.id.files_num_textview) as TextView).text = String.format(
                    mContext.resources.getString(R.string.library_menu_models_delete_files),
                    mListView.checkedItemCount
                )
            }
        } else {

            (deleteDialogView.findViewById<View>(R.id.files_num_textview) as TextView).setText(
                LibraryController.fileList.get(
                    ids.keyAt(0)
                ).getName()
            )

        }


        val modelsToDelete = mContext.resources.getQuantityString(R.plurals.library_models_delete_title, ids.size())

        MaterialDialog(mContext.activity!!)
            .title(text = modelsToDelete)
            .customView(view = deleteDialogView, scrollable = true)
            .positiveButton(R.string.confirm) {
                for (i in 0 until ids.size()) {
                    if (ids.valueAt(i)) {
                        val file = LibraryController.fileList.get(ids.keyAt(i))

                        LibraryController.deleteFiles(file)

                        Log.i("Delete", "Deleting " + file.getName())
                    }
                }
                hideActionBar()
                mContext.refreshFiles()
            }
            .negativeButton(R.string.cancel) {
                hideActionBar()
            }
            .show()

    }
}
