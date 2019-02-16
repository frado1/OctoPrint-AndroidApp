package de.domes_muc.printerappkotlin.viewer.sidepanel

import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Created by alberto-baeza on 10/30/14.
 */
class SidePanelPrinterAdapter(context: Context, resource: Int, objects: List<ModelPrinter>) :
    ArrayAdapter<ModelPrinter>(context, resource, objects) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView

        val m = getItem(position)

        //View not yet created
        if (v == null) {

            //Inflate the view
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            v = inflater.inflate(R.layout.print_panel_spinner_item, null, false)


        } else {
            //v = convertView;
        }

        val tv = v!!.findViewById<View>(R.id.print_panel_spinner_text) as TextView


        if (m!!.status != StateUtils.STATE_OPERATIONAL) {

            tv.setTextColor(Color.GRAY)

        } else
            tv.setTextColor(Color.BLACK)

        tv.text = m.displayName

        return v
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView

        val m = getItem(position)

        //View not yet created
        if (v == null) {

            //Inflate the view
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            v = inflater.inflate(R.layout.print_panel_spinner_item, null, false)


        } else {
            //v = convertView;
        }

        val tv = v!!.findViewById<View>(R.id.print_panel_spinner_text) as TextView


        if (m!!.status != StateUtils.STATE_OPERATIONAL) {

            tv.setTextColor(Color.GRAY)

        } else
            tv.setTextColor(Color.BLACK)

        tv.text = m.displayName

        return v
    }
}
