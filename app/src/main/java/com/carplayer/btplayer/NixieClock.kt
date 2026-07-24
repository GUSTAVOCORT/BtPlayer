package com.carplayer.btplayer

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reloj Nixie legible: el digito se dibuja SOLIDO y nitido; el glow va DETRAS
 * como halo, sin difuminar el numero. En hardware viejo el blur sobre el
 * propio texto lo borraba, por eso antes desaparecia. Aca el nucleo del
 * digito siempre es solido.
 */
class NixieClock @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var glow = true
    var use24h = false

    private val coreColor = Color.parseColor("#FFC66B")     // digito claro
    private val glowColor = Color.parseColor("#FF7A18")
    private val deepGlow = Color.parseColor("#E8420E")

    private val digitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.parseColor("#2E2016")
    }
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)

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
        val gapRatio = 0.14f
        val totalGap = w * gapRatio
        val tubeW = (w - totalGap) / n
        val tubeGap = totalGap / (n + 1)
        val tubeH = (tubeW * 2.1f).coerceAtMost(h * 0.92f)
        val top = (h - tubeH) / 2f

        var x = tubeGap
        for (i in 0 until n) {
            drawTube(canvas, x, top, tubeW, tubeH, digits[i])
            x += tubeW + tubeGap
        }

        if (cal.get(Calendar.SECOND) % 2 == 0) {
            val cx = w / 2f
            val dotR = tubeW * 0.055f
            drawDot(canvas, cx, top + tubeH * 0.4f, dotR)
            drawDot(canvas, cx, top + tubeH * 0.6f, dotR)
        }
    }

    private fun drawTube(canvas: Canvas, x: Float, top: Float, tw: Float, th: Float, digit: Int) {
        val rect = RectF(x, top, x + tw, top + th)
        val corner = tw * 0.2f

        // cuerpo del tubo
        glassPaint.shader = LinearGradient(x, top, x + tw, top + th,
            intArrayOf(Color.parseColor("#1E1610"), Color.parseColor("#0C0906")),
            null, Shader.TileMode.CLAMP)
        glassPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, corner, corner, glassPaint)
        glassPaint.shader = null

        // panal hexagonal tenue
        canvas.save()
        canvas.clipRect(rect)
        drawHexMesh(canvas, x, top, tw, th)
        canvas.restore()

        val cx = x + tw / 2f
        val ts = th * 0.58f
        digitPaint.textSize = ts
        glowPaint.textSize = ts
        val fm = digitPaint.fontMetrics
        val by = top + th / 2f - (fm.ascent + fm.descent) / 2f
        val d = digit.toString()

        // GLOW detras (difuso) — solo si glow activo
        if (glow) {
            glowPaint.color = deepGlow
            glowPaint.alpha = 110
            glowPaint.maskFilter = BlurMaskFilter(ts * 0.22f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawText(d, cx, by, glowPaint)
            glowPaint.color = glowColor
            glowPaint.alpha = 160
            glowPaint.maskFilter = BlurMaskFilter(ts * 0.10f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawText(d, cx, by, glowPaint)
            glowPaint.maskFilter = null
        }
        // NUCLEO del digito: SIEMPRE solido y nitido (nunca se difumina)
        digitPaint.color = coreColor
        digitPaint.maskFilter = null
        canvas.drawText(d, cx, by, digitPaint)

        // reflejo de vidrio arriba
        glassPaint.shader = LinearGradient(x, top, x, top + th * 0.4f,
            intArrayOf(Color.parseColor("#33FFFFFF"), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(x, top, x + tw, top + th * 0.4f), corner, corner, glassPaint)
        glassPaint.shader = null

        // borde
        glassPaint.style = Paint.Style.STROKE
        glassPaint.strokeWidth = tw * 0.018f
        glassPaint.color = Color.parseColor("#40FFFFFF")
        canvas.drawRoundRect(rect, corner, corner, glassPaint)
        glassPaint.style = Paint.Style.FILL
    }

    private fun drawHexMesh(canvas: Canvas, x: Float, top: Float, tw: Float, th: Float) {
        val hexR = tw * 0.1f
        val hStep = hexR * 1.5f
        val vStep = hexR * 0.866f
        var row = 0
        var cy = top + hexR
        while (cy < top + th) {
            val offset = if (row % 2 == 0) 0f else hStep * 0.5f
            var cx = x + offset
            while (cx < x + tw + hexR) {
                drawHex(canvas, cx, cy, hexR); cx += hStep
            }
            cy += vStep; row++
        }
    }

    private fun drawHex(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val p = Path()
        for (k in 0..6) {
            val ang = Math.toRadians((60 * k - 30).toDouble())
            val px = cx + r * cos(ang).toFloat()
            val py = cy + r * sin(ang).toFloat()
            if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        canvas.drawPath(p, meshPaint)
    }

    private fun drawDot(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        if (glow) {
            glowPaint.color = glowColor
            glowPaint.alpha = 160
            glowPaint.style = Paint.Style.FILL
            glowPaint.maskFilter = BlurMaskFilter(r * 0.8f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(cx, cy, r, glowPaint)
            glowPaint.maskFilter = null
        }
        digitPaint.color = coreColor
        canvas.drawCircle(cx, cy, r * 0.75f, digitPaint)
    }
}
