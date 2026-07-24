# BT Player v6 — reproductor Bluetooth para head unit Allwinner T3

## Estado de los controles
- Siguiente y anterior: FUNCIONAN (verificado con el firmware).
- Play/pausa: en v5 no respondía. Ahora se manda por triple vía (comando toggle
  + broadcast directo play/pause + tabla alternativa) para que enganche.

## Arreglos de esta versión
**Reloj Nixie legible.** Antes el dígito se difuminaba hasta desaparecer porque
el glow borroneaba el número. Ahora el dígito se dibuja SÓLIDO y nítido, con el
glow por detrás. Se lee siempre.

**Reloj + música por fin hace algo.** En modo "Reloj + música" el reloj Nixie
aparece en el panel derecho (donde va el visualizador). Al abrir el ecualizador
el reloj se oculta y vuelve el visualizador; al cerrarlo, vuelve el reloj.

**Fondo cargable más compatible.** Prueba tres métodos de selección de imagen
en orden. Si ninguno funciona en tu equipo, ahora avisa con un mensaje en vez
de fallar en silencio. La imagen elegida se copia a la app para que sobreviva
reinicios.

**Barras segmentadas tipo CarMusicPlayer, marco de neón, salir del reloj** —
todo lo de la v5 sigue.

## Compilar
Subí el contenido de esta carpeta a un repo (incluida la carpeta oculta
`.github`). El workflow corre `gradle assembleDebug` y sube el APK.

## Al probar, contame
1. ¿Play/pausa ya funciona?
2. ¿El reloj Nixie se ve nítido (número legible)?
3. En modo "Reloj + música", ¿aparece el reloj a la derecha y desaparece al
   abrir el ecualizador?
4. ¿El botón de cargar fondo abre algo o da el mensaje de aviso?
