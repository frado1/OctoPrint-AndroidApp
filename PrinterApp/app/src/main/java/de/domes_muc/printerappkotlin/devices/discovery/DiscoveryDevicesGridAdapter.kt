package de.domes_muc.printerappkotlin.devices.discovery

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

/**
 * Adapter that creates the items of the discovery grid view
 */
class DiscoveryDevicesGridAdapter(private val mContext: Context, private val mDevicesGridItems: List<ModelPrinter>) :
    BaseAdapter() {

    override fun getCount(): Int {
        return mDevicesGridItems.size
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
            convertView = inflater.inflate(R.layout.grid_item_discover_printer, parent, false)

            viewHolder = ViewHolder()
            viewHolder.printerName = convertView!!.findViewById<View>(R.id.discover_printer_name) as TextView
            viewHolder.printerIcon = convertView.findViewById<View>(R.id.discover_printer_icon) as ImageView

            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        val modelPrinter = mDevicesGridItems[position]

        if (modelPrinter.status == StateUtils.STATE_NEW) {
            viewHolder.printerName!!.text = modelPrinter.address.replace("/".toRegex(), "")
            viewHolder.printerIcon!!.setImageResource(R.drawable.printer_signal_add)
        } else {
            viewHolder.printerName!!.text = modelPrinter.name
            viewHolder.printerIcon!!.setImageResource(R.drawable.signal_octopidev)
        }

        return convertView
    }

    private class ViewHolder {
        internal var printerName: TextView? = null
        internal var printerIcon: ImageView? = null
    }

    companion object {

        private val TAG = "DrawerListAdapter"
    }
}
