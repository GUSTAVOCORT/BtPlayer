package com.carplayer.btplayer

import android.content.Context
import android.content.Intent

/**
 * Control de reproduccion del modulo BT.
 *
 * Verificado en BaseInterface$5.onReceive:
 *   com.nwd.ACTION_PLAY_BTMUSIC_CMD + extra_command:
 *      1 -> togglePlayPause    2 -> togglePrevious    3 -> toggleNext
 *
 * next/prev funcionan por esa via. El play/pausa por comando 1 no respondia en
 * este equipo, asi que ademas mandamos el broadcast directo de play/pause que
 * el propio sistema emite (ACTION_BT_MUSIC_PLAY / PAUSE).
 */
class BtController(private val ctx: Context) {

    private fun sendCmd(command: Int) {
        try {
            ctx.sendBroadcast(Intent(NwdProtocol.ACTION_CONTROL_ALT).apply {
                putExtra(NwdProtocol.EXTRA_COMMAND, command)
            })
        } catch (_: Throwable) {}
    }

    private fun sendAction(action: String) {
        try { ctx.sendBroadcast(Intent(action)) } catch (_: Throwable) {}
    }

    /**
     * Play/pausa: mandamos el toggle por comando (1) Y ademas el broadcast
     * directo segun el estado actual, para cubrir ambos mecanismos.
     */
    fun playPause(currentlyPlaying: Boolean) {
        sendCmd(1)
        if (currentlyPlaying) {
            sendAction(NwdProtocol.ACTION_BT_MUSIC_PAUSE)
            sendCmd(NwdProtocol.MC_PAUSE)   // por si acepta la otra tabla
        } else {
            sendAction(NwdProtocol.ACTION_BT_MUSIC_PLAY)
            sendCmd(NwdProtocol.MC_PLAY)
        }
    }

    fun next() = sendCmd(3)
    fun prev() = sendCmd(2)
}
