/**
 *
 */
package de.domes_muc.printerappkotlin.util.ui

import java.util.ArrayList

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.RelativeLayout

/**
 * Extension of a relative layout to provide a checkable behaviour
 *
 * @author marvinlabs
 */
class CheckableRelativeLayout : RelativeLayout, Checkable {

    private var isChecked: Boolean = false
    private var checkableViews: MutableList<Checkable>? = null
    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    /**
     * Interface definition for a callback to be invoked when the checked state of a CheckableRelativeLayout changed.
     */
    interface OnCheckedChangeListener {
        fun onCheckedChanged(layout: CheckableRelativeLayout, isChecked: Boolean)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initialise(attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialise(attrs)
    }

    constructor(context: Context, checkableId: Int) : super(context) {
        initialise(null)
    }

    /*
	 * @see android.widget.Checkable#isChecked()
	 */
    override fun isChecked(): Boolean {
        return isChecked
    }

    /*
	 * @see android.widget.Checkable#setChecked(boolean)
	 */
    override fun setChecked(isChecked: Boolean) {
        this.isChecked = isChecked
        for (c in checkableViews!!) {
            c.isChecked = isChecked
        }

        if (onCheckedChangeListener != null) {
            onCheckedChangeListener!!.onCheckedChanged(this, isChecked)
        }
    }

    /*
	 * @see android.widget.Checkable#toggle()
	 */
    override fun toggle() {
        this.isChecked = !this.isChecked
        for (c in checkableViews!!) {
            c.toggle()
        }
    }

    fun setOnCheckedChangeListener(onCheckedChangeListener: OnCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val childCount = this.childCount
        for (i in 0 until childCount) {
            findCheckableChildren(this.getChildAt(i))
        }
    }

    /**
     * Read the custom XML attributes
     */
    private fun initialise(attrs: AttributeSet?) {
        this.isChecked = false
        this.checkableViews = ArrayList(5)
    }

    /**
     * Add to our checkable list all the children of the view that implement the interface Checkable
     */
    private fun findCheckableChildren(v: View) {
        if (v is Checkable) {
            this.checkableViews!!.add(v as Checkable)
        }

        if (v is ViewGroup) {
            val childCount = v.childCount
            for (i in 0 until childCount) {
                findCheckableChildren(v.getChildAt(i))
            }
        }
    }
}
