package de.domes_muc.printerappkotlin

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import de.domes_muc.printerappkotlin.devices.DevicesFragment
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.discovery.InitialFragment
import de.domes_muc.printerappkotlin.devices.printview.GcodeCache
import de.domes_muc.printerappkotlin.devices.printview.PrintViewFragment
import de.domes_muc.printerappkotlin.history.HistoryDrawerAdapter
import de.domes_muc.printerappkotlin.history.SwipeDismissListViewTouchListener
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.library.LibraryFragment
import de.domes_muc.printerappkotlin.library.detail.DetailViewFragment
import de.domes_muc.printerappkotlin.settings.SettingsFragment
import de.domes_muc.printerappkotlin.util.ui.AnimationHelper
import de.domes_muc.printerappkotlin.viewer.ViewerMainFragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TabHost
import android.widget.TextView
import android.widget.Toast

/**
 * Created by alberto-baeza on 1/21/15.
 */
class MainActivity : AppCompatActivity() {

    //List of Fragments
    private var mDevicesFragment: DevicesFragment? = null //Devices fragment @static for refresh
    private var mLibraryFragment: LibraryFragment? = null //Storage fragment
    private var mViewerFragment: ViewerMainFragment? = null //Print panel fragment @static for model load
    private var mDrawerList: ListView? = null
    private var mDrawerAdapter: HistoryDrawerAdapter? = null

    //notify ALL adapters every time a notification is received
    private val mAdapterNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            // Extract data included in the Intent
            val message = intent.getStringExtra("message")

            if (message != null)
                if (message == "Devices") {

                    if (mDevicesFragment != null) mDevicesFragment!!.notifyAdapter()

                    //Refresh printview fragment if exists
                    val fragment = mManager.findFragmentByTag(ListContent.ID_PRINTVIEW)
                    if (fragment != null) (fragment as PrintViewFragment).refreshData()

                } else if (message == "Profile") {

                    if (mViewerFragment != null) {
                        mViewerFragment!!.notifyAdapter()
                    }

                } else if (message == "Files") {

                    if (mLibraryFragment != null) mLibraryFragment!!.refreshFiles()

                }

            /*else if (message.equals("Notification")){

                long id = intent.getLongExtra("printer", 0);

               notificationManager(id);

            }*/

        }
    }

    /*
Close app on locale change
 */
    private val mLocaleChange = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i("OUT", "Exiting app")
            finish()
            System.exit(0)

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        /**
         * Since API level 11, thread policy has changed and now does not allow network operation to
         * be executed on UI thread (NetworkOnMainThreadException), so we have to add these lines to
         * permit it.
         */
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        mTabHost = findViewById<View>(R.id.tabHost) as TabHost


        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)

        //Initialize variables
        mManager = supportFragmentManager
        mDialog = DialogController(this)

        //Initialize fragments
        mDevicesFragment = supportFragmentManager.findFragmentByTag(ListContent.ID_DEVICES) as DevicesFragment?
        mLibraryFragment = supportFragmentManager.findFragmentByTag(ListContent.ID_LIBRARY) as LibraryFragment?
        mViewerFragment = supportFragmentManager.findFragmentByTag(ListContent.ID_VIEWER) as ViewerMainFragment?


        initDrawer()

        //ItemListFragment.performClick(0);

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mAdapterNotification,
            IntentFilter("notify")
        )

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_LOCALE_CHANGED)

        this.registerReceiver(mLocaleChange, filter)

        mManager.addOnBackStackChangedListener { }

        //Init gcode cache
        GcodeCache()

        //Set tab host for the view
        setTabHost()

    }

    //Initialize history drawer
    private fun initDrawer() {

        mDrawerLayout = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        mDrawerLayout!!.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)

        mDrawerToggle = object : ActionBarDrawerToggle(
            this, /* host Activity */
            mDrawerLayout, /* DrawerLayout object */
            R.string.add, /* "open drawer" description */
            R.string.cancel         /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                invalidateOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {


                if (slideOffset.toDouble() == 1.0) {
                    mDrawerAdapter!!.notifyDataSetChanged()
                }
                super.onDrawerSlide(drawerView, slideOffset)
            }
        }

        // Set the drawer toggle as the DrawerListener
        mDrawerToggle!!.isDrawerIndicatorEnabled = true
        mDrawerLayout!!.setDrawerListener(mDrawerToggle)



        mDrawerList = findViewById<View>(R.id.left_drawer) as ListView
        mDrawerList!!.selector = resources.getDrawable(R.drawable.selectable_rect_background_green)

        val drawerListEmptyView = findViewById<View>(R.id.history_empty_view)
        mDrawerList!!.emptyView = drawerListEmptyView

        val inflater = layoutInflater
        mDrawerList!!.addHeaderView(inflater.inflate(R.layout.history_drawer_header, null))
        mDrawerAdapter = HistoryDrawerAdapter(this, LibraryController.historyList)

        mDrawerList!!.adapter = mDrawerAdapter
        mDrawerList!!.onItemClickListener = AdapterView.OnItemClickListener { _, view, i, _ ->
            mDrawerLayout!!.closeDrawers()
            requestOpenFile(LibraryController.historyList.get(i - 1).path)
        }

        mDrawerList!!.setOnTouchListener(
            SwipeDismissListViewTouchListener(
                mDrawerList!!,
                object : SwipeDismissListViewTouchListener.DismissCallbacks {

                    override fun canDismiss(position: Int): Boolean {
                        return true
                    }

                    override fun onDismiss(listView: ListView, reverseSortedPositions: IntArray) {
                        for (position in reverseSortedPositions) {

                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.delete) + " " + LibraryController.historyList.get(position - 1).model,
                                Toast.LENGTH_SHORT
                            ).show()
                            DatabaseController.removeFromHistory(LibraryController.historyList.get(position - 1).path)
                            mDrawerAdapter!!.removeItem(position - 1)


                        }
                    }
                })
        )

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle!!.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {

        super.onConfigurationChanged(newConfig)
        mDrawerToggle!!.onConfigurationChanged(newConfig)
    }


    fun setTabHost() {

        mTabHost!!.setup()

        //Models tab
        var spec: TabHost.TabSpec = mTabHost!!.newTabSpec("Library")
        spec.setIndicator(getTabIndicator(resources.getString(R.string.fragment_models)))
        spec.setContent(R.id.maintab1)
        mTabHost!!.addTab(spec)

        //Print panel tab
        spec = mTabHost!!.newTabSpec("Panel")
        spec.setIndicator(getTabIndicator(resources.getString(R.string.fragment_print)))
        spec.setContent(R.id.maintab2)
        mTabHost!!.addTab(spec)

        //Print view tab
        spec = mTabHost!!.newTabSpec("Printer")
        spec.setIndicator(getTabIndicator(resources.getString(R.string.fragment_devices)))
        spec.setContent(R.id.maintab3)
        mTabHost!!.addTab(spec)

        if (DatabaseController.count() > 0) {
            mTabHost!!.currentTab = 0
            onItemSelected(0)
        } else {
            mTabHost!!.currentTab = 2
            onItemSelected(2)

        }


        mTabHost!!.tabWidget.dividerDrawable = ColorDrawable(resources.getColor(R.color.transparent))

        mTabHost!!.setOnTabChangedListener {
            val currentView = mTabHost!!.currentView
            AnimationHelper.inFromRightAnimation(currentView)

            onItemSelected(mTabHost!!.currentTab)

            //getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

    }

    //handle action bar menu open
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return if (mDrawerToggle!!.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
        // Handle your other action bar items...
    }


    /**
     * Return the custom view of the tab
     *
     * @param title Title of the tab
     * @return Custom view of a tab layout
     */
    private fun getTabIndicator(title: String): View {
        val view = LayoutInflater.from(applicationContext).inflate(R.layout.main_activity_tab_layout, null)
        val tv = view.findViewById<View>(R.id.tab_title_textview) as TextView
        tv.text = title
        return view
    }

    fun onItemSelected(id: Int) {

        if (id != 1) {

            ViewerMainFragment.hideActionModePopUpWindow()
            ViewerMainFragment.hideCurrentActionPopUpWindow()
        }

        Log.i("OUT", "Pressed $id")
        //start transaction
        val fragmentTransaction = mManager.beginTransaction()


        //Pop backstack to avoid having bad references when coming from a Detail view
        mManager.popBackStack()

        //If there is a fragment being shown, hide it to show the new one
        mCurrent?.let {
            try {
                fragmentTransaction.hide(mCurrent!!)
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }

        //Select fragment
        when (id) {

            0 -> {
                closePrintView()
                //Check if we already created the Fragment to avoid having multiple instances
                if (supportFragmentManager.findFragmentByTag(ListContent.ID_LIBRARY) == null) {
                    mLibraryFragment = LibraryFragment()
                    mLibraryFragment?.let {
                        fragmentTransaction.add(R.id.maintab1, it, ListContent.ID_LIBRARY)
                    }
                }
                mCurrent = mLibraryFragment
            }
            1 -> {
                closePrintView()
                closeDetailView()
                //Check if we already created the Fragment to avoid having multiple instances
                if (supportFragmentManager.findFragmentByTag(ListContent.ID_VIEWER) == null) {
                    mViewerFragment = ViewerMainFragment()
                    mViewerFragment?.let {
                        fragmentTransaction.add(R.id.maintab2, it, ListContent.ID_VIEWER)
                    }
                }
                mCurrent = mViewerFragment
            }
            2 -> {
                closeDetailView()
                //Check if we already created the Fragment to avoid having multiple instances
                if (supportFragmentManager.findFragmentByTag(ListContent.ID_DEVICES) == null) {
                    mDevicesFragment = DevicesFragment()?.let {
                        fragmentTransaction.add(R.id.maintab3, it, ListContent.ID_DEVICES)
                        it
                    }
                }
                mCurrent = mDevicesFragment

                refreshDevicesCount()
            }
        }

        if (mViewerFragment != null) {
            if (mCurrent !== mViewerFragment) {
                //Make the surface invisible to avoid frame overlapping
                mViewerFragment!!.setSurfaceVisibility(0)
            } else {
                //Make the surface visible when we press
                mViewerFragment!!.setSurfaceVisibility(1)
            }
        }

        //Show current fragment
        mCurrent?.let {
            Log.i("OUT", "Changing " + it.tag)
            fragmentTransaction.show(it).commit()
            mDrawerToggle!!.isDrawerIndicatorEnabled = true
        }


    }

    private fun closeSettings() {
        //Refresh printview fragment if exists
        val fragment = mManager.findFragmentByTag(ListContent.ID_SETTINGS)
        if (fragment != null) refreshDevicesCount()
    }

    /**
     * Override to allow back navigation on the Storage fragment.
     */
    override fun onBackPressed() {

        //Update the actionbar to show the up carat/affordance
        //getSupportActionBar().setDisplayHomeAsUpEnabled(false);


        if (mCurrent != null) {
            val fragment = mManager.findFragmentByTag(ListContent.ID_SETTINGS)
            val c = DatabaseController.retrieveDeviceList()

            if (fragment != null || c.count > 1) {

                closePrintView()

                if (mManager.popBackStackImmediate()) {

                    mDrawerToggle!!.isDrawerIndicatorEnabled = true
                    mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

                    //Basically refresh printer count if all were deleted in Settings mode
                    if (mCurrent === mDevicesFragment)
                        refreshDevicesCount()

                } else
                    super.onBackPressed()
            } else
                super.onBackPressed()


        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {

        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAdapterNotification)

        super.onDestroy()
    }

    override fun onResume() {

        NotificationReceiver.setForeground(true)

        super.onResume()

    }

    override fun onPause() {


        NotificationReceiver.setForeground(false)

        super.onPause()

    }

    companion object {

        //Class specific variables
        private lateinit var mManager: FragmentManager //Fragment manager to handle transitions @static

        private var mCurrent: Fragment? = null //The current shown fragment @static
        private var mDialog: DialogController? = null //Dialog controller @static

        private var mTabHost: TabHost? = null

        //Drawer
        private var mDrawerLayout: DrawerLayout? = null
        private var mDrawerToggle: ActionBarDrawerToggle? = null

        fun performClick(i: Int) {

            mTabHost!!.currentTab = i

        }

        fun refreshDevicesCount() {

            mCurrent!!.setMenuVisibility(false)

            val c = DatabaseController.retrieveDeviceList()
            if (c.count == 0) {

                mManager.popBackStack()
                showExtraFragment(2, 0)

            } else {

                if (c.count == 1) {

                    c.moveToFirst()

                    Log.i("Extra", "Opening " + c.getInt(0))

                    showExtraFragment(1, c.getInt(0).toLong())

                    mDrawerToggle!!.isDrawerIndicatorEnabled = true
                    mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)


                } else {

                    mCurrent!!.setMenuVisibility(true)
                    mManager.popBackStack()
                    closePrintView()
                    closeInitialFragment()


                }
            }

            DatabaseController.closeDb()

        }

        /**
         * Method to create a new type of fragment to show special detailed views.
         *
         * @param type  Type of detailed view 0: DetailView 1: PrintView
         * @param id Extra argument to the fragment DetailView: File index, PrintView: Printer id
         */
        fun showExtraFragment(type: Int, id: Long) {

            //New transaction
            val mTransaction = mManager.beginTransaction()
            mTransaction.setCustomAnimations(0, 0, 0, R.animator.fragment_slide_out_left)

            //Add current fragment to the backstack and hide it (will show again later)
            mTransaction.addToBackStack(mCurrent!!.tag)
            mTransaction.hide(mCurrent!!)

            when (type) {

                0 -> {

                    closePrintView()
                    mManager.popBackStack()
                    val settings = SettingsFragment()
                    mTransaction.replace(R.id.container_layout, settings, ListContent.ID_SETTINGS).commit()

                    mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    mDrawerToggle!!.isDrawerIndicatorEnabled = false
                }

                1 -> {

                    mCurrent!!.setMenuVisibility(false)
                    //New detailview with the printer name as extra
                    val detailp = PrintViewFragment()
                    val argsp = Bundle()
                    argsp.putLong("id", id)
                    detailp.arguments = argsp
                    mTransaction.replace(R.id.maintab3, detailp as Fragment, ListContent.ID_PRINTVIEW).commit()

                    mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    mDrawerToggle!!.isDrawerIndicatorEnabled = false
                }

                2 -> {

                    val initial = InitialFragment()
                    mTransaction.replace(R.id.maintab3, initial as Fragment, ListContent.ID_INITIAL).commit()
                }
            }


        }

        private fun closeInitialFragment() {

            val fragment = mManager.findFragmentByTag(ListContent.ID_INITIAL)
            if (fragment != null) mManager.popBackStack()

        }

        private fun closePrintView() {
            //Refresh printview fragment if exists
            mManager.findFragmentByTag(ListContent.ID_PRINTVIEW)?.let {
                (it as PrintViewFragment).stopCameraPlayback()
            }

            mCurrent?.let {
                it.setMenuVisibility(true)
            }
        }

        fun closeDetailView() {
            //Refresh printview fragment if exists
            mManager?.findFragmentByTag(ListContent.ID_DETAIL)?.let {
                (it as DetailViewFragment).removeRightPanel()
            }
        }

        //Show dialog
        fun showDialog(msg: String) {
            mDialog?.displayDialog(msg)
        }

        /**
         * Send a file to the Viewer to display
         *
         * @param path File path
         */
        fun requestOpenFile(path: String?) {

            //This method will simulate a click and all its effects
            performClick(1)

            //Handler will avoid crash
            val handler = Handler()
            handler.post { if (path != null) ViewerMainFragment.openFileDialog(path) }

        }

        fun getCurrentNetwork(context: Context): String {

            val mWifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return mWifiManager.connectionInfo.ssid

        }
    }
}
