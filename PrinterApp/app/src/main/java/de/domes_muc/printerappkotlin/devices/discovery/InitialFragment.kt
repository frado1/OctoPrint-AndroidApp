package de.domes_muc.printerappkotlin.devices.discovery

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import de.domes_muc.printerappkotlin.R
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.material.widget.PaperButton

/**
 * This fragment will contain the initial screen before any printer is added
 *
 * Created by alberto-baeza on 2/4/15.
 */
class InitialFragment : Fragment() {

    private var mScanButton: PaperButton? = null


    override fun onCreate(savedInstanceState: Bundle?) {


        /**
         * Since API level 11, thread policy has changed and now does not allow network operation to
         * be executed on UI thread (NetworkOnMainThreadException), so we have to add these lines to
         * permit it.
         */
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        var rootView: View? = null

        //If is not new
        if (savedInstanceState == null) {

            //Show custom option menu
            setHasOptionsMenu(false)

            rootView = inflater.inflate(
                R.layout.printview_tab_initial_layout,
                container, false
            )

            mScanButton = rootView!!.findViewById<View>(R.id.initial_scan_button) as PaperButton
            mScanButton!!.setOnClickListener { DiscoveryController(activity!!).scanDelayDialog() }
        }

        return rootView
    }


}
