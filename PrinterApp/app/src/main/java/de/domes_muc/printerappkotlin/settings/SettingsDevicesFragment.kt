package de.domes_muc.printerappkotlin.settings

import androidx.fragment.app.Fragment
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by sara on 5/02/15.
 */
class SettingsDevicesFragment : Fragment() {

    private var mAdapter: SettingsListAdapter? = null

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

            //Inflate the fragment
            rootView = inflater.inflate(R.layout.settings_devices_fragment, container, false)

            /** */

            mAdapter = SettingsListAdapter(activity!!, R.layout.list_item_settings_device, DevicesListController.list)
            val l = rootView!!.findViewById<View>(R.id.lv_settings) as ListView
            l.adapter = mAdapter

            notifyAdapter()
        }
        return rootView
    }


    fun notifyAdapter() {
        mAdapter!!.notifyDataSetChanged()
    }

}
