package com.carplayer.btplayer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.carplayer.btplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var controller: BtController
    private lateinit var prefs: Prefs
    private val eq = EqualizerController()
    private lateinit var receiver: BtMediaReceiver

    private val ui = Handler(Looper.getMainLooper())
    private var lastCoverKey = ""
    private var eqVisible = false

    private var lastPosMs = 0L
    private var lastDurMs = 0L
    private var lastPosAt = 0L
    private var playing = false
    private var userSeeking = false

    private val PICK_BG = 777

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        prefs = Prefs(this)
        controller = BtController(applicationContext)

        askAudioPermission()

        receiver = BtMediaReceiver(
            onChange = { st -> ui.post { render(st) } },
            onLog = { log -> if (prefs.showDebug) ui.post { b.debugPanel.text = log } }
        )

        b.btnPrev.setOnClickListener { controller.prev() }
        b.btnNext.setOnClickListener { controller.next() }
        b.btnPlay.setOnClickListener {
            controller.playPause(playing); playing = !playing; updatePlayIcon()
        }
        b.btnEq.setOnClickListener { toggleEq() }
        b.btnSettings.setOnClickListener { openSettings() }

        b.seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(s: SeekBar?) { userSeeking = true }
            override fun onStopTrackingTouch(s: SeekBar?) { userSeeking = false }
        })

        b.txtTitle.isSelected = true   // activa marquee
        applyPrefs()
        updatePlayIcon()
        startClock()
    }

    // ---------------------------------------------------------------- permisos
    private fun askAudioPermission() {
        val need = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 31 &&
            ActivityCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_CONNECT")
            != PackageManager.PERMISSION_GRANTED) need.add("android.permission.BLUETOOTH_CONNECT")
        if (need.isNotEmpty()) ActivityCompat.requestPermissions(this, need.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        b.visualizer.start()
    }

    // ---------------------------------------------------------------- prefs
    private fun applyPrefs() {
        b.visualizer.applyConfig(prefs)

        val accent = prefs.accentColor
        b.txtArtist.setTextColor(accent)
        b.seek.progressTintList = android.content.res.ColorStateList.valueOf(accent)
        b.seek.thumbTintList = android.content.res.ColorStateList.valueOf(accent)

        // modo de pantalla
        when (prefs.screenMode) {
            0 -> { b.playerRoot.visibility = View.VISIBLE; b.clockRoot.visibility = View.GONE }
            1 -> { b.playerRoot.visibility = View.GONE; b.clockRoot.visibility = View.VISIBLE }
            2 -> { b.playerRoot.visibility = View.VISIBLE; b.clockRoot.visibility = View.GONE }
        }
        b.nixie.glow = prefs.nixieGlow
        b.nixie.use24h = prefs.nixie24h
        b.nixie.startClock()

        // fondo
        val uriStr = prefs.bgUri
        if (uriStr != null) {
            try {
                b.bgImage.setImageURI(Uri.parse(uriStr))
                b.bgImage.visibility = View.VISIBLE
                val alpha = (prefs.bgDim * 255 / 100)
                b.bgDim.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
            } catch (_: Throwable) {
                b.bgImage.visibility = View.GONE
            }
        } else {
            b.bgImage.visibility = View.GONE
            b.bgDim.setBackgroundColor(Color.parseColor("#00000000"))
        }

        // debug
        b.debugPanel.visibility = if (prefs.showDebug) View.VISIBLE else View.GONE

        // regenerar caratula con estilo nuevo
        lastCoverKey = ""
        render(receiver.state)
    }

    private fun openSettings() {
        SettingsDialog.show(this, prefs,
            onApply = { applyPrefs() },
            onPickBackground = { pickBackground() })
    }

    private fun pickBackground() {
        // OPEN_DOCUMENT permite permiso persistente; si el firmware viejo no lo
        // soporta, caemos a GET_CONTENT y copiamos la imagen a almacenamiento propio.
        try {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                         Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivityForResult(i, PICK_BG)
        } catch (_: Throwable) {
            try {
                val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                startActivityForResult(i, PICK_BG)
            } catch (_: Throwable) {}
        }
    }

    /** Copia el contenido de una uri a un archivo propio y devuelve su ruta. */
    private fun copyBgToLocal(uri: Uri): String? {
        return try {
            val out = java.io.File(filesDir, "bg_custom.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { o -> input.copyTo(o) }
            }
            Uri.fromFile(out).toString()
        } catch (_: Throwable) { null }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_BG && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            var stored: String? = null
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                stored = uri.toString()
            } catch (_: Throwable) {
                // sin permiso persistente: copiar a almacenamiento propio
                stored = copyBgToLocal(uri)
            }
            if (stored == null) stored = copyBgToLocal(uri)
            prefs.bgUri = stored
            applyPrefs()
        }
    }

    // ---------------------------------------------------------------- render
    private fun render(st: PlaybackState) {
        b.txtTitle.text = if (st.title.isBlank()) getString(R.string.no_track) else st.title
        b.txtArtist.text = st.artist
        b.txtAlbum.text = st.album
        b.txtDevice.text = when {
            st.deviceName.isNotBlank() -> st.deviceName
            st.connected -> "Bluetooth conectado"
            else -> getString(R.string.waiting)
        }
        b.clockTrack.text = if (st.title.isBlank()) "" else "♪ ${st.title} — ${st.artist}"

        val key = st.trackKey()
        if (key != lastCoverKey && (st.title.isNotBlank() || st.artist.isNotBlank())) {
            lastCoverKey = key
            val seed = st.artist.ifBlank { st.title }
            b.cover.setImageBitmap(CoverArt.generate(300, seed, prefs.coverStyle, prefs.accentColor))
        } else if (key != lastCoverKey) {
            lastCoverKey = key
            b.cover.setImageBitmap(CoverArt.generate(300, "BT", prefs.coverStyle, prefs.accentColor))
        }

        playing = st.isPlaying
        updatePlayIcon()

        lastDurMs = st.durationMs
        lastPosMs = st.positionMs
        lastPosAt = System.currentTimeMillis()
        b.txtDur.text = fmt(st.durationMs)
    }

    private fun updatePlayIcon() { b.btnPlay.text = if (playing) "⏸" else "▶" }

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

    // ---------------------------------------------------------------- eq
    private fun toggleEq() {
        eqVisible = !eqVisible
        if (eqVisible && eq.bandCount == 0) buildEq()
        b.eqPanel.visibility = if (eqVisible) View.VISIBLE else View.GONE
    }

    private fun buildEq() {
        if (!eq.open()) {
            b.eqPanel.addView(TextView(this).apply {
                text = "Ecualizador no disponible"; setTextColor(0xFF9AA0AA.toInt())
            })
            return
        }
        val min = eq.minMillibel(); val max = eq.maxMillibel()
        val span = (max - min).coerceAtLeast(1)
        for (band in 0 until eq.bandCount) {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }
            val bar = SeekBar(this).apply {
                rotation = 270f; this.max = span; progress = eq.level(band) - min
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        if (fromUser) eq.setLevel(band, min + p)
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            val holder = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                gravity = android.view.Gravity.CENTER
                addView(bar, LinearLayout.LayoutParams(220, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
            val hz = eq.centerHz(band)
            val label = TextView(this).apply {
                text = if (hz >= 1000) "${hz / 1000}k" else "$hz"
                setTextColor(0xFFF2F4F8.toInt()); textSize = 11f
                gravity = android.view.Gravity.CENTER
            }
            col.addView(holder); col.addView(label); b.eqPanel.addView(col)
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
            == PackageManager.PERMISSION_GRANTED) b.visualizer.start()
        b.nixie.startClock()
    }

    override fun onStop() {
        receiver.unregister(applicationContext)
        b.visualizer.stop()
        b.nixie.stopClock()
        super.onStop()
    }

    override fun onDestroy() { eq.release(); super.onDestroy() }
}
