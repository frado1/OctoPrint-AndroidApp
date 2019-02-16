package de.domes_muc.printerappkotlin.devices

import de.domes_muc.printerappkotlin.model.ModelPrinter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.material.widget.PaperButton
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.model.ModelProfile
import de.domes_muc.printerappkotlin.octoprint.OctoprintControl
import org.json.JSONObject

/**
 * This class will handle the View adapter for the Extruders temperature grid
 *
 * @author franz-domes
 */
class ExtrudersGridAdapter //Constructor
    (private val mContext: Context, resource: Int, printer: ModelPrinter) : BaseAdapter() {

    //Original list and current list to be filtered
    private var mModelPrinter: ModelPrinter? = null
    private var profile: JSONObject? = null
    private var numExtruder = 1
    private var heatedBed = false

    init {
        mModelPrinter = printer
        profile = printer.profile?.let { ModelProfile.retrieveProfile(mContext, it, ModelProfile.TYPE_P) }

        profile?.let {
            heatedBed = it.get("heatedBed") as Boolean
            numExtruder = it.getJSONObject("extruder").getInt("count")
        }
    }

    //Overriding our view to show the grid on screen
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        val holder: ExtruderViewHolder

        //View not yet created
        if (convertView == null) {

            //Inflate the view
            val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.grid_item_extruder_temperature, parent, false)

            holder = ExtruderViewHolder()
            holder.textViewExtruderTag = convertView!!.findViewById<View>(R.id.printview_extruder_tag) as TextView
            holder.textViewExtruderTemp = convertView!!.findViewById<View>(R.id.printview_extruder_temp) as TextView
            holder.textViewExtruderTempSlider =
                convertView!!.findViewById<View>(R.id.printview_extruder_temp_slider) as SeekBar
            holder.paperButtonExtruderTempButton =
                convertView!!.findViewById<View>(R.id.printview_extruder_temp_button) as PaperButton
            convertView!!.tag = holder

            holder.paperButtonExtruderTempButton?.setClickable(false);
            holder.paperButtonExtruderTempButton?.setFocusable(false);
            holder.paperButtonExtruderTempButton?.setEnabled(false);

            holder.paperButtonExtruderTempButton!!.setText(String.format(mContext.getString(R.string.printview_change_temp_button), 0))
            attachProgressUpdatedListener(holder, position)
        } else {
            holder = convertView.tag as ExtruderViewHolder
        }

        if (heatedBed && position == numExtruder) {
            holder.textViewExtruderTag!!.text = mContext.getString(R.string.printview_bed_tag)
            holder.textViewExtruderTemp!!.text =
                mModelPrinter!!.bedTemperature + "ºC / " + mModelPrinter!!.bedTempTarget + "ºC"

        } else {
            holder.textViewExtruderTag!!.text = String.format(mContext.getString(R.string.printview_extruder_tag), position)
            holder.textViewExtruderTemp!!.text =
                mModelPrinter!!.extruderTemp[position] + "ºC / " + mModelPrinter!!.extruderTempTarget[position] + "ºC"
        }

        return convertView
    }

    private fun attachProgressUpdatedListener(holder: ExtruderViewHolder, position: Int) {
        holder.textViewExtruderTempSlider!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Write code to perform some action when progress is changed.
                Toast.makeText(mContext, "ProgressChanged is " + seekBar.progress + "%", Toast.LENGTH_SHORT).show()
                if (heatedBed && position == numExtruder) {
                    OctoprintControl.sendToolCommand(
                        mContext!!,
                        mModelPrinter!!.address,
                        "target",
                        "bed",
                        progress.toDouble()
                    )
                } else {
                    OctoprintControl.sendToolCommand(
                        mContext!!,
                        mModelPrinter!!.address,
                        "target",
                        "tool$position",
                        progress.toDouble()
                    )
                }
                holder.paperButtonExtruderTempButton!!.setText(String.format(mContext.getString(R.string.printview_change_temp_button), progress))
                notifyDataSetChanged()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }

    //Retrieve item from current list by its position on the grid
    override fun getItem(position: Int): ModelPrinter? {
        return null
    }

    //Retrieve count for MAX items to show empty slots
    override fun getCount(): Int {
        return numExtruder + if (heatedBed) {
            1
        } else {
            0
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    internal class ExtruderViewHolder {

        var textViewExtruderTag: TextView? = null
        var textViewExtruderTemp: TextView? = null
        var textViewExtruderTempSlider: SeekBar? = null
        var paperButtonExtruderTempButton: PaperButton? = null
    }
}
