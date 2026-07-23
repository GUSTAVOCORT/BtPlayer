package com.carplayer.btplayer

import android.content.Context
import android.content.Intent

/**
 * Control de reproduccion del modulo BT.
 *
 * Valores VERIFICADOS descompilando BaseInterface$5.onReceive del firmware:
 *
 *   Accion: com.nwd.ACTION_PLAY_BTMUSIC_CMD
 *   Extra : extra_command (int)
 *      1 -> togglePlayPause
 *      2 -> togglePrevious
 *      3 -> toggleNext
 *
 * CLAVE: hay que mandar UN SOLO comando limpio. Antes se enviaban varias
 * combinaciones a la vez y el modulo recibia play+prev+next revueltos, por
 * eso "play cambiaba de cancion" y "retroceder pausaba". Ahora, un comando.
 */
class BtController(private val ctx: Context) {

    private fun sendCmd(command: Int) {
        try {
            ctx.sendBroadcast(Intent(NwdProtocol.ACTION_CONTROL_ALT).apply {
                putExtra(NwdProtocol.EXTRA_COMMAND, command)
            })
        } catch (_: Throwable) {}
    }

    /** 1 = toggle play/pausa. El firmware alterna solo, no necesita saber el estado. */
    fun playPause(currentlyPlaying: Boolean) = sendCmd(1)

    /** 3 = siguiente. */
    fun next() = sendCmd(3)

    /** 2 = anterior. */
    fun prev() = sendCmd(2)
}
