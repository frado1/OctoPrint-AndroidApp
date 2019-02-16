package de.domes_muc.printerappkotlin.devices

import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.model.ModelPrinter
import android.content.ClipData
import android.content.res.Resources
import android.view.DragEvent
import android.view.View
import android.view.View.OnDragListener
import android.widget.ArrayAdapter


/**
 * this class will handle drag listeners for empty cells on the GridView,
 * it will only listen to printers, and will update the position on the DB also
 * @author alberto-baeza
 */
class DevicesEmptyDragListener
/**
 * Constructor to the listener
 * @param pos the position on the grid
 */
    (//current position on the grid
    private val mPosition: Int, private val mAdapter: ArrayAdapter<ModelPrinter>
) : OnDragListener {


    /**
     * We have to parse the event tag separately per event because there is a bug with ACTION_DRAG_ENDED
     * that has a null getClipDescription().
     */
    override fun onDrag(v: View, event: DragEvent): Boolean {

        //Get the drop event
        val action = event.action

        when (action) {

            //If it's a drop
            DragEvent.ACTION_DROP -> {

                val tag = event.clipDescription.label
                val item = event.clipData.getItemAt(0)

                //If it's a printer
                if (tag == "printer") {


                    val id = Integer.parseInt(item.text.toString())
                    //Find a printer from it's name
                    var p: ModelPrinter? = null

                    if (id >= 0) {

                        p = DevicesListController.getPrinter(id.toLong())

                    } else {

                        p = DevicesListController.getPrinterByPosition(-(id + 1))

                    }

                    if (p != null) {
                        //update position
                        p.position = mPosition
                        mAdapter.notifyDataSetChanged()


                    }//update database
                    //DatabaseController.updateDB(FeedEntry.DEVICES_POSITION, p.getId(), String.valueOf(mPosition));
                    //static notification of the adapters

                }


                //Remove highlight
                v.setBackgroundColor(Resources.getSystem().getColor(android.R.color.transparent))

            }

            DragEvent.ACTION_DRAG_ENTERED -> {

                //TODO NullPointerException
                try {

                    val tag = event.clipDescription.label
                    //If it's a printer
                    if (tag == "printer") {

                        //Highlight on hover
                        v.setBackgroundColor(v.context.resources.getColor(R.color.drag_and_drop_hover_background))
                    }

                } catch (e: NullPointerException) {

                    e.printStackTrace()

                }

            }
            DragEvent.ACTION_DRAG_EXITED -> {

                //Remove highlight
                v.setBackgroundColor(Resources.getSystem().getColor(android.R.color.transparent))

            }

            else -> {
            }
        }

        return true
    }


}
