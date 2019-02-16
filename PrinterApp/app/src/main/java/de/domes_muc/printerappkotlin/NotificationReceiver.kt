package de.domes_muc.printerappkotlin

import android.app.NotificationManager
import android.app.PendingIntent
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.model.ModelPrinter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * This class will handle asynchronous notifications from the server when a printing is finished
 * and the app is listening. Declared globally in the manifest to avoid leaking intents
 *
 * Created by alberto-baeza on 11/20/14.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Gets an instance of the NotificationManager service
        val mNotifyMgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val resultIntent = Intent(context, SplashScreenActivity::class.java)
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.

        val resultPendingIntent = PendingIntent.getActivity(
            context,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )


        val id = intent.getLongExtra("printer", 0)
        val progress = intent.getIntExtra("progress", 0)

        //Target printer
        val p = DevicesListController.getPrinter(id)


        // Sets an ID for the notification
        val mNotificationId = id.toInt()

        if (!isForeground) {

            try {

                var text: String? = null

                val type = intent.getStringExtra("type")

                if (type == "finish") text = context.getString(R.string.finish_dialog_title) + " " + p!!.job.filename
                if (type == "print") text = context.getString(R.string.notification_printing_progress)
                if (type == "slice") text = context.getString(R.string.notification_slicing_progress)


                //Creates notification
                val mBuilder = NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.notification_logo)
                    .setContentTitle(p!!.displayName)
                    .setContentText("$text ($progress%)")
                    .setAutoCancel(true)

                mBuilder.setContentIntent(resultPendingIntent)


                // Builds the notification and issues it.
                mNotifyMgr.notify(mNotificationId, mBuilder.build())

            } catch (e: NullPointerException) {

                e.printStackTrace()
            }

        } else {

            mNotifyMgr.cancel(mNotificationId)


        }


    }

    companion object {

        private var isForeground = true

        //Change if the application goes background
        fun setForeground(foreground: Boolean) {
            isForeground = foreground

        }
    }
}
