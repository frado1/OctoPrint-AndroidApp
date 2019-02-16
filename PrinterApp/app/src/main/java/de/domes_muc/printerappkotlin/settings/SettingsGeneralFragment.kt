package de.domes_muc.printerappkotlin.settings

import androidx.fragment.app.Fragment
import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by sara on 5/02/15.
 */
class SettingsGeneralFragment : Fragment() {

    private val mAdapter: SettingsListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Retain instance to keep the Fragment from destroying itself
        retainInstance = true
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        //Reference to View
        var rootView: View? = null

        //If is not new
        if (savedInstanceState == null) {

            //Inflate the fragment
            rootView = inflater.inflate(R.layout.settings_general_fragment, container, false)

            val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)

            val checkBox_slice = rootView!!.findViewById<View>(R.id.settings_slicing_checkbox) as CheckBox
            checkBox_slice.isChecked = sharedPref.getBoolean(getString(R.string.shared_preferences_slice), false)
            checkBox_slice.setOnCheckedChangeListener { compoundButton, b ->
                sharedPref.edit().putBoolean(getString(R.string.shared_preferences_slice), b).apply()
            }
            val checkBox_print = rootView.findViewById<View>(R.id.settings_printing_checkbox) as CheckBox
            checkBox_print.isChecked = sharedPref.getBoolean(getString(R.string.shared_preferences_print), false)
            checkBox_print.setOnCheckedChangeListener { compoundButton, b ->
                sharedPref.edit().putBoolean(getString(R.string.shared_preferences_print), b).apply()
            }
            val checkBox_save = rootView.findViewById<View>(R.id.settings_save_files_checkbox) as CheckBox
            checkBox_save.isChecked = sharedPref.getBoolean(getString(R.string.shared_preferences_save), false)
            checkBox_save.setOnCheckedChangeListener { compoundButton, b ->
                sharedPref.edit().putBoolean(getString(R.string.shared_preferences_save), b).apply()
            }

            val checkBox_autoslice = rootView.findViewById<View>(R.id.settings_automatic_checkbox) as CheckBox
            checkBox_autoslice.isChecked =
                    sharedPref.getBoolean(getString(R.string.shared_preferences_autoslice), false)
            checkBox_autoslice.setOnCheckedChangeListener { compoundButton, b ->
                sharedPref.edit().putBoolean(getString(R.string.shared_preferences_autoslice), b).apply()
            }


            /** */

            getNetworkSsid(rootView)

        }
        return rootView
    }


    fun notifyAdapter() {
        mAdapter!!.notifyDataSetChanged()
    }

    //Return network without quotes
    fun getNetworkSsid(v: View) {

        val wifiManager = activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        val tv = v.findViewById<View>(R.id.network_name_textview) as TextView
        tv.text = wifiInfo.ssid.replace("\"", "")

        val iv = v.findViewById<View>(R.id.wifi_signal_imageview) as ImageView

        val signal = wifiInfo.rssi

        if (signal <= 0 && signal > -40) {
            iv.setImageResource(R.drawable.ic_signal_wifi_4)
        } else if (signal <= -40 && signal > -60) {
            iv.setImageResource(R.drawable.ic_signal_wifi_3)
        } else if (signal <= -60 && signal > -70) {
            iv.setImageResource(R.drawable.ic_signal_wifi_2)
        } else if (signal <= -70 && signal > -80) {
            iv.setImageResource(R.drawable.ic_signal_wifi_1)
        } else
            iv.setImageResource(R.drawable.ic_signal_wifi_0)

    }

    fun setBuildVersion(): String {

        var s = "Version v."

        try {

            //Get version name from package
            val pInfo = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0)
            val fString = pInfo.versionName

            //Parse version and date
            val hash = fString.substring(0, fString.indexOf(" "))
            val date = fString.substring(fString.indexOf(" "), fString.length)

            //Format hash
            val fHash = hash.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            //Format date
            val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale("es", "ES"))
            val fDate = sdf.format(java.util.Date(date))

            //Get version code / Jenkins build
            val code: String
            if (pInfo.versionCode == 0)
                code = "IDE"
            else
                code = "#" + pInfo.versionCode

            //Build string
            s = s + fHash[0] + " " + fHash[1] + " " + fDate + " " + code

        } catch (e: Exception) {

            e.printStackTrace()
        }

        return s
    }
}
