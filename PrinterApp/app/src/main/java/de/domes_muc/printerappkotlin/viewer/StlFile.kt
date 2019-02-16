package de.domes_muc.printerappkotlin.viewer

import android.app.AlertDialog
import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.library.LibraryController
import de.domes_muc.printerappkotlin.library.LibraryModelCreation
import de.domes_muc.printerappkotlin.viewer.Geometry.Point
import de.domes_muc.printerappkotlin.viewer.Geometry.Vector
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.opengl.Matrix
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.widget.LinearLayout
import android.widget.Toast

import com.alertdialogpro.ProgressDialogPro
import com.devsmart.android.IOUtils

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


class StlFile {

    internal var mStringAux = ""

    companion object {

        private val TAG = "gcode"

        private var mFile: File? = null

        private var mProgressDialog: ProgressDialogPro? = null
        private var mData: DataStorage? = null
        private var mContext: Context? = null

        private var mThread: Thread? = null
        private var mContinueThread = true

        private val COORDS_PER_TRIANGLE = 9
        private var mMode: Int = 0

        private val MAX_SIZE = 50000000 //50Mb


        fun openStlFile(context: Context, file: File, data: DataStorage, mode: Int) {
            Log.i(TAG, "Open STL File")

            mContext = context

            mMode = mode
            mContinueThread = true

            if (mMode != ViewerMainFragment.DO_SNAPSHOT)
                mProgressDialog = prepareProgressDialog(context)

            mData = data

            mFile = file
            val uri = Uri.fromFile(file)

            mData!!.pathFile = mFile!!.absolutePath
            mData!!.initMaxMin()


            startThreadToOpenFile(context, uri)


        }

        fun startThreadToOpenFile(context: Context, uri: Uri) {

            mThread = object : Thread() {
                override fun run() {
                    val arrayBytes = toByteArray(context, uri)

                    try {
                        if (isText(arrayBytes!!)) {
                            Log.e(TAG, "trying text... ")
                            if (mContinueThread) processText(mFile)
                        } else {
                            Log.e(TAG, "trying binary...")
                            if (mContinueThread) processBinary(arrayBytes)
                        }


                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (mContinueThread) mHandler.sendEmptyMessage(0)
                }
            }

            mThread!!.start()


        }


        private fun toByteArray(context: Context, filePath: Uri): ByteArray? {
            var inputStream: InputStream? = null
            var arrayBytes: ByteArray? = null
            try {
                inputStream = context.contentResolver.openInputStream(filePath)
                arrayBytes = IOUtils.toByteArray(inputStream!!)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    inputStream!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }

            return arrayBytes
        }

        private fun isText(bytes: ByteArray): Boolean {
            for (b in bytes) {
                if (b.toInt() == 0x0a || b.toInt() == 0x0d || b.toInt() == 0x09) {
                    // white spaces
                    continue
                }
                if (b.toInt() < 0x20 || 0xff and b.toInt() >= 0x80) {
                    // control codes
                    return false
                }
            }
            return true
        }


        /**
         * Progress Dialog
         * ----------------------------------
         */
        private fun prepareProgressDialog(context: Context): ProgressDialogPro {

            val progressDialog = ProgressDialogPro(context, R.style.Theme_AlertDialogPro_Material_Light_Green)
            progressDialog.setTitle(R.string.loading_stl)
            progressDialog.setMessage(context.resources.getString(R.string.be_patient))

            //val progressDialog = dialog
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

            progressDialog.setCancelable(false)
            progressDialog.setCanceledOnTouchOutside(false)
            if (mMode != ViewerMainFragment.DO_SNAPSHOT) {
                progressDialog.show()
                progressDialog.window!!.setLayout(500, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            return progressDialog
        }

        private fun getIntWithLittleEndian(bytes: ByteArray, offset: Int): Int {
            return 0xff and bytes[offset].toInt() or (0xff and bytes[offset + 1].toInt() shl 8) or (0xff and bytes[offset + 2].toInt() shl 16) or (0xff and bytes[offset + 3].toInt() shl 24)
        }

        private val mHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (mData!!.coordinateListSize < 1) {
                    Toast.makeText(mContext, R.string.error_opening_invalid_file, Toast.LENGTH_SHORT).show()
                    ViewerMainFragment.resetWhenCancel()
                    if (mMode != ViewerMainFragment.DO_SNAPSHOT) mProgressDialog!!.dismiss()
                    return
                }

                //only center again if it's a new file
                if (mFile!!.name.substring(0, 3).contains("tmp")) {
                    mData!!.fillVertexArray(true)
                } else
                    mData!!.fillVertexArray(true)
                mData!!.fillNormalArray()

                mData!!.clearNormalList()
                mData!!.clearVertexList()

                //Finish
                if (mMode == ViewerMainFragment.DONT_SNAPSHOT) {
                    ViewerMainFragment.draw()
                    ViewerMainFragment.doPress()
                    mProgressDialog!!.dismiss()

                    //TODO better filtering
                    if (!mFile!!.name.substring(0, 3).contains("tmp"))
                        ViewerMainFragment.slicingCallback()
                } else if (mMode == ViewerMainFragment.DO_SNAPSHOT) {
                    LibraryModelCreation.takeSnapshot()
                }
            }
        }

        private fun processText(file: File?) {
            try {
                var maxLines = 0
                val allLines = StringBuilder("")
                val countReader = BufferedReader(FileReader(file))

                val milis = SystemClock.currentThreadTimeMillis().toFloat()

                while (countReader.readLine()?.let {
                        if (it.trim { it <= ' ' }.startsWith("vertex ")) {
                            val line = it.replaceFirst("vertex ".toRegex(), "").trim { it <= ' ' }
                            allLines.append(line + "\n")
                            maxLines++
                            if (maxLines % 1000 == 0 && mMode != ViewerMainFragment.DO_SNAPSHOT)
                                mProgressDialog!!.max = maxLines
                        }
                    } != null && mContinueThread)

//                while ((line = countReader.readLine()) != null && mContinueThread) {
//                    if (line.trim { it <= ' ' }.startsWith("vertex ")) {
//                        line = line.replaceFirst("vertex ".toRegex(), "").trim { it <= ' ' }
//                        allLines.append(line + "\n")
//                        maxLines++
//                        if (maxLines % 1000 == 0 && mMode != ViewerMainFragment.DO_SNAPSHOT)
//                            mProgressDialog!!.max = maxLines
//                    }
//                }

                Log.i(TAG, "STL [Text] Read in: " + (SystemClock.currentThreadTimeMillis() - milis))

                if (mMode != ViewerMainFragment.DO_SNAPSHOT) mProgressDialog!!.max = maxLines

                countReader.close()


                var lines = 0

                var firstVertexIndex = 0
                var secondVertexIndex = 0
                var thirdVertexIndex = 0
                var initialVertexIndex = -1

                val milis2 = SystemClock.currentThreadTimeMillis().toFloat()

                while (lines < maxLines && mContinueThread) {
                    firstVertexIndex = allLines.indexOf("\n", thirdVertexIndex + 1)
                    secondVertexIndex = allLines.indexOf("\n", firstVertexIndex + 1)
                    thirdVertexIndex = allLines.indexOf("\n", secondVertexIndex + 1)

                    val line = allLines.substring(initialVertexIndex + 1, thirdVertexIndex)
                    initialVertexIndex = thirdVertexIndex

                    processTriangle(line)
                    lines += 3

                    if (lines % (maxLines / 10) == 0) {
                        if (mMode != ViewerMainFragment.DO_SNAPSHOT) mProgressDialog!!.progress = lines
                    }
                }

                Log.i(TAG, "STL [Text] Processed in: " + (SystemClock.currentThreadTimeMillis() - milis2))


            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        @Throws(Exception::class)
        fun processTriangle(line: String) {
            val vertex = line.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            var vertexValues = vertex[0].split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var x = java.lang.Float.parseFloat(vertexValues[0])
            var y = java.lang.Float.parseFloat(vertexValues[1])
            var z = java.lang.Float.parseFloat(vertexValues[2])
            val v0 = Vector(x, y, z)
            mData!!.adjustMaxMin(x, y, z)
            mData!!.addVertex(x)
            mData!!.addVertex(y)
            mData!!.addVertex(z)

            vertexValues = vertex[1].split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            x = java.lang.Float.parseFloat(vertexValues[0])
            y = java.lang.Float.parseFloat(vertexValues[1])
            z = java.lang.Float.parseFloat(vertexValues[2])
            val v1 = Vector(x, y, z)
            mData!!.adjustMaxMin(x, y, z)
            mData!!.addVertex(x)
            mData!!.addVertex(y)
            mData!!.addVertex(z)

            vertexValues = vertex[2].split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            x = java.lang.Float.parseFloat(vertexValues[0])
            y = java.lang.Float.parseFloat(vertexValues[1])
            z = java.lang.Float.parseFloat(vertexValues[2])
            val v2 = Vector(x, y, z)
            mData!!.adjustMaxMin(x, y, z)
            mData!!.addVertex(x)
            mData!!.addVertex(y)
            mData!!.addVertex(z)

            //Calculate triangle normal vector
            val normal = Vector.normalize(Vector.crossProduct(Vector.substract(v1, v0), Vector.substract(v2, v0)))

            mData!!.addNormal(normal.x)
            mData!!.addNormal(normal.y)
            mData!!.addNormal(normal.z)

        }

        @Throws(Exception::class)
        private fun processBinary(stlBytes: ByteArray?) {

            val vectorSize = getIntWithLittleEndian(stlBytes!!, 80)

            if (mMode != ViewerMainFragment.DO_SNAPSHOT) mProgressDialog!!.max = vectorSize

            val milis = SystemClock.currentThreadTimeMillis().toFloat()

            for (i in 0 until vectorSize) {
                if (!mContinueThread) break

                var x = java.lang.Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 12))
                var y = java.lang.Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 16))
                var z = java.lang.Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 20))
                val v0 = Vector(x, y, z)

                mData!!.adjustMaxMin(x, y, z)
                mData!!.addVertex(x)
                mData!!.addVertex(y)
                mData!!.addVertex(z)


                x = java.lang.Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 24))
                y = java.lang.Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 28))
                z = java.lang.Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 32))
                val v1 = Vector(x, y, z)

                mData!!.adjustMaxMin(x, y, z)
                mData!!.addVertex(x)
                mData!!.addVertex(y)
                mData!!.addVertex(z)

                x = java.lang.Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 36))
                y = java.lang.Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 40))
                z = java.lang.Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 44))
                val v2 = Vector(x, y, z)

                mData!!.adjustMaxMin(x, y, z)
                mData!!.addVertex(x)
                mData!!.addVertex(y)
                mData!!.addVertex(z)

                //Calculate triangle normal vector
                val normal = Vector.normalize(Vector.crossProduct(Vector.substract(v1, v0), Vector.substract(v2, v0)))

                mData!!.addNormal(normal.x)
                mData!!.addNormal(normal.y)
                mData!!.addNormal(normal.z)


                if (i % (vectorSize / 10) == 0) {
                    if (mMode != ViewerMainFragment.DO_SNAPSHOT) mProgressDialog!!.progress = i
                }
            }
            Log.i(TAG, "STL [BINARY] Read & Processed in: " + (SystemClock.currentThreadTimeMillis() - milis))

            Log.i(
                "Slicer", "Sizes: \n" +
                        "Width" + (mData!!.maxX - mData!!.minX) + "\n" +
                        "Depth" + (mData!!.maxY - mData!!.minY) + "\n" +
                        "Height" + (mData!!.maxZ - mData!!.minZ)
            )


        }

        private fun setTransformationVector(
            x: Float,
            y: Float,
            z: Float,
            rotationMatrix: FloatArray,
            scaleFactorX: Float,
            scaleFactorY: Float,
            scaleFactorZ: Float,
            adjustZ: Float,
            center: Point
        ): FloatArray {
            val vector = FloatArray(4)
            val result = FloatArray(4)

            vector[0] = x
            vector[1] = y
            vector[2] = z

            Matrix.multiplyMV(result, 0, rotationMatrix, 0, vector, 0)

            result[0] = result[0] * scaleFactorX + center.x
            result[1] = result[1] * scaleFactorY + center.y
            result[2] = result[2] * scaleFactorZ + center.z + adjustZ

            return result
        }

        fun checkIfNameExists(projectName: String): Boolean {
            val check = File(LibraryController.parentFolder.getAbsolutePath() + "/Files/" + projectName)
            return if (check.exists()) true else false

        }

        /**
         * This method will save the model to a file, either to slice or to make a new project.
         * I made a few adjustment to select between the two types of file creation. (Alberto)
         *
         * @param dataList
         * @param projectName
         */
        fun saveModel(dataList: List<DataStorage>, projectName: String, slicer: SlicingHandler?): Boolean {
            var coordinates: FloatArray? = null
            var coordinateCount = 0
            var rotationMatrix = FloatArray(16)
            var scaleFactorX = 0f
            var scaleFactorY = 0f
            var scaleFactorZ = 0f
            var center = Point(0f, 0f, 0f)
            var adjustZ = 0f
            var vector = FloatArray(3)

            //Calculating buffer size
            for (i in dataList.indices)
                coordinateCount += dataList[i].vertexArray!!.size

            if (coordinateCount == 0) {
                return false
            }

            //Each triangle has 3 vertex with 3 coordinates each. COORDS_PER_TRIANGLE = 9
            val normals = coordinateCount / COORDS_PER_TRIANGLE * 3 //number of normals coordinates in the file

            //The file consists of the header, the vertex and normal coordinates (4 bytes per component) and
            //a flag (2 bytes per triangle) to indicate the final of the triangle.
            val bb = ByteBuffer.allocate(84 + (coordinateCount + normals) * 4 + coordinateCount * 2)
            bb.order(ByteOrder.LITTLE_ENDIAN)

            //TODO Out of Memory when saving file to slice

            //Header
            val header = ByteArray(80)
            bb.put(header)

            //Number of triangles
            bb.putInt(coordinateCount / COORDS_PER_TRIANGLE)

            Log.i("Slicer", "Saving new model")

            for (i in dataList.indices) {
                val data = dataList[i]
                rotationMatrix = data.rotationMatrix
                scaleFactorX = data.lastScaleFactorX
                scaleFactorY = data.lastScaleFactorY
                scaleFactorZ = data.lastScaleFactorZ
                adjustZ = data.adjustZ
                center = data.lastCenter
                coordinates = data.vertexArray

                var j = 0
                while (j < coordinates!!.size) {

                    //Normal data. It is not necessary to store the info
                    bb.putFloat(0f)
                    bb.putFloat(0f)
                    bb.putFloat(0f)

                    //Triangle Data, 3 vertex with 3 coordinates (x,y,z) each one.
                    vector = setTransformationVector(
                        coordinates[j],
                        coordinates[j + 1],
                        coordinates[j + 2],
                        rotationMatrix,
                        scaleFactorX,
                        scaleFactorY,
                        scaleFactorZ,
                        adjustZ,
                        center
                    )
                    bb.putFloat(vector[0])
                    bb.putFloat(vector[1])
                    bb.putFloat(vector[2])

                    vector = setTransformationVector(
                        coordinates[j + 3],
                        coordinates[j + 4],
                        coordinates[j + 5],
                        rotationMatrix,
                        scaleFactorX,
                        scaleFactorY,
                        scaleFactorZ,
                        adjustZ,
                        center
                    )
                    bb.putFloat(vector[0])
                    bb.putFloat(vector[1])
                    bb.putFloat(vector[2])

                    vector = setTransformationVector(
                        coordinates[j + 6],
                        coordinates[j + 7],
                        coordinates[j + 8],
                        rotationMatrix,
                        scaleFactorX,
                        scaleFactorY,
                        scaleFactorZ,
                        adjustZ,
                        center
                    )
                    bb.putFloat(vector[0])
                    bb.putFloat(vector[1])
                    bb.putFloat(vector[2])

                    bb.putShort(0.toShort()) // end of triangle
                    j += 9
                }
            }

            bb.position(0)
            val data = bb.array()

            Log.i("Slicer", "Saved")

            if (slicer != null) {

                slicer.setData(data)
                //slicer.sendTimer();

            } else {
                val path = LibraryController.parentFolder.getAbsolutePath() + "/" + projectName + ".stl"
                try {
                    val fos = FileOutputStream(path)
                    fos.write(data)
                    fos.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val file = File(path)
                LibraryModelCreation.createFolderStructure(mContext, file)
                file.delete()
            }


            return true
        }

        /**
         * **********************************************************************************
         */

        /*
    Check file size or issue a notification
     */
        fun checkFileSize(file: File, context: Context): Boolean {

            return if (file.length() < MAX_SIZE)
                true
            else
                false

        }
    }

}