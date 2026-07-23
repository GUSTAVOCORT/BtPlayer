package com.carplayer.btplayer

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max

/**
 * Visualizador configurable. Estilos: barras, espejo, linea, puntos, ondas.
 * Paleta seleccionable, neon opcional (glow), barras redondeadas o rectas,
 * cantidad de barras ajustable. Engancha Visualizer(0) al mixer global.
 */
class VisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // Config (se setea desde afuera segun Prefs)
    var style = 0            // 0 barras, 1 espejo, 2 linea, 3 puntos, 4 ondas
    var paletteIndex = 0
    var neon = true
    var rounded = true
    var heightPct = 92      // alto maximo de barras como % (60..100)
    var widthPct = 72       // grosor de barras (20..100)
    var gain = 130          // ganancia/sensibilidad (100 = normal)
    var barCount = 40
        set(v) { field = v.coerceIn(16, 96); rebuildArrays() }

    private var visualizer: Visualizer? = null
    private var levels = FloatArray(barCount)
    private var target = FloatArray(barCount)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shader: Shader? = null
    private var running = false

    private fun rebuildArrays() {
        levels = FloatArray(barCount)
        target = FloatArray(barCount)
    }

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            for (i in levels.indices) levels[i] += (target[i] - levels[i]) * 0.28f
            invalidate()
            postDelayed(this, 33L)
        }
    }

    fun applyConfig(p: Prefs) {
        style = p.vizStyle
        paletteIndex = p.vizPalette
        neon = p.vizNeon
        rounded = p.vizRounded
        heightPct = p.vizHeight
        gain = p.vizGain
        widthPct = p.vizWidth
        barCount = p.vizBars
        buildShader(width, height)
        invalidate()
    }

    fun start() {
        if (running) return
        try {
            val v = Visualizer(0)
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            val cap = ByteArray(v.captureSize)
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vv: Visualizer?, wave: ByteArray?, rate: Int) {}
                override fun onFftDataCapture(vv: Visualizer?, data: ByteArray?, rate: Int) {
                    data?.let { mapFft(it) }
                }
            }, Visualizer.getMaxCaptureRate(), false, true)
            v.enabled = true
            visualizer = v
            running = true
            post(frame)
        } catch (_: Throwable) { running = false }
    }

    fun stop() {
        running = false
        removeCallbacks(frame)
        try { visualizer?.enabled = false } catch (_: Throwable) {}
        try { visualizer?.release() } catch (_: Throwable) {}
        visualizer = null
    }

    private fun mapFft(data: ByteArray) {
        val n = data.size / 2
        val per = max(1, n / barCount)
        for (b in 0 until barCount) {
            var sum = 0f
            val start = b * per
            for (j in 0 until per) {
                val idx = (start + j) * 2
                if (idx + 1 < data.size) {
                    val re = data[idx].toFloat(); val im = data[idx + 1].toFloat()
                    sum += abs(re) + abs(im)
                }
            }
            var v = (sum / per) / 128f
            v = (v * (gain / 100f)).coerceIn(0f, 1f)
            if (b < target.size) target[b] = v
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        buildShader(w, h)
    }

    private fun buildShader(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val pal = Palettes.get(paletteIndex)
        shader = LinearGradient(0f, h.toFloat(), 0f, 0f, pal.colors, pal.stops, Shader.TileMode.CLAMP)
        paint.shader = shader
        glowPaint.shader = shader
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (shader == null) buildShader(width, height)

        // Configurar glow neon
        if (neon) {
            glowPaint.maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
            glowPaint.style = Paint.Style.FILL
        }

        when (style) {
            0 -> drawSegments(canvas, w, h, reflect = true)   // estilo CarMusicPlayer (LED apilado)
            1 -> drawBars(canvas, w, h, mirror = false)        // barras solidas
            2 -> drawBars(canvas, w, h, mirror = true)         // espejo
            3 -> drawLine(canvas, w, h, fill = false)          // linea
            4 -> drawDots(canvas, w, h)                        // puntos
            5 -> drawLine(canvas, w, h, fill = true)           // onda
            else -> drawSegments(canvas, w, h, reflect = true)
        }
    }

    /**
     * Estilo CarMusicPlayer: cada barra es una pila de segmentos LED con
     * pequeños huecos entre ellos, y un reflejo espejado abajo que se desvanece.
     * El color de cada segmento sale del degradado de la paleta segun su altura.
     */
    private fun drawSegments(canvas: Canvas, w: Float, h: Float, reflect: Boolean) {
        val gapFactor = 0.5f - (widthPct / 100f) * 0.42f
        val gap = w / barCount * gapFactor
        val bw = (w / barCount) - gap
        val hp = heightPct / 100f

        // zona de barras arriba y reflejo abajo
        val reflectFrac = if (reflect) 0.26f else 0f
        val barZone = h * (1f - reflectFrac)
        val baseY = barZone
        val maxH = barZone * hp

        val segH = maxH / 22f              // ~22 segmentos posibles
        val segGap = segH * 0.28f
        val segDraw = segH - segGap
        val r = segDraw * 0.35f

        var x = gap / 2f
        paint.style = Paint.Style.FILL
        for (i in 0 until barCount) {
            val lvl = if (i < levels.size) levels[i] else 0f
            val lit = (lvl * 22).toInt().coerceIn(0, 22)
            for (s in 0 until lit) {
                val segBottom = baseY - s * segH
                val segTop = segBottom - segDraw
                if (neon) {
                    glowPaint.alpha = 90
                    canvas.drawRoundRect(x, segTop, x + bw, segBottom, r, r, glowPaint)
                }
                canvas.drawRoundRect(x, segTop, x + bw, segBottom, r, r, paint)
            }
            // reflejo espejado abajo, mas tenue
            if (reflect) {
                val refCount = (lit * 0.5f).toInt()
                for (s in 0 until refCount) {
                    val segTop = baseY + s * segH
                    val segBottom = segTop + segDraw
                    paint.alpha = (90 - s * 12).coerceIn(15, 90)
                    canvas.drawRoundRect(x, segTop, x + bw, segBottom, r, r, paint)
                }
                paint.alpha = 255
            }
            x += bw + gap
        }
    }

    private fun drawBars(canvas: Canvas, w: Float, h: Float, mirror: Boolean) {
        // widthPct alto => barras mas gordas (menos gap)
        val gapFactor = 0.5f - (widthPct / 100f) * 0.42f
        val gap = w / barCount * gapFactor
        val bw = (w / barCount) - gap
        var x = gap / 2f
        val hp = heightPct / 100f
        val baseY = if (mirror) h / 2f else h
        val maxH = if (mirror) h * 0.5f * hp else h * hp
        val minH = h * 0.03f
        val r = if (rounded) bw * 0.45f else 0f
        paint.style = Paint.Style.FILL
        for (i in 0 until barCount) {
            val lvl = if (i < levels.size) levels[i] else 0f
            val bh = minH + lvl * maxH
            if (mirror) {
                if (neon) { glowPaint.alpha = (120 * lvl).toInt().coerceIn(20,160)
                    canvas.drawRoundRect(x, baseY - bh, x + bw, baseY + bh, r, r, glowPaint) }
                canvas.drawRoundRect(x, baseY - bh, x + bw, baseY + bh, r, r, paint)
            } else {
                if (neon) { glowPaint.alpha = (120 * lvl).toInt().coerceIn(20,160)
                    canvas.drawRoundRect(x, baseY - bh, x + bw, baseY, r, r, glowPaint) }
                canvas.drawRoundRect(x, baseY - bh, x + bw, baseY, r, r, paint)
            }
            x += bw + gap
        }
    }

    private fun drawLine(canvas: Canvas, w: Float, h: Float, fill: Boolean) {
        val path = Path()
        val step = w / (barCount - 1)
        val baseY = h
        path.moveTo(0f, baseY - (if (levels.isNotEmpty()) levels[0] else 0f) * h * 0.9f)
        for (i in 0 until barCount) {
            val lvl = if (i < levels.size) levels[i] else 0f
            val x = i * step
            val y = baseY - lvl * h * 0.9f - h * 0.03f
            path.lineTo(x, y)
        }
        if (fill) {
            path.lineTo(w, baseY); path.lineTo(0f, baseY); path.close()
            paint.style = Paint.Style.FILL
            if (neon) { glowPaint.style = Paint.Style.FILL; glowPaint.alpha = 90; canvas.drawPath(path, glowPaint) }
            canvas.drawPath(path, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = h * 0.02f
            paint.strokeJoin = Paint.Join.ROUND
            if (neon) { glowPaint.style = Paint.Style.STROKE; glowPaint.strokeWidth = h * 0.02f
                glowPaint.alpha = 140; canvas.drawPath(path, glowPaint) }
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawDots(canvas: Canvas, w: Float, h: Float) {
        val step = w / barCount
        paint.style = Paint.Style.FILL
        for (i in 0 until barCount) {
            val lvl = if (i < levels.size) levels[i] else 0f
            val x = i * step + step / 2f
            val y = h - (h * 0.03f + lvl * h * 0.9f)
            val rad = (step * 0.28f) * (0.5f + lvl)
            if (neon) { glowPaint.style = Paint.Style.FILL; glowPaint.alpha = (150*lvl).toInt().coerceIn(30,170)
                canvas.drawCircle(x, y, rad * 1.8f, glowPaint) }
            canvas.drawCircle(x, y, rad, paint)
        }
    }

    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }
}
