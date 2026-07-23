package com.carplayer.btplayer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs

/**
 * Caratula generada (el A2DP no transmite la imagen del album).
 * Estilos: 0 inicial+degradado, 1 abstracto (blobs), 2 anillos concentricos.
 * Esquinas redondeadas y glow para que no se vea rigido.
 */
object CoverArt {

    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    fun generate(size: Int, seedText: String, style: Int = 0, accent: Int = 0xFFFFC400.toInt()): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val seed = if (seedText.isBlank()) "?" else seedText
        val h = abs(seed.hashCode())

        val hue1 = (h % 360).toFloat()
        val hue2 = ((h / 7) % 360).toFloat()
        val c1 = Color.HSVToColor(floatArrayOf(hue1, 0.55f, 0.45f))
        val c2 = Color.HSVToColor(floatArrayOf(hue2, 0.65f, 0.15f))

        // fondo degradado
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), c1, c2, Shader.TileMode.CLAMP)
        }
        val radius = size * 0.12f
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        c.drawRoundRect(rect, radius, radius, bg)

        when (style) {
            1 -> {
                // blobs abstractos
                val p = Paint(Paint.ANTI_ALIAS_FLAG)
                for (k in 0 until 5) {
                    val hue = ((h / (k + 1)) % 360).toFloat()
                    p.color = Color.HSVToColor(90, floatArrayOf(hue, 0.7f, 0.9f))
                    val cx = ((h * (k + 3)) % size).toFloat()
                    val cy = ((h * (k + 7)) % size).toFloat()
                    c.drawCircle(cx, cy, size * (0.14f + (k % 3) * 0.06f), p)
                }
            }
            2 -> {
                // anillos concentricos
                val p = Paint(Paint.ANTI_ALIAS_FLAG)
                p.style = Paint.Style.STROKE
                val cx = size / 2f; val cy = size / 2f
                for (k in 1..6) {
                    p.strokeWidth = size * 0.02f
                    p.color = if (k % 2 == 0) accent else Color.argb(120,255,255,255)
                    c.drawCircle(cx, cy, size * 0.08f * k, p)
                }
            }
        }

        // glow radial
        val glow = Paint().apply {
            shader = RadialGradient(size * 0.32f, size * 0.28f, size * 0.8f,
                Color.argb(70, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(rect, radius, radius, glow)

        // inicial (en estilos 0 y 1)
        if (style != 2) {
            val ch = seed.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            txt.textSize = size * 0.46f
            val fm = txt.fontMetrics
            val y = size / 2f - (fm.ascent + fm.descent) / 2f
            c.drawText(ch, size / 2f, y, txt)
        }

        return bmp
    }
}
