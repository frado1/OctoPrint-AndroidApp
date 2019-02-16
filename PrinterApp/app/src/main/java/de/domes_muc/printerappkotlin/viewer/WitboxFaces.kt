package de.domes_muc.printerappkotlin.viewer

import android.opengl.GLES20

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.ArrayList

class WitboxFaces
/*static float planeCoordsDown[] = {
        -WITBOX_LONG,  WITBOX_WITDH, 0,   // top left
        -WITBOX_LONG, -WITBOX_WITDH, 0,   // bottom left
         WITBOX_LONG, -WITBOX_WITDH, 0,   // bottom right
         WITBOX_LONG,  WITBOX_WITDH, 0 }; // top right

    static float planeCoordsBack[] = {
        -WITBOX_LONG,  WITBOX_WITDH, WITBOX_HEIGHT,   // top left
        -WITBOX_LONG,  WITBOX_WITDH, 0,   // bottom left
         WITBOX_LONG,  WITBOX_WITDH, 0,   // bottom right
         WITBOX_LONG,  WITBOX_WITDH, WITBOX_HEIGHT }; // top right

    static float planeCoordsRight[] = {
         WITBOX_LONG, -WITBOX_WITDH, WITBOX_HEIGHT,   // top left
         WITBOX_LONG, -WITBOX_WITDH, 0,   // bottom left
         WITBOX_LONG,  WITBOX_WITDH, 0,   // bottom right
         WITBOX_LONG,  WITBOX_WITDH, WITBOX_HEIGHT }; // top right

    static float planeCoordsLeft[] = {
        -WITBOX_LONG, -WITBOX_WITDH, WITBOX_HEIGHT,   // top left
        -WITBOX_LONG, -WITBOX_WITDH, 0,   // bottom left
        -WITBOX_LONG,  WITBOX_WITDH, 0,   // bottom right
        -WITBOX_LONG,  WITBOX_WITDH, WITBOX_HEIGHT }; // top right*/


/**
 * Sets up the drawing object data for use in an OpenGL ES context.
 *
 * Alberto: change alpha according to face
 */
    (var mType: Int, type: IntArray) {
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

    lateinit var mSizeArray: IntArray

    private var mVertexBuffer: FloatBuffer? = null
    private var mDrawListBuffer: ShortBuffer? = null

    private val mProgram: Int

    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0
    internal var lineCoordsList: List<Float> = ArrayList()
    private var mCoordsArray: FloatArray? = null
    internal var vertexCount: Int = 0
    internal val vertexStride = COORDS_PER_VERTEX * 4 // bytes per vertex

    //float color[] = {0.260784f, 0.460784f, 0.737255f, 0.6f };
    internal var color = floatArrayOf(1.0f, 1.0f, 1.0f, 0.6f)


    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

    private val planeCoordsDown: FloatArray? = null
    private val planeCoordsBack: FloatArray? = null
    private val planeCoordsRight: FloatArray? = null
    private val planeCoordsLeft: FloatArray? = null

    init {

        generatePlaneCoords(mType, type)

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
    }/*switch (face) {
    	case ViewerRenderer.DOWN:
    		mCoordsArray = planeCoordsDown;
    		break;
    	case ViewerRenderer.BACK:
    		mCoordsArray = planeCoordsBack;
            color[3] = 0.6f;
    		break;
    	case ViewerRenderer.RIGHT:
    		mCoordsArray = planeCoordsRight;
            color[3] = 0.5f;
    		break;
    	case ViewerRenderer.LEFT:
    		mCoordsArray = planeCoordsLeft;
            color[3] = 0.5f;
    		break;
    	}*/

    fun generatePlaneCoords(face: Int, type: IntArray) {


        mSizeArray = type


        when (face) {
            ViewerRenderer.DOWN -> mCoordsArray = floatArrayOf(
                (-mSizeArray[0]).toFloat(), mSizeArray[1].toFloat(), 0f, // top left
                (-mSizeArray[0]).toFloat(), (-mSizeArray[1]).toFloat(), 0f, // bottom left
                mSizeArray[0].toFloat(), (-mSizeArray[1]).toFloat(), 0f, // bottom right
                mSizeArray[0].toFloat(), mSizeArray[1].toFloat(), 0f
            ) // top right
            ViewerRenderer.BACK -> {
                mCoordsArray = floatArrayOf(
                    (-mSizeArray[0]).toFloat(), mSizeArray[1].toFloat(), mSizeArray[2].toFloat(), // top left
                    (-mSizeArray[0]).toFloat(), mSizeArray[1].toFloat(), 0f, // bottom left
                    mSizeArray[0].toFloat(), mSizeArray[1].toFloat(), 0f, // bottom right
                    mSizeArray[0].toFloat(), mSizeArray[1].toFloat(), mSizeArray[2].toFloat()
                ) // top right
                color[3] = 0.3f
            }
            ViewerRenderer.RIGHT -> {
                mCoordsArray = floatArrayOf(
                    mSizeArray[0].toFloat(), (-mSizeArray[1]).toFloat(), mSizeArray[2].toFloat(), // top left
                    mSizeArray[0].toFloat(), (-mSizeArray[1]).toFloat(), 0f, // bottom left
                    mSizeArray[0].toFloat(), mSizeArray[1].toFloat(), 0f, // bottom right
                    mSizeArray[0].toFloat(), mSizeArray[1].toFloat(), mSizeArray[2].toFloat()
                ) // top right
                color[3] = 0.35f
            }
            ViewerRenderer.LEFT -> {
                mCoordsArray = floatArrayOf(
                    (-mSizeArray[0]).toFloat(), (-mSizeArray[1]).toFloat(), mSizeArray[2].toFloat(), // top left
                    (-mSizeArray[0]).toFloat(), (-mSizeArray[1]).toFloat(), 0f, // bottom left
                    (-mSizeArray[0]).toFloat(), mSizeArray[1].toFloat(), 0f, // bottom right
                    (-mSizeArray[0]).toFloat(), mSizeArray[1].toFloat(), mSizeArray[2].toFloat()
                ) // top right

                color[3] = 0.35f
            }
            ViewerRenderer.FRONT -> {
                mCoordsArray = floatArrayOf(
                    (-mSizeArray[0]).toFloat(), (-mSizeArray[1]).toFloat(), mSizeArray[2].toFloat(), // top left
                    (-mSizeArray[0]).toFloat(), (-mSizeArray[1]).toFloat(), 0f, // bottom left
                    mSizeArray[0].toFloat(), (-mSizeArray[1]).toFloat(), 0f, // bottom right
                    mSizeArray[0].toFloat(), (-mSizeArray[1]).toFloat(), mSizeArray[2].toFloat()
                ) // top right

                color[3] = 0.3f
            }

            ViewerRenderer.TOP -> {
                mCoordsArray = floatArrayOf(
                    (-mSizeArray[0]).toFloat(), mSizeArray[1].toFloat(), mSizeArray[2].toFloat(), // top left
                    (-mSizeArray[0]).toFloat(), (-mSizeArray[1]).toFloat(), mSizeArray[2].toFloat(), // bottom left
                    mSizeArray[0].toFloat(), (-mSizeArray[1]).toFloat(), mSizeArray[2].toFloat(), // bottom right
                    mSizeArray[0].toFloat(), mSizeArray[1].toFloat(), mSizeArray[2].toFloat()
                ) // top right

                color[3] = 0.4f
            }
        }

        vertexCount = mCoordsArray!!.size / COORDS_PER_VERTEX

        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(
            // (# of coordinate values * 4 bytes per float)
            mCoordsArray!!.size * 4
        )
        bb.order(ByteOrder.nativeOrder())
        mVertexBuffer = bb.asFloatBuffer()
        mVertexBuffer!!.put(mCoordsArray)
        mVertexBuffer!!.position(0)

        // initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect(
            // (# of coordinate values * 2 bytes per short)
            drawOrder.size * 2
        )
        dlb.order(ByteOrder.nativeOrder())
        mDrawListBuffer = dlb.asShortBuffer()
        mDrawListBuffer!!.put(drawOrder)
        mDrawListBuffer!!.position(0)


    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    fun draw(mvpMatrix: FloatArray) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram)

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        //enable cull face to hide sides if they overlap with each other
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        //Since every wall is facing the same side, we need to treat them differently
        when (mType) {

            ViewerRenderer.RIGHT, ViewerRenderer.FRONT, ViewerRenderer.TOP ->

                GLES20.glCullFace(GLES20.GL_FRONT)

            ViewerRenderer.BACK, ViewerRenderer.LEFT ->


                GLES20.glCullFace(GLES20.GL_BACK)
        }

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
            mPositionHandle, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false, vertexStride, mVertexBuffer
        )

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0)

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        ViewerRenderer.checkGlError("glGetUniformLocation")

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        ViewerRenderer.checkGlError("glUniformMatrix4fv")

        // Draw the square
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer
        )


        /*******
         * Draw the boders of the plate
         */

        //Disable depth test to draw lines always in the front
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        //new color
        val color2 = floatArrayOf(1.0f, 1.0f, 1.0f, 0.01f)
        GLES20.glUniform4fv(mColorHandle, 1, color2, 0)

        //Draw as a line loop
        GLES20.glLineWidth(3f)
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, vertexCount)

        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)


    }

    companion object {

        /*
     public static int WITBOX_WITDH = 105;
	public static int WITBOX_HEIGHT = 200;
	public static int WITBOX_LONG = 148;
     */

        val TYPE_WITBOX = 0
        val TYPE_HEPHESTOS = 1
        val TYPE_CUSTOM = 2

        var WITBOX_WITDH = 105
        var WITBOX_HEIGHT = 200
        var WITBOX_LONG = 148

        var HEPHESTOS_WITDH = 105
        var HEPHESTOS_HEIGHT = 180
        var HEPHESTOS_LONG = 108

        // number of coordinates per vertex in this array
        internal val COORDS_PER_VERTEX = 3
    }
}

