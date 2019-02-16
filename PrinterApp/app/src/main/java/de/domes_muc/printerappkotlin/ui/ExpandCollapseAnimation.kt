package de.domes_muc.printerappkotlin.util.ui

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.FrameLayout

/**
 * Expand or collapse a specific view
 *
 * @author sara
 */
object ExpandCollapseAnimation {

    /**
     * Expand a view to its maximum height.
     *
     * @param view View to be expanded
     */
    fun expand(view: View) {
        view.measure(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        val targetHeight = view.measuredHeight

        view.layoutParams.height = 0

        if (view.visibility == View.INVISIBLE) view.visibility = View.VISIBLE
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                //                view.getLayoutParams().height = interpolatedTime == 1
                //                        ? FrameLayout.LayoutParams.WRAP_CONTENT
                //                        : (int) (targetHeight * interpolatedTime);
                view.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                view.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        //Duration of the animation: 1dp/ms
        a.duration = (targetHeight / view.context.resources.displayMetrics.density).toInt().toLong()
        view.startAnimation(a)
    }

    /**
     * Collapse a view to its minimum height.
     *
     * @param view      View to be collapsed
     * @param newHeight Height to be applied to the view. If this value is 0, it is applied an
     * automatic height depending on the interpolatedTime and the initial
     * height
     */
    fun collapse(view: View, newHeight: Int) {
        val initialHeight = view.measuredHeight

        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (newHeight != 0) {
                    view.layoutParams.height = newHeight
                    view.requestLayout()
                } else {
                    if (interpolatedTime == 1f) {
                        view.visibility = View.GONE
                    } else {
                        view.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                        view.requestLayout()
                    }
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        //Duration of the animation: 1dp/ms
        a.duration = (initialHeight / view.context.resources.displayMetrics.density).toInt().toLong()
        view.startAnimation(a)
    }
}
