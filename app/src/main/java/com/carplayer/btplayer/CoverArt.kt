package com.carplayer.btplayer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs

/**
 * El Bluetooth A2DP no transmite la imagen del album, asi que la generamos:
 * un degradado derivado del nombre del artista/tema (siempre el mismo color
 * para la misma cancion) con la inicial grande encima. Queda prolijo y
 * reemplaza el hueco vacio.
 */
object CoverArt {

    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    fun generate(size: Int, seedText: String): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val seed = if (seedText.isBlank()) "?" else seedText
        val h = abs(seed.hashCode())

        // Dos tonos derivados del hash, en la familia oscura para que la
        // inicial blanca resalte.
        val hue1 = (h % 360).toFloat()
        val hue2 = ((h / 7) % 360).toFloat()
        val c1 = Color.HSVToColor(floatArrayOf(hue1, 0.55f, 0.42f))
        val c2 = Color.HSVToColor(floatArrayOf(hue2, 0.65f, 0.16f))

        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(),
                c1, c2, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)

        // Brillo radial suave para dar profundidad.
        val glow = Paint().apply {
            shader = RadialGradient(size * 0.32f, size * 0.28f, size * 0.75f,
                Color.argb(60, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), glow)

        // Inicial.
        val ch = seed.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        paintText.textSize = size * 0.46f
        val fm = paintText.fontMetrics
        val y = size / 2f - (fm.ascent + fm.descent) / 2f
        c.drawText(ch, size / 2f, y, paintText)

        return bmp
    }
}
