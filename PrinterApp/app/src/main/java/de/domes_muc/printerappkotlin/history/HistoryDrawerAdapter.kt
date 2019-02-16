package de.domes_muc.printerappkotlin.history

import de.domes_muc.printerappkotlin.ListContent
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.model.ModelFile
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

import java.io.File

/**
 * Created by alberto-baeza on 2/18/15.
 */
class HistoryDrawerAdapter(
    private val mContext: Context,
    private val mDrawerListItems: MutableList<ListContent.DrawerListItem>
) : BaseAdapter() {


    override fun areAllItemsEnabled(): Boolean {
        return true
    }


    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mDrawerListItems.size
    }

    override fun getItem(i: Int): Any? {
        return null
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    fun removeItem(i: Int) {
        mDrawerListItems.removeAt(i)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        Log.d(TAG, "getView")

        val viewHolder: ViewHolder

        if (convertView == null) {
            val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.history_drawer_layout, parent, false)

            viewHolder = ViewHolder()
            viewHolder.drawerItemName = convertView!!.findViewById<View>(R.id.history_drawer_model) as TextView
            viewHolder.drawerItemIcon = convertView.findViewById<View>(R.id.history_icon_imageview) as ImageView
            viewHolder.drawerItemType = convertView.findViewById<View>(R.id.history_drawer_type) as TextView
            viewHolder.drawerItemTime = convertView.findViewById<View>(R.id.history_drawer_printtime) as TextView
            viewHolder.drawerItemDate = convertView.findViewById<View>(R.id.history_drawer_date) as TextView


            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        val drawerListItem = mDrawerListItems[position]

        viewHolder.drawerItemName!!.text = drawerListItem.model
        viewHolder.drawerItemType!!.text = drawerListItem.type
        viewHolder.drawerItemTime!!.text = drawerListItem.time
        viewHolder.drawerItemDate!!.text = drawerListItem.date

        if (drawerListItem.path != null) {

            val path = File(drawerListItem.path)
            val root = path.parentFile.parentFile.absolutePath

            for (m in LibraryController.fileList) {

                if (LibraryController.isProject(m)) {

                    if (m.getAbsolutePath() == root) {

                        viewHolder.drawerItemIcon!!.setImageDrawable((m as ModelFile).snapshot)
                        viewHolder.drawerItemName!!.text = path.name

                        break

                    }

                }

            }

        } else
            viewHolder.drawerItemIcon!!.setImageDrawable(mContext.resources.getDrawable(R.drawable.ic_file_gray))




        return convertView
    }

    private class ViewHolder {
        internal var drawerItemName: TextView? = null
        internal var drawerItemType: TextView? = null
        internal var drawerItemTime: TextView? = null
        internal var drawerItemDate: TextView? = null
        internal var drawerItemIcon: ImageView? = null
    }

    companion object {

        private val TAG = "DrawerListAdapter"
    }

}
