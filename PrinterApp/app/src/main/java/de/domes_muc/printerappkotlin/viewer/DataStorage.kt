package de.domes_muc.printerappkotlin.viewer

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.viewer.Geometry.Point
import android.opengl.Matrix

import java.util.ArrayList

class DataStorage {
    private val mVertexList = ArrayList<Float>()
    private val mNormalList = ArrayList<Float>()
    private val mLineLengthList = ArrayList<Int>()
    private val mLayerList = ArrayList<Int>()
    private val mTypeList = ArrayList<Int>()

    var vertexArray: FloatArray? = null
        private set
    var normalArray: FloatArray? = null
        private set
    var layerArray: IntArray? = null
        private set
    var typeArray: IntArray? = null
        private set

    private var mMaxLayer: Int = 0
    var actualLayer: Int = 0
    var maxLinesFile: Int = 0

    var minX: Float = 0.toFloat()
    var maxX: Float = 0.toFloat()
    var minY: Float = 0.toFloat()
    var maxY: Float = 0.toFloat()
    var minZ: Float = 0.toFloat()
    var maxZ: Float = 0.toFloat()

    var pathFile: String? = null
    var pathSnapshot: String? = null

    var rotationMatrix = FloatArray(16)
        set(m) {
            for (i in rotationMatrix.indices) {
                rotationMatrix[i] = m[i]
            }
        }
    var modelMatrix = FloatArray(16)
        set(m) {
            for (i in modelMatrix.indices) {
                modelMatrix[i] = m[i]
            }
        }
    var lastScaleFactorX = 1.0f
    var lastScaleFactorY = 1.0f
    var lastScaleFactorZ = 1.0f
    /************************* EDITION INFORMATION  */
    var lastCenter = Point(0f, 0f, 0f)
    var stateObject: Int = 0
    var adjustZ: Float = 0.toFloat()

    val coordinateListSize: Int
        get() = mVertexList.size

    val lineLengthList: List<Int>
        get() = mLineLengthList

    val typeListSize: Int
        get() = mTypeList.size

    var maxLayer: Int
        get() = mMaxLayer
        set(maxLayer) {
            mMaxLayer = maxLayer
            actualLayer = maxLayer
        }

    val height: Float
        get() = maxZ - minZ

    val width: Float
        get() = maxY - minY

    val long: Float
        get() = maxX - minX

    val trueCenter: Point
        get() {

            val x = (maxX + minX) / 2
            val y = (maxY + minY) / 2
            val z = (maxZ + minZ) / 2

            return Point(x, y, z)

        }

    init {
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.setIdentityM(modelMatrix, 0)
    }


    fun copyData(d: DataStorage) {
        for (i in 0 until d.lineLengthList.size) mLineLengthList.add(d.lineLengthList[i])

//        vertexArray = FloatArray(d.vertexArray!!.size)
//        for (i in 0 until d.vertexArray!!.size) {
//            vertexArray[i] = d.vertexArray!![i]
//        }
       vertexArray = d.normalArray!!.copyOf()

//        normalArray = FloatArray(d.normalArray!!.size)
//        for (i in 0 until d.normalArray!!.size) {
//            normalArray[i] = d.normalArray!![i]
//        }
        normalArray = d.normalArray!!.copyOf()

        mMaxLayer = d.maxLayer
        actualLayer = d.actualLayer
        minX = d.minX
        minY = d.minY
        minZ = d.minZ
        maxX = d.maxX
        maxY = d.maxY
        maxZ = d.maxZ

        pathFile = d.pathFile
        pathSnapshot = d.pathSnapshot

        lastScaleFactorX = d.lastScaleFactorX
        lastScaleFactorY = d.lastScaleFactorY
        lastScaleFactorZ = d.lastScaleFactorZ

        adjustZ = d.adjustZ

        lastCenter = Point(d.lastCenter.x, d.lastCenter.y, d.lastCenter.z)

        for (i in rotationMatrix.indices) rotationMatrix[i] = d.rotationMatrix[i]
        for (i in modelMatrix.indices) modelMatrix[i] = d.modelMatrix[i]
    }

    fun addVertex(v: Float) {
        mVertexList.add(v)
    }

    fun addLayer(layer: Int) {
        mLayerList.add(layer)
    }

    fun addType(type: Int) {
        mTypeList.add(type)
    }

    fun addNormal(normal: Float) {
        mNormalList.add(normal)
    }


    fun addLineLength(length: Int) {
        mLineLengthList.add(length)
    }

    fun fillVertexArray(center: Boolean) {
        vertexArray = FloatArray(mVertexList.size)

        centerSTL(center)
    }

    fun initMaxMin() {
        maxX = -java.lang.Float.MAX_VALUE
        maxY = -java.lang.Float.MAX_VALUE
        maxZ = -java.lang.Float.MAX_VALUE
        minX = java.lang.Float.MAX_VALUE
        minY = java.lang.Float.MAX_VALUE
        minZ = java.lang.Float.MAX_VALUE
    }

    fun centerSTL(center: Boolean) {

        var distX = 0f
        var distY = 0f
        var distZ = minZ

        if (center) {

            distX = minX + (maxX - minX) / 2
            distY = minY + (maxY - minY) / 2

            //Show the model slightly above the plate
            distZ = minZ - MIN_Z.toFloat()

        }



        Log.i("PrintView", distZ.toString() + "")

        var i = 0
        while (i < mVertexList.size) {
            vertexArray!![i] = mVertexList[i] - distX
            vertexArray!![i + 1] = mVertexList[i + 1] - distY
            vertexArray!![i + 2] = mVertexList[i + 2] - distZ
            i = i + 3
        }

        //Adjust max, min
        minX = minX - distX
        maxX = maxX - distX
        minY = minY - distY
        maxY = maxY - distY
        minZ = minZ - distZ
        maxZ = maxZ - distZ
    }

    fun fillNormalArray() {
        normalArray = FloatArray(mNormalList.size * TRIANGLE_VERTEX)
        var index = 0

        var x: Float
        var y: Float
        var z: Float

        var i = 0
        while (i < mNormalList.size) {
            x = mNormalList[i]
            y = mNormalList[i + 1]
            z = mNormalList[i + 2]

            for (j in 0 until TRIANGLE_VERTEX) {
                normalArray!![index] = x
                normalArray!![index + 1] = y
                normalArray!![index + 2] = z
                index += 3
            }
            i += 3

        }
    }

    fun fillLayerArray() {
        layerArray = IntArray(mLayerList.size)

        for (i in mLayerList.indices) {
            layerArray!![i] = mLayerList[i]
        }
    }

    fun fillTypeArray() {
        typeArray = IntArray(mTypeList.size)

        for (i in mTypeList.indices) {
            typeArray!![i] = mTypeList[i]
        }
    }

    fun clearVertexList() {
        mVertexList.clear()
    }

    fun clearNormalList() {
        mNormalList.clear()
    }

    fun clearLayerList() {
        mLayerList.clear()
    }

    fun clearTypeList() {
        mTypeList.clear()
    }

    fun changeTypeAtIndex(index: Int, type: Int) {
        mTypeList[index] = type
    }

    fun adjustMaxMin(x: Float, y: Float, z: Float) {
        if (x > maxX) {
            maxX = x
        }
        if (y > maxY) {
            maxY = y
        }

        if (z > maxZ) {
            maxZ = z
        }
        if (x < minX) {
            minX = x
        }
        if (y < minY) {
            minY = y
        }
        if (z < minZ) {
            minZ = z
        }
    }

    companion object {

        val MOVE = 0
        val FILL = 1
        val PERIMETER = 2
        val RETRACT = 3
        val COMPENSATE = 4
        val BRIDGE = 5
        val SKIRT = 6
        val WALL_INNER = 7
        val WALL_OUTER = 8
        val SUPPORT = 9

        val TRIANGLE_VERTEX = 3

        val MIN_Z = 0.1
    }
}