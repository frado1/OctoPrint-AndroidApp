package de.domes_muc.printerappkotlin

import android.content.Context

import com.afollestad.materialdialogs.MaterialDialog


/**
 * Temporary class to show dialogs from static classes
 * @author alberto-baeza
 */
class DialogController(private val mContext: Context) {

    /**
     * Display dialog
     * @param msg the message shown
     */
    fun displayDialog(msg: String) {

        MaterialDialog(mContext)
        .title(R.string.error)
        .icon(R.drawable.ic_warning_grey600_24dp)
        .message(text = msg)
        .positiveButton(R.string.ok)
        .show()
    }

}
