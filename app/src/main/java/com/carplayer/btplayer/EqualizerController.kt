package com.carplayer.btplayer

import android.media.audiofx.Equalizer

/**
 * Ecualizador de 5 bandas sobre la sesion global (id=0). Confirmado
 * disponible en este equipo (5 bandas, 10 presets).
 */
class EqualizerController {

    private var eq: Equalizer? = null
    var bandCount = 0; private set
    private var minLevel = -1500
    private var maxLevel = 1500

    fun open(): Boolean {
        return try {
            val e = Equalizer(0, 0)
            e.enabled = true
            val range = e.bandLevelRange
            minLevel = range[0].toInt()
            maxLevel = range[1].toInt()
            bandCount = e.numberOfBands.toInt()
            eq = e
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Frecuencia central de la banda en Hz, para etiquetar. */
    fun centerHz(band: Int): Int = try {
        (eq?.getCenterFreq(band.toShort()) ?: 0) / 1000
    } catch (_: Throwable) { 0 }

    fun minMillibel() = minLevel
    fun maxMillibel() = maxLevel

    fun level(band: Int): Int = try {
        (eq?.getBandLevel(band.toShort()) ?: 0).toInt()
    } catch (_: Throwable) { 0 }

    fun setLevel(band: Int, millibel: Int) {
        try { eq?.setBandLevel(band.toShort(), millibel.toShort()) } catch (_: Throwable) {}
    }

    fun setEnabled(on: Boolean) {
        try { eq?.enabled = on } catch (_: Throwable) {}
    }

    fun presetCount(): Int = try { (eq?.numberOfPresets ?: 0).toInt() } catch (_: Throwable) { 0 }
    fun presetName(i: Int): String = try { eq?.getPresetName(i.toShort()) ?: "" } catch (_: Throwable) { "" }
    fun usePreset(i: Int) { try { eq?.usePreset(i.toShort()) } catch (_: Throwable) {} }

    fun release() {
        try { eq?.release() } catch (_: Throwable) {}
        eq = null
    }
}
