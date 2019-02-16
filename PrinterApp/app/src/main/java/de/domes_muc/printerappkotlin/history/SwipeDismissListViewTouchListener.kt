/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.domes_muc.printerappkotlin.history

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView

import java.util.ArrayList
import java.util.Collections

/**
 * A [android.view.View.OnTouchListener] that makes the list items in a [ListView]
 * dismissable. [ListView] is given special treatment because by default it handles touches
 * for its list items... i.e. it's in charge of drawing the pressed state (the list selector),
 * handling list item clicks, etc.
 *
 *
 * After creating the listener, the caller should also call [ ][ListView.setOnScrollListener], passing in the scroll
 * listener returned by [.makeScrollListener]. If a scroll listener is already assigned, the
 * caller should still pass scroll changes through to this listener. This will ensure that this
 * [SwipeDismissListViewTouchListener] is paused during list view scrolling.
 *
 *
 * Example usage:
 *
 * <pre>
 * SwipeDismissListViewTouchListener touchListener =
 * new SwipeDismissListViewTouchListener(
 * listView,
 * new SwipeDismissListViewTouchListener.OnDismissCallback() {
 * public void onDismiss(ListView listView, int[] reverseSortedPositions) {
 * for (int position : reverseSortedPositions) {
 * adapter.remove(adapter.getItem(position));
 * }
 * adapter.notifyDataSetChanged();
 * }
 * });
 * listView.setOnTouchListener(touchListener);
 * listView.setOnScrollListener(touchListener.makeScrollListener());
</pre> *
 *
 *
 * This class Requires API level 12 or later due to use of [ ].
 */
class SwipeDismissListViewTouchListener
/**
 * Constructs a new swipe-to-dismiss touch listener for the given list view.
 *
 * @param listView  The list view whose items should be dismissable.
 * @param callbacks The callback to trigger when the user has indicated that she would like to
 * dismiss one or more list items.
 */
    (// Fixed properties
    private val mListView: ListView, private val mCallbacks: DismissCallbacks
) : View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private val mSlop: Int
    private val mMinFlingVelocity: Int
    private val mMaxFlingVelocity: Int
    private val mAnimationTime: Long
    private var mViewWidth = 1 // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private val mPendingDismisses = ArrayList<PendingDismissData>()
    private var mDismissAnimationRefCount = 0
    private var mDownX: Float = 0.toFloat()
    private var mSwiping: Boolean = false
    private var mVelocityTracker: VelocityTracker? = null
    private var mDownPosition: Int = 0
    private var mDownView: View? = null
    private var mPaused: Boolean = false

    /**
     * The callback interface used by [SwipeDismissListViewTouchListener] to inform its client
     * about a successful dismissal of one or more list item positions.
     */
    interface DismissCallbacks {
        /**
         * Called to determine whether the given position can be dismissed.
         */
        fun canDismiss(position: Int): Boolean

        /**
         * Called when the user has indicated they she would like to dismiss one or more list item
         * positions.
         *
         * @param listView               The originating [ListView].
         * @param reverseSortedPositions An array of positions to dismiss, sorted in descending
         * order for convenience.
         */
        fun onDismiss(listView: ListView, reverseSortedPositions: IntArray)
    }

    init {
        val vc = ViewConfiguration.get(mListView.context)
        mSlop = vc.scaledTouchSlop
        mMinFlingVelocity = vc.scaledMinimumFlingVelocity * 16
        mMaxFlingVelocity = vc.scaledMaximumFlingVelocity
        mAnimationTime = mListView.context.resources.getInteger(
            android.R.integer.config_shortAnimTime
        ).toLong()
    }

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    fun setEnabled(enabled: Boolean) {
        mPaused = !enabled
    }

    /**
     * Returns an [android.widget.AbsListView.OnScrollListener] to be added to the [ ] using [ListView.setOnScrollListener].
     * If a scroll listener is already assigned, the caller should still pass scroll changes through
     * to this listener. This will ensure that this [SwipeDismissListViewTouchListener] is
     * paused during list view scrolling.
     *
     * @see SwipeDismissListViewTouchListener
     */
    fun makeScrollListener(): AbsListView.OnScrollListener {
        return object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(absListView: AbsListView, scrollState: Int) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
            }

            override fun onScroll(absListView: AbsListView, i: Int, i1: Int, i2: Int) {}
        }
    }

    /**
     * Manually cause the item at the given position to be dismissed (trigger the dismiss
     * animation).
     */
    fun dismiss(position: Int) {
        dismiss(getViewForPosition(position), position, true)
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (mViewWidth < 2) {
            mViewWidth = mListView.width
        }

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> run {
                if (mPaused) {
                    return false
                }

                // TODO: ensure this is a finger, and set a flag

                // Find the child view that was touched (perform a hit test)
                val rect = Rect()
                val childCount = mListView.childCount
                val listViewCoords = IntArray(2)
                mListView.getLocationOnScreen(listViewCoords)
                val x = motionEvent.rawX.toInt() - listViewCoords[0]
                val y = motionEvent.rawY.toInt() - listViewCoords[1]
                var child: View
                for (i in 0 until childCount) {
                    child = mListView.getChildAt(i)
                    child.getHitRect(rect)
                    if (rect.contains(x, y)) {
                        mDownView = child
                        return@run
                    }
                }

                if (mDownView != null) {
                    mDownX = motionEvent.rawX
                    mDownPosition = mListView.getPositionForView(mDownView)
                    if (mCallbacks.canDismiss(mDownPosition)) {
                        mVelocityTracker = VelocityTracker.obtain()
                        mVelocityTracker!!.addMovement(motionEvent)
                    } else {
                        mDownView = null
                    }
                }
                view.onTouchEvent(motionEvent)
                return true
            }

            MotionEvent.ACTION_UP -> run {
                if (mVelocityTracker == null) {
                    return@run
                }

                val deltaX = motionEvent.rawX - mDownX
                mVelocityTracker!!.addMovement(motionEvent)
                mVelocityTracker!!.computeCurrentVelocity(1000)
                val velocityX = mVelocityTracker!!.xVelocity
                val absVelocityX = Math.abs(velocityX)
                val absVelocityY = Math.abs(mVelocityTracker!!.yVelocity)
                var dismiss = false
                var dismissRight = false
                if (Math.abs(deltaX) > mViewWidth / 2) {
                    dismiss = true
                    dismissRight = deltaX > 0
                } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                    && absVelocityY < absVelocityX
                ) {
                    // dismiss only if flinging in the same direction as dragging
                    dismiss = velocityX < 0 == deltaX < 0
                    dismissRight = mVelocityTracker!!.xVelocity > 0
                }
                if (dismiss) {
                    // dismiss
                    dismiss(mDownView, mDownPosition, dismissRight)
                } else {
                    // cancel
                    mDownView!!.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(mAnimationTime)
                        .setListener(null)
                }
                mVelocityTracker!!.recycle()
                mVelocityTracker = null
                mDownX = 0f
                mDownView = null
                mDownPosition = ListView.INVALID_POSITION
                mSwiping = false
            }

            MotionEvent.ACTION_CANCEL -> run {
                if (mVelocityTracker == null) {
                    return@run
                }

                if (mDownView != null) {
                    // cancel
                    mDownView!!.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(mAnimationTime)
                        .setListener(null)
                }
                mVelocityTracker!!.recycle()
                mVelocityTracker = null
                mDownX = 0f
                mDownView = null
                mDownPosition = ListView.INVALID_POSITION
                mSwiping = false
            }

            MotionEvent.ACTION_MOVE -> run {
                if (mVelocityTracker == null || mPaused) {
                    return@run
                }

                mVelocityTracker!!.addMovement(motionEvent)
                val deltaX = motionEvent.rawX - mDownX
                if (Math.abs(deltaX) > mSlop) {
                    mSwiping = true
                    mListView.requestDisallowInterceptTouchEvent(true)

                    // Cancel ListView's touch (un-highlighting the item)
                    val cancelEvent = MotionEvent.obtain(motionEvent)
                    cancelEvent.action = MotionEvent.ACTION_CANCEL or
                            (motionEvent.actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                    mListView.onTouchEvent(cancelEvent)
                    cancelEvent.recycle()
                }

                if (mSwiping) {
                    mDownView!!.translationX = deltaX
                    mDownView!!.alpha = Math.max(
                        0.15f, Math.min(
                            1f,
                            1f - 2f * Math.abs(deltaX) / mViewWidth
                        )
                    )
                    return true
                }
            }
        }
        return false
    }

    private fun dismiss(view: View?, position: Int, dismissRight: Boolean) {
        ++mDismissAnimationRefCount
        if (view == null) {
            // No view, shortcut to calling onDismiss to let it deal with adapter
            // updates and all that.
            mCallbacks.onDismiss(mListView, intArrayOf(position))
            return
        }

        view.animate()
            .translationX((if (dismissRight) mViewWidth else -mViewWidth).toFloat())
            .alpha(0f)
            .setDuration(mAnimationTime)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    performDismiss(view, position)
                }
            })
    }

    private fun getViewForPosition(position: Int): View? {
        val index = position - (mListView.firstVisiblePosition - mListView.headerViewsCount)
        return if (index >= 0 && index < mListView.childCount)
            mListView.getChildAt(index)
        else
            null
    }

    internal inner class PendingDismissData(var position: Int, var view: View) : Comparable<PendingDismissData> {

        override fun compareTo(other: PendingDismissData): Int {
            // Sort by descending position
            return other.position - position
        }
    }

    private fun performDismiss(dismissView: View, dismissPosition: Int) {
        // Animate the dismissed list item to zero-height and fire the dismiss callback when
        // all dismissed list item animations have completed. This triggers layout on each animation
        // frame; in the future we may want to do something smarter and more performant.

        val lp = dismissView.layoutParams
        val originalHeight = dismissView.height

        val animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime)

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                --mDismissAnimationRefCount
                if (mDismissAnimationRefCount == 0) {
                    // No active animations, process all pending dismisses.
                    // Sort by descending position
                    Collections.sort(mPendingDismisses)

                    val dismissPositions = IntArray(mPendingDismisses.size)
                    for (i in mPendingDismisses.indices.reversed()) {
                        dismissPositions[i] = mPendingDismisses[i].position
                    }
                    mCallbacks.onDismiss(mListView, dismissPositions)

                    var lp: ViewGroup.LayoutParams
                    for (pendingDismiss in mPendingDismisses) {
                        // Reset view presentation
                        pendingDismiss.view.alpha = 1f
                        pendingDismiss.view.translationX = 0f
                        lp = pendingDismiss.view.layoutParams
                        lp.height = originalHeight
                        pendingDismiss.view.layoutParams = lp
                    }

                    mPendingDismisses.clear()
                }
            }
        })

        animator.addUpdateListener { valueAnimator ->
            lp.height = valueAnimator.animatedValue as Int
            dismissView.layoutParams = lp
        }

        mPendingDismisses.add(PendingDismissData(dismissPosition, dismissView))
        animator.start()
    }
}
