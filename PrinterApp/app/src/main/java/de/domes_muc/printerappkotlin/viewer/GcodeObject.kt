package de.domes_muc.printerappkotlin.viewer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.ArrayList

import de.domes_muc.printerappkotlin.Log
import android.content.Context
import android.opengl.GLES20

class GcodeObject(private val mData: DataStorage, context: Context) {

    private val vertexShaderCode = // This matrix member variable provides a hook to manipulate
    // the coordinates of the objects that use this vertex shader
        "uniform mat4 u_MVPMatrix;" +

                "attribute vec4 a_Position;" +
                "attribute vec4 a_Color;" +

                "varying vec4 v_Color;" +// This will be passed into the fragment shader.

                "void main() {" +
                "v_Color = a_Color ;" +
                // The matrix must be included as a modifier of gl_Position.
                // Note that the uMVPMatrix factor *must be first* in order
                // for the matrix multiplication product to be correct.
                "  gl_Position = u_MVPMatrix * a_Position;" +
                "}"

    private val fragmentShaderCode = "precision mediump float;" +
            "varying vec4 v_Color;" +
            "void main() {" +
            "  gl_FragColor = v_Color;" +
            "}"


    private val mProgram: Int
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0

    private val VERTEX_STRIDE = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    private val COLORS_STRIDE = COLORS_PER_VERTEX * 4 // 4 bytes per vertex


    internal var colorBlue = floatArrayOf(0.2f, 0.709803922f, 0.898039216f, 1.0f)
    internal var colorRed = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)
    internal var colorYellow = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f)
    internal var colorGreen = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)
    internal var colorWhite = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

    private val mVertexArray: FloatArray?
    private val mLayerArray: IntArray?
    private val mTypeArray: IntArray?
    private val mColorArray: FloatArray
    private var mLineLength = ArrayList<Int>()
    private val mVertexBuffer: FloatBuffer
    private val mColorBuffer: FloatBuffer

    private var mLayer: Int = 0

    var transparent: Boolean = false
    var xray: Boolean = false

    val colorArray: FloatArray
        get() {
            val list = ArrayList<Float>()
            var color: FloatArray? = null

            for (i in mTypeArray!!.indices) {
                when (mTypeArray[i]) {
                    DataStorage.WALL_INNER -> color = colorGreen
                    DataStorage.WALL_OUTER -> color = colorRed
                    DataStorage.FILL -> color = colorYellow
                    DataStorage.SKIRT -> color = colorGreen
                    DataStorage.SUPPORT -> color = colorBlue
                    else -> color = colorYellow
                }

                for (j in color.indices) list.add(color[j])

            }

            val finalColor = FloatArray(list.size)

            for (i in list.indices) {
                finalColor[i] = list[i]
            }

            return finalColor

        }

    init {
        this.mLayer = mData.actualLayer

        Log.i(TAG, "Creating GCode Object")

        mVertexArray = mData.vertexArray
        mLayerArray = mData.layerArray
        mTypeArray = mData.typeArray
        mLineLength = ArrayList(mData.lineLengthList)
        mColorArray = colorArray


        //Vertex buffer
        val vbb = ByteBuffer.allocateDirect(mVertexArray!!.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        mVertexBuffer = vbb.asFloatBuffer()
        mVertexBuffer.put(mVertexArray)
        mVertexBuffer.position(0)

        //Color buffer
        val cbb = ByteBuffer.allocateDirect(mColorArray.size * 4)
        cbb.order(ByteOrder.nativeOrder())
        mColorBuffer = cbb.asFloatBuffer()
        mColorBuffer.put(mColorArray)
        mColorBuffer.position(0)


        // prepare shaders and OpenGL program
        val vertexShader = ViewerRenderer.loadShader(
            GLES20.GL_VERTEX_SHADER,
            vertexShaderCode
        )
        val fragmentShader = ViewerRenderer.loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            fragmentShaderCode
        )

        mProgram = GLES20.glCreateProgram()             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader)   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program

        GLES20.glBindAttribLocation(mProgram, 0, "a_Position")
        GLES20.glBindAttribLocation(mProgram, 1, "a_Color")

        GLES20.glLinkProgram(mProgram)                  // create OpenGL program executables
    }

    /**
     * DRAW GCOde
     * ----------------------------------
     */

    fun draw(mvpMatrix: FloatArray) {
        mLayer = mData.actualLayer

        val layerMin = mLayer - LAYERS_TO_RENDER
        var vertexCount = 0
        var vertexCountMin = 0

        for (i in mLayerArray!!.indices)
            if (mLayerArray[i] <= mLayer)
                vertexCount++ //Total number of vertex from layer 0 to mLayer

        for (i in mLayerArray.indices)
            if (mLayerArray[i] <= layerMin)
                vertexCountMin++ //Total number of vertex from layer 0 to layerMin

        var length = 0
        GLES20.glUseProgram(mProgram)

        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_CONSTANT_COLOR)

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position")

        // Prepare the Vertex coordinate data
        GLES20.glVertexAttribPointer(
            mPositionHandle, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            VERTEX_STRIDE, mVertexBuffer
        )

        // Enable a handle to the facet vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle)

        mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color")
        ViewerRenderer.checkGlError("glGetAttribLocation")

        GLES20.glVertexAttribPointer(
            mColorHandle, COLORS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            COLORS_STRIDE, mColorBuffer
        )

        GLES20.glEnableVertexAttribArray(mColorHandle)


        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix")
        ViewerRenderer.checkGlError("glGetUniformLocation")

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        ViewerRenderer.checkGlError("glUniformMatrix4fv")


        for (i in mLineLength.indices) {
            if (mLineLength[i] > 1) {
                if (mTypeArray!![length] != DataStorage.FILL || length > vertexCountMin)
                    GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, length, mLineLength[i])
                if (length >= vertexCount) break
            }
            length += mLineLength[i]
        }

    }

    companion object {

        private val TAG = "GCodeObject"

        internal val COORDS_PER_VERTEX = 3
        internal val COLORS_PER_VERTEX = 4

        private val LAYERS_TO_RENDER = 50
    }
}
