package de.domes_muc.printerappkotlin.devices.discovery

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

/**
 * Adapter that creates the items of the wifi networks list view
 */
class DiscoveryWifiNetworksListAdapter(
    private val mContext: Context,
    private val mWifiNetworksList: List<String>,
    private val mWifiSignalList: List<String>
) : BaseAdapter() {

    override fun getCount(): Int {
        return mWifiNetworksList.size
    }

    override fun getItem(i: Int): Any? {
        return null
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }


    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        Log.d(TAG, "getView")

        val viewHolder: ViewHolder

        if (convertView == null) {
            val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.list_item_wifi_network, parent, false)

            viewHolder = ViewHolder()
            viewHolder.wifiNetworkSsid = convertView!!.findViewById<View>(R.id.wifi_ssid_textview) as TextView
            viewHolder.wifiNetworkSignalIcon = convertView.findViewById<View>(R.id.wifi_signal_icon) as ImageView

            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        viewHolder.wifiNetworkSsid!!.text = mWifiNetworksList[position]

        val signal = Integer.parseInt(mWifiSignalList[position])

        if (signal <= 0 && signal > -40) {
            viewHolder.wifiNetworkSignalIcon!!.setImageResource(R.drawable.ic_signal_wifi_4)
        } else if (signal <= -40 && signal > -60) {
            viewHolder.wifiNetworkSignalIcon!!.setImageResource(R.drawable.ic_signal_wifi_3)
        } else if (signal <= -60 && signal > -70) {
            viewHolder.wifiNetworkSignalIcon!!.setImageResource(R.drawable.ic_signal_wifi_2)
        } else if (signal <= -70 && signal > -80) {
            viewHolder.wifiNetworkSignalIcon!!.setImageResource(R.drawable.ic_signal_wifi_1)
        } else
            viewHolder.wifiNetworkSignalIcon!!.setImageResource(R.drawable.ic_signal_wifi_0)

        return convertView
    }

    private class ViewHolder {
        internal var wifiNetworkSsid: TextView? = null
        internal var wifiNetworkSignalIcon: ImageView? = null
    }

    companion object {

        private val TAG = "DiscoveryWifiNetworksListAdapter"
    }
}
