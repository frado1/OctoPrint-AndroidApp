package de.domes_muc.printerappkotlin.library

import android.annotation.SuppressLint
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.model.ModelFile
import de.domes_muc.printerappkotlin.model.ModelPrinter
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import de.domes_muc.printerappkotlin.octoprint.OctoprintFiles

import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList


/**
 * This clas will handle the adapter for the library items
 *
 * @author alberto-baeza
 */
class LibraryAdapter(
    context: Context,
    internal var mContext: LibraryFragment,
    private val mResource: Int,
    objects: List<File>
) : ArrayAdapter<File>(context, mResource, objects), Filterable {

    //Original list and current list to be filtered
    private var mCurrent: ArrayList<File>? = null
    private val mOriginal: ArrayList<File>

    //Filter
    private var mFilter: ListFilter? = null

    //Flag to know if the list is being modified
    private var mListInSelectionMode: Boolean = false

    //List of selected items in selection mode
    private var mCheckedItems = ArrayList<Boolean>()

    init {
        mOriginal = objects as ArrayList<File>
        mCurrent = objects
        mFilter = ListFilter()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var v = convertView
        val m = getItem(position)

        //View not yet created
        if (v == null) {

            //Inflate the view
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            v = inflater.inflate(mResource, null, false)

        } else {
            //v = convertView;
        }

        val nameTextView = v!!.findViewById<View>(R.id.model_name_textview) as TextView
        nameTextView.text = m!!.name

        val dateTextView = v.findViewById<View>(R.id.model_mod_date_textview) as TextView

        val sdf = SimpleDateFormat("dd/MM/yyyy")
        dateTextView.text = sdf.format(m.lastModified()) + " " + m.parentFile.name + "/"

        val iv = v.findViewById<View>(R.id.model_icon) as ImageView

        //If selection mode is on, show the selection checkbox
        val selectModelCheckbox = v.findViewById<View>(R.id.select_model_checkbox) as CheckBox
        if (mListInSelectionMode) {

            try {
                selectModelCheckbox.isChecked = mCheckedItems[position]
                selectModelCheckbox.visibility = View.VISIBLE
            } catch (e: IndexOutOfBoundsException) {

                e.printStackTrace()
            }

        } else {
            selectModelCheckbox.isChecked = false
            selectModelCheckbox.visibility = View.INVISIBLE
        }

        if (m.isDirectory) {

            if (LibraryController.isProject(m)) {
                val d: Drawable?
                d = (m as ModelFile).snapshot

                if (d != null) {
                    iv.setImageDrawable(d)
                    iv.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    iv.setImageResource(R.drawable.ic_folder_grey600_36dp)
                    iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
                }

            } else {
                iv.setImageResource(R.drawable.ic_folder_grey600_36dp)
                iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }

        } else {

            //TODO Handle printer internal files
            if (m.parent == "/printer") {
                iv.setImageResource(R.drawable.ic_folder_grey600_36dp)
                iv.scaleType = ImageView.ScaleType.CENTER_INSIDE

                val p = DevicesListController.getPrinter(OctoprintFiles.getPrinterIdFromPath(m.path) ?: -1L)
                nameTextView.text = p?.displayName


            } else {
                iv.setImageResource(R.drawable.ic_file_gray)
                iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }

            dateTextView.text = null
        }

        val overflowButton = v.findViewById<View>(R.id.model_settings_imagebutton) as ImageButton
        if (overflowButton != null) {
            overflowButton.setColorFilter(
                context.resources.getColor(R.color.body_text_3),
                PorterDuff.Mode.MULTIPLY
            )
            overflowButton.setOnClickListener { v ->
                val onClickListener = LibraryOnClickListener(mContext, null)
                onClickListener.onOverflowButtonClick(v, position)
            }
        }


        //Hide overflow button in printer tab
        if (mListInSelectionMode || mContext.currentTab == LibraryController.TAB_PRINTER) {

            overflowButton.visibility = View.GONE

        } else
            overflowButton.visibility = View.VISIBLE

        return v
    }

    //Retrieve item from current list
    override fun getItem(position: Int): File? {
        return mCurrent!![position]
    }

    //Retrieve count from current list
    override fun getCount(): Int {
        return mCurrent!!.size
    }

    //Get filter
    override fun getFilter(): Filter {

        if (mFilter == null)
            mFilter = ListFilter()

        return mFilter!!
    }

    fun removeFilter() {

        mCurrent = mOriginal
        notifyDataSetChanged()

    }

    fun setSelectionMode(isSelectionMode: Boolean) {
        this.mListInSelectionMode = isSelectionMode
        mCheckedItems = ArrayList()
        for (i in mCurrent!!.indices) {
            mCheckedItems.add(false)
        }
    }

    fun setItemChecked(position: Int, checked: Boolean) {
        mCheckedItems[position] = checked
    }

    /**
     * This class is the custom filter for the Library
     *
     * @author alberto-baeza
     */
    private inner class ListFilter : Filter() {

        @SuppressLint("DefaultLocale")
        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {

            //New filter result object
            val result = Filter.FilterResults()

            if (constraint != null && constraint.toString().length > 0) {
                //Temporal list
                val filt = ArrayList<File>()

                // if ((constraint.equals("gcode"))||(constraint.equals("stl"))){

                //Check if every item from the CURRENT list has the constraint
                for (m in mCurrent!!) {

                    if (!m.isDirectory) {
                        if (m.name.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            filt.add(m)
                        }
                    } else {

                        if (!LibraryController.isProject(m)) {
                            filt.add(m)
                        } else {
                            if (m.name.toLowerCase().contains(constraint.toString().toLowerCase())) {
                                filt.add(m)
                            }
                        }
                    }
                }
                /* } else {
                     //Check if every item from the original list has the constraint
                    for (File m : mOriginal){

                    	if (m.isDirectory()){

                    		if (LibraryController.isProject(m)){
                    			if (((ModelFile)m).getStorage().contains(constraint)){
                            		filt.add(m);
                            	}
                    		}

                    	}


                    }*/
                //}


                //New list is filtered list
                result.count = filt.size
                result.values = filt
            } else {
                //New list is original list (no filter, default)
                result.count = mOriginal.size
                result.values = mOriginal

            }
            return result
        }

        override fun publishResults(
            constraint: CharSequence,
            results: Filter.FilterResults
        ) {

            //If there are results, update list
            mCurrent = results.values as ArrayList<File>
            notifyDataSetChanged()

        }

    }

}
