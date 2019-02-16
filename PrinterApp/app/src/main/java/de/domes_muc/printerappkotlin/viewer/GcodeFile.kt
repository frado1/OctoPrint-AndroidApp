package de.domes_muc.printerappkotlin.viewer

import android.annotation.SuppressLint
import android.app.AlertDialog
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.devices.printview.PrintViewFragment
import de.domes_muc.printerappkotlin.library.LibraryModelCreation
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.widget.LinearLayout

import com.alertdialogpro.ProgressDialogPro

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class GcodeFile {

    fun saveNameFile() {
        mData!!.pathFile = mFile!!.name.replace(".", "-")
    }

    companion object {
        private val TAG = "gcode"
        val COORDS_PER_VERTEX = 3
        private val mContext: Context? = null
        private var mFile: File? = null
        private var mData: DataStorage? = null

        private var mProgressDialog: ProgressDialogPro? = null
        private var mThread: Thread? = null

        private var mSplitLine: Array<String>? = null
        private var mMaxLayer: Int = 0

        private var mMode = 0

        private var mContinueThread = true

        fun openGcodeFile(context: Context, file: File, data: DataStorage, mode: Int) {
            Log.i(TAG, " Open GcodeFile ")
            mFile = file
            mData = data
            mMode = mode
            mContinueThread = true
            if (mMode != ViewerMainFragment.DO_SNAPSHOT) mProgressDialog = prepareProgressDialog(context)

            mData!!.pathFile = mFile!!.absolutePath
            mData!!.initMaxMin()
            mMaxLayer = -1
            startThreadToOpenFile(context)
        }

        fun startThreadToOpenFile(context: Context) {
            mThread = object : Thread() {
                override fun run() {
                    var line: String
                    val allLines = StringBuilder("")

                    try {
                        var maxLines = 0
                        val countReader = BufferedReader(FileReader(mFile))

                        val milis = SystemClock.currentThreadTimeMillis().toFloat()

                        while (countReader.readLine()?.let {
                                allLines.append(it + "\n")
                                maxLines++
                                it
                                } != null && mContinueThread);
                        countReader.close()

                        Log.i(TAG, "GCODE Read in: " + (SystemClock.currentThreadTimeMillis() - milis))


                        if (mMode == ViewerMainFragment.PRINT_PREVIEW) mData!!.maxLinesFile = maxLines
                        if (mMode != ViewerMainFragment.DO_SNAPSHOT) mProgressDialog!!.max = maxLines
                        if (mContinueThread) processGcode(allLines, maxLines)

                        if (mContinueThread) mHandler.sendEmptyMessage(0)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }

            mThread!!.start()
        }

        private fun prepareProgressDialog(context: Context): ProgressDialogPro {

            val dialog = ProgressDialogPro(context, R.style.Theme_AlertDialogPro_Material_Light_Green)
            dialog.setTitle(R.string.loading_gcode)
            dialog.setMessage(context.resources.getString(R.string.be_patient))

            val progressDialog = dialog
            progressDialog.setProgressStyle(ProgressDialogPro.STYLE_HORIZONTAL)
            progressDialog.isIndeterminate = false

            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { dialog, which ->
                mContinueThread = false
                try {
                    mThread!!.join()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                ViewerMainFragment.resetWhenCancel()
            }

            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            if (mMode != ViewerMainFragment.DO_SNAPSHOT) {
                dialog.show()
                dialog.window!!.setLayout(500, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            return progressDialog
        }

        @SuppressLint("DefaultLocale")
        fun processGcode(allLines: StringBuilder, maxLines: Int) {
            var index = 0
            var lastIndex = 0
            var lines = 0
            var line = ""

            var end = false
            var start = false
            var x = 0f
            var y = 0f
            var z = 0f
            var type = -1
            var length = 0
            var layer = 0

            val milis = SystemClock.currentThreadTimeMillis().toFloat()

            //Default plate size for printview panel
            var auxPlate = intArrayOf(WitboxFaces.WITBOX_LONG, WitboxFaces.WITBOX_WITDH, WitboxFaces.WITBOX_HEIGHT)

            if (ViewerMainFragment.currentPlate != null)
                auxPlate = ViewerMainFragment.currentPlate

            while (lines < maxLines && mContinueThread) {

                //Log.i("gcode","Processing!! " + lines);
                index = allLines.indexOf("\n", lastIndex)
                line = allLines.substring(lastIndex, index)

                if (line.contains("END GCODE")) end = true

                //if (line.contains("end of START GCODE")) start = true;
                if (line.toLowerCase().contains("layer count")) start = true

                if (line.contains("MOVE")) {
                    type = DataStorage.MOVE
                } else if (line.contains("FILL")) {
                    type = DataStorage.FILL
                } else if (line.contains("PERIMETER")) {
                    type = DataStorage.PERIMETER
                } else if (line.contains("RETRACT")) {
                    type = DataStorage.RETRACT
                } else if (line.contains("COMPENSATE")) {
                    type = DataStorage.COMPENSATE
                } else if (line.contains("BRIDGE")) {
                    type = DataStorage.BRIDGE
                } else if (line.contains("SKIRT")) {
                    type = DataStorage.SKIRT
                } else if (line.contains("WALL-INNER")) {
                    type = DataStorage.WALL_INNER
                } else if (line.contains("WALL-OUTER")) {
                    type = DataStorage.WALL_OUTER
                } else if (line.contains("SUPPORT")) {
                    type = DataStorage.SUPPORT
                }

                //From comments
                if (line.contains("LAYER")) {
                    val pos = line.indexOf(":")
                    layer = Integer.parseInt(line.substring(pos + 1, line.length))
                }

                if (line.startsWith("G0") || line.startsWith("G1")) {
                    mSplitLine = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                    //Get the coord of the line
                    for (i in mSplitLine!!.indices) {
                        if (mSplitLine!![i].length <= 1) continue
                        if (mSplitLine!![i].startsWith("X")) {
                            mSplitLine!![i] = mSplitLine!![i].replace("X", "")
                            x = java.lang.Float.valueOf(mSplitLine!![i]) - auxPlate[0] //TODO
                        } else if (mSplitLine!![i].startsWith("Y")) {
                            mSplitLine!![i] = mSplitLine!![i].replace("Y", "")
                            y = java.lang.Float.valueOf(mSplitLine!![i]) - auxPlate[1] //TODO
                        } else if (mSplitLine!![i].startsWith("Z")) {
                            mSplitLine!![i] = mSplitLine!![i].replace("Z", "")
                            z = java.lang.Float.valueOf(mSplitLine!![i])
                        }
                    }

                    if (line.startsWith("G0")) {
                        mData!!.addLineLength(length)
                        length = 1

                    } else if (line.startsWith("G1")) {
                        //GCode saves the movement from one type to another (i.e wall_inner-wall_outer) in the list of the previous type.
                        //If we have just started a line, we set again the colour of the first vertex to avoid wrong colour
                        //This avoids gradients in rendering.
                        if (length == 1) mData!!.changeTypeAtIndex(mData!!.typeListSize - 1, type)

                        length++


                        if (start && !end) mData!!.adjustMaxMin(x, y, z)
                    }

                    mData!!.addVertex(x)
                    mData!!.addVertex(y)
                    mData!!.addVertex(z)
                    mData!!.addLayer(layer)
                    mData!!.addType(type)

                    if (layer > mMaxLayer) mMaxLayer = layer

                }

                lines++
                lastIndex = index + 1

                if (mMode != ViewerMainFragment.DO_SNAPSHOT && lines % (maxLines / 10) == 0) mProgressDialog!!.progress =
                        lines
            }

            Log.i(TAG, "GCODE Processed in: " + (SystemClock.currentThreadTimeMillis() - milis))
        }

        private val mHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (mData!!.coordinateListSize < 1) {

                    /**
                     * If there is an invalid gcode, breaks here for some reason.
                     *
                     * Alberto
                     */
                    //Toast.makeText(mContext, R.string.error_opening_invalid_file, Toast.LENGTH_SHORT).show();
                    ViewerMainFragment.resetWhenCancel()
                    if (mMode != ViewerMainFragment.DO_SNAPSHOT) mProgressDialog!!.dismiss()

                    return
                }

                mData!!.maxLayer = mMaxLayer

                mData!!.fillVertexArray(false)
                mData!!.fillTypeArray()
                mData!!.fillLayerArray()

                mData!!.clearVertexList()
                mData!!.clearLayerList()
                mData!!.clearTypeList()


                if (mMode == ViewerMainFragment.DONT_SNAPSHOT) {
                    ViewerMainFragment.initSeekBar(mMaxLayer)
                    ViewerMainFragment.draw()
                    mProgressDialog!!.dismiss()
                } else if (mMode == ViewerMainFragment.PRINT_PREVIEW) {
                    PrintViewFragment.drawPrintView()
                    mProgressDialog!!.dismiss()
                } else if (mMode == ViewerMainFragment.DO_SNAPSHOT) {
                    LibraryModelCreation.takeSnapshot()
                }
            }
        }
    }
}