package de.domes_muc.printerappkotlin.util.ui

import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.PopupWindow

/**
 * Util class that allow to create a pop up window with a custom content view
 */
class CustomPopupWindow {

    private var mContentView: View? = null
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mAnimationId: Int = 0
    private var mOutsideTouchable = false

    //Needed for dismiss the popup window when clicked outside the popup window
    //Set the animation of the pop up window
    //Clear the default translucent background
    val popupWindow: PopupWindow
        get() {

            val popupWindow = PopupWindow(
                mContentView, mWidth, mHeight
            )
            popupWindow.isOutsideTouchable = mOutsideTouchable
            popupWindow.isFocusable = false
            if (mAnimationId > 0) popupWindow.animationStyle = mAnimationId
            popupWindow.setBackgroundDrawable(BitmapDrawable())

            return popupWindow
        }

    /**
     * Initialize the params of the pop up window
     * @param contentView View to be included in the pop up window
     * @param width Width of the pop up window. This need not be the same size of the content view.
     * @param height Height of the pop up window. This need not be the same size of the content view.
     * @param animationStyleId int id of the animation to be included when the pop up window is displayed. If the id
     * is -1, an animation is not be included.
     */
    constructor(contentView: View, width: Int, height: Int, animationStyleId: Int) {
        mContentView = contentView
        mWidth = width
        mHeight = height
        mAnimationId = animationStyleId
    }

    /**
     * Initialize the params of the pop up window
     * @param contentView View to be included in the pop up window
     * @param width Width of the pop up window. This need not be the same size of the content view.
     * @param height Height of the pop up window. This need not be the same size of the content view.
     * @param animationStyleId int id of the animation to be included when the pop up window is displayed. If the id
     * is -1, an animation is not be included.
     * @param outsideTouchable Indicates if the popup window can be dismissed when clicked outside of it
     */
    constructor(contentView: View, width: Int, height: Int, animationStyleId: Int, outsideTouchable: Boolean) {
        mContentView = contentView
        mWidth = width
        mHeight = height
        mAnimationId = animationStyleId
        mOutsideTouchable = outsideTouchable
    }

    fun hidePopup() {

        popupWindow.update(0, 0)

    }

}