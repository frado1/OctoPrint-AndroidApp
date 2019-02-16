package de.domes_muc.printerappkotlin.util.ui

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.content.res.TypedArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView


class ListIconPopupWindowAdapter : BaseAdapter {

    private var mContext: Context? = null
    private var mInflater: LayoutInflater? = null

    private var mSelectableOptions: Array<String>? = null
    private var mCurrentSelectedOption: String? = null
    private var mListDrawables: TypedArray? = null
    private var mListDrawablesId: IntArray? = null

    constructor(
        context: Context,
        selectableOptions: Array<String>,
        listDrawables: TypedArray,
        currentSelectedOption: String
    ) {
        this.mContext = context
        this.mSelectableOptions = selectableOptions
        this.mListDrawables = listDrawables
        this.mCurrentSelectedOption = currentSelectedOption
        this.mInflater = LayoutInflater.from(mContext)
    }

    constructor(
        context: Context,
        selectableOptions: Array<String>,
        listDrawablesId: IntArray,
        currentSelectedOption: String
    ) {
        this.mContext = context
        this.mSelectableOptions = selectableOptions
        this.mListDrawablesId = listDrawablesId
        this.mCurrentSelectedOption = currentSelectedOption
        this.mInflater = LayoutInflater.from(mContext)
    }

    override fun getCount(): Int {
        return mSelectableOptions!!.size
    }

    override fun getItem(i: Int): Any {
        return mSelectableOptions!![i]
    }

    override fun getItemId(i: Int): Long {
        return 0
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        val holder: ViewHolder
        if (convertView == null) {
            //Create a custom view with the action icon
            convertView = mInflater!!.inflate(R.layout.item_list_popup_action_icon, null)
            holder = ViewHolder()
            holder.listImageButton = convertView!!.findViewById<View>(R.id.item_list_button) as ImageView
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        //Set the info of the button
        holder.listImageButton!!.contentDescription = mSelectableOptions!![position]
        if (mListDrawables != null) {
            holder.listImageButton!!.setImageResource(mListDrawables?.getResourceId(position, -1)!!)
            holder.listImageButton!!.tag = mListDrawables?.getResourceId(position, -1)
        } else if (mListDrawablesId != null) {
            holder.listImageButton!!.setImageResource(mListDrawablesId!![position])
            holder.listImageButton!!.tag = mListDrawablesId!![position]
        }

        if (mCurrentSelectedOption != null && mCurrentSelectedOption == mSelectableOptions!![position]) {
            Log.d(
                TAG,
                "Selected option: " + mCurrentSelectedOption + " Position " + position + ": " + mSelectableOptions!![position]
            )
            holder.listImageButton!!.setBackgroundDrawable(mContext!!.resources.getDrawable(R.drawable.oval_background_green))
        }

        return convertView
    }

    internal class ViewHolder {
        var listImageButton: ImageView? = null
    }

    companion object {

        private val TAG = "ListPopupWindowAdapter"
    }
}