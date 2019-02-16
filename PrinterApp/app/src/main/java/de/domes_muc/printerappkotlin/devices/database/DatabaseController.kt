package de.domes_muc.printerappkotlin.devices.database

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.MainActivity
import de.domes_muc.printerappkotlin.devices.database.DeviceInfo.FeedEntry
import de.domes_muc.printerappkotlin.model.ModelPrinter
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.domes_muc.printerappkotlin.devices.database.DatabaseController.Companion.mDb


/**
 * This class will handle Database interaction on a static way
 * Also will contain the SharedPreferences to handle Favorites
 * @author alberto-baeza
 */
class DatabaseController(context: Context) {

    init {

        mContext = context
        mDbHelper = DatabaseHelper(mContext)

    }

    companion object {

        val TAG_NETWORK = "Network"
        val TAG_REFERENCES = "References"
        val TAG_FAVORITES = "Favorites"
        val TAG_KEYS = "Keys"
        val TAG_SLICING = "Slicing"
        val TAG_PROFILE = "ProfilePreferences"
        val TAG_RESTORE = "Restore"

        internal lateinit var mDbHelper: DatabaseHelper
        internal lateinit var mDb: SQLiteDatabase
        internal lateinit var mContext: Context

        //Add a new element to the permanent database
        fun writeDb(name: String, address: String, position: String, type: String, network: String): Long {

            // Gets the data repository in write mode
            mDb = mDbHelper.writableDatabase

            Log.i("OUT", "Adding: $name")
            // Create a new map of values, where column names are the keys
            val values = ContentValues()
            values.put(FeedEntry.DEVICES_NAME, name)
            values.put(FeedEntry.DEVICES_ADDRESS, address)
            values.put(FeedEntry.DEVICES_POSITION, position)
            values.put(FeedEntry.DEVICES_DISPLAY, name)
            values.put(FeedEntry.DEVICES_TYPE, type)
            values.put(FeedEntry.DEVICES_NETWORK, network)

            val id = mDb.insert(FeedEntry.TABLE_NAME, null, values)
            mDb.close()

            MainActivity.refreshDevicesCount()

            return id

        }

        fun deleteFromDb(id: Long) {

            mDb = mDbHelper.writableDatabase

            mDb.delete(FeedEntry.TABLE_NAME, FeedEntry._ID + " = '" + id + "'", null)
            mDb.close()


        }

        //Retrieve the cursor with the elements from the database
        fun retrieveDeviceList(): Cursor {

            // Gets the data repository in read mode
            mDb = mDbHelper.readableDatabase

            val selectQuery = "SELECT * FROM " + FeedEntry.TABLE_NAME

            return mDb.rawQuery(selectQuery, null)
        }

        //Close database statically
        fun closeDb() {


            if (mDb.isOpen) mDb.close()

        }

        //Check if a printer exists to avoid multiple insertions
        fun checkExisting(m: ModelPrinter): Boolean {

            var exists = true

            // Gets the data repository in read mode
            mDb = mDbHelper.readableDatabase

            val selectQuery =
                "SELECT * FROM " + FeedEntry.TABLE_NAME + " WHERE " + FeedEntry.DEVICES_NAME + " = '" + m.name + "' AND " +
                        FeedEntry.DEVICES_ADDRESS + " = '" + m.address + "'"

            val c = mDb.rawQuery(selectQuery, null)

            exists = c.moveToFirst()

            closeDb()

            return exists

        }

        //update new position
        fun updateDB(tableName: String, id: Long, newValue: String) {

            mDb = mDbHelper.readableDatabase

            // New value for one column
            val values = ContentValues()
            values.put(tableName, newValue)

            val count = mDb.update(
                FeedEntry.TABLE_NAME, values,
                FeedEntry._ID + " = '" + id + "'", null
            )

            Log.i("OUT", "Updated: $count with $tableName updated with $newValue where $id")

            mDb.close()

        }

        fun deleteDB() {
            //TODO Database deletion for testing
            mContext.deleteDatabase("Devices.db")
        }

        fun count(): Int {

            val c = retrieveDeviceList()
            val count = c.count
            closeDb()
            return count
        }


        /*****************************************************************************************
         * SHARED PREFERENCES HANDLER
         */

        /**
         * Check if a file is favorite
         * @return
         */
        fun isPreference(where: String, key: String): Boolean {

            val prefs = mContext.getSharedPreferences(where, Context.MODE_PRIVATE)

            /*
        Map<String,?> keys = prefs.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            Log.i("map values",entry.getKey() + ": " +
                    entry.getValue().toString());
        }*/

            return if (prefs.contains(key)) true else false

        }

        /**
         * Get the list of favorites to add to the file list
         * @return
         */
        fun getPreferences(where: String): Map<String, *> {
            val prefs = mContext.getSharedPreferences(where, Context.MODE_PRIVATE)
            return prefs.all
        }

        /**
         * Get a single item from the list
         * @param where
         * @param key
         * @return
         */
        fun getPreference(where: String, key: String): String? {

            val prefs = mContext.getSharedPreferences(where, Context.MODE_PRIVATE)
            return prefs.getString(key, null)
        }

        /**
         * Set/remove as favorite using SharedPreferences, can't repeat names
         * The type of operation is switched by a boolean
         */
        fun handlePreference(where: String, key: String, value: String?, add: Boolean) {


            val prefs = mContext.getSharedPreferences(where, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            if (!add) {
                Log.i("OUT", "Removing $key")
                editor.remove(key)
            } else {
                Log.i("OUT", "Putting favorite $key")
                editor.putString(key, value)
            }

            editor.commit()

        }


        /***********************************************  HISTORY TABLE   */

        fun writeDBHistory(name: String, path: String, time: String, type: String, date: String) {

            // Gets the data repository in write mode
            mDb = mDbHelper.writableDatabase

            Log.i("OUT", "Adding: $name")

            // Create a new map of values, where column names are the keys
            val values = ContentValues()
            values.put(FeedEntry.HISTORY_MODEL, name)
            values.put(FeedEntry.HISTORY_PATH, path)
            values.put(FeedEntry.HISTORY_TIME, time)
            values.put(FeedEntry.HISTORY_PRINTER, type)
            values.put(FeedEntry.HISTORY_DATE, date)

            mDb.insert(FeedEntry.TABLE_HISTORY_NAME, null, values)
            mDb.close()

        }

        //Retrieve the cursor with the elements from the database
        fun retrieveHistory(): Cursor {

            // Gets the data repository in read mode
            mDb = mDbHelper.readableDatabase

            val selectQuery = "SELECT * FROM " + FeedEntry.TABLE_HISTORY_NAME

            return mDb.rawQuery(selectQuery, null)
        }

        fun updateHistoryPath(oldPath: String, newPath: String) {

            mDb = mDbHelper.readableDatabase

            // New value for one column
            val values = ContentValues()
            values.put(FeedEntry.HISTORY_PATH, newPath)

            mDb.update(
                FeedEntry.TABLE_HISTORY_NAME, values,
                FeedEntry.HISTORY_PATH + " = '" + oldPath + "'", null
            )


            mDb.close()

        }

        fun removeFromHistory(path: String) {
            mDb = mDbHelper.writableDatabase
            mDb.delete(FeedEntry.TABLE_HISTORY_NAME, FeedEntry.HISTORY_PATH + " = '" + path + "'", null)
            mDb.close()

        }
    }


}
