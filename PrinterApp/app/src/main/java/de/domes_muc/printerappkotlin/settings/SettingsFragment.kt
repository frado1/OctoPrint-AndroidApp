package de.domes_muc.printerappkotlin.settings

import android.app.AlertDialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

import de.domes_muc.printerappkotlin.ListContent
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.Context
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Class to manage the application and printer settings
 */
class SettingsFragment : Fragment() {

    private var mOnNavTextViewClick: View.OnClickListener? = null

    private var mRootView: View? = null

    /**
     * Listener for the navigation text views
     *
     * @return
     */
    private//New transaction
    val onNavTextViewClickListener: View.OnClickListener
        get() {
            if (mOnNavTextViewClick != null) return mOnNavTextViewClick!!

            mOnNavTextViewClick = View.OnClickListener { v ->
                selectNavItem(v.id)
                val fragmentTransaction = mManager!!.beginTransaction()
                fragmentTransaction.setCustomAnimations(
                    R.animator.fragment_slide_in_top,
                    R.animator.fragment_slide_out_down
                )

                when (v.id) {
                    R.id.settings_nav_general_textview -> {
                        val generalSettings = SettingsGeneralFragment()
                        fragmentTransaction.replace(R.id.settings_fragment_container, generalSettings).commit()
                    }
                    R.id.settings_nav_devices_textview -> {
                        val devicesSettings = SettingsDevicesFragment()
                        fragmentTransaction.replace(
                            R.id.settings_fragment_container,
                            devicesSettings,
                            ListContent.ID_DEVICES_SETTINGS
                        ).commit()
                    }
                    R.id.settings_nav_about_textview -> {
                        val aboutSettings = SettingsAboutFragment()
                        fragmentTransaction.replace(
                            R.id.settings_fragment_container,
                            aboutSettings,
                            ListContent.ID_DEVICES_SETTINGS
                        ).commit()
                    }
                    else -> {
                    }
                }
            }

            return mOnNavTextViewClick!!
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Retain instance to keep the Fragment from destroying itself
        retainInstance = true
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        //Reference to View
        mRootView = null

        mManager = activity!!.supportFragmentManager

        //If is not new
        if (savedInstanceState == null) {

            //Show custom option menu
            setHasOptionsMenu(true)

            //Update the actionbar to show the up carat/affordance
            (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

            //Inflate the fragment
            mRootView = inflater.inflate(R.layout.settings_layout, container, false)

            /** */

            //Set left navigation menu behavior
            mRootView!!.findViewById<View>(R.id.settings_nav_general_textview)
                .setOnClickListener(onNavTextViewClickListener)
            (mRootView!!.findViewById<View>(R.id.settings_nav_general_icon) as ImageView).setColorFilter(
                R.color.dark_gray,
                PorterDuff.Mode.MULTIPLY
            )
            mRootView!!.findViewById<View>(R.id.settings_nav_devices_textview)
                .setOnClickListener(onNavTextViewClickListener)
            (mRootView!!.findViewById<View>(R.id.settings_nav_devices_icon) as ImageView).setColorFilter(
                R.color.dark_gray,
                PorterDuff.Mode.MULTIPLY
            )
            mRootView!!.findViewById<View>(R.id.settings_nav_about_textview)
                .setOnClickListener(onNavTextViewClickListener)
            (mRootView!!.findViewById<View>(R.id.settings_nav_about_icon) as ImageView).setColorFilter(
                R.color.dark_gray,
                PorterDuff.Mode.MULTIPLY
            )

            //Set the general settings fragment as the main view
            val fragmentTransaction = mManager!!.beginTransaction()
            fragmentTransaction.setCustomAnimations(
                R.animator.fragment_slide_in_top,
                R.animator.fragment_slide_out_down
            )
            val generalSettings = SettingsGeneralFragment()
            fragmentTransaction.replace(R.id.settings_fragment_container, generalSettings).commit()


        }
        return mRootView
    }

    /**
     * Set the state of the selected nav item
     *
     * @param selectedId Id of the nav item that has been pressed
     */
    fun selectNavItem(selectedId: Int) {

        if (mRootView != null) {
            //Get the left nav menu
            val navMenu = mRootView!!.findViewById<View>(R.id.settings_nav_menu) as LinearLayout

            //Set the behavior of the nav items
            for (i in 0 until navMenu.childCount) {
                val v = navMenu.getChildAt(i)
                if (v is LinearLayout) {
                    for (j in 0 until navMenu.childCount) {
                        val l = v.getChildAt(j)
                        if (l is TextView) {
                            if (l.id == selectedId)
                                l.setTextAppearance(activity, R.style.SelectedNavigationMenuItem)
                            else
                                l.setTextAppearance(activity, R.style.NavigationMenuItem)
                        }
                    }
                }
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.settings_menu, menu)
    }

    //Option menu
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {

        when (item.itemId) {

            android.R.id.home -> {
                activity!!.onBackPressed()
                return true
            }

            R.id.settings_menu_add //Add a new printer
            ->
                //optionAddPrinter();
                //new DiscoveryController(getActivity());
                return true

            else -> return super.onOptionsItemSelected(item)
        }
    }


    fun notifyAdapter() {
        val fragment = mManager!!.findFragmentByTag(ListContent.ID_DEVICES_SETTINGS)
        if (fragment != null) (fragment as SettingsDevicesFragment).notifyAdapter()
    }

    companion object {

        private var mManager: FragmentManager? = null //Fragment manager to handle transitions @static
    }

}
