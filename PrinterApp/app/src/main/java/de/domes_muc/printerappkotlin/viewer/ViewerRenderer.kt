package de.domes_muc.printerappkotlin.viewer

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.library.LibraryModelCreation
import de.domes_muc.printerappkotlin.viewer.Geometry.Box
import de.domes_muc.printerappkotlin.viewer.Geometry.Point
import de.domes_muc.printerappkotlin.viewer.Geometry.Ray
import de.domes_muc.printerappkotlin.viewer.Geometry.Vector
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.AsyncTask
import android.view.View

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class ViewerRenderer(
    private val mDataList: MutableList<DataStorage>?,
    internal var mContext: Context,
    private val mState: Int,
    mode: Int
) : GLSurfaceView.Renderer {

    private val mStlObjectList = ArrayList<StlObject>()
    private var mGcodeObject: GcodeObject? = null
    private var mWitboxFaceDown: WitboxPlate? = null
    private var mWitboxFaceRight: WitboxFaces? = null
    private var mWitboxFaceBack: WitboxFaces? = null
    private var mWitboxFaceLeft: WitboxFaces? = null
    private var mWitboxFaceFront: WitboxFaces? = null
    private var mWitboxFaceTop: WitboxFaces? = null
    private var mInfinitePlane: WitboxPlate? = null


    var showLeftWitboxFace = true
        private set
    var showRightWitboxFace = true
        private set
    var showBackWitboxFace = true
        private set
    var showDownWitboxFace = true
        private set
    private val mShowFrontWitboxFace = true
    private val mShowTopWitboxFace = true

    var final_matrix_R_Render = FloatArray(16)
    var final_matrix_S_Render = FloatArray(16)
    var final_matrix_T_Render = FloatArray(16)

    private val mVPMatrix = FloatArray(16) //Model View Projection Matrix
    private val mModelMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mRotationMatrix = FloatArray(16)
    private val mTemporaryMatrix = FloatArray(16)

    internal var mMVMatrix = FloatArray(16)
    internal var mMVPMatrix = FloatArray(16)
    internal var mMVPObjectMatrix = FloatArray(16)
    internal var mMVObjectMatrix = FloatArray(16)
    internal var mTransInvMVMatrix = FloatArray(16)
    internal var mObjectModel = FloatArray(16)
    internal var mTemporaryModel = FloatArray(16)

    //Light
    internal var mLightPosInModelSpace = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
    internal var mLightPosInEyeSpace = FloatArray(4)
    internal var mLightPosInWorldSpace = FloatArray(4)
    internal var mLightModelMatrix = FloatArray(16)

    private var mMode = 0

    //Variables Touch events
    private var mObjectPressed = -1

    //Variables for object edition
    internal var mDx = POSITION_DEFAULT_X
    internal var mDy = POSITION_DEFAULT_Y
    internal var mDz: Float = 0.toFloat()

    private var mScaleFactorX = 1.0f
    private var mScaleFactorY = 1.0f
    private var mScaleFactorZ = 1.0f

    private var mVector = Vector(1f, 0f, 0f) //default

    private var mCircle: Circles? = null
    private var mPlate: IntArray? = null

    private var mAxis = -1

    private val isStl: Boolean
        get() {
            if (mDataList!!.size > 0)
                if (mDataList[0].pathFile!!.endsWith(".stl") || mDataList[0].pathFile!!.endsWith(".STL")) return true

            return false
        }

    val widthScreen: Float
        get() = mWidth.toFloat()

    val heightScreen: Float
        get() = mHeight.toFloat()

    var cameraPosX: Float
        get() = mCameraX
        set(x) {
            mCameraX = x
        }

    var cameraPosY: Float
        get() = mCameraY
        set(y) {
            mCameraY = y
        }

    var cameraPosZ: Float
        get() = mCameraZ
        set(z) {
            mCameraZ = z
        }

    init {
        this.mMode = mode
        this.mPlate = ViewerMainFragment.currentPlate
    }

    fun showBackWitboxFace(draw: Boolean) {
        showBackWitboxFace = draw
    }

    fun showRightWitboxFace(draw: Boolean) {
        showRightWitboxFace = draw
    }

    fun showLeftWitboxFace(draw: Boolean) {
        showLeftWitboxFace = draw
    }

    fun showDownWitboxFace(draw: Boolean) {
        showDownWitboxFace = draw
    }

    fun setTransparent(transparent: Boolean) {
        for (i in mStlObjectList.indices)
            mStlObjectList[i].setTransparent(transparent)
    }

    fun setXray(xray: Boolean) {
        for (i in mStlObjectList.indices)
            mStlObjectList[i].setXray(xray)
    }

    fun setOverhang(overhang: Boolean) {
        for (i in mStlObjectList.indices)
            mStlObjectList[i].setOverhang(overhang)
    }

    fun setRotationVector(vector: Vector) {
        mVector = vector
    }

    fun setCurrentaxis(axis: Int) {

        mAxis = axis

    }

    fun setObjectPressed(i: Int) {
        mObjectPressed = i
    }

    fun deleteObject(i: Int) {
        if (!mDataList!!.isEmpty()) {
            mStlObjectList.removeAt(i)
            mDataList.removeAt(i)
            mObjectPressed = -1
            changeTouchedState()

        }

    }

    fun objectPressed(x: Float, y: Float): Int {
        var `object` = -1
        if (mDataList != null && !mDataList.isEmpty()) {
            val ray = convertNormalized2DPointToRay(x, y)

            for (i in mDataList.indices) {
                val objectBox = Box(
                    mDataList[i].minX,
                    mDataList[i].maxX,
                    mDataList[i].minY,
                    mDataList[i].maxY,
                    mDataList[i].minZ,
                    mDataList[i].maxZ
                )

                // If the ray intersects (if the user touched a part of the screen that
                // intersects the stl object's bounding box), then set objectPressed
                if (Geometry.intersects(objectBox, ray)) {
                    `object` = i
                    break
                }
            }
        }

        if (mObjectPressed != `object` && `object` != -1) setObjectPressed(`object`)
        changeTouchedState()
        return `object`
    }

    fun changeTouchedState() {
        for (i in mDataList!!.indices) {
            val d = mDataList[i]
            if (i == mObjectPressed) {
                if (!Geometry.isValidPosition(d.maxX, d.minX, d.maxY, d.minY, mDataList, i))
                    mDataList[i].stateObject = OUT_TOUCHED
                else
                    mDataList[i].stateObject = INSIDE_TOUCHED
            } else {
                if (!Geometry.isValidPosition(d.maxX, d.minX, d.maxY, d.minY, mDataList, i))
                    mDataList[i].stateObject = OUT_NOT_TOUCHED
                else
                    mDataList[i].stateObject = INSIDE_NOT_TOUCHED
            }
        }
    }


    fun dragObject(x: Float, y: Float) {
        val ray = convertNormalized2DPointToRay(x, y)

        val touched = Geometry.intersectionPointWitboxPlate(ray)

        val data = mDataList!![mObjectPressed]

        val dx = touched.x - data.lastCenter.x
        val dy = touched.y - data.lastCenter.y

        val maxX = data.maxX + dx
        val maxY = data.maxY + dy
        val minX = data.minX + dx
        val minY = data.minY + dy

        //Out of the plate
        if (maxX > mPlate!![0] + data.long || minX < -mPlate!![0] - data.long
            || maxY > mPlate!![1] + data.width || minY < -mPlate!![1] - data.width
        ) {

            return

        } else {
            mDataList[mObjectPressed].lastCenter = Point(touched.x, touched.y, data.lastCenter.z)

            data.maxX = maxX
            data.maxY = maxY
            data.minX = minX
            data.minY = minY

            /******
             * Calculate new center by adding all previous centers
             */
            var finalx = 0f
            var finaly = 0f
            var i = 0

            for (element in mDataList) {

                finalx += element.lastCenter.x
                finaly += element.lastCenter.y
                i++

            }

            finalx = finalx / i
            finaly = finaly / i

            ViewerMainFragment.setSlicingPosition(finalx, finaly)
        }


    }


    fun scaleObject(fx: Float, fy: Float, fz: Float, error: Boolean) {
        if (/*Math.abs(fx)>0.1 && */Math.abs(fx) < 10 && /*Math.abs(fy)>0.1 && */ Math.abs(fy) < 10 &&/* Math.abs(fz)>0.1 && */ Math.abs(
                fz
            ) < 10
        ) {    //Removed min value
            mScaleFactorX = fx
            mScaleFactorY = fy
            mScaleFactorZ = fz

            val data = mDataList!![mObjectPressed]

            val lastCenter = data.lastCenter

            var maxX = data.maxX - lastCenter.x
            var maxY = data.maxY - lastCenter.y
            var maxZ = data.maxZ
            var minX = data.minX - lastCenter.x
            var minY = data.minY - lastCenter.y
            var minZ = data.minZ

            val lastScaleFactorX = data.lastScaleFactorX
            val lastScaleFactorY = data.lastScaleFactorY
            val lastScaleFactorZ = data.lastScaleFactorZ

            maxX = maxX + (Math.abs(mScaleFactorX) - Math.abs(lastScaleFactorX)) * (maxX / Math.abs(lastScaleFactorX)) +
                    lastCenter.x
            maxY = maxY + (mScaleFactorY - lastScaleFactorY) * (maxY / lastScaleFactorY) + lastCenter.y
            maxZ = maxZ + (mScaleFactorZ - lastScaleFactorZ) * (maxZ / lastScaleFactorZ) + lastCenter.z

            minX = minX + (Math.abs(mScaleFactorX) - Math.abs(lastScaleFactorX)) * (minX / Math.abs(lastScaleFactorX)) +
                    lastCenter.x
            minY = minY + (mScaleFactorY - lastScaleFactorY) * (minY / lastScaleFactorY) + lastCenter.y
            minZ = minZ + (mScaleFactorZ - lastScaleFactorZ) * (minZ / lastScaleFactorZ) + lastCenter.z

            //Out of the plate
            if (maxX > mPlate!![0] || minX < -mPlate!![0]
                || maxY > mPlate!![1] || minY < -mPlate!![1]
            ) {

                if (error) {
                    if (maxX > mPlate!![0] || minX < -mPlate!![0]) ViewerMainFragment.displayErrorInAxis(0)
                    if (maxY > mPlate!![1] || minY < -mPlate!![1]) ViewerMainFragment.displayErrorInAxis(1)
                }


                return

            } else {

                data.maxX = maxX
                data.maxY = maxY
                data.maxZ = maxZ

                data.minX = minX
                data.minY = minY
                data.minZ = minZ

                data.lastScaleFactorX = mScaleFactorX
                data.lastScaleFactorY = mScaleFactorY
                data.lastScaleFactorZ = mScaleFactorZ
            }


        }
    }

    /**
     * Changed rotation logic to rotate around plate's global axes
     *
     * Alberto
     * @param angle degrees to rotate
     */
    fun setRotationObject(angle: Float) {
        val data = mDataList!![mObjectPressed]


        //Get the object's rotation matrix
        val rotateObjectMatrix = data.rotationMatrix

        val center = data.lastCenter

        val mTemporaryMatrix = FloatArray(16)
        val mFinalMatrix = FloatArray(16)

        //Set a new identity matrix
        Matrix.setIdentityM(mTemporaryMatrix, 0)

        //Move the matrix to the origin
        Matrix.translateM(mTemporaryMatrix, 0, 0.0f, 0.0f, 0.0f)

        //Rotate in the origin
        Matrix.rotateM(mTemporaryMatrix, 0, angle, mVector.x, mVector.y, mVector.z)

        //Multiply by the object's matrix to get the new position
        Matrix.multiplyMM(mFinalMatrix, 0, mTemporaryMatrix, 0, rotateObjectMatrix, 0)


        //Set the new rotation matrix
        data.rotationMatrix = mFinalMatrix
    }

    fun refreshRotatedObjectCoordinates() {
        val task = object : AsyncTask<Void, Void, Void>() {

            override fun onPreExecute() {
                ViewerMainFragment.configureProgressState(View.VISIBLE)
            }

            override fun doInBackground(vararg params: Void): Void? {

                //TODO Random crash
                try {

                    val data = mDataList!![mObjectPressed]

                    data.initMaxMin()
                    val coordinatesArray = data.vertexArray
                    var x: Float
                    var y: Float
                    var z: Float

                    val vector = FloatArray(4)
                    val result = FloatArray(4)
                    val aux = FloatArray(16)

                    val rotationMatrix = data.rotationMatrix

                    var i = 0
                    while (i < coordinatesArray!!.size) {
                        vector[0] = coordinatesArray[i]
                        vector[1] = coordinatesArray[i + 1]
                        vector[2] = coordinatesArray[i + 2]

                        Matrix.setIdentityM(aux, 0)
                        Matrix.multiplyMM(aux, 0, rotationMatrix, 0, aux, 0)
                        Matrix.multiplyMV(result, 0, aux, 0, vector, 0)

                        x = result[0]
                        y = result[1]
                        z = result[2]

                        data.adjustMaxMin(x, y, z)
                        i += 3
                    }

                    var maxX = data.maxX
                    var minX = data.minX
                    var minY = data.minY
                    var maxY = data.maxY
                    var maxZ = data.maxZ
                    var minZ = data.minZ


                    val lastCenter = data.lastCenter
                    //We have to introduce the rest of transformations.
                    maxX = maxX * Math.abs(mScaleFactorX) + lastCenter.x
                    maxY = maxY * mScaleFactorY + lastCenter.y
                    maxZ = maxZ * mScaleFactorZ + lastCenter.z

                    minX = minX * Math.abs(mScaleFactorX) + lastCenter.x
                    minY = minY * mScaleFactorY + lastCenter.y
                    minZ = minZ * mScaleFactorZ + lastCenter.z

                    data.maxX = maxX
                    data.maxY = maxY

                    data.minX = minX
                    data.minY = minY

                    var adjustZ = 0f
                    if (minZ != 0f) adjustZ = -data.minZ + DataStorage.MIN_Z.toFloat() //TODO CHECK

                    data.adjustZ = adjustZ
                    data.minZ = minZ + adjustZ //Readjust min and max
                    data.maxZ = maxZ + adjustZ

                } catch (e: ArrayIndexOutOfBoundsException) {

                    e.printStackTrace()
                }



                return null
            }

            override fun onPostExecute(unused: Void) {
                ViewerMainFragment.configureProgressState(View.GONE)
                ViewerMainFragment.displayModelSize(mObjectPressed)

            }
        }

        task.execute()

    }

    private fun setColor(`object`: Int) {
        val stl = mStlObjectList[`object`]
        when (mDataList!![`object`].stateObject) {
            INSIDE_NOT_TOUCHED -> stl.setColor(StlObject.colorNormal)
            INSIDE_TOUCHED -> stl.setColor(StlObject.colorSelectedObject)
            OUT_NOT_TOUCHED -> stl.setColor(StlObject.colorObjectOut)
            OUT_TOUCHED -> stl.setColor(StlObject.colorObjectOutTouched)
        }
    }

    fun generatePlate(type: IntArray) {

        try {

            mPlate = type

            //Create plate to pre-generate the plate
            if (mMode == ViewerMainFragment.PRINT_PREVIEW) {
                mWitboxFaceBack = WitboxFaces(BACK, mPlate!!)
                mWitboxFaceRight = WitboxFaces(RIGHT, mPlate!!)
                mWitboxFaceLeft = WitboxFaces(LEFT, mPlate!!)
                mWitboxFaceFront = WitboxFaces(FRONT, mPlate!!)
                mWitboxFaceTop = WitboxFaces(TOP, mPlate!!)
                mWitboxFaceDown = WitboxPlate(mContext, false, mPlate!!)
            }


            mWitboxFaceBack!!.generatePlaneCoords(BACK, type)
            mWitboxFaceRight!!.generatePlaneCoords(RIGHT, type)
            mWitboxFaceLeft!!.generatePlaneCoords(LEFT, type)
            mWitboxFaceFront!!.generatePlaneCoords(FRONT, type)
            mWitboxFaceTop!!.generatePlaneCoords(TOP, type)
            mWitboxFaceDown!!.generatePlaneCoords(type, false)


        } catch (e: NullPointerException) {

            e.printStackTrace()
        }


    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

        // Set the background frame color
        //GLES20.glClearColor( 0.9f, 0.9f, 0.9f, 1.0f);
        GLES20.glClearColor(0.149f, 0.196f, 0.22f, 1.0f)

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setIdentityM(mModelMatrix, 0)
        mCurrentSceneAngleX = 0f
        mCurrentSceneAngleY = 0f

        mSceneAngleX = ANGLE_X
        mSceneAngleY = ANGLE_Y

        if (mDataList!!.size > 0)
            if (isStl) {

                //First, reset the stl object list
                mStlObjectList.clear()

                //Add the new ones.
                for (i in mDataList.indices) {
                    if (mDataList[i].vertexArray != null) {

                        Log.i("VERTEX", "adding")
                        mStlObjectList.add(StlObject(mDataList[i], mContext, mState))
                    } else
                        Log.i("VERTEX", "ONE NULL $i")
                }

            } else if (mDataList.size > 0) {

                //TODO Random crash
                try {
                    mGcodeObject = GcodeObject(mDataList[0], mContext)
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }

            }



        if (mMode == ViewerMainFragment.DO_SNAPSHOT || mMode == ViewerMainFragment.PRINT_PREVIEW) mInfinitePlane =
                WitboxPlate(mContext, true, ViewerMainFragment.currentPlate)

        mWitboxFaceBack = WitboxFaces(BACK, mPlate!!)
        mWitboxFaceRight = WitboxFaces(RIGHT, mPlate!!)
        mWitboxFaceLeft = WitboxFaces(LEFT, mPlate!!)
        mWitboxFaceFront = WitboxFaces(FRONT, mPlate!!)
        mWitboxFaceTop = WitboxFaces(TOP, mPlate!!)
        mWitboxFaceDown = WitboxPlate(mContext, false, mPlate!!)


        mCircle = Circles()

    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        mWidth = width
        mHeight = height

        Log.i("OUT", "Width: $width ; Height: $height")

        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height

        // this projection matrix is applied to object coordinates
        Matrix.perspectiveM(mProjectionMatrix, 0, 45f, ratio, Z_NEAR, Z_FAR)

        if (mMode == ViewerMainFragment.DO_SNAPSHOT || mMode == ViewerMainFragment.PRINT_PREVIEW) {
            val data = mDataList!![0]

            val h = data.height
            var l = data.long
            var w = data.width

            l = l / ratio //We calculate the height related to the square in the frustum with this width
            w = w / ratio

            val dh = (h / Math.tan(Math.toRadians((45 / 2).toDouble()))).toFloat()
            var dl = (l / (2 * Math.tan(Math.toRadians((45 / 2).toDouble())))).toFloat()
            var dw = (w / (2 * Math.tan(Math.toRadians((45 / 2).toDouble())))).toFloat()

            if (dw > dh && dw > dl)
                mCameraZ = OFFSET_BIG_HEIGHT * h
            else if (dh > dl)
                mCameraZ = OFFSET_HEIGHT * h
            else
                mCameraZ = OFFSET_BIG_HEIGHT * h

            dl = dl + Math.abs(data.minY)
            dw = dw + Math.abs(data.minX)

            if (dw > dh && dw > dl)
                mCameraY = -dw
            else if (dh > dl)
                mCameraY = -dh
            else
                mCameraY = -dl

            mDx = -data.lastCenter.x
            mDy = -data.lastCenter.y

            mSceneAngleX = -40f
            mSceneAngleY = 0f

        } else {

            mCameraY = CAMERA_DEFAULT_Y
            mCameraZ = CAMERA_DEFAULT_Z
        }

    }

    /**
     * Translate the matrix x;y pixels
     * Check for screen limits
     * @param x
     * @param y
     * @param z
     */
    internal fun matrixTranslate(x: Float, y: Float, z: Float) {

        // Translate slots.
        mDx += x
        mDy += y

        if (mDx < -300 || mDx > 300) mDx -= x
        if (mDy < -250 || mDy > 250) mDy -= y
        mViewMatrix[14] += z
    }


    override fun onDrawFrame(unused: GL10) {
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (isStl)
            for (i in mStlObjectList.indices)
                setColor(i)

        GLES20.glEnable(GLES20.GL_BLEND)

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, mCameraX, mCameraY, mCameraZ, mCenterX, mCenterY, mCenterZ, 0f, 0.0f, 1.0f)

        //Apply translation
        mViewMatrix[12] += mDx
        mViewMatrix[13] += mDy

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)

        /**
         * ROTATE FIRST THE X AXIS AROUND ITSELF MODEL X ROTATION
         */

        //Set Identity
        Matrix.setIdentityM(mRotationMatrix, 0)

        //Move the matrix to the origin
        Matrix.translateM(mRotationMatrix, 0, 0.0f, 0.0f, 0.0f)

        //Rotation x
        Matrix.rotateM(mRotationMatrix, 0, mSceneAngleX, 0.0f, 0.0f, 1.0f)

        mCurrentSceneAngleX += mSceneAngleX

        //Reset angle, we store the rotation in the matrix
        mSceneAngleX = 0f

        //Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
        Matrix.multiplyMM(mTemporaryMatrix, 0, mModelMatrix, 0, mRotationMatrix, 0)
        System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16)


        /**
         * ROTATE SECOND THE Y AXIS AROUND THE SCENE ROTATION X MODEL
         */

        //Set Identity
        Matrix.setIdentityM(mRotationMatrix, 0)

        //Move the matrix to the origin
        Matrix.translateM(mRotationMatrix, 0, 0.0f, 0.0f, 0.0f)

        //RotationY
        Matrix.rotateM(mRotationMatrix, 0, mSceneAngleY, 1.0f, 0.0f, 0.0f)

        mCurrentSceneAngleY += mSceneAngleY

        mSceneAngleY = 0f

        //Transport to degrees
        if (mCurrentSceneAngleX > 180)
            mCurrentSceneAngleX -= 360f
        else if (mCurrentSceneAngleX < -180) mCurrentSceneAngleX += 360f

        //Transport to degrees
        if (mCurrentSceneAngleY > 180)
            mCurrentSceneAngleY -= 360f
        else if (mCurrentSceneAngleY < -180) mCurrentSceneAngleY += 360f

        //Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
        Matrix.multiplyMM(mTemporaryMatrix, 0, mRotationMatrix, 0, mModelMatrix, 0)
        System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16)

        Matrix.multiplyMM(mMVPMatrix, 0, mVPMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)

        //invertedMVPMatrix is used to detect clicks
        Matrix.invertM(invertedMVPMatrix, 0, mMVPMatrix, 0)

        //Set Light direction
        Matrix.setIdentityM(mLightModelMatrix, 0)
        Matrix.translateM(mLightModelMatrix, 0, LIGHT_X, LIGHT_Y, LIGHT_Z)

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0)
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0)

        if (mDataList!!.size > 0) {
            if (mObjectPressed != -1) {

                //Check pressed object to avoid index out of bounds when deleting models
                if (mObjectPressed < mDataList.size) {
                    val data = mDataList[mObjectPressed]
                    val center = data.lastCenter

                    Log.i("CENTER", "Settings center @" + center.x + ";" + center.y + ";" + center.z)

                    Matrix.setIdentityM(mTemporaryModel, 0)
                    Matrix.translateM(mTemporaryModel, 0, center.x, center.y, center.z)
                    Matrix.scaleM(
                        mTemporaryModel,
                        0,
                        data.lastScaleFactorX,
                        data.lastScaleFactorY,
                        data.lastScaleFactorZ
                    )

                    Matrix.translateM(mTemporaryModel, 0, 0f, 0f, data.adjustZ)

                    //Object rotation
                    val rotateObjectMatrix = data.rotationMatrix

                    //Multiply the model by the accumulated rotation
                    Matrix.multiplyMM(mObjectModel, 0, mTemporaryModel, 0, rotateObjectMatrix, 0)
                    Matrix.multiplyMM(mMVPObjectMatrix, 0, mMVPMatrix, 0, mObjectModel, 0)

                    Matrix.multiplyMM(mMVObjectMatrix, 0, mMVMatrix, 0, mObjectModel, 0)
                    Matrix.transposeM(mTransInvMVMatrix, 0, mMVObjectMatrix, 0)
                    Matrix.invertM(mTransInvMVMatrix, 0, mTransInvMVMatrix, 0)
                } else {

                    Log.i("Multiply", "IndexOutOfBounds $mObjectPressed")

                }


            }


            if (isStl)
                for (i in mStlObjectList.indices) {
                    if (i == mObjectPressed) {

                        try {

                            if (mDataList.size > 0) {
                                mDataList[mObjectPressed].modelMatrix = mObjectModel
                                mStlObjectList[mObjectPressed].draw(
                                    mMVPObjectMatrix,
                                    mTransInvMVMatrix,
                                    mLightPosInEyeSpace,
                                    mObjectModel
                                )
                                mCircle!!.draw(mDataList[mObjectPressed], mMVPMatrix, mAxis)

                            }


                        } catch (e: IndexOutOfBoundsException) {

                            Log.i("Slicer", "IndexOutOfBounds $mObjectPressed")

                        }


                    } else {
                        val modelMatrix = mDataList[i].modelMatrix
                        val mvpMatrix = FloatArray(16)
                        val mvMatrix = FloatArray(16)
                        val mvFinalMatrix = FloatArray(16)

                        Matrix.multiplyMM(mvpMatrix, 0, mMVPMatrix, 0, modelMatrix, 0)

                        Matrix.multiplyMM(mvMatrix, 0, mMVMatrix, 0, modelMatrix, 0)

                        Matrix.transposeM(mvFinalMatrix, 0, mvMatrix, 0)
                        Matrix.invertM(mvFinalMatrix, 0, mvFinalMatrix, 0)

                        mStlObjectList[i].draw(mvpMatrix, mvFinalMatrix, mLightPosInEyeSpace, modelMatrix)
                    }
                }
            else {

                //TODO Random crash
                try {


                    if (mGcodeObject != null) mGcodeObject!!.draw(mMVPMatrix)
                } catch (e: NullPointerException) {

                    e.printStackTrace()
                }

            }
        }




        if (mMode == ViewerMainFragment.DO_SNAPSHOT) {
            mInfinitePlane!!.draw(mMVPMatrix, mMVMatrix)
            takeSnapshot(unused)


        } else {

            if (showDownWitboxFace) mWitboxFaceDown!!.draw(mMVPMatrix, mMVMatrix)
            if (showBackWitboxFace) mWitboxFaceBack!!.draw(mMVPMatrix)
            if (showRightWitboxFace) mWitboxFaceRight!!.draw(mMVPMatrix)
            if (showLeftWitboxFace) mWitboxFaceLeft!!.draw(mMVPMatrix)
            if (mShowFrontWitboxFace) mWitboxFaceFront!!.draw(mMVPMatrix)
            if (mShowTopWitboxFace) mWitboxFaceTop!!.draw(mMVPMatrix)

        }
    }

    private fun takeSnapshot(unused: GL10) {
        Log.i(TAG, "TAKING SNAPSHOT")
        val minX = 0
        val minY = 0

        val screenshotSize = mWidth * mHeight
        val bb = ByteBuffer.allocateDirect(screenshotSize * 4)
        bb.order(ByteOrder.nativeOrder())


        GLES20.glReadPixels(minX, minY, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb)



        LibraryModelCreation.saveSnapshot(mWidth, mHeight, bb)
    }


    fun setSceneAngleX(x: Float) {
        mSceneAngleX += x
    }

    fun setSceneAngleY(y: Float) {
        mSceneAngleY += y
    }

    fun setCenterX(x: Float) {
        mCenterX += x
    }

    fun setCenterY(y: Float) {
        mCenterY += y
    }

    fun setCenterZ(z: Float) {
        mCenterZ += z
    }

    fun setZNear(h: Float) {
        val ang = Math.toRadians((45 / 2).toDouble())
        val valor = Math.tan(ang).toFloat()

        Z_NEAR = valor * (h / 2)
    }


    /**
     * Animation to restore initial position
     * @return
     */
    fun restoreInitialCameraPosition(dx: Float, dy: Float, zoom: Boolean, rotation: Boolean): Boolean {

        var dyx = 0f

        if (!zoom) dyx += POSITION_DEFAULT_Y

        //Plate translation
        if (mDx.toInt() < (POSITION_DEFAULT_X - dx).toInt())
            mDx += POSITION_MIN_TRANSLATION_DISTANCE.toFloat()
        else if (mDx.toInt() > (POSITION_DEFAULT_X - dx).toInt()) mDx -= POSITION_MIN_TRANSLATION_DISTANCE.toFloat()

        if (mDy.toInt() < (dyx - dy).toInt())
            mDy += POSITION_MIN_TRANSLATION_DISTANCE.toFloat()
        else if (mDy.toInt() > (dyx - dy).toInt()) mDy -= POSITION_MIN_TRANSLATION_DISTANCE.toFloat()


        if (!zoom) {

            //Move X axis
            if (mCameraX.toInt() < CAMERA_DEFAULT_X)
                mCameraX += CAMERA_MIN_TRANSLATION_DISTANCE.toFloat()
            else if (mCameraX.toInt() > CAMERA_DEFAULT_X) mCameraX -= CAMERA_MIN_TRANSLATION_DISTANCE.toFloat()

            //Move Y axis
            if (mCameraY.toInt() < CAMERA_DEFAULT_Y)
                mCameraY += CAMERA_MIN_TRANSLATION_DISTANCE.toFloat()
            else if (mCameraY.toInt() > CAMERA_DEFAULT_Y) mCameraY -= CAMERA_MIN_TRANSLATION_DISTANCE.toFloat()


            //Move Z axis
            if (mCameraZ.toInt() < CAMERA_DEFAULT_Z)
                mCameraZ += CAMERA_MIN_TRANSLATION_DISTANCE.toFloat()
            else if (mCameraZ.toInt() > CAMERA_DEFAULT_Z) mCameraZ -= CAMERA_MIN_TRANSLATION_DISTANCE.toFloat()

        }

        if (rotation) {

            //Rotate X axis
            if (mCurrentSceneAngleX.toInt() < ANGLE_X) {

                //Slow rotation when approaching the final value
                if (mCurrentSceneAngleX.toInt() > ANGLE_X - 10f)
                    mSceneAngleX = CAMERA_MIN_ROTATION_DISTANCE.toFloat()
                else
                    mSceneAngleX = CAMERA_MAX_ROTATION_DISTANCE.toFloat()
            } else if (mCurrentSceneAngleX.toInt() > ANGLE_X) {

                //Slow rotation when approaching the final value
                if (mCurrentSceneAngleX.toInt() < ANGLE_X + 10f)
                    mSceneAngleX = (-CAMERA_MIN_ROTATION_DISTANCE).toFloat()
                else
                    mSceneAngleX = (-CAMERA_MAX_ROTATION_DISTANCE).toFloat()
            }

            //Rotate Y axis
            if (mCurrentSceneAngleY.toInt() < ANGLE_Y) {

                //Slow rotation when approaching the final value
                if (mCurrentSceneAngleY.toInt() > ANGLE_Y - 10f)
                    mSceneAngleY = CAMERA_MIN_ROTATION_DISTANCE.toFloat()
                else
                    mSceneAngleY = CAMERA_MAX_ROTATION_DISTANCE.toFloat()
            } else if (mCurrentSceneAngleY.toInt() > ANGLE_Y) {

                //Slow rotation when approaching the final value
                if (mCurrentSceneAngleY.toInt() < ANGLE_Y + 10f)
                    mSceneAngleY = (-CAMERA_MIN_ROTATION_DISTANCE).toFloat()
                else
                    mSceneAngleY = (-CAMERA_MAX_ROTATION_DISTANCE).toFloat()
            }

        }


        //Return true when we get the final values
        return if ((mCameraZ.toInt().toFloat() == CAMERA_DEFAULT_Z && mCameraY.toInt().toFloat() == CAMERA_DEFAULT_Y && mCameraX.toInt().toFloat() == CAMERA_DEFAULT_X || zoom)
            && (mCurrentSceneAngleX.toInt().toFloat() == ANGLE_X && mCurrentSceneAngleY.toInt().toFloat() == ANGLE_Y || !rotation)
            && mDx.toInt() == (POSITION_DEFAULT_X - dx).toInt() && mDy.toInt() == (dyx - dy).toInt()
        )
            true
        else {

            false
        }

    }

    companion object {
        private val TAG = "ViewerRenderer"

        var Z_NEAR = 1f
        var Z_FAR = 3000f

        private val OFFSET_HEIGHT = 2f
        private val OFFSET_BIG_HEIGHT = 5f

        private val ANGLE_X = 0f
        private val ANGLE_Y = -5f
        private val CAMERA_DEFAULT_X = 0f
        private val CAMERA_DEFAULT_Y = -300f
        private val CAMERA_DEFAULT_Z = 350f
        private val POSITION_DEFAULT_X = 0f
        private val POSITION_DEFAULT_Y = -50f

        private var mWidth: Int = 0
        private var mHeight: Int = 0

        var mCameraX = 0f
        var mCameraY = 0f
        var mCameraZ = 0f

        var mCenterX = 0f
        var mCenterY = 0f
        var mCenterZ = 0f

        var mSceneAngleX = 0f
        var mSceneAngleY = 0f
        var mCurrentSceneAngleX = 0f
        var mCurrentSceneAngleY = 0f

        var RED = 0.80f
        var GREEN = 0.1f
        var BLUE = 0.1f
        var ALPHA = 0.9f

        val DOWN = 0
        val RIGHT = 1
        val BACK = 2
        val LEFT = 3
        val FRONT = 4
        val TOP = 5

        val LIGHT_X = 0f
        val LIGHT_Y = 0f
        val LIGHT_Z = 2000f

        val NORMAL = 0
        val XRAY = 1
        val TRANSPARENT = 2
        val LAYERS = 3
        private val invertedMVPMatrix = FloatArray(16)

        val INSIDE_NOT_TOUCHED = 0
        val OUT_NOT_TOUCHED = 1
        val INSIDE_TOUCHED = 2
        val OUT_TOUCHED = 3


        private fun convertNormalized2DPointToRay(normalizedX: Float, normalizedY: Float): Ray {
            // We'll convert these normalized device coordinates into world-space
            // coordinates. We'll pick a point on the near and far planes, and draw a
            // line between them. To do this transform, we need to first multiply by
            // the inverse matrix, and then we need to undo the perspective divide.
            val nearPointNdc = floatArrayOf(normalizedX, normalizedY, -1f, 1f)
            val farPointNdc = floatArrayOf(normalizedX, normalizedY, 1f, 1f)

            val nearPointWorld = FloatArray(4)
            val farPointWorld = FloatArray(4)


            Matrix.multiplyMV(nearPointWorld, 0, invertedMVPMatrix, 0, nearPointNdc, 0)
            Matrix.multiplyMV(farPointWorld, 0, invertedMVPMatrix, 0, farPointNdc, 0)

            // Why are we dividing by W? We multiplied our vector by an inverse
            // matrix, so the W value that we end up is actually the *inverse* of
            // what the projection matrix would create. By dividing all 3 components
            // by W, we effectively undo the hardware perspective divide.
            divideByW(nearPointWorld)
            divideByW(farPointWorld)

            // We don't care about the W value anymore, because our points are now
            // in world coordinates.
            val nearPointRay = Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2])

            val farPointRay = Point(farPointWorld[0], farPointWorld[1], farPointWorld[2])

            return Ray(nearPointRay, Geometry.vectorBetween(nearPointRay, farPointRay))
        }

        private fun divideByW(vector: FloatArray) {
            vector[0] /= vector[3]
            vector[1] /= vector[3]
            vector[2] /= vector[3]
        }


        /**
         * Utility method for compiling a OpenGL shader.
         *
         *
         * **Note:** When developing shaders, use the checkGlError()
         * method to debug shader coding errors.
         *
         * @param type - Vertex or fragment shader type.
         * @param shaderCode - String containing the shader code.
         * @return - Returns an id for the shader.
         */
        fun loadShader(type: Int, shaderCode: String): Int {

            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            val shader = GLES20.glCreateShader(type)

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            return shader
        }

        /**
         * Utility method for debugging OpenGL calls. Provide the name of the call
         * just after making it:
         *
         * <pre>
         * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
         * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
         *
         * If the operation is not successful, the check throws an error.
         *
         * @param glOperation - Name of the OpenGL call to check.
         */
        fun checkGlError(glOperation: String) {
            val error: Int
            do {
                error = GLES20.glGetError()
                if ( error.equals(GLES20.GL_NO_ERROR) ) break

                Log.e(TAG, "$glOperation: glError $error")
                throw RuntimeException("$glOperation: glError $error")
            }
            while(true)

//            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
//                Log.e(TAG, "$glOperation: glError $error")
//                throw RuntimeException("$glOperation: glError $error")
//            }
        }


        /***********************************************************************************
         *
         * Methods that use camera angles and position
         *
         */

        /**
         * Static values for camera auto movement and rotation
         */
        private val CAMERA_MIN_TRANSLATION_DISTANCE = 0.1
        private val CAMERA_MAX_ROTATION_DISTANCE = 5
        private val CAMERA_MIN_ROTATION_DISTANCE = 1
        private val POSITION_MIN_TRANSLATION_DISTANCE = 0.05
    }


}