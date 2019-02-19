package de.domes_muc.printerappkotlin.devices

import com.pnikosis.materialishprogress.ProgressWheel
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.Context
import android.graphics.PorterDuff.Mode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

//import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.ArrayList

/**
 * This class will handle the View adapter for the Devices fragment
 *
 * @author alberto-baeza
 */
class DevicesGridAdapter//Constructor
    (private val mContext: Context, resource: Int, objects: List<ModelPrinter>) :
    ArrayAdapter<ModelPrinter>(mContext, resource, objects), Filterable {

    //Original list and current list to be filtered
    private var mCurrent: ArrayList<ModelPrinter>? = null
    private val mOriginal: ArrayList<ModelPrinter>

    //Filter
    private var mFilter: GridFilter? = null


    init {
        mOriginal = objects as ArrayList<ModelPrinter>
        mCurrent = objects

    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun isEnabled(position: Int): Boolean {
        return super.isEnabled(position)
    }

    //Overriding our view to show the grid on screen
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        val holder: ViewHolder

        //For every element on the list we create a model printer, but only use the
        //ones that are actually holding printers, else are empty spaces
        val m = getItem(position)

        //View not yet created
        if (convertView == null) {

            //Inflate the view
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.grid_item_printer, null, false)

            holder = ViewHolder()
            holder.textViewTag = convertView!!.findViewById<View>(R.id.discover_printer_name) as TextView
            holder.textViewLoading = convertView.findViewById<View>(R.id.grid_text_loading) as TextView
            holder.imageIcon = convertView.findViewById<View>(R.id.discover_printer_icon) as ImageView
            holder.progressBarPrinting = convertView.findViewById<View>(R.id.grid_element_progressbar) as ProgressBar
            holder.progressBarLoading = convertView.findViewById<View>(R.id.grid_element_loading) as ProgressWheel
            holder.imageWarning = convertView.findViewById<View>(R.id.grid_warning_icon) as ImageView
            holder.gridItem = convertView.findViewById<View>(R.id.grid_item_printer_container) as LinearLayout
            convertView.tag = holder

        } else {

            holder = convertView.tag as ViewHolder
        }

        //Hide icons and progress bars
        holder.textViewLoading!!.visibility = View.GONE
        holder.imageWarning!!.visibility = View.GONE
        holder.progressBarPrinting!!.visibility = View.GONE
        holder.progressBarLoading!!.visibility = View.INVISIBLE

        //Check if it's an actual printer or just an empty slot
        if (m == null) {

            //Empty slot is an invisible printer on the current position
            convertView.setOnDragListener(DevicesEmptyDragListener(position, this))
            holder.textViewTag!!.text = ""
            holder.imageIcon!!.visibility = View.INVISIBLE
            holder.imageIcon!!.clearColorFilter()
            //holder.gridItem.setBackgroundResource(0);

            //It's a printer
        } else {

            if (m.status == StateUtils.STATE_NEW || m.status == StateUtils.STATE_ADHOC) {

                //Empty slot is an invisible printer on the current position
                convertView.setOnDragListener(null)
                holder.textViewTag!!.text = ""
                holder.imageIcon!!.visibility = View.GONE
                holder.imageIcon!!.clearColorFilter()

            } else {


                //Intialize visual parameters
                convertView.setOnDragListener(DevicesDragListener(mContext, m, this))
                holder.textViewTag!!.text = m.displayName
                //holder.textViewTag.setTextColor(m.getDisplayColor());
                holder.imageIcon!!.visibility = View.VISIBLE
                holder.imageIcon!!.setColorFilter(m.displayColor, Mode.SRC_ATOP)


                val status = m.status


                //LinearLayout gridItem = (LinearLayout) convertView.findViewById(R.id.grid_item_printer_container);
                holder.gridItem!!.setBackgroundResource(R.drawable.selectable_rect_background_green)

                //Printer icon
                when (status) {

                    /*case StateUtils.STATE_NONE: {
                    holder.imageIcon.setImageResource(R.drawable.icon_printer);
                }
                break;*/

                    StateUtils.STATE_NEW ->

                        holder.imageIcon!!.setImageResource(R.drawable.printer_signal_add)
                    StateUtils.STATE_ADHOC -> holder.imageIcon!!.setImageResource(R.drawable.signal_octopidev)

                    else -> {

                        when (m.type) {
                            StateUtils.TYPE_WITBOX ->
                                if (m.displayColor != 0) {
                                    holder.imageIcon!!.setImageResource(R.drawable.printer_witbox_alpha)
                                    holder.imageIcon!!.setColorFilter(m.displayColor, Mode.DST_ATOP)
                                } else
                                    holder.imageIcon!!.setImageResource(R.drawable.printer_witbox_default)

                            StateUtils.TYPE_PRUSA ->
                                if (m.network != null)
                                    if (m.network == MainActivity.getCurrentNetwork(context)) {
                                        if (m.displayColor != 0) {

                                            holder.imageIcon!!.setImageResource(R.drawable.printer_prusa_alpha)
                                            holder.imageIcon!!.setColorFilter(m.displayColor, Mode.DST_ATOP)

                                        } else
                                            holder.imageIcon!!.setImageResource(R.drawable.printer_prusa_default)
                                    } else
                                        holder.imageIcon!!.setImageResource(R.drawable.printer_prusa_nowifi)

                            StateUtils.TYPE_CUSTOM ->
                                if (m.displayColor != 0) {
                                    holder.imageIcon!!.setImageResource(R.drawable.printer_custom_alpha)
                                    holder.imageIcon!!.setColorFilter(m.displayColor, Mode.DST_ATOP)
                                } else
                                    holder.imageIcon!!.setImageResource(R.drawable.printer_custom_default)

                            else -> holder.imageIcon!!.setImageResource(R.drawable.printer_custom_default)
                        }

                    }
                }

                //Status icon
                when (status) {

                    StateUtils.STATE_OPERATIONAL -> {

                        //Check for printing completion
                        if (m.job != null) {

                            //Currently finished means operational + file loaded with 100% progress
                            if (m.job.progress != "null") {

                                if (m.job.finished) {
                                    holder.progressBarPrinting!!.visibility = View.VISIBLE
                                    holder.progressBarPrinting!!.progress = 100
                                    //holder.progressBarPrinting.getProgressDrawable().setColorFilter(Color.parseColor("#ff009900"), Mode.SRC_IN);
                                    holder.textViewLoading!!.setText(R.string.devices_text_completed)
                                    holder.textViewLoading!!.visibility = View.VISIBLE
                                }

                                /*Double n = Double.parseDouble(m.getJob().getProgress() );

							if (n.intValue() == 100){


								pb.setVisibility(View.VISIBLE);
								pb.setProgress(n.intValue());
								pb.getProgressDrawable().setColorFilter(Color.GREEN, Mode.SRC_IN);
								tvl.setText(R.string.devices_text_completed);
								tvl.setVisibility(View.VISIBLE);


								//DevicesFragment.playMusic();
							}*/
                            }
                        }

                        //Must put this second because loading has priority over completion
                        if (!m.loaded) {

                            //check if a file is loading
                            holder.progressBarLoading!!.visibility = View.VISIBLE
                            holder.textViewLoading!!.setText(R.string.devices_text_loading)
                            holder.textViewLoading!!.visibility = View.VISIBLE
                        }

                    }


                    //When printing, show status bar and update progress
                    StateUtils.STATE_PRINTING -> {

                        holder.progressBarPrinting!!.visibility = View.VISIBLE
                        if (m.job.progress != "null") {
                            val n = java.lang.Double.valueOf(m.job.progress)
                            holder.progressBarPrinting!!.progress = n.toInt()
                        }
                    }

                    StateUtils.STATE_PAUSED -> {
                        holder.progressBarPrinting!!.visibility = View.VISIBLE
                        if (m.job.progress != "null") {
                            val n = java.lang.Double.valueOf(m.job.progress)
                            holder.progressBarPrinting!!.progress = n.toInt()
                        }
                        holder.textViewLoading!!.setText(R.string.devices_text_paused)
                        holder.textViewLoading!!.visibility = View.VISIBLE

                    }

                    //when closed or error, show error icon
                    StateUtils.STATE_CLOSED, StateUtils.STATE_ERROR -> {
                        holder.imageWarning!!.setImageResource(R.drawable.icon_error)
                        holder.imageWarning!!.visibility = View.VISIBLE
                    }

                    //When connecting show status bar
                    StateUtils.STATE_CONNECTING -> {
                        holder.textViewLoading!!.setText(R.string.devices_text_connecting)
                        holder.textViewLoading!!.visibility = View.VISIBLE
                        holder.progressBarLoading!!.visibility = View.VISIBLE
                    }

                    StateUtils.STATE_NONE -> {
                        holder.textViewLoading!!.text = "Offline"
                        holder.textViewLoading!!.visibility = View.VISIBLE
                        holder.progressBarLoading!!.visibility = View.VISIBLE
                    }

                    else -> {
                    }
                }


                if (m.network != null)
                    if (m.network != MainActivity.getCurrentNetwork(context)) {
                        holder.imageIcon!!.clearColorFilter()
                        holder.imageIcon!!.setImageResource(R.drawable.printer_witbox_nowifi)
                    }

            }
        }

        return convertView
    }

    //Retrieve item from current list by its position on the grid
    override fun getItem(position: Int): ModelPrinter? {

        for (p in mCurrent!!) {
            if (p.position == position) return p
        }
        return null
    }

    //Retrieve count for MAX items to show empty slots
    override fun getCount(): Int {
        return DevicesListController.GRID_MAX_ITEMS
    }

    //Get filter
    override fun getFilter(): Filter {

        if (mFilter == null)
            mFilter = GridFilter()
        return mFilter ?: GridFilter()
    }


    internal class ViewHolder {

        var textViewTag: TextView? = null
        var textViewLoading: TextView? = null
        var imageIcon: ImageView? = null
        var progressBarPrinting: ProgressBar? = null
        var progressBarLoading: ProgressWheel? = null
        var imageWarning: ImageView? = null
        var gridItem: LinearLayout? = null


    }


    /**
     * This class is the custom filter for the Library
     *
     * @author alberto-baeza
     */
    private inner class GridFilter : Filter() {

        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {

            //New filter result object
            val result = Filter.FilterResults()

            if (constraint != null && constraint.toString().length > 0) {
                //Temporal list
                val filt = ArrayList<ModelPrinter>()


                //TODO Should change filter logic to avoid redundancy
                if (constraint != StateUtils.STATE_NEW.toString()) {
                    //Check if every item from the original list has the constraint
                    for (m in mOriginal) {

                        if (m.status == Integer.parseInt(constraint.toString())) {
                            filt.add(m)
                        }

                    }
                } else {

                    //Check if every item from the original list has the constraint
                    for (m in mOriginal) {

                        if (m.status != Integer.parseInt(constraint.toString())) {
                            filt.add(m)
                        }

                    }
                }


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
            mCurrent = results.values as ArrayList<ModelPrinter>
            notifyDataSetChanged()

        }

    }

}
