package de.domes_muc.printerappkotlin.viewer.sidepanel

import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by alberto-baeza on 10/30/14.
 */
class SidePanelProfileAdapter(context: Context, resource: Int, objects: List<JSONObject>) :
    ArrayAdapter<JSONObject>(context, resource, objects) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        val holder: ViewHolder

        val `object` = getItem(position)

        //View not yet created
        if (convertView == null) {

            //Inflate the view
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.print_panel_spinner_item, null, false)

            holder = ViewHolder()
            holder.mProfileName = convertView!!.findViewById<View>(R.id.print_panel_spinner_text) as TextView
            convertView.tag = holder


        } else {

            holder = convertView.tag as ViewHolder
        }

        try {
            holder.mProfileName!!.text = `object`!!.getString("displayName")
        } catch (e: JSONException) {
            e.printStackTrace()
        }


        return convertView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        val holder: ViewHolder

        val `object` = getItem(position)

        //View not yet created
        if (convertView == null) {

            //Inflate the view
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.print_panel_spinner_item, null, false)

            holder = ViewHolder()
            holder.mProfileName = convertView!!.findViewById<View>(R.id.print_panel_spinner_text) as TextView
            convertView.tag = holder


        } else {

            holder = convertView.tag as ViewHolder
        }

        try {
            holder.mProfileName!!.text = `object`!!.getString("displayName")
        } catch (e: JSONException) {
            e.printStackTrace()
        }


        return convertView
    }

    internal class ViewHolder {

        var mProfileName: TextView? = null

    }
}
