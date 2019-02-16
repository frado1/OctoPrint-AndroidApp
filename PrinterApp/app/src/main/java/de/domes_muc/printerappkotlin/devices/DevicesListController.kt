package de.domes_muc.printerappkotlin.devices

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.OctoprintFiles
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import de.domes_muc.printerappkotlin.viewer.SlicingHandler
import de.domes_muc.printerappkotlin.viewer.ViewerMainFragment
import android.content.Context
import android.database.Cursor
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView

import java.io.File
import java.util.ArrayList


/**
 * This class will handle list events such as add, remove or update
 *
 * @author alberto-baeza
 */
object DevicesListController {

    //max items in the grid
    val GRID_MAX_ITEMS = 15

    //List for the printers found
    //Return the list
    val list = ArrayList<ModelPrinter>()

    //Add element to the list
    fun addToList(m: ModelPrinter) {
        list.add(m)

        Log.i("Extra", "Added " + m.id)

    }

    //Return a specific printer
    /*public static ModelPrinter getPrinter(String name) {

		for (ModelPrinter p : mList){

			if (p.getName().equals(name)){
				return p;
			}
		}

		return null;
	}*/

    //Return a specific printer by id
    fun getPrinter(id: Long): ModelPrinter? {

        //If it's a linked printer, retrieve id
        for (p in list) {

            if (DatabaseController.checkExisting(p)) {

                if (p.id == id) {
                    return p
                }
            }

        }

        return null
    }

    //Return a specific printer by position
    fun getPrinterByPosition(pos: Int): ModelPrinter? {
        //Else just retrieve position since it's a new printer
        for (p in list) {

            if (p.position == pos) {
                return p
            }
        }

        return null
    }

    //Load device list from the Database
    fun loadList(context: Context) {

        list.clear()

        val c = DatabaseController.retrieveDeviceList()

        c.moveToFirst()

        while (!c.isAfterLast) {

            Log.i("OUT", "Entry: " + c.getString(1) + ";" + c.getString(2) + ";" + c.getString(3))

            val m = ModelPrinter(c.getString(1), c.getString(2), Integer.parseInt(c.getString(5)))

            if (Integer.parseInt(c.getString(5)) == StateUtils.TYPE_CUSTOM) {

                m.setType(StateUtils.TYPE_CUSTOM, c.getString(6))
            }

            m.id = c.getInt(0).toLong()

            //Custom name
            m.displayName = c.getString(4)

            m.position = Integer.parseInt(c.getString(3))
            m.network = c.getString(7)

            addToList(m)

            if (DatabaseController.isPreference(DatabaseController.TAG_REFERENCES, m.name)) {

                m.jobPath = DatabaseController.getPreference(DatabaseController.TAG_REFERENCES, m.name)

            }


            //Timeout for reconnection
            val handler = Handler()
            handler.post { m.startUpdate(context) }
            c.moveToNext()
        }

        DatabaseController.closeDb()


    }

    //Search first available position by listing the printers
    //TODO HARDCODED MAXIMUM CELLS
    fun searchAvailablePosition(): Int {

        // GRID_MAX_ITEMS initialized to false
        val mFree = BooleanArray(GRID_MAX_ITEMS) { false }

//        for (i in 0 until GRID_MAX_ITEMS) {
//            mFree[i] = false
//
//        }

        for (p in list) {

            if (p.position != -1) mFree[p.position] = true

        }

        for (i in 0 until GRID_MAX_ITEMS) {

            if (!mFree[i]) return i

        }


        return -1

    }


    fun removeElement(position: Int) {

        var target: ModelPrinter? = null

        //search printer by position
        for (mp in list) {
            if (mp.position == position) {
                target = mp
            }
        }

        if (target != null) {
            Log.i("OUT", "Removing " + target.name + " with  index " + list.indexOf(target))
            list.removeAt(list.indexOf(target))
        }

    }

    /**
     * Create a select printer dialog to open the print panel or to upload a file with the selected
     * printer. 0 is for print panel, 1 is for upload
     *
     * @param c App context
     * @param f File to upload/open
     */
    fun selectPrinter(c: Context, f: File, slicer: SlicingHandler?) {

        val tempList = ArrayList<ModelPrinter>()
        val dialogTitle = c.getString(R.string.library_select_printer_title) + " " + f.name

        //Fill the list with operational printers
        for (p in list) {
            if (p.status == StateUtils.STATE_OPERATIONAL) {
                tempList.add(p)
            }
        }

        val printersList = arrayOfNulls<String>(tempList.size)
        var i = 0

        //New array with names only for the adapter
        for (p in tempList) {
            printersList[i] = p.displayName
            i++
        }

        if (printersList.size > 1) {
            //Get the dialog view
            val inflater = c.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val selectPrinterView = inflater.inflate(R.layout.dialog_select_printer, null)
            val selectPrinterSpinner = selectPrinterView.findViewById<View>(R.id.select_printer_spinner) as Spinner

            //Set the list of printers into the spinner
            val printerAdapter = ArrayAdapter<String>(
                c,
                R.layout.print_panel_spinner_item, printersList
            )
            printerAdapter.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item)
            selectPrinterSpinner.adapter = printerAdapter

            //Show the dialog
            val selectPrinterDialog = MaterialDialog(c)
            selectPrinterDialog.title(text = dialogTitle)
                .customView(view = selectPrinterView)
                .positiveButton(R.string.library_print_model) {

                    val m = tempList[selectPrinterSpinner.selectedItemPosition]

                    if (slicer != null) {

                        slicer.setPrinter(m)
                        slicer.setExtras("print", true)

                        m.loaded = false

                    } else {
                        OctoprintFiles.getFiles(c, m, f)
                        //OctoprintFiles.uploadFile(context, file, m);
                    }
                    MainActivity.performClick(2)
                    MainActivity.showExtraFragment(1, m.id)

                    it.dismiss()

                    ViewerMainFragment.optionClean()


                }
                .negativeButton(R.string.cancel) {
                    it.dismiss()
                }
                .noAutoDismiss()
                .show()
        } else if (printersList.size == 1) {

            val m = tempList[0]

            if (slicer != null) {

                slicer.setPrinter(m)
                slicer.setExtras("print", true)

                m.loaded = false

            } else {
                OctoprintFiles.getFiles(c, m, f)
                //OctoprintFiles.uploadFile(context, file, m);
            }
            MainActivity.performClick(2)
            MainActivity.showExtraFragment(1, m.id)

            ViewerMainFragment.optionClean()

        } else
            Toast.makeText(c, R.string.viewer_printer_selected, Toast.LENGTH_SHORT).show()


    }

    /**
     * check if there's already the printer listed filtered by ip
     *
     * @param ip
     * @return
     */
    fun checkExisting(ip: String): Boolean {

        for (p in list) {

            if (p.address == ip) {

                if (p.status != StateUtils.STATE_ADHOC) {

                    Log.i("OUT", "Printer $ip already added.")

                    return true
                }


            }

        }

        return false

    }

    /**
     * Return the first Operational printer on the list
     *
     * @return
     */
    fun selectAvailablePrinter(type: Int, typeName: String): ModelPrinter? {

        //search for operational printers

        for (p in DevicesListController.list) {

            if (type < 3) {
                if (p.type == type)
                    if (p.status == StateUtils.STATE_OPERATIONAL)
                        return p
            } else {

                if (p.profile == typeName)
                    if (p.status == StateUtils.STATE_OPERATIONAL)
                        return p

            }


        }
        return null

    }

}
