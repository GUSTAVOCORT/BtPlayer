package com.carplayer.btplayer

import android.content.Context
import android.content.Intent

/**
 * Envia comandos de control al modulo BT del fabricante.
 *
 * Descubierto en A2DPManager.onReceive del APK: el broadcast
 * com.nwd.ACTION_A2DP_CONTROL_COMMAND lleva "extra_command" con un ENTERO:
 *   NEXT=0  FORCE_PLAY=1  PREVIOUS=2  PAUSE=3  PLAY=4
 *
 * Mandamos el entero (camino confirmado) y como respaldo tambien los
 * broadcasts de conveniencia com.bt.ACTION_BT_MUSIC_PLAY/PAUSE.
 */
class BtController(private val ctx: Context) {

    private fun sendCommand(command: Int) {
        try {
            ctx.sendBroadcast(Intent(NwdProtocol.ACTION_CONTROL).apply {
                putExtra(NwdProtocol.EXTRA_COMMAND, command)
            })
        } catch (_: Throwable) {}
    }

    fun playPause(currentlyPlaying: Boolean) {
        if (currentlyPlaying) {
            sendCommand(NwdProtocol.CMD_PAUSE)   // 3
            try { ctx.sendBroadcast(Intent(NwdProtocol.ACTION_BT_MUSIC_PAUSE)) } catch (_: Throwable) {}
        } else {
            sendCommand(NwdProtocol.CMD_PLAY)    // 4
            try { ctx.sendBroadcast(Intent(NwdProtocol.ACTION_BT_MUSIC_PLAY)) } catch (_: Throwable) {}
        }
    }

    fun next() = sendCommand(NwdProtocol.CMD_NEXT)   // 0
    fun prev() = sendCommand(NwdProtocol.CMD_PREV)   // 2
    fun stop() = sendCommand(NwdProtocol.CMD_PAUSE)  // 3
}
