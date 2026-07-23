package com.carplayer.btplayer

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
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
 * Reloj Nixie realista: cada digito en su tubo de vidrio con panal hexagonal
 * de fondo, digito con filamento naranja-ambar que irradia (varias capas de
 * glow), reflejo de vidrio y base con pines. Inspirado en las fotos reales de
 * tubos IN-14 / IN-12.
 */
class NixieClock @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var glow = true
    var use24h = false

    // Colores del filamento ardiente (como neon de sodio)
    private val coreColor = Color.parseColor("#FFB347")     // centro claro
    private val glowColor = Color.parseColor("#FF7A18")     // halo naranja
    private val deepGlow = Color.parseColor("#E8420E")      // halo profundo rojizo

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
        strokeWidth = 1.4f
        color = Color.parseColor("#3A2418")
    }
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tubeBottom = Paint(Paint.ANTI_ALIAS_FLAG)

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
        val gapRatio = 0.16f
        val totalGap = w * gapRatio
        val tubeW = (w - totalGap) / n
        val tubeGap = totalGap / (n + 1)
        val tubeH = (tubeW * 2.4f).coerceAtMost(h * 0.9f)
        val top = (h - tubeH) / 2f

        var x = tubeGap
        for (i in 0 until n) {
            drawTube(canvas, x, top, tubeW, tubeH, digits[i])
            x += tubeW + tubeGap
        }

        // dos puntos entre HH:MM
        if (cal.get(Calendar.SECOND) % 2 == 0) {
            val cx = w / 2f
            val dotR = tubeW * 0.05f
            drawGlowDot(canvas, cx, top + tubeH * 0.4f, dotR)
            drawGlowDot(canvas, cx, top + tubeH * 0.6f, dotR)
        }
    }

    private fun drawTube(canvas: Canvas, x: Float, top: Float, tw: Float, th: Float, digit: Int) {
        val rect = RectF(x, top, x + tw, top + th)
        val corner = tw * 0.22f

        // 1. cuerpo del tubo: vidrio oscuro con leve calidez
        glassPaint.shader = LinearGradient(x, top, x + tw, top + th,
            intArrayOf(Color.parseColor("#241A14"), Color.parseColor("#0E0A08")),
            null, Shader.TileMode.CLAMP)
        glassPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, corner, corner, glassPaint)

        // 2. cuello superior del tubo (domo)
        val neckW = tw * 0.28f
        val neckPath = Path().apply {
            moveTo(x + tw/2 - neckW/2, top + 2f)
            quadTo(x + tw/2, top - th*0.08f, x + tw/2 + neckW/2, top + 2f)
            close()
        }
        canvas.drawPath(neckPath, glassPaint)

        // 3. panal hexagonal de fondo (la malla caracteristica)
        canvas.save()
        canvas.clipRect(rect)
        drawHexMesh(canvas, x, top, tw, th)
        canvas.restore()

        // 4. digito con multiples capas de glow (de mas difuso a mas nitido)
        val cx = x + tw / 2f
        val ts = th * 0.62f
        val fm = run { digitPaint.textSize = ts; digitPaint.fontMetrics }
        val by = top + th / 2f - (fm.ascent + fm.descent) / 2f
        val d = digit.toString()

        if (glow) {
            glowPaint.textSize = ts
            // halo profundo rojizo, muy difuso
            glowPaint.color = deepGlow
            glowPaint.alpha = 130
            glowPaint.maskFilter = BlurMaskFilter(ts * 0.28f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawText(d, cx, by, glowPaint)
            // halo naranja medio
            glowPaint.color = glowColor
            glowPaint.alpha = 200
            glowPaint.maskFilter = BlurMaskFilter(ts * 0.14f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawText(d, cx, by, glowPaint)
            glowPaint.maskFilter = null
        }
        // nucleo claro del digito
        digitPaint.color = coreColor
        digitPaint.textSize = ts
        canvas.drawText(d, cx, by, digitPaint)

        // 5. reflejo de vidrio: brillo diagonal arriba-izquierda
        glassPaint.shader = LinearGradient(x, top, x + tw*0.6f, top + th*0.5f,
            intArrayOf(Color.parseColor("#40FFFFFF"), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(x, top, x + tw, top + th*0.5f), corner, corner, glassPaint)
        glassPaint.shader = null

        // 6. borde del tubo
        glassPaint.style = Paint.Style.STROKE
        glassPaint.strokeWidth = tw * 0.02f
        glassPaint.color = Color.parseColor("#55FFFFFF")
        canvas.drawRoundRect(rect, corner, corner, glassPaint)
        glassPaint.style = Paint.Style.FILL

        // 7. base con pines abajo
        val baseTop = top + th
        tubeBottom.color = Color.parseColor("#1A1410")
        canvas.drawRoundRect(RectF(x + tw*0.15f, baseTop - tw*0.05f, x + tw*0.85f, baseTop + th*0.05f),
            corner*0.3f, corner*0.3f, tubeBottom)
    }

    /** Malla hexagonal tenue (el "panal" de los tubos reales). */
    private fun drawHexMesh(canvas: Canvas, x: Float, top: Float, tw: Float, th: Float) {
        val hexR = tw * 0.09f
        val hStep = hexR * 1.5f
        val vStep = (hexR * 0.866f) * 2f
        var row = 0
        var cy = top + hexR
        while (cy < top + th) {
            val offset = if (row % 2 == 0) 0f else hStep * 0.5f
            var cx = x + offset
            while (cx < x + tw + hexR) {
                drawHex(canvas, cx, cy, hexR)
                cx += hStep
            }
            cy += vStep * 0.5f
            row++
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

    private fun drawGlowDot(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        if (glow) {
            glowPaint.color = glowColor
            glowPaint.alpha = 200
            glowPaint.maskFilter = BlurMaskFilter(r, BlurMaskFilter.Blur.NORMAL)
            glowPaint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, r, glowPaint)
            glowPaint.maskFilter = null
        }
        digitPaint.color = coreColor
        canvas.drawCircle(cx, cy, r * 0.7f, digitPaint)
    }
}
