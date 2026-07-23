package com.carplayer.btplayer

/**
 * Protocolo privado del firmware NWD / modulo BC03, reconstruido a partir
 * de los APK del fabricante (com.bt.bc03 y com.nwd.bt.music).
 *
 * Hay DOS familias de mensajes que traen lo mismo por caminos distintos:
 *
 *  1) Familia "media_play" (la completa): el sistema emite metadata y tiempos
 *     con estos broadcasts y extras.
 *  2) Familia "AVRCP ID3" (la simple, del BC03): solo titulo y artista.
 *
 * La app escucha las dos y se queda con lo que llegue. Para CONTROLAR la
 * reproduccion se envia ACTION_A2DP_CONTROL_COMMAND con EXTRA_COMMAND.
 */
object NwdProtocol {

    // ---- Broadcasts que TRAEN informacion (los escuchamos) ----

    /** Info de pista: nombre, artista, album, duracion total. */
    const val ACTION_MEDIA_INFO = "com.nwd.action.send_media_play_info"
    /** Tiempo de reproduccion: posicion actual que va corriendo. */
    const val ACTION_MEDIA_TIME = "com.nwd.action.send_media_play_time"
    /** Modo de reproduccion (shuffle/repeat) del origen. */
    const val ACTION_MEDIA_MODEL = "com.nwd.action.send_media_play_MODEL"

    /** Camino viejo BC03: ID3 con titulo/artista. */
    const val ACTION_AVRCP_ID3 = "com.bt.ACTION_AVRCP_MUSIC_ID3"
    /** Estado play/pausa por el camino BC03. */
    const val ACTION_BT_MUSIC_PLAY = "com.bt.ACTION_BT_MUSIC_PLAY"
    const val ACTION_BT_MUSIC_PAUSE = "com.bt.ACTION_BT_MUSIC_PAUSE"
    const val ACTION_BT_MUSIC_CONNECTING = "com.bt.ACTION_BT_MUSIC_CONNECTING"

    /** Conexion/desconexion A2DP y AVRCP. */
    const val ACTION_A2DP_ESTABLISHED = "com.bt.ACTION_A2DP_ESTABLISHED"
    const val ACTION_A2DP_RELEASE = "com.bt.ACTION_A2DP_RELEASE"
    const val ACTION_AVRCP_ESTABLISHED = "com.bt.ACTION_AVRCP_ESTABLISHED"
    const val ACTION_AVRCP_RELEASE = "com.bt.ACTION_AVRCP_RELEASE"
    const val ACTION_BT_CONNECTION_CHANGE = "com.bt.ACTION_BT_CONNECTION_CHANGE"

    /** Nombre del dispositivo BT conectado. */
    const val ACTION_BT_NAME_CHANGE = "com.bt.ACTION_BT_NAME_CHANGE"

    // ---- Extras de la familia media_play (metadata completa) ----
    const val EXTRA_MEDIA_NAME = "EXTRA_MEDIA_NAME"
    const val EXTRA_MEDIA_ARTIST = "EXTRA_MEDIA_ARTIST"
    const val EXTRA_MEDIA_ALBUM = "EXTRA_MEDIA_ABLUM"          // (sic) asi esta escrito en el firmware
    const val EXTRA_MEDIA_DURATION = "EXTRA_MEDIA_DURATION"
    const val EXTRA_MEDIA_TOTAL_TIME = "EXTRA_MEDIA_TOTAL_TIME"
    const val EXTRA_MEDIA_CURRENT_POSITION = "EXTRA_MEDIA_CURRENT_POSITION"

    // variantes en minuscula vistas tambien en el dex (por si acaso)
    val NAME_KEYS = listOf(EXTRA_MEDIA_NAME, "extra_media_name", "btm_title", "btm_btmusic_title", "mediaName")
    val ARTIST_KEYS = listOf(EXTRA_MEDIA_ARTIST, "extra_media_artist", "btm_artist", "mediaArtist")
    val ALBUM_KEYS = listOf(EXTRA_MEDIA_ALBUM, "extra_media_ablum", "btm_album", "mediaAlbum")
    val DURATION_KEYS = listOf(EXTRA_MEDIA_DURATION, "extra_meida_duration", EXTRA_MEDIA_TOTAL_TIME, "extra_media_total_time")
    val POSITION_KEYS = listOf(EXTRA_MEDIA_CURRENT_POSITION, "extra_media_current_position")

    // ---- Extras del camino AVRCP ID3 (BC03) ----
    const val EXTRA_ID3_TITLE = "EXTRA_AVRCP_ID3_TITLE"
    const val EXTRA_ID3_ARTIST = "EXTRA_AVRCP_ID3_ARTIST"
    val ID3_TITLE_KEYS = listOf(EXTRA_ID3_TITLE, "extra_avrcp_id3_title", "a2dp_id3_title", "title")
    val ID3_ARTIST_KEYS = listOf(EXTRA_ID3_ARTIST, "extra_avrcp_id3_artist", "a2dp_id3_artist", "artist")

    const val EXTRA_BT_NAME = "EXTRA_BT_NAME"

    // ---- Control de reproduccion (los ENVIAMOS nosotros) ----
    const val ACTION_CONTROL = "com.nwd.ACTION_A2DP_CONTROL_COMMAND"
    const val ACTION_CONTROL_ALT = "com.nwd.ACTION_PLAY_BTMUSIC_CMD"
    const val EXTRA_COMMAND = "EXTRA_COMMAND"

    /**
     * Codigos de comando AVRCP passthrough estandar. El BTService del BC03
     * los reenvia al modulo. Son los valores AVRCP universales; si el
     * firmware usara otros, se ajusta tras la primera prueba.
     */
    const val CMD_PLAY = 0x44
    const val CMD_PAUSE = 0x46
    const val CMD_NEXT = 0x4B
    const val CMD_PREV = 0x4C
    const val CMD_STOP = 0x45
    const val CMD_FF = 0x49
    const val CMD_REW = 0x48

    /** Pregunta al sistema que reenvie la ID3 actual (por si arrancamos tarde). */
    const val ACTION_QUERY_ID3 = "com.nwd.ACTION_QUERY_A2DP_ID3"

    /** Todas las acciones entrantes, para registrar el receiver de una. */
    val ALL_INCOMING = listOf(
        ACTION_MEDIA_INFO, ACTION_MEDIA_TIME, ACTION_MEDIA_MODEL,
        ACTION_AVRCP_ID3, ACTION_BT_MUSIC_PLAY, ACTION_BT_MUSIC_PAUSE,
        ACTION_BT_MUSIC_CONNECTING, ACTION_A2DP_ESTABLISHED, ACTION_A2DP_RELEASE,
        ACTION_AVRCP_ESTABLISHED, ACTION_AVRCP_RELEASE, ACTION_BT_CONNECTION_CHANGE,
        ACTION_BT_NAME_CHANGE
    )
}
