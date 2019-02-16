package de.domes_muc.printerappkotlin.devices.printview

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.viewer.DataStorage

import java.util.ArrayList

/**
 * This class will hold a reference to every gcode already loaded in memory in the app
 * to avoid having to open it every single time
 * Created by alberto-baeza on 12/19/14.
 */
class GcodeCache {
    //Generic constructor
    init {

        mGcodeCacheList = ArrayList()

    }

    companion object {

        private lateinit var mGcodeCacheList: ArrayList<DataStorage>

        //Add a new gcode to the list
        fun addGcodeToCache(data: DataStorage) {

            mGcodeCacheList.add(data)

        }

        //Retrieve a gcode from the list by its path file
        fun retrieveGcodeFromCache(path: String): DataStorage? {


            for (data in mGcodeCacheList) {

                if (data.pathFile == path) return data

            }

            return null

        }

        //Remove a gcode from the list
        fun removeGcodeFromCache(path: String) {

            var index = -1

            for (i in mGcodeCacheList.indices) {

                if (mGcodeCacheList[i].pathFile == path) {

                    Log.i("PrintView", mGcodeCacheList.size.toString() + " Removed " + path + " from cache")
                    index = i

                }

            }

            if (index >= 0) mGcodeCacheList.removeAt(index)

        }
    }


}
