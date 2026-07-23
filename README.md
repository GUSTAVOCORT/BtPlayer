# BT Player v2 — reproductor Bluetooth para head unit Allwinner T3

Muestra la música que llega por Bluetooth desde el teléfono, con visualizador
configurable, ecualizador, reloj Nixie, fondo cargable y neón.

## Novedades v2
- **Visualizador configurable**: 5 estilos (barras, espejo, línea, puntos,
  onda), 8 paletas de color, cantidad de barras ajustable, neón on/off,
  barras redondeadas o rectas.
- **Reloj Nixie**: estilo tubos antiguos con halo de colores, como modo de
  pantalla dedicado o combinado con el reproductor.
- **Fondo cargable**: elegí una imagen del equipo como fondo, con
  oscurecimiento ajustable.
- **Carátula mejorada**: 3 estilos (inicial, abstracto, anillos), esquinas
  redondeadas y glow.
- **Panel de ajustes** (botón ⚙) con todo lo anterior, y persistente.
- **Diagnóstico de tiempos**: en Ajustes → "Mostrar datos crudos del
  Bluetooth" aparece abajo un panel con TODO lo que manda el firmware
  (acción + extras + tipos + valores). Sirve para ver por qué los tiempos
  no cuadran.

## Sobre los tiempos
Si la posición/duración se ve mal, activá el diagnóstico y mirá el panel
verde de abajo mientras suena música. Fijate qué clave trae la duración y la
posición, y en qué unidad (segundos o milisegundos). Con esa línea puedo
afinar el mapeo exacto — está preparado para varias variantes pero el dato
real manda.

## Compilar
Subí el contenido de esta carpeta a un repo de GitHub (incluida la carpeta
oculta `.github`). El workflow corre `gradle assembleDebug` y sube el APK
como artifact `BtPlayer-debug-apk`.

## Uso
1. Instalá, aceptá el permiso de micrófono (para el visualizador).
2. Emparejá el teléfono y poné música.
3. Botón ⚙ para personalizar todo. Botón EQ para el ecualizador.
4. Si los botones de control no responden, avisá: los códigos de comando
   se ajustan en `NwdProtocol.kt`.
