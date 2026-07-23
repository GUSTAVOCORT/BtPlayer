package com.carplayer.btplayer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max

/**
 * Barras de espectro reactivas a la musica. Engancha Visualizer(0) sobre la
 * mezcla global de audio (confirmado que funciona en este equipo: el audio
 * Bluetooth pasa por el mixer de Android). Dibuja a ~30 FPS con suavizado,
 * igual que el visualizador de CarMusicPlayer.
 */
class VisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var visualizer: Visualizer? = null
    private val bars = 48
    private val levels = FloatArray(bars)
    private val target = FloatArray(bars)
    private var fft: ByteArray? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shader: Shader? = null
    private var running = false

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            // Interpolar hacia el objetivo para movimiento fluido.
            for (i in 0 until bars) {
                levels[i] += (target[i] - levels[i]) * 0.28f
            }
            invalidate()
            postDelayed(this, 33L)   // ~30 FPS
        }
    }

    fun start() {
        if (running) return
        try {
            val v = Visualizer(0)
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            fft = ByteArray(v.captureSize)
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vv: Visualizer?, wave: ByteArray?, rate: Int) {}
                override fun onFftDataCapture(vv: Visualizer?, data: ByteArray?, rate: Int) {
                    data ?: return
                    mapFft(data)
                }
            }, Visualizer.getMaxCaptureRate(), false, true)
            v.enabled = true
            visualizer = v
            running = true
            post(frame)
        } catch (_: Throwable) {
            // Sin permiso de micro o Visualizer no disponible: se queda quieto.
            running = false
        }
    }

    fun stop() {
        running = false
        removeCallbacks(frame)
        try { visualizer?.enabled = false } catch (_: Throwable) {}
        try { visualizer?.release() } catch (_: Throwable) {}
        visualizer = null
    }

    /** Convierte el FFT crudo en alturas de barra (0..1) con escala log. */
    private fun mapFft(data: ByteArray) {
        val n = data.size / 2
        val per = max(1, n / bars)
        for (b in 0 until bars) {
            var sum = 0f
            val start = b * per
            for (j in 0 until per) {
                val idx = (start + j) * 2
                if (idx + 1 < data.size) {
                    val re = data[idx].toFloat()
                    val im = data[idx + 1].toFloat()
                    sum += abs(re) + abs(im)
                }
            }
            var v = (sum / per) / 128f
            v = (v * 1.6f).coerceIn(0f, 1f)
            // dar mas presencia a graves sin saturar agudos
            target[b] = v
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        shader = LinearGradient(0f, h.toFloat(), 0f, 0f,
            intArrayOf(
                Color.parseColor("#FFC400"),  // amarillo (base, combina con el icono)
                Color.parseColor("#FF7A00"),  // naranja
                Color.parseColor("#FF2D95")   // magenta arriba
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP)
        paint.shader = shader
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = w / bars * 0.28f
        val bw = (w / bars) - gap
        var x = gap / 2f
        val minH = h * 0.04f
        for (i in 0 until bars) {
            val bh = minH + levels[i] * (h * 0.92f)
            val top = h - bh
            val r = bw * 0.35f
            canvas.drawRoundRect(x, top, x + bw, h, r, r, paint)
            x += bw + gap
        }
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }
}
