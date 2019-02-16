package de.domes_muc.printerappkotlin.library.detail

import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.library.LibraryController
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import com.material.widget.PaperButton

import java.io.File


/**
 * This is the adapter for the detail view
 *
 * @author alberto-baeza
 */
class DetailViewAdapter(context: Context, resource: Int, objects: List<File>, private val mDrawable: Drawable) :
    ArrayAdapter<File>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var v = convertView

        val f = getItem(position)

        //View not yet created
        if (v == null) {

            //Inflate the view
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            v = inflater.inflate(R.layout.detailview_list_element, null, false)

        } else {
            //v = convertView;
        }

        //UI references
        val tv1 = v!!.findViewById<View>(R.id.detailview_list_tv1) as TextView
        tv1.text = f!!.name

        val ib = v.findViewById<View>(R.id.detailview_list_iv1) as PaperButton
        val ibe = v.findViewById<View>(R.id.detailview_list_iv2) as PaperButton

        if (LibraryController.hasExtension(1, f.name)) {
            ibe.visibility = View.GONE
            ib.visibility = View.VISIBLE
            //it's an stl
        } else {
            ibe.visibility = View.VISIBLE
            ib.visibility = View.GONE
        }

        //Print button
        ib.setOnClickListener { v ->
            if (LibraryController.hasExtension(0, f.name))
                MainActivity.requestOpenFile(f.absolutePath)
            else
                DevicesListController.selectPrinter(v.context, f, null)
        }

        //Edit button
        ibe.setOnClickListener { MainActivity.requestOpenFile(f.absolutePath) }

        return v
    }


}
