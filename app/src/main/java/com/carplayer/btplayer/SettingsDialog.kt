package com.carplayer.btplayer

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Panel de ajustes armado por codigo (sin XML extra). Cambia estilo de
 * visualizador, paleta, neon, forma, cantidad de barras, modo de pantalla,
 * reloj y fondo. Todo se guarda en Prefs y se aplica al vuelo via onApply.
 */
object SettingsDialog {

    fun show(activity: Activity, prefs: Prefs, onApply: () -> Unit, onPickBackground: () -> Unit) {
        val ctx = activity
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx,18), dp(ctx,14), dp(ctx,18), dp(ctx,14))
        }

        fun header(t: String) = TextView(ctx).apply {
            text = t; setTextColor(Color.parseColor("#FFC400"))
            textSize = 15f; setPadding(0, dp(ctx,12), 0, dp(ctx,6))
        }

        // --- Estilo de visualizador ---
        root.addView(header("Estilo de barras"))
        val styles = listOf("Barras", "Espejo", "Línea", "Puntos", "Onda rellena")
        root.addView(chips(ctx, styles, prefs.vizStyle) { i ->
            prefs.vizStyle = i; onApply()
        })

        // --- Paleta ---
        root.addView(header("Color / paleta"))
        val palNames = Palettes.list.map { it.name }
        root.addView(chips(ctx, palNames, prefs.vizPalette) { i ->
            prefs.vizPalette = i; prefs.accentColor = Palettes.accentOf(i); onApply()
        })

        // --- Cantidad de barras ---
        root.addView(header("Cantidad de barras"))
        val counts = listOf(20, 28, 40, 56, 72)
        root.addView(chips(ctx, counts.map { it.toString() }, counts.indexOf(prefs.vizBars).coerceAtLeast(0)) { i ->
            prefs.vizBars = counts[i]; onApply()
        })

        // --- Tamaño (altura) de las barras ---
        root.addView(header("Tamaño de las barras"))
        root.addView(slider(ctx, 60, 100, prefs.vizHeight) { v ->
            prefs.vizHeight = v; onApply()
        })

        // --- Sensibilidad ---
        root.addView(header("Sensibilidad (qué tanto reaccionan)"))
        root.addView(slider(ctx, 80, 250, prefs.vizGain) { v ->
            prefs.vizGain = v; onApply()
        })

        // --- Toggles ---
        root.addView(header("Efectos"))
        root.addView(check(ctx, "Neón (glow) en barras", prefs.vizNeon) { prefs.vizNeon = it; onApply() })
        root.addView(check(ctx, "Barras redondeadas", prefs.vizRounded) { prefs.vizRounded = it; onApply() })
        root.addView(check(ctx, "Neón tipo letrero en el texto", prefs.maskNeon) { prefs.maskNeon = it; onApply() })

        // --- Modo de pantalla ---
        root.addView(header("Pantalla"))
        val modes = listOf("Reproductor", "Reloj Nixie", "Reloj + música")
        root.addView(chips(ctx, modes, prefs.screenMode) { i ->
            prefs.screenMode = i; onApply()
        })
        root.addView(check(ctx, "Reloj 24 horas", prefs.nixie24h) { prefs.nixie24h = it; onApply() })
        root.addView(check(ctx, "Glow del reloj", prefs.nixieGlow) { prefs.nixieGlow = it; onApply() })

        // --- Caratula ---
        root.addView(header("Carátula"))
        val covers = listOf("Inicial", "Abstracto", "Anillos")
        root.addView(chips(ctx, covers, prefs.coverStyle) { i ->
            prefs.coverStyle = i; onApply()
        })

        // --- Fondo ---
        root.addView(header("Fondo"))
        val bgRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        bgRow.addView(Button(ctx).apply {
            text = "Cargar imagen"
            setOnClickListener { onPickBackground() }
        })
        bgRow.addView(Button(ctx).apply {
            text = "Quitar fondo"
            setOnClickListener { prefs.bgUri = null; onApply() }
        })
        root.addView(bgRow)

        // --- Diagnostico de tiempos ---
        root.addView(header("Diagnóstico"))
        root.addView(check(ctx, "Mostrar datos crudos del Bluetooth", prefs.showDebug) {
            prefs.showDebug = it; onApply()
        })

        val scroll = ScrollView(ctx).apply { addView(root) }

        AlertDialog.Builder(ctx)
            .setTitle("Ajustes")
            .setView(scroll)
            .setPositiveButton("Listo", null)
            .show()
    }

    // ---- helpers de UI ----

    private fun chips(ctx: Context, labels: List<String>, selected: Int, onPick: (Int) -> Unit): View {
        val wrap = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val scroll = android.widget.HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false; addView(wrap)
        }
        val buttons = mutableListOf<Button>()
        labels.forEachIndexed { i, label ->
            val bt = Button(ctx).apply {
                text = label
                isAllCaps = false
                textSize = 13f
                setPadding(dp(ctx,14),0,dp(ctx,14),0)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(ctx,42))
                lp.rightMargin = dp(ctx,8)
                layoutParams = lp
                setOnClickListener {
                    onPick(i)
                    buttons.forEachIndexed { j, b -> style(b, j == i) }
                }
            }
            style(bt, i == selected)
            buttons.add(bt)
            wrap.addView(bt)
        }
        return scroll
    }

    private fun style(b: Button, on: Boolean) {
        b.setBackgroundColor(if (on) Color.parseColor("#FFC400") else Color.parseColor("#22242C"))
        b.setTextColor(if (on) Color.parseColor("#0A0A0C") else Color.parseColor("#F2F4F8"))
    }

    private fun check(ctx: Context, label: String, on: Boolean, onChange: (Boolean) -> Unit): View {
        return CheckBox(ctx).apply {
            text = label
            isChecked = on
            setTextColor(Color.parseColor("#F2F4F8"))
            setOnCheckedChangeListener { _, v -> onChange(v) }
        }
    }

    /** Slider horizontal de min..max con etiqueta de valor a la derecha. */
    private fun slider(ctx: Context, min: Int, max: Int, current: Int, onChange: (Int) -> Unit): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val label = TextView(ctx).apply {
            text = current.toString()
            setTextColor(Color.parseColor("#FFC400"))
            textSize = 14f
            width = dp(ctx, 52)
            gravity = Gravity.END
        }
        val bar = android.widget.SeekBar(ctx).apply {
            this.max = max - min
            progress = (current - min).coerceIn(0, max - min)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                    val value = min + p
                    label.text = value.toString()
                    if (fromUser) onChange(value)
                }
                override fun onStartTrackingTouch(s: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(s: android.widget.SeekBar?) {}
            })
        }
        row.addView(bar)
        row.addView(label)
        return row
    }

    private fun dp(ctx: Context, v: Int): Int =
        (v * ctx.resources.displayMetrics.density).toInt()
}
