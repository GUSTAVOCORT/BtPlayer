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

    // Respaldo de tiempo cuando el firmware manda todo en 0
    private var firmwareGivesTime = false
    private var lastProgress = -1
    private var lastTimedKey = ""
    private var elapsedBaseMs = 0L        // tiempo acumulado hasta la ultima pausa
    private var elapsedSince = 0L         // instante en que arranco a correr

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

        b.btnPrev.setOnClickListener {
            controller.prev(); resetOwnTimer()
        }
        b.btnNext.setOnClickListener {
            controller.next(); resetOwnTimer()
        }
        b.btnPlay.setOnClickListener {
            controller.playPause(playing)
            // Congelar/reanudar el cronometro propio junto con el estado
            if (playing) {
                elapsedBaseMs += System.currentTimeMillis() - elapsedSince
            } else {
                elapsedSince = System.currentTimeMillis()
            }
            playing = !playing
            updatePlayIcon()
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

        // Neon tipo letrero antiguo en las mascaras del reproductor
        NeonFx.resetFlicker()
        if (prefs.maskNeon) {
            NeonFx.neonText(b.txtTitle, accent, 26f)
            b.txtTitle.setTextColor(NeonFx.litColor(accent))
            NeonFx.neonText(b.txtArtist, accent, 18f)
            NeonFx.neonText(b.txtDevice, accent, 10f)
            NeonFx.neonText(b.txtPos, accent, 10f)
            NeonFx.neonText(b.txtDur, accent, 10f)
            if (prefs.maskFlicker) {
                // titileo tipo tubo de gas viejo en los textos principales
                NeonFx.addFlicker(b.txtTitle, accent, 26f)
                NeonFx.addFlicker(b.txtArtist, accent, 18f)
                NeonFx.startFlicker()
            }
        } else {
            NeonFx.clear(b.txtTitle); b.txtTitle.setTextColor(0xFFF2F4F8.toInt())
            NeonFx.clear(b.txtArtist)
            NeonFx.clear(b.txtDevice)
            NeonFx.clear(b.txtPos); NeonFx.clear(b.txtDur)
        }

        // modo de pantalla
        when (prefs.screenMode) {
            0 -> { b.playerRoot.visibility = View.VISIBLE; b.clockRoot.visibility = View.GONE }
            1 -> { b.playerRoot.visibility = View.GONE; b.clockRoot.visibility = View.VISIBLE }
            2 -> { b.playerRoot.visibility = View.VISIBLE; b.clockRoot.visibility = View.GONE }
        }
        b.nixie.glow = prefs.nixieGlow
        b.nixie.use24h = prefs.nixie24h
        b.nixie.startClock()

        // Tocar el reloj vuelve al reproductor (evita quedar atrapado)
        b.clockRoot.setOnClickListener {
            prefs.screenMode = 0
            applyPrefs()
        }

        // Marco de neon tipo aviso luminoso
        b.neonFrame.enabled2 = prefs.frameNeon
        b.neonFrame.neonColor = accent
        b.neonFrame.flicker = prefs.maskFlicker
        if (prefs.frameNeon) b.neonFrame.startFx() else b.neonFrame.stopFx()
        b.neonFrame.invalidate()

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

        // Al cambiar de cancion, reiniciar el cronometro propio de respaldo.
        if (key != lastTimedKey) {
            lastTimedKey = key
            elapsedBaseMs = 0L
            elapsedSince = System.currentTimeMillis()
        }

        lastDurMs = st.durationMs
        lastProgress = st.progress
        // Solo confiar en la posicion del firmware si es > 0 (el tuyo manda 0).
        if (st.positionMs > 0) {
            lastPosMs = st.positionMs
            lastPosAt = System.currentTimeMillis()
            firmwareGivesTime = true
        }
        b.txtDur.text = if (st.durationMs > 0) fmt(st.durationMs) else "--:--"
    }

    private fun updatePlayIcon() { b.btnPlay.text = if (playing) "⏸" else "▶" }

    /** Reinicia el cronometro propio (al saltar de cancion con next/prev). */
    private fun resetOwnTimer() {
        elapsedBaseMs = 0L
        elapsedSince = System.currentTimeMillis()
    }

    /**
     * Reloj de tiempos con tres estrategias, en orden de preferencia:
     *  1. Posicion absoluta del firmware (si la manda > 0).
     *  2. Progreso 0-100 del firmware (si lo manda), mapeado a la duracion.
     *  3. Cronometro propio desde que empezo la cancion (fallback cuando el
     *     firmware manda todo en 0, que es el caso de este equipo).
     */
    private fun startClock() {
        ui.post(object : Runnable {
            override fun run() {
                if (!userSeeking) {
                    val now = System.currentTimeMillis()
                    val posMs: Long
                    val progressThousandths: Int

                    when {
                        firmwareGivesTime -> {
                            posMs = if (playing) lastPosMs + (now - lastPosAt) else lastPosMs
                            progressThousandths = if (lastDurMs > 0)
                                ((posMs * 1000) / lastDurMs).toInt().coerceIn(0, 1000) else 0
                        }
                        lastProgress in 0..100 -> {
                            progressThousandths = lastProgress * 10
                            posMs = if (lastDurMs > 0) lastDurMs * lastProgress / 100 else 0L
                        }
                        else -> {
                            // cronometro propio
                            val run = if (playing) now - elapsedSince else 0L
                            posMs = elapsedBaseMs + run
                            progressThousandths = if (lastDurMs > 0)
                                ((posMs * 1000) / lastDurMs).toInt().coerceIn(0, 1000) else 0
                        }
                    }

                    val clamped = if (lastDurMs > 0) posMs.coerceAtMost(lastDurMs) else posMs
                    b.txtPos.text = fmt(clamped)
                    b.seek.progress = progressThousandths
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Si estamos en el reloj, volver al reproductor en vez de salir.
        if (prefs.screenMode == 1) {
            prefs.screenMode = 0
            applyPrefs()
        } else {
            super.onBackPressed()
        }
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
        b.neonFrame.stopFx()
        NeonFx.stopFlicker()
        super.onStop()
    }

    override fun onDestroy() { eq.release(); super.onDestroy() }
}
