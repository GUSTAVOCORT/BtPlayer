# BT Player — reproductor Bluetooth para head unit Allwinner T3

App nativa Kotlin que muestra la música que llega al equipo por Bluetooth
desde el teléfono: título, artista, álbum, tiempos y barra de progreso,
controles play/pausa/anterior/siguiente, visualizador de espectro y
ecualizador de 5 bandas.

## Cómo funciona
El audio A2DP no pasa por la app (lo decodifica el teléfono y lo entrega al
mixer de Android). Esta app:
- **Escucha** los broadcasts internos del firmware NWD/BC03 para la metadata
  (`send_media_play_info`, `send_media_play_time`, `ACTION_AVRCP_MUSIC_ID3`).
- **Envía** comandos de control por `ACTION_A2DP_CONTROL_COMMAND`.
- **Engancha** `Visualizer(0)` y `Equalizer(0)` a la mezcla global de audio.

La carátula se genera localmente (inicial + degradado por canción) porque el
Bluetooth no transmite la imagen del álbum.

## Compilar
Subí el contenido de esta carpeta a un repo de GitHub (incluyendo la carpeta
oculta `.github`). El workflow corre `gradle assembleDebug` y sube el APK
como artifact `BtPlayer-debug-apk`.

## Uso en el auto
1. Instalá el APK.
2. Al abrir, aceptá el permiso de micrófono (lo necesita el visualizador).
3. Emparejá el teléfono y poné música.
4. Deberías ver título/artista/tiempos y las barras moverse.
5. Probá play/pausa/next: si el módulo responde, listo. Si no reacciona,
   avisame y ajusto los códigos de comando (ver `NwdProtocol.CMD_*`).

## Nota sobre los controles
Los códigos de comando son los AVRCP estándar. Si tu firmware espera otros
valores, es un ajuste de una línea en `NwdProtocol.kt` tras la primera prueba.
