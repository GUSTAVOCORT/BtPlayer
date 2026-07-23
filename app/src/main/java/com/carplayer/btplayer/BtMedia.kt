package com.carplayer.btplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

/** Estado actual de lo que suena, tal como lo entiende la app. */
data class PlaybackState(
    var title: String = "",
    var artist: String = "",
    var album: String = "",
    var durationMs: Long = 0L,
    var positionMs: Long = 0L,
    var isPlaying: Boolean = false,
    var deviceName: String = "",
    var connected: Boolean = false
) {
    /** Clave para saber si cambio la cancion (y regenerar la caratula). */
    fun trackKey() = "$title|$artist|$album"
}

/**
 * Escucha los broadcasts del firmware NWD/BC03 y mantiene un PlaybackState.
 * Es tolerante: prueba varias claves de extra porque el firmware no siempre
 * usa el mismo nombre, y acepta duracion/posicion tanto en ms como en seg.
 */
class BtMediaReceiver(
    private val onChange: (PlaybackState) -> Unit
) : BroadcastReceiver() {

    val state = PlaybackState()

    fun register(ctx: Context) {
        val f = IntentFilter().apply {
            NwdProtocol.ALL_INCOMING.forEach { addAction(it) }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(this, f, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            ctx.registerReceiver(this, f)
        }
        // Pedir al sistema que reemita la pista actual, por si ya venia sonando.
        try { ctx.sendBroadcast(Intent(NwdProtocol.ACTION_QUERY_ID3)) } catch (_: Throwable) {}
    }

    fun unregister(ctx: Context) {
        try { ctx.unregisterReceiver(this) } catch (_: Throwable) {}
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        val a = intent.action ?: return
        var changed = true

        when (a) {
            NwdProtocol.ACTION_MEDIA_INFO -> {
                firstString(intent, NwdProtocol.NAME_KEYS)?.let { state.title = it }
                firstString(intent, NwdProtocol.ARTIST_KEYS)?.let { state.artist = it }
                firstString(intent, NwdProtocol.ALBUM_KEYS)?.let { state.album = it }
                firstLong(intent, NwdProtocol.DURATION_KEYS)?.let { state.durationMs = normalizeMs(it) }
                state.connected = true
            }
            NwdProtocol.ACTION_MEDIA_TIME -> {
                firstLong(intent, NwdProtocol.POSITION_KEYS)?.let { state.positionMs = normalizeMs(it) }
                firstLong(intent, NwdProtocol.DURATION_KEYS)?.let {
                    if (it > 0) state.durationMs = normalizeMs(it)
                }
            }
            NwdProtocol.ACTION_AVRCP_ID3 -> {
                firstString(intent, NwdProtocol.ID3_TITLE_KEYS)?.let { state.title = it }
                firstString(intent, NwdProtocol.ID3_ARTIST_KEYS)?.let { state.artist = it }
                state.connected = true
            }
            NwdProtocol.ACTION_BT_MUSIC_PLAY -> state.isPlaying = true
            NwdProtocol.ACTION_BT_MUSIC_PAUSE -> state.isPlaying = false
            NwdProtocol.ACTION_A2DP_ESTABLISHED,
            NwdProtocol.ACTION_AVRCP_ESTABLISHED -> state.connected = true
            NwdProtocol.ACTION_A2DP_RELEASE,
            NwdProtocol.ACTION_AVRCP_RELEASE -> {
                state.connected = false; state.isPlaying = false
            }
            NwdProtocol.ACTION_BT_NAME_CHANGE -> {
                intent.getStringExtra(NwdProtocol.EXTRA_BT_NAME)?.let { state.deviceName = it }
            }
            NwdProtocol.ACTION_BT_CONNECTION_CHANGE -> { /* solo refresca */ }
            else -> changed = false
        }
        if (changed) onChange(state)
    }

    /** Devuelve el primer extra de tipo String que exista entre las claves. */
    private fun firstString(i: Intent, keys: List<String>): String? {
        for (k in keys) {
            val v = i.getStringExtra(k)
            if (!v.isNullOrBlank()) return v.trim()
        }
        return null
    }

    /** Igual pero para numeros; acepta long, int o incluso string numerica. */
    private fun firstLong(i: Intent, keys: List<String>): Long? {
        for (k in keys) {
            if (!i.hasExtra(k)) continue
            val asLong = i.getLongExtra(k, Long.MIN_VALUE)
            if (asLong != Long.MIN_VALUE) return asLong
            val asInt = i.getIntExtra(k, Int.MIN_VALUE)
            if (asInt != Int.MIN_VALUE) return asInt.toLong()
            val asStr = i.getStringExtra(k)
            asStr?.toLongOrNull()?.let { return it }
        }
        return null
    }

    /** El firmware manda a veces segundos y a veces milisegundos: unificar. */
    private fun normalizeMs(v: Long): Long = if (v in 1..99_999) v * 1000 else v
}
