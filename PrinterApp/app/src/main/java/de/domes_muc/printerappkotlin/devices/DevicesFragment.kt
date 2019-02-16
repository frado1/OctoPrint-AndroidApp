package de.domes_muc.printerappkotlin.devices

import android.app.Activity
import androidx.fragment.app.Fragment
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.discovery.DiscoveryController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.OctoprintConnection
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import de.domes_muc.printerappkotlin.util.ui.AnimationHelper
import android.content.ClipData
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.View.DragShadowBuilder
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

/**
 * This is the fragment that will contain the Device Grid and functionality
 *
 * @author alberto-baeza
 */
//Empty constructor
class DevicesFragment : Fragment() {


    //Controllers and adapters
    private var mGridAdapter: DevicesGridAdapter? = null
    private var mHideOption: ImageView? = null


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
        var rootView: View? = null

        //If is not new
        if (savedInstanceState == null) {

            //Show custom option menu
            setHasOptionsMenu(true)

            //Inflate the fragment
            rootView = inflater.inflate(R.layout.devices_layout, container, false)


            //------------------------------- View references -----------------//

            //Grid


            mGridAdapter = DevicesGridAdapter(
                activity!!,
                R.layout.grid_item_printer, DevicesListController.list
            )
            val gridView = rootView!!.findViewById<View>(R.id.devices_grid) as GridView
            gridView.selector = ColorDrawable(resources.getColor(R.color.transparent))
            gridView.onItemClickListener = gridClickListener()
            gridView.onItemLongClickListener = gridLongClickListener()

            gridView.adapter = mGridAdapter

            /** */

            mHideOption = rootView.findViewById<View>(R.id.hide_icon) as ImageView
            hideOptionHandler()

            //Custom service listener
            //mServiceListener = new JmdnsServiceListener(this);
            //mNetworkManager = new PrintNetworkManager(this);

        }
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.devices_menu, menu)
    }

    //Option menu
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {

        when (item.itemId) {

            R.id.devices_add -> {
                DiscoveryController(activity!!).scanDelayDialog()
                return true
            }

            R.id.settings -> {
                MainActivity.showExtraFragment(0, 0)
                return true
            }

            R.id.devices_menu_reload //Reload service discovery
            ->

                //optionReload();
                return true
            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
    }


    /****************************************** UI HANDLING  */


    //TODO get rid of this
    //Notify all adapters
    fun notifyAdapter() {

        try {
            mGridAdapter!!.notifyDataSetChanged()

            //TODO removed for list video bugs
            //mCameraAdapter.notifyDataSetChanged();
        } catch (e: NullPointerException) {
            //Random adapter crash
            e.printStackTrace()
        }

    }


    /**
     * ***************************** click listeners ********************************
     */


    //onclick listener will open the action mode
    fun gridClickListener(): OnItemClickListener {

        return OnItemClickListener { arg0, arg1, arg2, arg3 ->
            var m: ModelPrinter? = null

            //search printer by position
            for (mp in DevicesListController.list) {
                if (mp.position == arg2) {
                    m = mp
                }
            }

            if (m != null) {

                if (m.status == StateUtils.STATE_NEW) {
                    //codeDialog(m);
                } else if (m.status == StateUtils.STATE_ADHOC) {
                    //mNetworkManager.setupNetwork(m, arg2);
                } else {
                    //show custom dialog
                    if (m.status == StateUtils.STATE_ERROR) {
                        val toast = Toast(activity)
                        val inflater = activity!!.layoutInflater
                        val toastView = inflater.inflate(R.layout.toast_layout, null)
                        val tv = toastView.findViewById<View>(R.id.toast_text) as TextView
                        tv.text = m.message
                        toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.TOP, 0, 50)
                        toast.duration = Toast.LENGTH_SHORT
                        toast.view = toastView
                        toast.show()
                    }

                    //Check if the Job has finished, and create a dialog to remove the file / send a new one
                    if (m.status > 0 && m.status <= 7) {

                        //if job finished, create dialog
                        if (m.job.finished) {
                            FinishDialog(activity!!, m)

                            //if not finished, normal behavior
                        } else {
                            MainActivity.showExtraFragment(1, m.id)
                        }
                    } else {

                        OctoprintConnection.getNewConnection(activity!!, m)


                    }
                }
            }
        }

    }

    //onlongclick will start the draggable printer handler
    fun gridLongClickListener(): OnItemLongClickListener {
        return OnItemLongClickListener { arg0, arg1, arg2, arg3 ->
            var m: ModelPrinter? = null

            //createFloatingIcon();

            for (mp in DevicesListController.list) {
                if (mp.position == arg2) m = mp
            }

            if (m != null) {

                var data: ClipData? = null

                if (m.status == StateUtils.STATE_ADHOC || m.status == StateUtils.STATE_NEW) {

                    //Calculate a negative number to differentiate between position search and id search
                    //Must be always < 0 since it's a valid position
                    data = ClipData.newPlainText("printer", "" + (-1 * m.position - 1))

                } else {
                    data = ClipData.newPlainText("printer", "" + m.id)

                }


                val shadowBuilder = View.DragShadowBuilder(arg1)
                arg1.startDrag(data, shadowBuilder, arg1, 0)


            }


            false
        }
    }

    /********************************************************************
     * HIDE PRINTER OPTION
     */

    fun createFloatingIcon() {
        mHideOption!!.visibility = View.VISIBLE
        AnimationHelper.slideToLeft(mHideOption!!)
    }

    //Method to create and handle the hide option icon
    fun hideOptionHandler() {

        mHideOption!!.visibility = View.GONE
        mHideOption!!.setOnDragListener { view, event ->
            //Get the drop event
            val action = event.action
            when (action) {

                DragEvent.ACTION_DRAG_ENTERED ->

                    //Highlight on hover
                    view.setBackgroundColor(activity!!.resources.getColor(android.R.color.holo_orange_light))

                //If it's a drop
                DragEvent.ACTION_DROP -> {

                    val tag = event.clipDescription.label


                    //If it's a file (avoid draggable printers)
                    if (tag == "printer") {

                        val item = event.clipData.getItemAt(0)

                        val id = Integer.parseInt(item.text.toString())
                        //Find a printer from it's name
                        var p: ModelPrinter? = null

                        if (id >= 0) {

                            p = DevicesListController.getPrinter(id.toLong())

                        } else {

                            p = DevicesListController.getPrinterByPosition(-(id + 1))

                        }
                        if (p != null) {


                            if (p.status == StateUtils.STATE_ADHOC || p.status == StateUtils.STATE_NEW) {

                                //DatabaseController.handlePreference(DatabaseController.TAG_BLACKLIST,p.getName() + " " + p.getAddress(),null,true);
                                DevicesListController.removeElement(p.position)


                            } else {

                                DatabaseController.deleteFromDb(p.id)
                                MainActivity.refreshDevicesCount()
                                DevicesListController.list.remove(p)
                                p.position = -1

                            }
                            notifyAdapter()

                            //SEND NOTIFICATION
                            val intent = Intent("notify")
                            intent.putExtra("message", "Settings")
                            LocalBroadcastManager.getInstance(activity!!).sendBroadcast(intent)
                        }

                    }

                    //Highlight on hover
                    view.setBackgroundColor(activity!!.resources.getColor(android.R.color.transparent))
                }

                DragEvent.ACTION_DRAG_EXITED ->

                    //Highlight on hover
                    view.setBackgroundColor(activity!!.resources.getColor(android.R.color.transparent))

                DragEvent.ACTION_DRAG_ENDED -> mHideOption!!.visibility = View.GONE
            }

            true
        }

    }

    override fun onDestroyView() {

        //TODO random crash
        //mNetworkManager.destroy();
        super.onDestroyView()
    }

    /**
     * *****************************************
     * FINISH DIALOG
     * ******************************************
     */


}
