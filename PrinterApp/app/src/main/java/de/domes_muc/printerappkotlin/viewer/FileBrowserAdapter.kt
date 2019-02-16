package de.domes_muc.printerappkotlin.viewer

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

import java.io.File

/**
 * Adapter that creates the items of the file browser
 */
class FileBrowserAdapter(
    private val mContext: Context,
    private val mBrowserFileNamesList: List<String>,
    private val mBrowserFilesList: List<File>
) : BaseAdapter() {

    private var mActivatedPosition: Int = 0

    override fun getCount(): Int {
        return mBrowserFilesList.size
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
            convertView = inflater.inflate(R.layout.list_item_file_browser, parent, false)

            viewHolder = ViewHolder()
            viewHolder.fileItemName = convertView!!.findViewById<View>(R.id.wifi_ssid_textview) as TextView
            viewHolder.fileItemIcon = convertView.findViewById<View>(R.id.wifi_signal_icon) as ImageView

            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        viewHolder.fileItemName!!.text = mBrowserFileNamesList[position]

        if (mBrowserFilesList[position].isDirectory)
            viewHolder.fileItemIcon!!.setImageDrawable(mContext.resources.getDrawable(R.drawable.ic_folder_grey600_36dp))
        else
            viewHolder.fileItemIcon!!.setImageDrawable(mContext.resources.getDrawable(R.drawable.ic_file_gray))

        return convertView
    }

    fun setActivatedPosition(activatedPosition: Int) {
        Log.d(TAG, "New activated position [$activatedPosition]")
        this.mActivatedPosition = activatedPosition
    }

    private class ViewHolder {
        internal var fileItemName: TextView? = null
        internal var fileItemIcon: ImageView? = null
    }

    companion object {

        private val TAG = "FileBrowserAdapter"
    }
}
