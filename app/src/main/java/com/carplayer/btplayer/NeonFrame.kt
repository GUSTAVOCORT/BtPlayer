package com.carplayer.btplayer

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * Marco de neon tipo aviso luminoso alrededor de toda la pantalla.
 * Dibuja un rectangulo redondeado con doble trazo (nucleo claro + halo
 * difuso del color de acento) y un titileo sutil como los letreros viejos.
 * Es transparente por dentro para no tapar el contenido.
 */
class NeonFrame @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var neonColor = Color.parseColor("#FF2D95")
        set(v) { field = v; invalidate() }
    var enabled2 = true
    var flicker = true

    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private var flickerAlpha = 1f

    private val ui = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (!flicker) { flickerAlpha = 1f; invalidate(); return }
            val r = Random.nextFloat()
            flickerAlpha = when {
                r > 0.94f -> 0.45f
                r > 0.88f -> 0.7f
                else -> 1f
            }
            invalidate()
            ui.postDelayed(this, (80 + Random.nextInt(160)).toLong())
        }
    }

    fun startFx() { ui.removeCallbacks(tick); if (enabled2) ui.post(tick) }
    fun stopFx() { ui.removeCallbacks(tick) }
    override fun onDetachedFromWindow() { stopFx(); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        if (!enabled2) return
        val w = width.toFloat(); val h = height.toFloat()
        val inset = w * 0.012f
        val rect = RectF(inset, inset, w - inset, h - inset)
        val radius = inset * 3.5f

        // halo difuso exterior
        glow.color = neonColor
        glow.alpha = (150 * flickerAlpha).toInt().coerceIn(0, 255)
        glow.strokeWidth = inset * 1.6f
        glow.maskFilter = BlurMaskFilter(inset * 1.8f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRoundRect(rect, radius, radius, glow)

        // nucleo claro (casi blanco con tinte)
        core.color = litColor(neonColor)
        core.alpha = (235 * flickerAlpha).toInt().coerceIn(0, 255)
        core.strokeWidth = inset * 0.5f
        core.maskFilter = null
        canvas.drawRoundRect(rect, radius, radius, core)
    }

    private fun litColor(c: Int): Int {
        val r = (Color.red(c) + 255) / 2
        val g = (Color.green(c) + 255) / 2
        val b = (Color.blue(c) + 255) / 2
        return Color.rgb(r, g, b)
    }
}
