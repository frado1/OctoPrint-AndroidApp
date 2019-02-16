package de.domes_muc.printerappkotlin.viewer

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.viewer.Geometry.Vector
import android.content.Context
import android.graphics.PointF
import android.opengl.GLSurfaceView
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent

import java.util.ArrayList

class ViewerSurfaceView : GLSurfaceView {

    internal lateinit var mRenderer: ViewerRenderer
    private lateinit var mDataList : MutableList<DataStorage>
    //Touch
    private var mMode: Int = 0
    private val TOUCH_SCALE_FACTOR_ROTATION = 90.0f / 320  //180.0f / 320;
    private var mPreviousX: Float = 0.toFloat()
    private var mPreviousY: Float = 0.toFloat()
    private var mPreviousDragX: Float = 0.toFloat()
    private var mPreviousDragY: Float = 0.toFloat()

    // zoom rate (larger > 1.0f > smaller)
    private var pinchScale = 1.0f

    private val pinchStartPoint = PointF()
    private var pinchStartY = 0.0f
    private var pinchStartZ = 0.0f
    private var pinchStartDistance = 0.0f
    private var pinchStartFactorX = 0.0f
    private var pinchStartFactorY = 0.0f
    private var pinchStartFactorZ = 0.0f
    private var touchMode = TOUCH_NONE

    private var mMovementMode: Int = 0

    //Edition mode
    private var mEdition = false
    /**
     * Set edition mode
     * @param mode MOVE_EDITION_MODE, ROTATION_EDITION_MODE, SCALED_EDITION_MODE, MIRROR_EDITION_MODE
     */
    var editionMode: Int = 0
    private var mRotateMode: Int = 0

    /**
     * Get the object that has been pressed
     * @return mObjectPressed
     */
    var objectPresed = -1
        private set

    private var mCurrentAngle = floatArrayOf(0f, 0f, 0f)

    //Double tap logic
    internal var mDoubleTapFirstTouch = false
    internal var mDoubleTapCurrentTime: Long = 0

    /**
     * Check if it is an stl model.
     * @return
     */
    private val isStl: Boolean
        get() {
            if (mDataList.size > 0)
                if (mDataList[0].pathFile!!.endsWith(".stl") || mDataList[0].pathFile!!.endsWith(".STL")) return true

            return false
        }

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    /**
     *
     * @param context Context
     * @param data Data to render
     * @param state Type of rendering: normal, triangle, overhang, layers
     * @param mode Mode of rendering: do snapshot (take picture for library), dont snapshot (normal) and print_preview (gcode preview in print progress)
     */
    constructor(
        context: Context,
        data: MutableList<DataStorage>,
        state: Int,
        mode: Int,
        handler: SlicingHandler?
    ) : super(context) {
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2)

        this.mMode = mode
        this.mDataList = data
        this.mRenderer = ViewerRenderer(data, context, state, mode)
        setRenderer(mRenderer)

        // Render the view only when there is a change in the drawing data
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    /**
     * Set the view options depending on the model
     * @param state
     */
    fun configViewMode(state: Int) {
        when (state) {
            ViewerSurfaceView.NORMAL -> {
                setOverhang(false)
                setXray(false)
                setTransparent(false)
            }
            ViewerSurfaceView.XRAY -> {
                setOverhang(false)
                setXray(true)
                setTransparent(false)
            }
            ViewerSurfaceView.TRANSPARENT -> {
                setOverhang(false)
                setXray(false)
                setTransparent(true)
            }
            ViewerSurfaceView.OVERHANG -> {
                setOverhang(true)
                setXray(false)
                setTransparent(false)
            }
        }

        requestRender()
    }

    /**
     * Show/Hide back Witbox Face
     */
    fun showBackWitboxFace() {
        if (mRenderer.showBackWitboxFace)
            mRenderer.showBackWitboxFace(false)
        else
            mRenderer.showBackWitboxFace(true)
        requestRender()
    }

    fun showRightWitboxFace() {
        if (mRenderer.showRightWitboxFace)
            mRenderer.showRightWitboxFace(false)
        else
            mRenderer.showRightWitboxFace(true)
        requestRender()
    }

    fun showLeftWitboxFace() {
        if (mRenderer.showLeftWitboxFace)
            mRenderer.showLeftWitboxFace(false)
        else
            mRenderer.showLeftWitboxFace(true)
        requestRender()
    }

    fun showDownWitboxFace() {
        if (mRenderer.showDownWitboxFace)
            mRenderer.showDownWitboxFace(false)
        else
            mRenderer.showDownWitboxFace(true)
        requestRender()
    }

    /**
     * Tells the render if overhang is activated or not
     * @param overhang
     */
    fun setOverhang(overhang: Boolean) {
        mRenderer.setOverhang(overhang)
    }

    /**
     * Tell the render if transparent view is activated or not
     * @param trans
     */
    fun setTransparent(trans: Boolean) {
        mRenderer.setTransparent(trans)
    }

    /**
     * Tells render if xray view (triangles view) is activated or not
     * @param xray
     */
    fun setXray(xray: Boolean) {
        mRenderer.setXray(xray)
    }

    /**
     * Delete selected object
     */
    fun deleteObject() {
        mRenderer.deleteObject(objectPresed)
    }

    /**
     * Set the rotation axis
     * @param mode ROTATION_X, ROTATION_Y, ROTATION_Z
     */
    fun setRotationVector(mode: Int) {
        when (mode) {

            ROTATE_X -> {
                mRotateMode = ROTATE_X
                mRenderer.setRotationVector(Vector(1f, 0f, 0f))
            }
            ROTATE_Y -> {
                mRotateMode = ROTATE_Y
                mRenderer.setRotationVector(Vector(0f, 1f, 0f))
            }
            ROTATE_Z -> {
                mRotateMode = ROTATE_Z
                mRenderer.setRotationVector(Vector(0f, 0f, 1f))
            }
        }

        //Reset current angle to do a new calculation on 0º
        mCurrentAngle = floatArrayOf(0f, 0f, 0f)
    }


    /**
     * Rotate the object in the X axis
     * @param angle angle to rotate
     */
    fun rotateAngleAxisX(angle: Float) {
        if (mRotateMode != ROTATE_X) setRotationVector(ROTATE_X)

        val rotation = angle - mCurrentAngle[0]
        mCurrentAngle[0] = mCurrentAngle[0] + (angle - mCurrentAngle[0])

        mRenderer.setRotationObject(rotation)
        //mRenderer.refreshRotatedObjectCoordinates();

    }

    /**
     * Rotate the object in the Y axis
     * @param angle angle to rotate
     */
    fun rotateAngleAxisY(angle: Float) {
        if (mRotateMode != ROTATE_Y) setRotationVector(ROTATE_Y)

        val rotation = angle - mCurrentAngle[1]
        mCurrentAngle[1] = mCurrentAngle[1] + (angle - mCurrentAngle[1])

        mRenderer.setRotationObject(rotation)
        //mRenderer.refreshRotatedObjectCoordinates();
    }

    /**
     * Rotate the object in the Z axis
     * @param angle angle to rotate
     */
    fun rotateAngleAxisZ(angle: Float) {
        if (mRotateMode != ROTATE_Z) setRotationVector(ROTATE_Z)

        val rotation = angle - mCurrentAngle[2]
        mCurrentAngle[2] = mCurrentAngle[2] + (angle - mCurrentAngle[2])

        mRenderer.setRotationObject(rotation)
        //mRenderer.refreshRotatedObjectCoordinates();
    }

    /*
    Refresh only when the user stops tracking the angle
     */
    fun refreshRotatedObject() {

        mRenderer.refreshRotatedObjectCoordinates()

    }

    /*
    Set new axis to request a render from here
     */
    fun setRendererAxis(axis: Int) {
        mRenderer.setCurrentaxis(axis)
        requestRender()

    }

    /*
    Change plate coords
     */
    fun changePlate(type: IntArray) {

        mRenderer.generatePlate(type)

    }

    fun doPress(i: Int) {


        mRenderer.setObjectPressed(i)
        mRenderer.changeTouchedState()
        mEdition = true
        objectPresed = i
        ViewerMainFragment.showActionModePopUpWindow()
        ViewerMainFragment.displayModelSize(objectPresed)

        touchMode = TOUCH_DRAG
    }

    /**
     * On touch events
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //if (mMode == ViewerMainFragment.PRINT_PREVIEW) return false;

        val x = event.x
        val y = event.y

        val normalizedX = event.x / mRenderer.widthScreen * 2 - 1
        val normalizedY = -(event.y / mRenderer.heightScreen * 2 - 1)

        when (event.action and MotionEvent.ACTION_MASK) {
            // starts pinch
            MotionEvent.ACTION_POINTER_DOWN ->

                if (mMovementMode != TRANSLATION_MODE)
                    if (event.pointerCount >= 2) {

                        mMovementMode = TRANSLATION_MODE

                        pinchStartDistance = getPinchDistance(event)
                        pinchStartY = mRenderer.cameraPosY
                        pinchStartZ = mRenderer.cameraPosZ

                        if (objectPresed != -1) {
                            pinchStartFactorX = mDataList[objectPresed].lastScaleFactorX
                            pinchStartFactorY = mDataList[objectPresed].lastScaleFactorY
                            pinchStartFactorZ = mDataList[objectPresed].lastScaleFactorZ
                        }

                        if (pinchStartDistance > 0f) {
                            getPinchCenterPoint(event, pinchStartPoint)
                            mPreviousX = pinchStartPoint.x
                            mPreviousY = pinchStartPoint.y
                            touchMode = TOUCH_ZOOM

                        }

                    }
            MotionEvent.ACTION_DOWN -> {

                mPreviousX = event.x
                mPreviousY = event.y
                mPreviousDragX = mPreviousX
                mPreviousDragY = mPreviousY

                if (mMode != ViewerMainFragment.PRINT_PREVIEW) {

                    if (touchMode == TOUCH_NONE && event.pointerCount == 1) {
                        val objPressed = mRenderer.objectPressed(normalizedX, normalizedY)
                        if (objPressed != -1 && isStl) {
                            mEdition = true
                            objectPresed = objPressed
                            ViewerMainFragment.showActionModePopUpWindow()
                            ViewerMainFragment.displayModelSize(objectPresed)

                            val p = mDataList[objectPresed].lastCenter

                            //Move the camera to the initial values once per frame
                            while (!mRenderer.restoreInitialCameraPosition(p.x, p.y, true, false)) {
                                requestRender()
                            }


                        } else {

                            ViewerMainFragment.hideActionModePopUpWindow()
                            ViewerMainFragment.hideCurrentActionPopUpWindow()
                        }

                    }

                    /*
                Detect double-tapping to restore the panel
                 */


                    if (mDoubleTapFirstTouch && System.currentTimeMillis() - mDoubleTapCurrentTime <= DOUBLE_TAP_MAX_TIME) { //Second touch

                        //do stuff here for double tap
                        mDoubleTapFirstTouch = false

                        //Move the camera to the initial values once per frame
                        while (!mRenderer.restoreInitialCameraPosition(0f, 0f, false, true)) {
                            requestRender()
                        }

                    } else { //First touch

                        mDoubleTapFirstTouch = true
                        mDoubleTapCurrentTime = System.currentTimeMillis()
                    }

                }

                touchMode = TOUCH_DRAG
            }
            MotionEvent.ACTION_MOVE -> {

                if (touchMode == TOUCH_ZOOM && pinchStartDistance > 0f) {

                    pinchScale = getPinchDistance(event) / pinchStartDistance

                    // on pinch
                    val pt = PointF()
                    getPinchCenterPoint(event, pt)

                    mPreviousX = pt.x
                    mPreviousY = pt.y

                    if (mEdition && editionMode == SCALED_EDITION_MODE) {
                        val fx = pinchStartFactorX * pinchScale
                        val fy = pinchStartFactorY * pinchScale
                        val fz = pinchStartFactorZ * pinchScale

                        Log.i("Scale", "Scale touch @$fx;$fy;$fz")
                        mRenderer.scaleObject(fx, fy, fz, false)
                        ViewerMainFragment.displayModelSize(objectPresed)

                    } else {

                        /**
                         * Zoom controls will be limited to MIN and MAX
                         */

                        if (mRenderer.cameraPosY < MIN_ZOOM && pinchScale < 1.0) {


                        } else if (mRenderer.cameraPosY > MAX_ZOOM && pinchScale > 1.0) {


                        } else {
                            mRenderer.cameraPosY = pinchStartY / pinchScale
                            mRenderer.cameraPosZ = pinchStartZ / pinchScale
                        }

                        requestRender()

                    }


                }

                //Drag plate
                if (touchMode != TOUCH_NONE)
                    if (pinchScale < 1.5f) { //Min value to end dragging

                        //Hold its own previous drag
                        val dx = x - mPreviousDragX
                        val dy = y - mPreviousDragY

                        mPreviousDragX = x
                        mPreviousDragY = y


                        if (mEdition && editionMode == MOVE_EDITION_MODE) {
                            mRenderer.dragObject(normalizedX, normalizedY)
                        } else if (!mEdition) dragAccordingToMode(dx, dy) //drag if there is no model


                    }


                requestRender()
            }

            // end pinch
            MotionEvent.ACTION_UP -> {

                mMovementMode = ROTATION_MODE

                if (touchMode == TOUCH_ZOOM) {
                    pinchScale = 1.0f
                    pinchStartPoint.x = 0.0f
                    pinchStartPoint.y = 0.0f
                }

                if (mEdition) {

                    mRenderer.changeTouchedState()

                    Log.i("Slicer", "Callback from surface")
                    ViewerMainFragment.slicingCallback()
                }

                touchMode = TOUCH_NONE

                requestRender()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (touchMode == TOUCH_ZOOM) {
                    pinchScale = 1.0f
                    pinchStartPoint.x = 0.0f
                    pinchStartPoint.y = 0.0f
                }
                if (mEdition) {
                    mRenderer.changeTouchedState()
                    Log.i("Slicer", "Callback from surface")
                    ViewerMainFragment.slicingCallback()
                }
                touchMode = TOUCH_NONE
                requestRender()
            }
        }
        return true
    }

    /**
     * Exit edition mode.
     * Set object pressed to -1 (no object pressed)
     * Set mEditionMode to NONE_EDITION_MODE
     * Change state of the object (which means the colour of the models in the plate will probably change)
     */
    fun exitEditionMode() {

        //We can exit edition mode at clicking in the menu or at deleting a model. If the model has been deleted, it is possible that
        //mRenderer.exitEditionModel fails because of the size of the arrays.

        mEdition = false
        editionMode = NONE_EDITION_MODE


        //Delay the render slightly to avoid inconsistency while drawing models
        val handler = Handler()


        handler.postDelayed({
            objectPresed = -1
            mRenderer.setObjectPressed(objectPresed)


            mRenderer.changeTouchedState()


            requestRender()
        }, 100)


    }

    /**
     * It rotates the plate (ROTATION or TRANSLATION)
     * @param dx movement on x axis
     * @param dy movement on y axis
     */
    private fun dragAccordingToMode(dx: Float, dy: Float) {
        when (mMovementMode) {
            ROTATION_MODE -> doRotation(dx, dy)
            TRANSLATION_MODE -> {
                val scale = -mRenderer.cameraPosY / 500f
                doTranslation(dx * scale, dy * scale)
            }
        }//doTranslation (dx,dy);
    }

    fun doScale(x: Float, y: Float, z: Float, uniform: Boolean) {

        if (mEdition && editionMode == SCALED_EDITION_MODE) {

            val factorX = mDataList[objectPresed].lastScaleFactorX
            val factorY = mDataList[objectPresed].lastScaleFactorY
            val factorZ = mDataList[objectPresed].lastScaleFactorZ

            val data = mDataList[objectPresed]

            val scaleX = x / (data.maxX - data.minX)
            val scaleY = y / (data.maxY - data.minY)
            val scaleZ = z / (data.maxZ - data.minZ)

            var fx = factorX
            var fy = factorY
            var fz = factorZ


            if (!uniform) {
                if (x > 0) fx = factorX * scaleX
                if (y > 0) fy = factorY * scaleY
                if (z > 0) fz = factorZ * scaleZ
            } else {

                if (x > 0) {
                    fx = factorX * scaleX
                    fy = fx
                    fz = fx
                }
                if (y > 0) {
                    fy = factorY * scaleY
                    fx = fy
                    fz = fy
                }
                if (z > 0) {
                    fz = factorZ * scaleZ
                    fx = fz
                    fy = fz
                }
            }

            mRenderer.scaleObject(fx, fy, fz, true)
            ViewerMainFragment.displayModelSize(objectPresed)
            requestRender()

        }
    }

    /**
     * Mirror option
     */
    /*public void doMirror () {
		float fx = mDataList.get(mObjectPressed).getLastScaleFactorX();
		float fy = mDataList.get(mObjectPressed).getLastScaleFactorY();
		float fz = mDataList.get(mObjectPressed).getLastScaleFactorZ();

		mRenderer.scaleObject(-1*fx, fy, fz);
		requestRender();
	}*/

    /**
     * Do rotation (plate rotation, not model rotation)
     * @param dx movement on x axis
     * @param dy movement on y axis
     */
    private fun doRotation(dx: Float, dy: Float) {
        mRenderer.setSceneAngleX(dx * TOUCH_SCALE_FACTOR_ROTATION)
        mRenderer.setSceneAngleY(dy * TOUCH_SCALE_FACTOR_ROTATION)
    }

    /**
     * Do rotation (plate rotation, not model rotation)
     * @param dx movement on x axis
     * @param dy movement on y axis
     */
    private fun doTranslation(dx: Float, dy: Float) {

        //mRenderer.setCenterX(-1);
        //mRenderer.setCenterY(-1);
        mRenderer.matrixTranslate(dx, -dy, 0f)
    }

    /**
     * Movement mode (plate)
     * @param mode
     */
    fun setMovementMode(mode: Int) {
        mMovementMode = mode
    }

    /**
     * Get distanced pinched
     * @param event
     * @return
     */
    private fun getPinchDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }


    /**
     * Get center point
     * @param event
     * @param pt pinched point
     */
    private fun getPinchCenterPoint(event: MotionEvent, pt: PointF) {
        pt.x = (event.getX(0) + event.getX(1)) * 0.5f
        pt.y = (event.getY(0) + event.getY(1)) * 0.5f
    }

    companion object {
        //View Modes
        val NORMAL = 0
        val XRAY = 4
        val TRANSPARENT = 2
        val LAYERS = 3
        val OVERHANG = 1

        //Zoom limits
        val MIN_ZOOM = -500
        val MAX_ZOOM = -30
        val SCALE_FACTOR = 400f

        // for touch event handling
        private val TOUCH_NONE = 0
        private val TOUCH_DRAG = 1
        private val TOUCH_ZOOM = 2

        //Viewer modes
        val ROTATION_MODE = 0
        val TRANSLATION_MODE = 1
        val LIGHT_MODE = 2


        //Edition modes
        val NONE_EDITION_MODE = 0
        val MOVE_EDITION_MODE = 1
        val ROTATION_EDITION_MODE = 2
        val SCALED_EDITION_MODE = 3
        val MIRROR_EDITION_MODE = 4

        val ROTATE_X = 0
        val ROTATE_Y = 1
        val ROTATE_Z = 2

        val DOUBLE_TAP_MAX_TIME = 300
    }
}


