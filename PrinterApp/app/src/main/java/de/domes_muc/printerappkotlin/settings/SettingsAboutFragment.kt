package de.domes_muc.printerappkotlin.settings

import androidx.fragment.app.Fragment
import de.domes_muc.printerappkotlin.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by sara on 5/02/15.
 */
class SettingsAboutFragment : Fragment() {

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
            rootView = inflater.inflate(R.layout.settings_about_fragment, container, false)

            /** */

            val tv = rootView!!.findViewById<View>(R.id.app_version_textview) as TextView
            tv.text = setBuildVersion()

        }
        return rootView
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
