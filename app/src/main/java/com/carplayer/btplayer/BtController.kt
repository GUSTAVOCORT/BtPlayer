package com.carplayer.btplayer

import android.content.Context
import android.content.Intent

/**
 * Envia comandos de control al modulo BT del fabricante.
 *
 * Manda cada comando por los dos caminos conocidos (ACTION_A2DP_CONTROL_COMMAND
 * y el alternativo) y ademas dispara los broadcasts de conveniencia
 * ACTION_BT_MUSIC_PLAY/PAUSE que algunos firmwares usan. Es "cinturon y
 * tiradores": lo que no exista simplemente se ignora sin romper nada.
 */
class BtController(private val ctx: Context) {

    private fun send(action: String, command: Int) {
        try {
            ctx.sendBroadcast(Intent(action).apply {
                putExtra(NwdProtocol.EXTRA_COMMAND, command)
            })
        } catch (_: Throwable) {}
    }

    private fun fire(command: Int) {
        send(NwdProtocol.ACTION_CONTROL, command)
        send(NwdProtocol.ACTION_CONTROL_ALT, command)
    }

    fun playPause(currentlyPlaying: Boolean) {
        // Muchos firmwares tratan PLAY como toggle; si no, usamos el opuesto.
        if (currentlyPlaying) {
            fire(NwdProtocol.CMD_PAUSE)
            try { ctx.sendBroadcast(Intent(NwdProtocol.ACTION_BT_MUSIC_PAUSE)) } catch (_: Throwable) {}
        } else {
            fire(NwdProtocol.CMD_PLAY)
            try { ctx.sendBroadcast(Intent(NwdProtocol.ACTION_BT_MUSIC_PLAY)) } catch (_: Throwable) {}
        }
    }

    fun next() = fire(NwdProtocol.CMD_NEXT)
    fun prev() = fire(NwdProtocol.CMD_PREV)
    fun stop() = fire(NwdProtocol.CMD_STOP)
}
