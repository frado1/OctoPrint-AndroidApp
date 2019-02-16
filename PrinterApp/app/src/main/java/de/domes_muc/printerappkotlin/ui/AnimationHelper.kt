package de.domes_muc.printerappkotlin.util.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout

/**
 * Created by alberto-baeza on 11/20/14.
 */
class AnimationHelper {

    private var mCaptureAnimator: AnimatorSet? = null

    /**
     * Translate a view in the x axis
     * @param view View to be translated
     * @param from Initial position
     * @param to   FInal position
     */
    fun translateXAnimation(view: View, from: Float, to: Float) {

        if (mCaptureAnimator != null && mCaptureAnimator!!.isStarted) {
            mCaptureAnimator!!.cancel()
        }

        mCaptureAnimator = AnimatorSet()
        mCaptureAnimator!!.playTogether(
            ObjectAnimator.ofFloat(view, "x", from, to)
                .setDuration(TRANSLATE_ANIMATION_DURATION.toLong())
        )
        mCaptureAnimator!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {
                view.isClickable = false
                view.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animator: Animator) {
                view.isClickable = true
                if (mCaptureAnimator != null) {
                    mCaptureAnimator!!.removeAllListeners()
                }
                mCaptureAnimator = null
            }

            override fun onAnimationCancel(animator: Animator) {
                view.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(animator: Animator) {
                // Do nothing.
            }
        })
        mCaptureAnimator!!.start()
    }

    companion object {
        val TRANSLATE_ANIMATION_DURATION = 400
        val ALPHA_DURATION = 700


        /**
         * To animate view slide out from right to left
         *
         * @param view
         */
        fun slideToLeft(view: View) {
            val animate = TranslateAnimation(40f, 0f, 0f, 0f)
            animate.duration = 500
            animate.fillAfter = false
            view.startAnimation(animate)
        }

        /**
         * Animate fragments
         *
         * @param view
         */
        fun inFromRightAnimation(view: View) {
            val inFromRight = TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f
            )
            inFromRight.duration = 240
            inFromRight.interpolator = AccelerateInterpolator()
            view.startAnimation(inFromRight)
        }
    }


}
