package de.domes_muc.printerappkotlin.devices.database

import de.domes_muc.printerappkotlin.devices.database.DeviceInfo.FeedEntry
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
        db.execSQL(SQL_CREATE_ENTRIES_HISTORY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {

        // If you change the database schema, you must increment the database version.
        val DATABASE_VERSION = 1
        val DATABASE_NAME = "Devices.db"

        private val TEXT_TYPE = " TEXT"
        private val COMMA_SEP = ","
        private val SQL_CREATE_ENTRIES = "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                FeedEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                FeedEntry.DEVICES_NAME + TEXT_TYPE + COMMA_SEP +
                FeedEntry.DEVICES_ADDRESS + TEXT_TYPE + COMMA_SEP +
                FeedEntry.DEVICES_POSITION + TEXT_TYPE + COMMA_SEP +
                FeedEntry.DEVICES_DISPLAY + TEXT_TYPE + COMMA_SEP +
                FeedEntry.DEVICES_TYPE + TEXT_TYPE + COMMA_SEP +
                FeedEntry.DEVICES_PROFILE + TEXT_TYPE + COMMA_SEP +
                FeedEntry.DEVICES_NETWORK + TEXT_TYPE + ")"


        private val SQL_CREATE_ENTRIES_HISTORY = "CREATE TABLE " + FeedEntry.TABLE_HISTORY_NAME + " (" +
                FeedEntry.HISTORY_MODEL + TEXT_TYPE + COMMA_SEP +
                FeedEntry.HISTORY_PATH + TEXT_TYPE + COMMA_SEP +
                FeedEntry.HISTORY_TIME + TEXT_TYPE + COMMA_SEP +
                FeedEntry.HISTORY_PRINTER + TEXT_TYPE + COMMA_SEP +
                FeedEntry.HISTORY_DATE + TEXT_TYPE + ")"


        private val SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME + COMMA_SEP + FeedEntry.TABLE_HISTORY_NAME
    }

}
