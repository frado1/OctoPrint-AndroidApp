package de.domes_muc.printerappkotlin.devices.database

import android.provider.BaseColumns

// To prevent someone from accidentally instantiating the contract class,
// give it an empty constructor.
class DeviceInfo {

    /* Inner class that defines the table contents */
    abstract class FeedEntry : BaseColumns {
        companion object {
            val _ID = "_id"
            val TABLE_NAME = "Devices"
            val DEVICES_NAME = "name"
            val DEVICES_ADDRESS = "address"
            val DEVICES_POSITION = "position"
            val DEVICES_DISPLAY = "display"
            val DEVICES_TYPE = "type"
            val DEVICES_PROFILE = "profile"
            val DEVICES_NETWORK = "network"

            val TABLE_HISTORY_NAME = "History"
            val HISTORY_MODEL = "model"
            val HISTORY_PATH = "path"
            val HISTORY_TIME = "printTime"
            val HISTORY_PRINTER = "printer"
            val HISTORY_DATE = "date"
        }


    }

}
