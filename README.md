# BT Player v5 — reproductor Bluetooth para head unit Allwinner T3

## LO IMPORTANTE DE ESTA VERSIÓN: controles arreglados de raíz

Descompilando el firmware encontré el mecanismo EXACTO de control. El receptor
`BaseInterface$5.onReceive` del sistema NWD procesa así:

    Acción: com.nwd.ACTION_PLAY_BTMUSIC_CMD
    Extra : extra_command
       1 → togglePlayPause
       2 → togglePrevious
       3 → toggleNext

El problema anterior: yo mandaba VARIOS comandos a la vez (dos tablas de código
distintas + broadcasts de play/pause). El módulo recibía play + previous + next
revueltos y hacía cualquier cosa — por eso "play cambiaba de canción" y
"retroceder pausaba". Se pisaban entre sí.

Ahora se manda UN SOLO comando limpio con el valor correcto. Los botones
deberían funcionar bien: anterior, play/pausa y siguiente cada uno con su acción.

## También en esta versión (de la v4)
- Barras estilo CarMusicPlayer (segmentos LED apilados con reflejo).
- Reloj Nixie realista (panal hexagonal, glow multicapa, vidrio).
- Se puede salir del reloj (tocar la pantalla o botón atrás).
- Marco de neón tipo aviso luminoso alrededor de la pantalla.

## Compilar
Subí el contenido de esta carpeta a un repo (incluida la carpeta oculta
`.github`). El workflow corre `gradle assembleDebug` y sube el APK.

## Al probar
Los botones anterior / play-pausa / siguiente ahora deberían hacer cada uno
lo suyo, sin descoordinarse. Si algo sigue raro, el diagnóstico (⚙ → datos
crudos) sigue disponible para verlo.
