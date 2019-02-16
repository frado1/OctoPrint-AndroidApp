package de.domes_muc.printerappkotlin.devices

import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.OctoprintFiles
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.content.ClipData
import android.content.Context
import android.content.res.Resources
import android.view.DragEvent
import android.view.View
import android.view.View.OnDragListener
import android.widget.ArrayAdapter

import java.io.File

/**
 * OnDragListener to handle printing and other possible events such as
 * printer positions on the grid
 * @author alberto-baeza
 */
class DevicesDragListener
/**
 * Class constructor
 * @param model The model currently being dragged on
 */
    (
    private val mContext: Context, //Reference to model
    private val mModel: ModelPrinter, private val mAdapter: ArrayAdapter<ModelPrinter>
) : OnDragListener {


    /**
     * Possible drag event tags:
     *
     * printer: Dragging a printer
     * name: Dragging a file
     */
    override fun onDrag(v: View, event: DragEvent): Boolean {

        //Get the drop event
        val action = event.action
        when (action) {

            //If it's a drop
            DragEvent.ACTION_DROP -> {

                val tag = event.clipDescription.label


                //If it's a file (avoid draggable printers)
                if (tag == "name") {

                    //If it's not online, don't send to printer
                    //Now files can also be uploaded to printers with errors
                    if (mModel.status == StateUtils.STATE_OPERATIONAL || mModel.status == StateUtils.STATE_ERROR) {

                        // Gets the item containing the dragged data
                        val item = event.clipData.getItemAt(0)

                        //Get parent folder and upload to device
                        val file = File(item.text.toString())

                        //Call to the static method to upload
                        OctoprintFiles.uploadFile(v.context, file, mModel)

                        mAdapter.notifyDataSetChanged()

                    } else {
                        //Error dialog
                        MainActivity.showDialog(
                            v.context.getString(R.string.devices_dialog_loading) + "\n" + v.context.getString(
                                R.string.viewer_printer_unavailable
                            )
                        )
                    }
                }

                //Remove highlight
                v.setBackgroundColor(Resources.getSystem().getColor(android.R.color.transparent))

            }

            DragEvent.ACTION_DRAG_ENTERED -> {

                val tag = event.clipDescription.label
                //If it's a file (avoid draggable printers)
                if (tag == "name") {

                    if (mModel.status == StateUtils.STATE_OPERATIONAL || mModel.status == StateUtils.STATE_ERROR) {

                        //Highlight on hover
                        v.setBackgroundColor(mContext.resources.getColor(R.color.drag_and_drop_hover_background))
                    }
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
