package com.carplayer.btplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

/** Estado actual de lo que suena. */
data class PlaybackState(
    var title: String = "",
    var artist: String = "",
    var album: String = "",
    var durationMs: Long = 0L,
    var positionMs: Long = 0L,
    var isPlaying: Boolean = false,
    var deviceName: String = "",
    var connected: Boolean = false,
    var progress: Int = -1          // 0..100 si el firmware lo manda, si no -1
) {
    fun trackKey() = "$title|$artist|$album"
}

/**
 * Escucha los broadcasts del firmware NWD/BC03. Ademas de mantener el estado,
 * guarda un LOG CRUDO de cada intent (accion + todos los extras con tipo y
 * valor). Ese log revela por que los tiempos no cuadran.
 */
class BtMediaReceiver(
    private val onChange: (PlaybackState) -> Unit,
    private val onLog: (String) -> Unit = {}
) : BroadcastReceiver() {

    val state = PlaybackState()
    private val logBuf = StringBuilder()

    fun register(ctx: Context) {
        val f = IntentFilter().apply { NwdProtocol.ALL_INCOMING.forEach { addAction(it) } }
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(this, f, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            ctx.registerReceiver(this, f)
        }
        try { ctx.sendBroadcast(Intent(NwdProtocol.ACTION_QUERY_ID3)) } catch (_: Throwable) {}
    }

    fun unregister(ctx: Context) {
        try { ctx.unregisterReceiver(this) } catch (_: Throwable) {}
    }

    fun dumpLog(): String = logBuf.toString()

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        val a = intent.action ?: return

        val short = a.substringAfterLast('.')
        logBuf.append('[').append(short).append("] ")
        val ex = intent.extras
        if (ex != null) {
            for (k in ex.keySet()) {
                val v = try { ex.get(k) } catch (_: Throwable) { "?" }
                val typ = v?.javaClass?.simpleName ?: "null"
                logBuf.append(k).append('(').append(typ).append(")=").append(v).append("  ")
            }
        } else {
            logBuf.append("(sin extras)")
        }
        logBuf.append('\n')
        if (logBuf.length > 6000) logBuf.delete(0, logBuf.length - 4000)
        onLog(logBuf.toString())

        var changed = true
        when (a) {
            NwdProtocol.ACTION_MEDIA_INFO -> {
                firstString(intent, NwdProtocol.NAME_KEYS)?.let { state.title = it }
                firstString(intent, NwdProtocol.ARTIST_KEYS)?.let { state.artist = it }
                firstString(intent, NwdProtocol.ALBUM_KEYS)?.let { state.album = it }
                firstLong(intent, NwdProtocol.DURATION_KEYS)?.let { state.durationMs = normalizeMs(it) }
                firstLong(intent, NwdProtocol.POSITION_KEYS)?.let { state.positionMs = normalizeMs(it) }
                firstLong(intent, NwdProtocol.PROGRESS_KEYS)?.let {
                    if (it in 0..100) state.progress = it.toInt()
                }
                state.connected = true
            }
            NwdProtocol.ACTION_MEDIA_TIME -> {
                firstLong(intent, NwdProtocol.POSITION_KEYS)?.let { state.positionMs = normalizeMs(it) }
                firstLong(intent, NwdProtocol.DURATION_KEYS)?.let {
                    if (it > 0) state.durationMs = normalizeMs(it)
                }
                firstLong(intent, NwdProtocol.PROGRESS_KEYS)?.let {
                    if (it in 0..100) state.progress = it.toInt()
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
            NwdProtocol.ACTION_AVRCP_RELEASE -> { state.connected = false; state.isPlaying = false }
            NwdProtocol.ACTION_BT_NAME_CHANGE ->
                intent.getStringExtra(NwdProtocol.EXTRA_BT_NAME)?.let { state.deviceName = it }
            NwdProtocol.ACTION_BT_CONNECTION_CHANGE -> {}
            else -> changed = false
        }
        if (changed) onChange(state)
    }

    private fun firstString(i: Intent, keys: List<String>): String? {
        for (k in keys) {
            val v = i.getStringExtra(k)
            if (!v.isNullOrBlank()) return v.trim()
        }
        return null
    }

    private fun firstLong(i: Intent, keys: List<String>): Long? {
        for (k in keys) {
            if (!i.hasExtra(k)) continue
            val asLong = i.getLongExtra(k, Long.MIN_VALUE)
            if (asLong != Long.MIN_VALUE) return asLong
            val asInt = i.getIntExtra(k, Int.MIN_VALUE)
            if (asInt != Int.MIN_VALUE) return asInt.toLong()
            val asStr = i.getStringExtra(k)?.toLongOrNull()
            if (asStr != null) return asStr
        }
        return null
    }

    private fun normalizeMs(v: Long): Long = if (v in 1..99_999) v * 1000 else v
}
