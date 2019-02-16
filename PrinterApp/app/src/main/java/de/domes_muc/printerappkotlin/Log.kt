package de.domes_muc.printerappkotlin


/**
 * Created by alberto-baeza on 2/13/15.
 */
object Log {

    private val isLogsEnabled = true

    fun i(logTag: String, logString: String) {

        if (isLogsEnabled) {
            android.util.Log.i(logTag, logString)
        }
    }

    fun v(logTag: String, logString: String) {

        if (isLogsEnabled) {
            android.util.Log.v(logTag, logString)
        }
    }

    fun e(logTag: String, logString: String) {

        if (isLogsEnabled) {
            android.util.Log.e(logTag, logString)
        }
    }

    fun d(logTag: String, logString: String) {

        if (isLogsEnabled) {
            android.util.Log.d(logTag, logString)
        }
    }

    fun w(logTag: String, logString: String) {

        if (isLogsEnabled) {
            android.util.Log.w(logTag, logString)
        }
    }


}
