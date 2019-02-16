package de.domes_muc.printerappkotlin.util.ui

import android.app.Dialog
import de.domes_muc.printerappkotlin.Log
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.RelativeLayout
import android.widget.TextView

import com.gc.materialdesign.R
import com.gc.materialdesign.utils.Utils
import com.gc.materialdesign.views.CustomView
import com.nineoldandroids.view.ViewHelper


/**
 * Copy of the Slider class by MaterialDesign
 */

class CustomEditableSlider(context: Context, attrs: AttributeSet) : CustomView(context, attrs) {

    internal var backgroundColor = Color.parseColor("#4CAF50")

    internal var ball = Ball(context)
    internal var numberIndicator: NumberIndicator? = null

    internal var showNumberIndicator = false
    internal var press = false

    internal var value = 0
    var shownValue : Int = 0
//        set(value) { field = value }

    var max = 100
    var min = 0

    // GETERS & SETTERS

    var onValueChangedListener: OnValueChangedListener? = null

    var isShowNumberIndicator: Boolean
        get() = showNumberIndicator
        set(showNumberIndicator) {
            this.showNumberIndicator = showNumberIndicator
            numberIndicator = if (showNumberIndicator)
                NumberIndicator(
                    context
                )
            else
                null
        }

    internal var placedBall = false

    // Event when slider change value
    interface OnValueChangedListener {
        fun onValueChanged(value: Int)
    }

    init {
        setAttributes(attrs)
    }

    // Set atributtes of XML to View
    protected fun setAttributes(attrs: AttributeSet) {

        setBackgroundResource(R.drawable.background_transparent)

        // Set size of view
        minimumHeight = Utils.dpToPx(48f, resources)
        minimumWidth = Utils.dpToPx(80f, resources)
        //ball = Ball(context)
        val params = RelativeLayout.LayoutParams(
            Utils.dpToPx(
                20f,
                resources
            ), Utils.dpToPx(20f, resources)
        )
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
        ball.layoutParams = params
        addView(ball)

        // Set if slider content number indicator
        if (showNumberIndicator) {
            numberIndicator = NumberIndicator(context)
        }

    }

    override fun invalidate() {
        ball.invalidate()
        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!placedBall)
            placeBall()

        if (value == DEFAULT_SLIDER_VALUE) {
            // Crop line to transparent effect
            val bitmap = Bitmap.createBitmap(
                canvas.width,
                canvas.height, Bitmap.Config.ARGB_8888
            )
            val temp = Canvas(bitmap)
            val paint = Paint()
            paint.color = Color.parseColor("#B0B0B0")
            paint.strokeWidth = Utils.dpToPx(2f, resources).toFloat()
            temp.drawLine(
                (height / 2).toFloat(),
                (height / 2).toFloat(),
                (width - height / 2).toFloat(),
                (height / 2).toFloat(),
                paint
            )
            val transparentPaint = Paint()
            transparentPaint.color = resources.getColor(
                android.R.color.transparent
            )
            transparentPaint.xfermode = PorterDuffXfermode(
                PorterDuff.Mode.CLEAR
            )
            temp.drawCircle(
                ViewHelper.getX(ball) + ball.width / 2,
                ViewHelper.getY(ball) + ball.height / 2,
                (ball.width / 2).toFloat(), transparentPaint
            )

            canvas.drawBitmap(bitmap, 0f, 0f, Paint())
        } else {
            val paint = Paint()
            paint.color = Color.parseColor("#B0B0B0")
            paint.strokeWidth = Utils.dpToPx(2f, resources).toFloat()
            canvas.drawLine(
                (height / 2).toFloat(),
                (height / 2).toFloat(),
                (width - height / 2).toFloat(),
                (height / 2).toFloat(),
                paint
            )
            paint.color = backgroundColor
            val division = (ball.xFin - ball.xIni) / (max - min)
            val value = this.value - min
            canvas.drawLine(
                (height / 2).toFloat(),
                (height / 2).toFloat(),
                value * division + height / 2,
                (height / 2).toFloat(),
                paint
            )

        }

        if (press && !showNumberIndicator) {
            val paint = Paint()
            paint.color = backgroundColor
            paint.isAntiAlias = true
            canvas.drawCircle(
                ViewHelper.getX(ball) + ball.width / 2,
                (height / 2).toFloat(), (height / 3).toFloat(), paint
            )
        }
        invalidate()

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        isLastTouch = true
        if (isEnabled) {
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                if (numberIndicator != null && numberIndicator!!.isShowing == false)
                    numberIndicator!!.show()
                if (event.x <= width && event.x >= 0) {
                    press = true
                    // calculate value
                    var newValue = 0
                    val division = (ball.xFin - ball.xIni) / (max - min)
                    if (event.x > ball.xFin) {
                        newValue = max
                    } else if (event.x < ball.xIni) {
                        newValue = min
                    } else {
                        newValue = min + ((event.x - ball.xIni) / division).toInt()
                    }
                    if (value != newValue) {
                        value = newValue
                        if (onValueChangedListener != null)
                            onValueChangedListener!!.onValueChanged(newValue)
                    }
                    // move ball indicator
                    var x = event.x
                    x = if (x < ball.xIni) ball.xIni else x
                    x = if (x > ball.xFin) ball.xFin else x
                    ViewHelper.setX(ball, x)
                    ball.changeBackground()

                    // If slider has number indicator
                    if (numberIndicator != null) {
                        // move number indicator
                        numberIndicator!!.indicator.x = x
                        numberIndicator!!.indicator.finalY = (Utils
                            .getRelativeTop(this) - height / 2).toFloat()
                        numberIndicator!!.indicator.finalSize = (height / 2).toFloat()
                        numberIndicator!!.numberIndicator.text = ""
                    }

                } else {
                    press = false
                    isLastTouch = false
                    if (numberIndicator != null)
                        numberIndicator!!.dismiss()

                }

            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (numberIndicator != null)
                    numberIndicator!!.dismiss()
                isLastTouch = false
                press = false
            }
        }
        return true
    }

    /**
     * Make a dark color to press effect
     *
     * @return
     */
    protected fun makePressColor(): Int {
        var r = this.backgroundColor shr 16 and 0xFF
        var g = this.backgroundColor shr 8 and 0xFF
        var b = this.backgroundColor shr 0 and 0xFF
        r = if (r - 30 < 0) 0 else r - 30
        g = if (g - 30 < 0) 0 else g - 30
        b = if (b - 30 < 0) 0 else b - 30
        return Color.argb(70, r, g, b)
    }

    private fun placeBall() {
        ViewHelper.setX(ball, (height / 2 - ball.width / 2).toFloat())
        ball.xIni = ViewHelper.getX(ball)
        ball.xFin = (width - height / 2 - ball.width / 2).toFloat()
        ball.xCen = (width / 2 - ball.width / 2).toFloat()
        placedBall = true
    }

    fun getValue(): Int {
        return value
    }

    fun setValue(value: Int) {
        if (placedBall == false)
            post { setValue(value) }
        else {
            this.value = value
            val division = (ball.xFin - ball.xIni) / max
            ViewHelper.setX(
                ball,
                value * division + height / 2 - ball.width / 2
            )
            ball.changeBackground()
        }


    }

//    fun setShownValue(value: Int) {
//        this.shownValue = value
//    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
    }

    internal inner class Ball(context: Context) : View(context) {

        var xIni: Float = 0.toFloat()
        var xFin: Float = 0.toFloat()
        var xCen: Float = 0.toFloat()

        init {
            setBackgroundResource(R.drawable.background_switch_ball_uncheck)
        }

        fun changeBackground() {
            if (value != DEFAULT_SLIDER_VALUE) { //Modified min value for empty ball
                setBackgroundResource(R.drawable.background_checkbox)
                val layer = background as LayerDrawable
                val shape = layer
                    .findDrawableByLayerId(R.id.shape_bacground) as GradientDrawable
                shape.setColor(backgroundColor)
            } else {
                setBackgroundResource(R.drawable.background_switch_ball_uncheck)
            }
        }

    }

    // Slider Number Indicator

    internal inner class NumberIndicator(context: Context) : Dialog(context, android.R.style.Theme_Translucent) {

        lateinit var indicator: Indicator
        lateinit var numberIndicator: TextView

        override fun onCreate(savedInstanceState: Bundle) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            super.onCreate(savedInstanceState)
            setContentView(R.layout.number_indicator_spinner)
            setCanceledOnTouchOutside(false)

            val content = this
                .findViewById<View>(R.id.number_indicator_spinner_content) as RelativeLayout
            indicator = Indicator(this.context)
            content.addView(indicator)

            numberIndicator = TextView(context)
            numberIndicator.setTextColor(Color.WHITE)
            numberIndicator.gravity = Gravity.CENTER
            content.addView(numberIndicator)

            indicator.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT
            )
        }

        override fun dismiss() {
            super.dismiss()
            indicator.y = 0f
            indicator.size = 0f
            indicator.animate = true
        }

        override fun onBackPressed() {}

    }

    internal inner class Indicator(context: Context) : RelativeLayout(context) {

        // Position of number indicator
        var xPos = 0f
        var yPos = 0f

        // Size of number indicator
        var size = 0f

        // Final y position after animation
        var finalY = 0f
        // Final size after animation
        var finalSize = 0f

        var animate = true

        var numberIndicatorResize = false

        init {
            setBackgroundColor(
                resources.getColor(
                    android.R.color.transparent
                )
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            if (numberIndicatorResize == false) {
                val params = numberIndicator!!.numberIndicator
                    .layoutParams as RelativeLayout.LayoutParams
                params.height = finalSize.toInt() * 2
                params.width = finalSize.toInt() * 2
                numberIndicator!!.numberIndicator.layoutParams = params
            }

            val paint = Paint()
            paint.isAntiAlias = true
            paint.color = backgroundColor
            if (animate) {
                if (y == 0f)
                    y = finalY + finalSize * 2
                y -= Utils.dpToPx(6f, resources).toFloat()
                size += Utils.dpToPx(2f, resources).toFloat()
            }
            canvas.drawCircle(
                ViewHelper.getX(ball)
                        + Utils.getRelativeLeft(ball.parent as View).toFloat()
                        + (ball.width / 2).toFloat(), y, size, paint
            )
            if (animate && size >= finalSize)
                animate = false
            if (animate == false) {
                ViewHelper
                    .setX(
                        numberIndicator!!.numberIndicator,
                        (ViewHelper.getX(ball)
                                + Utils.getRelativeLeft(
                            ball
                                .parent as View
                        ).toFloat() + (ball.width / 2).toFloat()) - size
                    )
                ViewHelper.setY(numberIndicator!!.numberIndicator, y - size)
                numberIndicator!!.numberIndicator.text = shownValue.toString() + "º" //Modified shown value
            }

            invalidate()
        }

    }

    companion object {

        val DEFAULT_SLIDER_VALUE = 12
    }
}