package com.carplayer.btplayer

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import kotlin.random.Random

/**
 * Efecto "letrero de neon" para las mascaras del reproductor.
 * Usa setShadowLayer para que el texto irradie un halo de su color, como los
 * avisos luminosos antiguos. Incluye un parpadeo irregular opcional (flicker)
 * que imita el titileo de un tubo de gas viejo.
 */
object NeonFx {

    private val ui = Handler(Looper.getMainLooper())
    private val flickering = mutableListOf<Triple<TextView, Int, Float>>()
    private var running = false

    /** Aplica glow de neon a un texto con el color dado. */
    fun neonText(tv: TextView, glowColor: Int, radius: Float = 24f) {
        tv.setShadowLayer(radius, 0f, 0f, glowColor)
    }

    fun clear(tv: TextView) {
        tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }

    /** Tono claro con tinte del acento para el cuerpo del texto "encendido". */
    fun litColor(accent: Int): Int {
        val r = (Color.red(accent) + 255 * 2) / 3
        val g = (Color.green(accent) + 255 * 2) / 3
        val b = (Color.blue(accent) + 255 * 2) / 3
        return Color.rgb(r, g, b)
    }

    /** Registra un texto para que titile como neon viejo (radio base dado). */
    fun addFlicker(tv: TextView, glowColor: Int, baseRadius: Float = 24f) {
        flickering.add(Triple(tv, glowColor, baseRadius))
    }

    fun startFlicker() {
        if (running || flickering.isEmpty()) return
        running = true
        ui.post(loop)
    }

    fun stopFlicker() {
        running = false
        ui.removeCallbacks(loop)
        // dejar todos en brillo pleno
        flickering.forEach { (tv, c, r) -> tv.setShadowLayer(r, 0f, 0f, c) }
    }

    fun resetFlicker() {
        stopFlicker()
        flickering.clear()
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            for ((tv, c, base) in flickering) {
                val r = Random.nextFloat()
                val radius = when {
                    r > 0.93f -> base * 0.35f   // titileo tenue
                    r > 0.86f -> base * 0.65f
                    else -> base                 // brillo pleno la mayoria del tiempo
                }
                tv.setShadowLayer(radius, 0f, 0f, c)
            }
            ui.postDelayed(this, (70 + Random.nextInt(150)).toLong())
        }
    }
}
