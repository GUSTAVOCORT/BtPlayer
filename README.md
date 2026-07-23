# BT Player v3 — reproductor Bluetooth para head unit Allwinner T3

## Novedades v3 (lo importante)
- **CONTROLES ARREGLADOS**: los botones next/prev/play/pausa ahora usan los
  códigos reales del firmware (NEXT=0, PREVIOUS=2, PAUSE=3, PLAY=4 vía
  `com.nwd.ACTION_A2DP_CONTROL_COMMAND` con `extra_command`), extraídos del
  APK del fabricante. Antes usaban códigos AVRCP estándar que este módulo
  BC03 ignora.
- **TIEMPOS ARREGLADOS**: ahora lee `key_a2dp_cur_time` y
  `key_a2dp_total_time` (las claves reales del firmware) además de las
  anteriores. Nota: si tu teléfono no reporta la duración por Bluetooth,
  la barra igual avanza con la posición.
- **Barras más grandes y ajustables**: en Ajustes → tamaño de barras
  (60–100%), cantidad (20–72) y sensibilidad (80–250%).
- **Neón tipo letrero antiguo**: el título/artista y los tiempos irradian un
  halo de color como un aviso luminoso. Se activa/desactiva en Ajustes →
  "Neón tipo letrero en el texto".

## Resto de funciones (de v2)
Visualizador con 5 estilos y 8 paletas, reloj Nixie, fondo cargable,
carátula generada con 3 estilos, ecualizador de 5 bandas, diagnóstico.

## Compilar
Subí el contenido a un repo de GitHub (incluida `.github`). El workflow
corre `gradle assembleDebug` y sube el APK como `BtPlayer-debug-apk`.

## Si algo aún no responde
Con el diagnóstico activado, tocá los botones y mirá si aparece algún
broadcast nuevo. Los códigos de comando están en `NwdProtocol.kt` (CMD_*).
