package de.domes_muc.printerappkotlin.util.ui

import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Custom scroll view that allows you to enable or disable the scroll
 */
class LockableScrollView : uk.co.androidalliance.edgeeffectoverride.ScrollView {

    private var mScrollColor: Int = 0

    //True if we can scroll (not locked)
    //False if we cannot scroll (locked)
    var isScrollable = true
        private set

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        setAttrs(attrs)
    }

    private fun setAttrs(attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.LockableScrollView, 0, 0)
            setScrollColor(a.getColor(R.styleable.LockableScrollView_scroll_effect_color, Color.WHITE))
            a.recycle()
        }
    }

    private fun setScrollColor(scrollColor: Int) {
        mScrollColor = scrollColor
        super.setEdgeEffectColor(mScrollColor)
    }

    fun setScrollingEnabled(enabled: Boolean) {
        isScrollable = enabled
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                //If we can scroll pass the event to the superclass
                return if (isScrollable) super.onTouchEvent(ev) else isScrollable
                //Only continue to handle the touch event if scrolling enabled
                // mScrollable is always false at this point
            }
            else -> return super.onTouchEvent(ev)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Don't do anything with intercepted touch events if
        // we are not scrollable
        return if (!isScrollable)
            false
        else
            super.onInterceptTouchEvent(ev)
    }
}
