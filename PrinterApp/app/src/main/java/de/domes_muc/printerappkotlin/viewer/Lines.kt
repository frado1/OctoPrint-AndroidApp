package de.domes_muc.printerappkotlin.viewer

import android.opengl.GLES20

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Created by alberto-baeza on 12/2/14.
 */
class Lines {

    private val vertexShaderCode = // This matrix member variable provides a hook to manipulate
    // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                // The matrix must be included as a modifier of gl_Position.
                // Note that the uMVPMatrix factor *must be first* in order
                // for the matrix multiplication product to be correct.
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}"

    private val fragmentShaderCode = "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}"


    internal val mVertexBuffer: FloatBuffer

    private val mDrawListBuffer: ShortBuffer
    internal val mProgram: Int
    internal var mPositionHandle: Int = 0
    internal var mColorHandle: Int = 0
    internal var mCoordsArray: FloatArray? = null
    internal var mCurrentColor: FloatArray = X_COLOR

    private var mMVPMatrixHandle: Int = 0

    // number of coordinates per vertex in this array
    internal val COORDS_PER_VERTEX = 3
    internal var lineCoords = FloatArray(6)
    internal var vertexCount: Int = 0
    internal var vertexStride = COORDS_PER_VERTEX * 4 // bytes per vertex
    // Set color with red, green, blue and alpha (opacity) values
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

    init {

        lineCoords[0] = 0.0f
        lineCoords[1] = 0.0f
        lineCoords[2] = 0.0f
        lineCoords[3] = 0.0f
        lineCoords[4] = 0.0f
        lineCoords[5] = 0.0f

        mCoordsArray = lineCoords

        vertexCount = mCoordsArray!!.size / COORDS_PER_VERTEX

        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(
            // (# of coordinate values * 4 bytes per float)
            mCoordsArray!!.size * 4
        )
        bb.order(ByteOrder.nativeOrder())
        mVertexBuffer = bb.asFloatBuffer()
        //mVertexBuffer.put(mCoordsArray);
        //mVertexBuffer.position(0);

        // initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect(
            // (# of coordinate values * 2 bytes per short)
            drawOrder.size * 2
        )
        dlb.order(ByteOrder.nativeOrder())
        mDrawListBuffer = dlb.asShortBuffer()
        mDrawListBuffer.put(drawOrder)
        mDrawListBuffer.position(0)


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
        GLES20.glLinkProgram(mProgram)                  // create OpenGL program executables
    }

    private fun drawXAxis(point: Geometry.Point, z: Float): FloatArray {

        val tempAxis = FloatArray(6)

        tempAxis[0] = point.x - 100
        tempAxis[1] = point.y
        tempAxis[2] = z
        tempAxis[3] = point.x + 100
        tempAxis[4] = point.y
        tempAxis[5] = z

        return tempAxis

    }

    private fun drawYAxis(point: Geometry.Point, z: Float): FloatArray {

        val tempAxis = FloatArray(6)

        tempAxis[0] = point.x
        tempAxis[1] = point.y - 100
        tempAxis[2] = z
        tempAxis[3] = point.x
        tempAxis[4] = point.y + 100
        tempAxis[5] = z

        return tempAxis

    }

    private fun drawZAxis(point: Geometry.Point, z: Float): FloatArray {

        val tempAxis = FloatArray(6)

        tempAxis[0] = point.x
        tempAxis[1] = point.y
        tempAxis[2] = z - 100
        tempAxis[3] = point.x
        tempAxis[4] = point.y
        tempAxis[5] = z + 100

        return tempAxis

    }

    fun draw(data: DataStorage, mvpMatrix: FloatArray, currentAxis: Int) {

        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram)

        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle)


        //Draw a different axis depending on the selected one
        when (currentAxis) {

            X_AXIS -> {
                mCoordsArray = drawXAxis(data.lastCenter, data.trueCenter.z)
                mCurrentColor = X_COLOR
            }
            Y_AXIS -> {
                mCoordsArray = drawYAxis(data.lastCenter, data.trueCenter.z)
                mCurrentColor = Y_COLOR
            }
            Z_AXIS -> {
                mCoordsArray = drawZAxis(data.lastCenter, data.trueCenter.z)
                mCurrentColor = Z_COLOR
            }
            else -> mCoordsArray = null
        }

        if (mCoordsArray != null) {

            mVertexBuffer.put(mCoordsArray)
            mVertexBuffer.position(0)


            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, mVertexBuffer
            )

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")

            // Set color for drawing the triangle
            GLES20.glUniform4fv(mColorHandle, 1, mCurrentColor, 0)

            // get handle to shape's transformation matrix
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
            ViewerRenderer.checkGlError("glGetUniformLocation")

            // Apply the projection and view transformation
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
            ViewerRenderer.checkGlError("glUniformMatrix4fv")

            GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount)

            val points = FloatArray(20)

            var i = 0
            val radius = 0.75.toFloat()

            var angle = 0f
            while (angle < 2 * Math.PI) {
                points[i] = radius * Math.cos(angle.toDouble()).toFloat()
                points[i] = radius * Math.sin(angle.toDouble()).toFloat()

                i++
                angle += 0.630f
            }

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount)


            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle)
        }


    }

    companion object {

        val X_AXIS = 0
        val Y_AXIS = 1
        val Z_AXIS = 2

        private val TRANSPARENCY = 0.5f

        private val X_COLOR = floatArrayOf(0.0f, 0.9f, 0.0f, TRANSPARENCY)
        private val Y_COLOR = floatArrayOf(1.0f, 0.0f, 0.0f, TRANSPARENCY)
        private val Z_COLOR = floatArrayOf(0.0f, 0.0f, 1.0f, TRANSPARENCY)
    }

}

