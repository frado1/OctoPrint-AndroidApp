package de.domes_muc.printerappkotlin.library

import android.annotation.SuppressLint
import android.annotation.TargetApi

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.octoprint.HttpUtils
import de.domes_muc.printerappkotlin.viewer.FileBrowser
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.rengwuxian.materialedittext.MaterialEditText
import de.domes_muc.printerappkotlin.octoprint.OctoprintFiles
//import com.afollestad.materialdialogs.MaterialDialogCompat;
//import com.rengwuxian.materialedittext.MaterialEditText;

import java.io.File
import java.util.Comparator

/**
 * Fragment to show the library with files on the system/remote
 *
 * @author alberto-baeza
 */
class LibraryFragment : Fragment() {

    private var mListAdapter: LibraryAdapter? = null
    private var mListClickListener: LibraryOnClickListener? = null

    private var mListView: ListView? = null
    private var mListHeader: View? = null

    private var mCurrentFilter: String? = null
    var currentTab: String = LibraryController.TAB_ALL
        private set
    private var mSortType = SORT_NAME

    private var mMoveFile: File? = null

    private var mOnNavTextViewClick: View.OnClickListener? = null

    private var mRootView: View? = null

    /**
     * Listener for the navigation text views
     *
     * @return
     */
    private val onNavTextViewClickListener: View.OnClickListener
        get() {
            if (mOnNavTextViewClick != null) return mOnNavTextViewClick!!

            mOnNavTextViewClick = View.OnClickListener { v ->
                selectNavItem(v.id)

                if (mListClickListener != null) mListClickListener!!.hideActionBar()

                LibraryController.setCurrentPath(LibraryController.parentFolder.toString() + "/Files")

                when (v.id) {
                    R.id.library_nav_all_models -> currentTab = LibraryController.TAB_ALL
                    R.id.library_nav_local_models -> currentTab = LibraryController.TAB_CURRENT
                    R.id.library_nav_printer_models -> currentTab = LibraryController.TAB_PRINTER
                    R.id.library_nav_fav_models -> currentTab = LibraryController.TAB_FAVORITES
                    else -> {
                    }
                }
                refreshFiles()
                hideListHeader()
                MainActivity.closeDetailView()
                activity?.invalidateOptionsMenu()
            }

            return mOnNavTextViewClick!!
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Retain instance to keep the Fragment from destroying itself
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Reference to View
        mRootView = null

        //If is not new
        if (savedInstanceState == null) {

            //Show custom option menu
            setHasOptionsMenu(true)

            //Inflate the fragment
            mRootView = inflater.inflate(
                R.layout.library_layout,
                container, false
            )

            mRootView!!.isFocusableInTouchMode = true
            /*mRootView.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {

                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        return true;
                    }
                    return false;
                }
            });*/

            /**
             * CUSTOM VIEW METHODS
             */

            //References to adapters
            //TODO maybe share a gridview

            //Initial file list
            LibraryController.reloadFiles(LibraryController.TAB_ALL)

            mListAdapter = LibraryAdapter(activity!!, this, R.layout.list_item_library, LibraryController.fileList)

            mListHeader = mRootView!!.findViewById(R.id.list_storage_header) as View
            hideListHeader()

            mListView = mRootView!!.findViewById(R.id.list_storage) as ListView

            if (LibraryController.fileList.size == 0) {

                val emptyView = mRootView!!.findViewById<View>(R.id.library_empty_view) as LinearLayout

                mListView!!.emptyView = emptyView
                emptyView.findViewById<View>(R.id.obtain_models_button).setOnClickListener { optionGetModelsDialog() }

                emptyView.findViewById<View>(R.id.scan_device_button).setOnClickListener { optionSearchSystem() }
            }




            mListClickListener = LibraryOnClickListener(this, mListView)
            mListView!!.selector = resources.getDrawable(R.drawable.list_selector)
            mListView!!.onItemClickListener = mListClickListener
            mListView!!.onItemLongClickListener = mListClickListener
            mListView!!.divider = null
            mListView!!.adapter = mListAdapter

            val backButton = mRootView!!.findViewById<View>(R.id.go_back_icon) as ImageButton
            backButton?.setColorFilter(
                activity!!.resources.getColor(R.color.body_text_2),
                PorterDuff.Mode.MULTIPLY
            )
            backButton.setOnClickListener {
                //Logic to go back on the library navigation
                //                    if(!LibraryController.getCurrentPath().getParent().equals(LibraryController.getParentFolder().getAbsolutePath())) {
                //                        hideListHeader();
                //                    }
                val aktPath = LibraryController.currentPath!!.parent
                val printerId = OctoprintFiles.getPrinterIdFromPath(aktPath)
                if (aktPath.startsWith("/printer")) {
                    if ( ! aktPath.equals("/printer/$printerId") ) {
                        LibraryController.retrievePrinterFiles(printerId, File(aktPath))
                        showListHeader(File(LibraryController.currentPath?.parent).name)
                        sortAdapter()
                    }

                    if ( aktPath.equals("/printer/$printerId/local"))
                        hideListHeader()
                } else {
                    if (LibraryController.currentPath!!.getAbsolutePath() != LibraryController.parentFolder.getAbsolutePath()) {
                        LibraryController.reloadFiles(LibraryController.currentPath!!.getParent())
                        showListHeader(LibraryController.currentPath!!.getName())
                        sortAdapter()

                        if (LibraryController.currentPath!!.getAbsolutePath() == LibraryController.parentFolder.getAbsolutePath() + "/Files") {
                            hideListHeader()
                        }
                    }
                }

            }

            //Set left navigation menu behavior
            (mRootView!!.findViewById<View>(R.id.library_nav_all_models) as TextView).setOnClickListener(
                onNavTextViewClickListener
            )
            (mRootView!!.findViewById<View>(R.id.library_nav_local_models) as TextView).setOnClickListener(
                onNavTextViewClickListener
            )
            (mRootView!!.findViewById<View>(R.id.library_nav_printer_models) as TextView).setOnClickListener(
                onNavTextViewClickListener
            )
            (mRootView!!.findViewById<View>(R.id.library_nav_fav_models) as TextView).setOnClickListener(
                onNavTextViewClickListener
            )


            //Close detailview when clicking outside
            mRootView!!.findViewById<View>(R.id.library_nav_menu).setOnTouchListener { view, motionEvent ->
                MainActivity.closeDetailView()
                false
            }

            mListView!!.setOnTouchListener { view, motionEvent ->
                MainActivity.closeDetailView()
                false
            }

            sortAdapter()

        }
        return mRootView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (mListClickListener != null) mListClickListener!!.hideActionBar()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.library_menu, menu)

        if (mMoveFile != null && (currentTab == LibraryController.TAB_ALL || currentTab == LibraryController.TAB_CURRENT)) {

            menu.findItem(R.id.library_paste).isVisible = true

        } else
            menu.findItem(R.id.library_paste).isVisible = false

        if (currentTab == LibraryController.TAB_FAVORITES || currentTab == LibraryController.TAB_PRINTER) {
            menu.findItem(R.id.library_create).isVisible = false
        } else
            menu.findItem(R.id.library_create).isVisible = true
    }

    //Option menu
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {

        return when (item.itemId) {
            R.id.library_search -> {
                optionSearchLibrary()
                true
            }
            R.id.library_add -> {
                //optionAddLibrary();
                optionSearchSystem()
                true
            }
            R.id.library_sort -> {
                optionSort()
                true
            }
            R.id.library_create -> {
                optionCreateLibrary()
                true
            }
            R.id.library_paste -> {
                optionPaste()
                true
            }
            R.id.library_reload -> {
                refreshFiles()
                true
            }
            R.id.library_models -> {
                optionGetModelsDialog()
                true
            }
            R.id.library_settings -> {
                MainActivity.showExtraFragment(0, 0)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.e("Tag", "Se para el fragmento")
    }

    override fun onPause() {
        super.onPause()
        Log.e("Tag", "Se pausa el fragmento")
    }


    /**
     * Set the state of the selected nav item
     *
     * @param selectedId Id of the nav item that has been pressed
     */
    fun selectNavItem(selectedId: Int) {

        if (mRootView != null) {
            //Get the left nav menu
            val navMenu = mRootView!!.findViewById<View>(R.id.library_nav_menu) as LinearLayout

            //Set the behavior of the nav items
            for (i in 0 until navMenu.childCount) {
                val v = navMenu.getChildAt(i)
                if (v is TextView) {
                    if (v.id == selectedId)
                        v.setTextAppearance(activity, R.style.SelectedNavigationMenuItem)
                    else
                        v.setTextAppearance(activity, R.style.NavigationMenuItem)
                }
            }

        }
    }

    //Reload file list with the currently selected tab
    fun refreshFiles() {

        Log.i("Files", "Refresh for $currentTab")

        LibraryController.reloadFiles(currentTab)
        sortAdapter()
    }

    //Search an item within the library applying a filter to the adapter
    fun optionSearchLibrary() {

        val et = EditText(activity)

        val adb = MaterialDialog(activity!!)
                 .title(R.string.library_search_dialog_title)
                 .customView(view = et)
                 .positiveButton(R.string.search) {
                        mCurrentFilter = et.text.toString()
                        refreshFiles()
                }
                .negativeButton(R.string.cancel)
                .show()
    }

    //Search for models in filesystem
    fun optionSearchSystem() {

        FileScanner(Environment.getExternalStorageDirectory().absolutePath, activity!!)

    }

    //Add a new project using the viewer file browser
    fun optionAddLibrary() {
        //TODO fix filebrowser parameters
        FileBrowser.openFileBrowser(activity!!, FileBrowser.LIBRARY, getString(R.string.library_menu_add), ".stl", "")
    }

    //Create a single new folder via mkdir
    fun optionCreateLibrary() {

        val inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val getModelsDialogView = inflater.inflate(R.layout.dialog_create_folder, null)
        val nameEditText = getModelsDialogView.findViewById(R.id.new_folder_name_edittext) as MaterialEditText

        val createFolderDialog = MaterialDialog(activity!!)
        createFolderDialog.title(R.string.library_create_dialog_title)
            .customView(view =getModelsDialogView, scrollable = true)
            .positiveButton(R.string.create) {
                val name = nameEditText.getText().toString().trim()
                if (name == null || name == "") {
                    nameEditText.setError(getString(R.string.library_create_folder_name_error))
                } else {
                    LibraryController.createFolder(name)
                    refreshFiles()
                    it.dismiss()
                }
            }
            .negativeButton(R.string.cancel) {
                it.dismiss()
            }
            .noAutoDismiss()
            .show()
    }

    fun optionPaste() {

        //Copy file to new folder
        val fileTo = File(LibraryController.currentPath.toString() + "/" + mMoveFile!!.name)

        //Delete file if success
        if (!mMoveFile!!.renameTo(fileTo)) {
            mMoveFile!!.delete()
        }

        LibraryController.reloadFiles(LibraryController.currentPath!!.getAbsolutePath())
        sortAdapter()

        setMoveFile(null)

        refreshFiles()
    }

    /**
     * Sort library by parameter sort type
     */
    fun optionSort() {

        val inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val librarySortView = inflater.inflate(R.layout.dialog_library_sort, null)
        val radioGroup = librarySortView.findViewById<View>(R.id.sort_library_radiogroup) as RadioGroup


        //Pre-select option
        when (mSortType) {

            SORT_NAME -> radioGroup.check(R.id.sort_name_checkbox)
            SORT_SIZE -> radioGroup.check(R.id.sort_size_checkbox)
            SORT_DATE -> radioGroup.check(R.id.sort_recent_checkbox)
        }


        MaterialDialog(activity!!).title(R.string.library_menu_sort)
            .customView(view = librarySortView, scrollable = true)
            .positiveButton(R.string.ok) {

                when (radioGroup.checkedRadioButtonId) {

                    R.id.sort_name_checkbox ->

                        mSortType = SORT_NAME

                    R.id.sort_recent_checkbox ->

                        mSortType = SORT_DATE

                    R.id.sort_size_checkbox ->

                        mSortType = SORT_SIZE

                    else -> {
                    }
                }

                sortAdapter()

            }
            .show()

    }

    /**
     * Show a dialog to select between Thingiverse or Yoymagine and open the selected url in the browser
     */
    fun optionGetModelsDialog() {

        val inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val getModelsDialogView = inflater.inflate(R.layout.dialog_get_models, null)

        val getModelsDialog = MaterialDialog(activity!!)
            .title(R.string.library_get_models_title)
            .customView(view = getModelsDialogView, scrollable = true)
            .positiveButton(R.string.close)

        getModelsDialog.show()

        val thingiverseButton = getModelsDialogView.findViewById<View>(R.id.thingiverse_button) as LinearLayout
        thingiverseButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(HttpUtils.URL_THINGIVERSE))
            startActivity(browserIntent)
            getModelsDialog.dismiss()
        }

        val youmagineButton = getModelsDialogView.findViewById<View>(R.id.youmagine_button) as LinearLayout
        youmagineButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(HttpUtils.URL_YOUMAGINE))
            startActivity(browserIntent)
            getModelsDialog.dismiss()
        }
    }

    fun showListHeader(folderName: String) {
        val goBackTextView = mListHeader!!.findViewById<View>(R.id.model_name_column_textview) as TextView
        goBackTextView.text = getString(R.string.library_list_go_back_tag) + " (" + folderName + ")"
        mListHeader!!.visibility = View.VISIBLE
    }

    fun hideListHeader() {
        mListHeader!!.visibility = View.GONE
    }

    @SuppressLint("DefaultLocale")
    fun sortAdapter() {

        if (mCurrentFilter != null) mListAdapter!!.removeFilter()


        when (mSortType) {

            SORT_NAME ->

                //Sort by absolute file (puts folders before files)
                mListAdapter!!.sort(Comparator { arg0, arg1 ->
                    if (arg0.parent == "printer") return@Comparator -1

                    //Must check all cases, Folders > Projects > Files
                    if (arg0.isDirectory) {

                        if (LibraryController.isProject(arg0)) {

                            if (arg1.isDirectory) {
                                if (LibraryController.isProject(arg1))
                                    arg0.name.toLowerCase().compareTo(arg1.name.toLowerCase())
                                else
                                    1
                            } else
                                -1

                        } else {
                            if (arg1.isDirectory) {
                                if (LibraryController.isProject(arg1))
                                    -1
                                else
                                    arg0.name.toLowerCase().compareTo(arg1.name.toLowerCase())

                            } else
                                -1
                        }
                    } else {
                        if (arg1.isDirectory)
                            1
                        else
                            arg0.name.toLowerCase().compareTo(arg1.name.toLowerCase())
                    }
                })

            SORT_DATE ->

                //Sort by modified date
                mListAdapter!!.sort { arg0, arg1 -> java.lang.Long.compare(arg1.lastModified(), arg0.lastModified()) }

            SORT_SIZE ->

                //Sort by file size
                mListAdapter!!.sort { arg0, arg1 -> java.lang.Long.compare(arg1.length(), arg0.length()) }
        }


        //Apply the current filter to the folder
        if (mCurrentFilter != null) mListAdapter!!.filter.filter(mCurrentFilter)
        notifyAdapter()
    }


    fun notifyAdapter() {
        mListAdapter!!.notifyDataSetChanged()
    }

    fun setMoveFile(file: File?) {
        mMoveFile = file
        activity?.invalidateOptionsMenu()
    }

    companion object {

        private val SORT_NAME = 0
        private val SORT_DATE = 1
        private val SORT_SIZE = 2
    }

}
