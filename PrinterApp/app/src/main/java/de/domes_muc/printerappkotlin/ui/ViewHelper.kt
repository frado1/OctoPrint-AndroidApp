package de.domes_muc.printerappkotlin.util.ui

import de.domes_muc.printerappkotlin.R
import android.media.Image
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView

import com.material.widget.PaperButton

import java.lang.reflect.ParameterizedType

/**
 * Created by alberto-baeza on 2/26/15.
 */
object ViewHelper {

    fun disableEnableAllViews(enable: Boolean, vg: ViewGroup) {


        for (i in 0 until vg.childCount) {
            val child = vg.getChildAt(i)
            child.isEnabled = enable
            if (child is ViewGroup) {
                disableEnableAllViews(enable, child)
            }

            if (child is PaperButton) {
                child.isClickable = enable
                child.refreshTextColor(enable)
            }

            if (child is LockableScrollView) {
                val scrollView = child as LockableScrollView
                scrollView.setScrollingEnabled(enable)
                scrollView.setVerticalScrollBarEnabled(enable)
                scrollView.setHorizontalScrollBarEnabled(enable)
            }
        }
    }
}
