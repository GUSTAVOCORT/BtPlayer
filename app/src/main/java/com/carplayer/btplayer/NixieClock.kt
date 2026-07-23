package com.carplayer.btplayer

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Calendar

/**
 * Reloj estilo Nixie: cada digito en su "tubo" con un halo de color distinto
 * (rojo, verde, cyan, azul) imitando la foto de referencia. El digito tiene
 * glow ambar calido tipo filamento y el tubo un borde iluminado.
 */
class NixieClock @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var glow = true
    var use24h = false

    private val tubeColors = intArrayOf(
        Color.parseColor("#FF3B3B"),  // rojo
        Color.parseColor("#39FF6A"),  // verde
        Color.parseColor("#00E5FF"),  // cyan
        Color.parseColor("#2E7BFF")   // azul
    )
    private val filament = Color.parseColor("#FF9A2E")   // ambar del digito

    private val digitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = filament
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = filament
    }
    private val tubePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val tubeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val ui = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { invalidate(); ui.postDelayed(this, 1000L) }
    }

    fun startClock() { ui.removeCallbacks(tick); ui.post(tick) }
    fun stopClock() { ui.removeCallbacks(tick) }

    override fun onDetachedFromWindow() { stopClock(); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        val cal = Calendar.getInstance()
        var hh = cal.get(if (use24h) Calendar.HOUR_OF_DAY else Calendar.HOUR)
        if (!use24h && hh == 0) hh = 12
        val mm = cal.get(Calendar.MINUTE)
        val digits = intArrayOf(hh / 10, hh % 10, mm / 10, mm % 10)

        val w = width.toFloat(); val h = height.toFloat()
        val n = 4
        val gapRatio = 0.18f
        val totalGap = w * gapRatio
        val tubeW = (w - totalGap) / n
        val tubeGap = totalGap / (n + 1)
        val tubeH = h * 0.82f
        val top = (h - tubeH) / 2f

        val textSize = tubeH * 0.62f
        digitPaint.textSize = textSize
        glowPaint.textSize = textSize
        val fm = digitPaint.fontMetrics
        val baseY = top + tubeH / 2f - (fm.ascent + fm.descent) / 2f

        var x = tubeGap
        for (i in 0 until n) {
            val tubeColor = tubeColors[i % tubeColors.size]
            val rect = RectF(x, top, x + tubeW, top + tubeH)

            // cuerpo del tubo: oscuro con leve tinte del color
            tubePaint.color = Color.argb(40, Color.red(tubeColor), Color.green(tubeColor), Color.blue(tubeColor))
            canvas.drawRoundRect(rect, tubeW * 0.18f, tubeW * 0.18f, tubePaint)

            // borde iluminado del tubo
            tubeBorder.strokeWidth = tubeW * 0.04f
            tubeBorder.color = tubeColor
            if (glow) {
                tubeBorder.maskFilter = BlurMaskFilter(tubeW * 0.10f, BlurMaskFilter.Blur.NORMAL)
                canvas.drawRoundRect(rect, tubeW * 0.18f, tubeW * 0.18f, tubeBorder)
                tubeBorder.maskFilter = null
            }
            canvas.drawRoundRect(rect, tubeW * 0.18f, tubeW * 0.18f, tubeBorder)

            // digito con glow ambar
            val cx = x + tubeW / 2f
            val d = digits[i].toString()
            if (glow) {
                glowPaint.maskFilter = BlurMaskFilter(textSize * 0.14f, BlurMaskFilter.Blur.NORMAL)
                canvas.drawText(d, cx, baseY, glowPaint)
                glowPaint.maskFilter = null
            }
            canvas.drawText(d, cx, baseY, digitPaint)

            x += tubeW + tubeGap
        }

        // dos puntos parpadeantes entre HH y MM
        if (cal.get(Calendar.SECOND) % 2 == 0) {
            val cx = w / 2f
            val rDot = w * 0.008f
            digitPaint.maskFilter = if (glow) BlurMaskFilter(rDot, BlurMaskFilter.Blur.NORMAL) else null
            canvas.drawCircle(cx, top + tubeH * 0.36f, rDot, digitPaint)
            canvas.drawCircle(cx, top + tubeH * 0.64f, rDot, digitPaint)
            digitPaint.maskFilter = null
        }
    }
}
