package de.domes_muc.printerappkotlin.library.detail

import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.model.ModelComment
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class DetailViewCommAdapter(
    context: Context, resource: Int,
    objects: List<ModelComment>
) : ArrayAdapter<ModelComment>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var v = convertView

        val c = getItem(position)

        //View not yet created
        if (v == null) {


            //Inflate the view
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            v = inflater.inflate(R.layout.detailview_list_comment, null, false)


        } else {
            //v = convertView;
        }

        val tv1 = v!!.findViewById<View>(R.id.detailview_comment_tv1) as TextView
        val tv2 = v.findViewById<View>(R.id.detailview_comment_tv2) as TextView
        val tv3 = v.findViewById<View>(R.id.detailview_comment_tv3) as TextView

        tv1.text = c!!.author
        tv2.text = c.date
        tv3.text = c.comment

        return v
    }

}
