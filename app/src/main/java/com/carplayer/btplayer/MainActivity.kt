package com.carplayer.btplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.carplayer.btplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var controller: BtController
    private val eq = EqualizerController()
    private lateinit var receiver: BtMediaReceiver

    private val ui = Handler(Looper.getMainLooper())
    private var lastCoverKey = ""
    private var eqVisible = false

    // Interpolacion local del tiempo: el firmware manda posicion cada tanto,
    // pero entre updates hacemos correr el reloj para que la barra sea fluida.
    private var lastPosMs = 0L
    private var lastDurMs = 0L
    private var lastPosAt = 0L
    private var playing = false
    private var userSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        controller = BtController(applicationContext)

        askAudioPermission()

        receiver = BtMediaReceiver { st -> ui.post { render(st) } }

        b.btnPrev.setOnClickListener { controller.prev() }
        b.btnNext.setOnClickListener { controller.next() }
        b.btnPlay.setOnClickListener {
            controller.playPause(playing)
            playing = !playing
            updatePlayIcon()
        }
        b.btnEq.setOnClickListener { toggleEq() }

        b.seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(s: SeekBar?) { userSeeking = true }
            override fun onStopTrackingTouch(s: SeekBar?) { userSeeking = false }
            // Nota: el A2DP remoto no siempre acepta seek; se deja informativo.
        })

        // Caratula inicial
        b.cover.setImageBitmap(CoverArt.generate(300, "BT"))
        updatePlayIcon()
        startClock()
    }

    private fun askAudioPermission() {
        val need = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) need.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 31 &&
            ActivityCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_CONNECT")
            != PackageManager.PERMISSION_GRANTED
        ) need.add("android.permission.BLUETOOTH_CONNECT")
        if (need.isNotEmpty()) ActivityCompat.requestPermissions(this, need.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        b.visualizer.start()   // ahora que hay permiso de micro
    }

    private fun render(st: PlaybackState) {
        b.txtTitle.text = if (st.title.isBlank()) getString(R.string.no_track) else st.title
        b.txtArtist.text = st.artist
        b.txtAlbum.text = st.album
        b.txtDevice.text = when {
            st.deviceName.isNotBlank() -> st.deviceName
            st.connected -> "Bluetooth conectado"
            else -> getString(R.string.waiting)
        }

        // Regenerar caratula solo si cambio la cancion
        val key = st.trackKey()
        if (key != lastCoverKey && (st.title.isNotBlank() || st.artist.isNotBlank())) {
            lastCoverKey = key
            val seed = st.artist.ifBlank { st.title }
            b.cover.setImageBitmap(CoverArt.generate(300, seed))
        }

        playing = st.isPlaying
        updatePlayIcon()

        lastDurMs = st.durationMs
        lastPosMs = st.positionMs
        lastPosAt = System.currentTimeMillis()

        b.txtDur.text = fmt(st.durationMs)
    }

    private fun updatePlayIcon() {
        b.btnPlay.text = if (playing) "⏸" else "▶"
    }

    /** Reloj local a 4 Hz: interpola la posicion entre updates del firmware. */
    private fun startClock() {
        ui.post(object : Runnable {
            override fun run() {
                if (!userSeeking) {
                    val now = System.currentTimeMillis()
                    val pos = if (playing) lastPosMs + (now - lastPosAt) else lastPosMs
                    val clamped = if (lastDurMs > 0) pos.coerceAtMost(lastDurMs) else pos
                    b.txtPos.text = fmt(clamped)
                    b.seek.progress = if (lastDurMs > 0)
                        ((clamped * 1000) / lastDurMs).toInt().coerceIn(0, 1000) else 0
                }
                ui.postDelayed(this, 250L)
            }
        })
    }

    private fun toggleEq() {
        eqVisible = !eqVisible
        if (eqVisible && eq.bandCount == 0) buildEq()
        b.eqPanel.visibility = if (eqVisible) View.VISIBLE else View.GONE
    }

    /** Construye dinamicamente los sliders verticales de las bandas. */
    private fun buildEq() {
        if (!eq.open()) {
            val t = TextView(this).apply {
                text = "Ecualizador no disponible"
                setTextColor(0xFF9AA0AA.toInt())
            }
            b.eqPanel.addView(t)
            return
        }
        val min = eq.minMillibel()
        val max = eq.maxMillibel()
        val span = (max - min).coerceAtLeast(1)

        for (band in 0 until eq.bandCount) {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }
            val bar = SeekBar(this).apply {
                rotation = 270f
                this.max = span
                progress = eq.level(band) - min
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        if (fromUser) eq.setLevel(band, min + p)
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            // Contenedor para que el slider rotado ocupe verticalmente
            val holder = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                gravity = android.view.Gravity.CENTER
                addView(bar, LinearLayout.LayoutParams(220, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
            val hz = eq.centerHz(band)
            val label = TextView(this).apply {
                text = if (hz >= 1000) "${hz / 1000}k" else "$hz"
                setTextColor(0xFFF2F4F8.toInt())
                textSize = 11f
                gravity = android.view.Gravity.CENTER
            }
            col.addView(holder)
            col.addView(label)
            b.eqPanel.addView(col)
        }
    }

    private fun fmt(ms: Long): String {
        if (ms <= 0) return "0:00"
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    override fun onStart() {
        super.onStart()
        receiver.register(applicationContext)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) b.visualizer.start()
    }

    override fun onStop() {
        receiver.unregister(applicationContext)
        b.visualizer.stop()
        super.onStop()
    }

    override fun onDestroy() {
        eq.release()
        super.onDestroy()
    }
}
