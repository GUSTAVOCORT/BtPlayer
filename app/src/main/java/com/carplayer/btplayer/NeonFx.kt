package com.carplayer.btplayer

import android.graphics.Color
import android.widget.TextView

/**
 * Efecto "letrero de neon" para las mascaras del reproductor.
 * Usa setShadowLayer para hacer que el texto irradie un halo de su
 * propio color, como los avisos luminosos antiguos.
 */
object NeonFx {

    /** Aplica glow de neon a un texto con el color dado. */
    fun neonText(tv: TextView, glowColor: Int, radius: Float = 24f) {
        // El halo se dibuja con el color pedido; el texto encima queda claro.
        tv.setShadowLayer(radius, 0f, 0f, glowColor)
    }

    /** Quita el efecto. */
    fun clear(tv: TextView) {
        tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }

    /**
     * Da un tono claro casi blanco pero con tinte del acento, para que el
     * cuerpo del texto se vea "encendido" sobre su halo de color.
     */
    fun litColor(accent: Int): Int {
        val r = (Color.red(accent) + 255 * 2) / 3
        val g = (Color.green(accent) + 255 * 2) / 3
        val b = (Color.blue(accent) + 255 * 2) / 3
        return Color.rgb(r, g, b)
    }
}
