package com.carplayer.btplayer

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda todas las preferencias visuales para que sobrevivan reinicios.
 * Un solo lugar para leer/escribir, asi los distintos paneles no se pisan.
 */
class Prefs(ctx: Context) {

    private val sp: SharedPreferences =
        ctx.getSharedPreferences("btplayer", Context.MODE_PRIVATE)

    // ---- Visualizador ----
    var vizStyle: Int
        get() = sp.getInt("viz_style", 0)          // 0 barras, 1 espejo, 2 linea, 3 puntos, 4 ondas
        set(v) { sp.edit().putInt("viz_style", v).apply() }

    var vizPalette: Int
        get() = sp.getInt("viz_palette", 0)        // indice en Palettes.list
        set(v) { sp.edit().putInt("viz_palette", v).apply() }

    var vizBars: Int
        get() = sp.getInt("viz_bars", 48)
        set(v) { sp.edit().putInt("viz_bars", v).apply() }

    var vizNeon: Boolean
        get() = sp.getBoolean("viz_neon", true)
        set(v) { sp.edit().putBoolean("viz_neon", v).apply() }

    var vizRounded: Boolean
        get() = sp.getBoolean("viz_rounded", true)
        set(v) { sp.edit().putBoolean("viz_rounded", v).apply() }

    var vizMirror: Boolean
        get() = sp.getBoolean("viz_mirror", false)
        set(v) { sp.edit().putBoolean("viz_mirror", v).apply() }

    // ---- Pantalla / modo ----
    var screenMode: Int
        get() = sp.getInt("screen_mode", 0)        // 0 player, 1 reloj nixie, 2 mixto
        set(v) { sp.edit().putInt("screen_mode", v).apply() }

    // ---- Fondo ----
    var bgUri: String?
        get() = sp.getString("bg_uri", null)
        set(v) { sp.edit().putString("bg_uri", v).apply() }

    var bgDim: Int
        get() = sp.getInt("bg_dim", 60)            // 0..100 oscurecimiento
        set(v) { sp.edit().putInt("bg_dim", v).apply() }

    // ---- Acento / caratula ----
    var accentColor: Int
        get() = sp.getInt("accent", 0xFFFFC400.toInt())
        set(v) { sp.edit().putInt("accent", v).apply() }

    var coverStyle: Int
        get() = sp.getInt("cover_style", 0)        // 0 inicial+degradado, 1 abstracto, 2 anillos
        set(v) { sp.edit().putInt("cover_style", v).apply() }

    // ---- Reloj Nixie ----
    var nixieGlow: Boolean
        get() = sp.getBoolean("nixie_glow", true)
        set(v) { sp.edit().putBoolean("nixie_glow", v).apply() }

    var nixie24h: Boolean
        get() = sp.getBoolean("nixie_24h", false)
        set(v) { sp.edit().putBoolean("nixie_24h", v).apply() }

    // ---- Diagnostico ----
    var showDebug: Boolean
        get() = sp.getBoolean("show_debug", false)
        set(v) { sp.edit().putBoolean("show_debug", v).apply() }
}
