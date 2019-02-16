package de.domes_muc.printerappkotlin.viewer

import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.ArrayList


class WitboxPlate
/**
 * Sets up the drawing object data for use in an OpenGL ES context.
 */
    (private val mContext: Context, infinite: Boolean, type: IntArray) {
    private val vertexShaderCode = (
            "uniform mat4 u_MVPMatrix;      \n"        // A constant representing the combined model/view/projection matrix.

                    + "uniform mat4 u_MVMatrix;       \n"        // A constant representing the combined model/view matrix.


                    + "uniform vec4 a_Color;          \n"        // Color information we will pass in.

                    + "attribute vec4 a_Position;     \n"        // Per-vertex position information we will pass in.

                    + "attribute vec3 a_Normal;       \n"        // Per-vertex normal information we will pass in.

                    + "attribute vec2 a_TexCoordinate;\n"

                    + "varying vec4 v_Color;          \n"        // This will be passed into the fragment shader.

                    + "varying vec3 v_Position;		\n"
                    + "varying vec3 v_Normal;			\n"
                    + "varying vec2 v_TexCoordinate;	\n"

                    + "void main()                    \n"    // The entry point for our vertex shader.

                    + "{                              \n"
                    // Transform the vertex into eye space.
                    + "	v_Position = vec3(u_MVMatrix*a_Position);							\n"
                    // Pass through the color.
                    + "   v_Color = a_Color;  												\n"
                    // Pass through the texture coordinate.
                    + "	v_TexCoordinate = a_TexCoordinate;    								\n"
                    // Transform the normal's orientation into eye space.
                    + "	v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));					\n"
                    // gl_Position is a special variable used to store the final position.
                    // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
                    + "   gl_Position = u_MVPMatrix * a_Position;                            \n"
                    + "}                                                                     \n")


    private val fragmentShaderCode = (
            "precision mediump float;       \n"        // Set the default precision to medium. We don't need as high of a precision in the fragment shader.


                    + "uniform vec3 u_LightPos;		\n"    // The position of the light in eye space.

                    + "uniform sampler2D u_Texture; \n"        // The input texture.


                    + "varying vec3 v_Position;		\n"        // Interpolated position for this fragment.

                    + "varying vec4 v_Color;        \n"        // This is the color from the vertex shader interpolated across the triangle per fragment.

                    + "varying vec3 v_Normal;       \n"        // Interpolated normal for this fragment.

                    + "varying vec2 v_TexCoordinate;\n"     // Interpolated texture coordinate per fragment.

                    + "void main()					\n"
                    + "{                            \n"
                    // Will be used for attenuation.
                    + "	float distance = length(u_LightPos - v_Position); 							\n"

                    // Get a lighting direction vector from the light to the vertex.
                    + "	vec3 lightVector = normalize(u_LightPos - v_Position);						\n"

                    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are pointing in the same direction then it will get max illumination.
                    + " 	float diffuse = max(dot(v_Normal, lightVector), 0.0);						\n"

                    // Add attenuation. Alberto: Removed to show the texture original color
                    //diffuse * (1.0 / (1.0 + (0.10 * distance)));
                    // Add ambient lighting
                    + "	diffuse = 0.8;													\n"

                    // Multiply the color by the diffuse illumination level and texture value to get final output color.
                    + "	gl_FragColor = (v_Color * diffuse * texture2D(u_Texture, v_TexCoordinate));	\n"
                    + "}               																\n")

    private var mVertexBuffer: FloatBuffer? = null
    private var mOrderListBuffer: ShortBuffer? = null
    private var mTextureBuffer: FloatBuffer? = null
    private var mNormalBuffer: FloatBuffer? = null


    private val mProgram: Int

    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mNormalHandle: Int = 0
    private val mTextureDataHandle: Int
    private var mTextureUniformHandle: Int = 0
    private var mTextureCoordHandle: Int = 0
    private var mMVPMatrixHandle: Int = 0
    private var mMVMatrixHandle: Int = 0

    internal var lineCoordsList: List<Float> = ArrayList()

    //float mColor[] = {0.260784f, 0.460784f, 0.737255f, 0.6f };
    internal var mColor = floatArrayOf(1.0f, 1.0f, 1.0f, 0.5f)

    private val mDrawOrder = shortArrayOf(0, 1, 3, 1, 2, 3) // order to draw vertices

    private var mCoordsArray: FloatArray? = null


    init {

        generatePlaneCoords(type, infinite)

        mTextureDataHandle = loadTexture(mContext, R.drawable.witbox_plate)

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

    fun generatePlaneCoords(type: IntArray, infinite: Boolean) {


        val auxPlane: FloatArray

        if (infinite)
            auxPlane = mCoordsInfiniteArray
        else {

            auxPlane = floatArrayOf(
                (-type[0]).toFloat(), type[1].toFloat(), 0f, // top left
                (-type[0]).toFloat(), (-type[1]).toFloat(), 0f, // bottom left
                type[0].toFloat(), (-type[1]).toFloat(), 0f, // bottom right
                type[0].toFloat(), type[1].toFloat(), 0f    // top right
            )

        }



        this.mCoordsArray = auxPlane

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
            mDrawOrder.size * 2
        )
        dlb.order(ByteOrder.nativeOrder())
        mOrderListBuffer = dlb.asShortBuffer()
        mOrderListBuffer!!.put(mDrawOrder)
        mOrderListBuffer!!.position(0)

        val tb = ByteBuffer.allocateDirect(
            // (# of coordinate values * 4 bytes per float)
            mTextureCoordinateData.size * 4
        )
        tb.order(ByteOrder.nativeOrder())
        mTextureBuffer = tb.asFloatBuffer()
        mTextureBuffer!!.put(mTextureCoordinateData)
        mTextureBuffer!!.position(0)

        //Normal buffer
        val nbb = ByteBuffer.allocateDirect(mNormalData.size * 4)
        nbb.order(ByteOrder.nativeOrder())
        mNormalBuffer = nbb.asFloatBuffer()
        mNormalBuffer!!.put(mNormalData)
        mNormalBuffer!!.position(0)

    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    fun draw(mvpMatrix: FloatArray, mvMatrix: FloatArray) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram)

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)


        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate")
        ViewerRenderer.checkGlError("glGetAttribLocation Texture Coord Handle")

        GLES20.glVertexAttribPointer(
            mTextureCoordHandle, COORDS_PER_TEXTURE,
            GLES20.GL_FLOAT, false,
            0, mTextureBuffer
        )

        GLES20.glEnableVertexAttribArray(mTextureCoordHandle)
        ViewerRenderer.checkGlError("glGetAttribLocation Texture Coord Handle")


        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture")

        ViewerRenderer.checkGlError("glGetUniformLocation Texture Uniform Handle")

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle)

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0)

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position")
        ViewerRenderer.checkGlError("glGetAttribLocation Position Handle")


        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
            mPositionHandle, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            0, mVertexBuffer
        )

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "a_Color")

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, mColor, 0)

        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal")
        ViewerRenderer.checkGlError("glGetAttribLocation Normal Handle")

        // Pass in the normal information
        GLES20.glVertexAttribPointer(
            mNormalHandle, COORDS_PER_NORMAL,
            GLES20.GL_FLOAT, false,
            0, mNormalBuffer
        )

        GLES20.glEnableVertexAttribArray(mNormalHandle)

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix")

        try {
            ViewerRenderer.checkGlError("glGetUniformLocation") //TODO error
        } catch (e: RuntimeException) {

            //Catches runtime exception on Nexus tablets
        }


        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        ViewerRenderer.checkGlError("glUniformMatrix4fv")

        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVMatrix")
        ViewerRenderer.checkGlError("glGetUniformLocation")

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0)
        ViewerRenderer.checkGlError("glUniformMatrix4fv")


        // Draw the square
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, mDrawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, mOrderListBuffer
        )

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

    companion object {


        // number of coordinates per vertex in this array
        internal val COORDS_PER_VERTEX = 3
        internal val COORDS_PER_NORMAL = 3
        internal val COORDS_PER_TEXTURE = 2


        private val INFINITE = 800

        private val mCoordsInfiniteArray = floatArrayOf(
            (-INFINITE).toFloat(),
            INFINITE.toFloat(),
            0f,
            (-INFINITE).toFloat(),
            (-INFINITE).toFloat(),
            0f,
            INFINITE.toFloat(),
            (-INFINITE).toFloat(),
            0f,
            INFINITE.toFloat(),
            INFINITE.toFloat(),
            0f
        )

        private val mCoordsNormalArray = floatArrayOf(
            (-WitboxFaces.WITBOX_LONG).toFloat(), WitboxFaces.WITBOX_WITDH.toFloat(), 0f, // top left
            (-WitboxFaces.WITBOX_LONG).toFloat(), (-WitboxFaces.WITBOX_WITDH).toFloat(), 0f, // bottom left
            WitboxFaces.WITBOX_LONG.toFloat(), (-WitboxFaces.WITBOX_WITDH).toFloat(), 0f, // bottom right
            WitboxFaces.WITBOX_LONG.toFloat(), WitboxFaces.WITBOX_WITDH.toFloat(), 0f    // top right
        )


        // S, T (or X, Y)
        // Texture coordinate data.
        // Because images have a Y axis pointing downward (values increase as you move down the image) while
        // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
        // What's more is that the texture coordinates are the same for every face.
        internal val mTextureCoordinateData = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f)

        internal val mNormalData = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f)

        fun loadTexture(context: Context, resourceId: Int): Int {
            val textureHandle = IntArray(1)

            GLES20.glGenTextures(1, textureHandle, 0)

            if (textureHandle[0] != 0) {
                val options = BitmapFactory.Options()
                options.inScaled = false   // No pre-scaling

                // Read in the resource
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

                // Bind to the texture in OpenGL
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bitmap.recycle()
            }

            if (textureHandle[0] == 0) {
                //            throw new RuntimeException("Error loading texture.");
            }

            return textureHandle[0]
        }
    }
}
