package com.carplayer.btplayer

import android.graphics.Color

/** Paletas de color seleccionables para el visualizador y acentos. */
object Palettes {

    data class Palette(val name: String, val colors: IntArray, val stops: FloatArray)

    val list: List<Palette> = listOf(
        Palette("Fuego",
            intArrayOf(Color.parseColor("#FFC400"), Color.parseColor("#FF7A00"), Color.parseColor("#FF2D95")),
            floatArrayOf(0f, 0.55f, 1f)),
        Palette("Neón Cyan",
            intArrayOf(Color.parseColor("#00F5D4"), Color.parseColor("#00BBF9"), Color.parseColor("#9B5DE5")),
            floatArrayOf(0f, 0.5f, 1f)),
        Palette("Verde ácido",
            intArrayOf(Color.parseColor("#B5FF00"), Color.parseColor("#00E676"), Color.parseColor("#00B0FF")),
            floatArrayOf(0f, 0.5f, 1f)),
        Palette("Magenta",
            intArrayOf(Color.parseColor("#FF006E"), Color.parseColor("#8338EC"), Color.parseColor("#3A86FF")),
            floatArrayOf(0f, 0.5f, 1f)),
        Palette("Hielo",
            intArrayOf(Color.parseColor("#CAF0F8"), Color.parseColor("#48CAE4"), Color.parseColor("#0077B6")),
            floatArrayOf(0f, 0.5f, 1f)),
        Palette("Atardecer",
            intArrayOf(Color.parseColor("#FFD60A"), Color.parseColor("#FF9E00"), Color.parseColor("#FF0054")),
            floatArrayOf(0f, 0.5f, 1f)),
        Palette("Blanco puro",
            intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#B0BEC5"), Color.parseColor("#607D8B")),
            floatArrayOf(0f, 0.5f, 1f)),
        Palette("Arcoíris",
            intArrayOf(Color.parseColor("#FF0000"), Color.parseColor("#FFFF00"),
                       Color.parseColor("#00FF00"), Color.parseColor("#00FFFF"),
                       Color.parseColor("#FF00FF")),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f))
    )

    fun get(i: Int): Palette = list[i.coerceIn(0, list.size - 1)]
    fun accentOf(i: Int): Int = get(i).colors[0]
}
