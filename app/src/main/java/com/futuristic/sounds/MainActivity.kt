package com.futuristic.sounds

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private val BG = Color.parseColor("#0A0F1E")
    private val CARD = Color.parseColor("#141B33")
    private val CYAN = Color.parseColor("#00E5FF")
    private val DIM = Color.parseColor("#9AA5C4")

    private lateinit var status: TextView
    private val customLabels = HashMap<SoundEvent, TextView>()
    private var pending: SoundEvent? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun seek(progress: Int, onChange: (Int) -> Unit, onDone: () -> Unit) =
        SeekBar(this).apply {
            max = 100
            this.progress = progress
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) onChange(p)
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) = onDone()
            })
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SoundEngine.init(this)

        if (!License.isActivated(this)) {
            showActivationScreen()
        } else {
            showMainUi()
        }
    }

    // ---------------- एक्टिवेशन स्क्रीन ----------------

    private fun showActivationScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(24))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(BG)
            addView(root)
        }
        setContentView(scroll)

        val deviceId = License.deviceId(this)

        root.addView(tv("FUTURISTIC SOUNDS", 22f, CYAN, true))
        root.addView(tv("यह ऐप एक्टिवेट नहीं है", 16f, Color.WHITE, true).apply {
            setPadding(0, dp(16), 0, dp(6))
        })
        root.addView(tv(
            "नीचे दी गई Device ID को license_generator.html (कंप्यूटर पर, Windows में डबल-क्लिक करके खोलें) में डालें, वहाँ से मिला कोड यहाँ नीचे डालकर 'एक्टिवेट करें' दबाएँ।",
            13f, DIM
        ).apply { setPadding(0, dp(4), 0, dp(16)) })

        root.addView(tv("Device ID:", 12f, DIM))
        val idBox = tv(deviceId, 16f, Color.WHITE, true).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(CARD)
        }
        root.addView(idBox)

        root.addView(Button(this).apply {
            text = "Device ID कॉपी करें"
            setOnClickListener {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("device_id", deviceId))
                Toast.makeText(this@MainActivity, "कॉपी हो गया", Toast.LENGTH_SHORT).show()
            }
        }.apply { (layoutParams as? LinearLayout.LayoutParams)?.topMargin = dp(8) })

        val input = EditText(this).apply {
            hint = "एक्टिवेशन कोड (XXXX-XXXX-XXXX-XXXX)"
            setTextColor(Color.WHITE)
            setHintTextColor(DIM)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        root.addView(input.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
        })

        root.addView(Button(this).apply {
            text = "एक्टिवेट करें"
            setOnClickListener {
                if (License.activate(this@MainActivity, input.text.toString())) {
                    Toast.makeText(this@MainActivity, "✅ एक्टिवेशन सफल", Toast.LENGTH_SHORT).show()
                    recreate()
                } else {
                    Toast.makeText(this@MainActivity, "❌ गलत कोड, दोबारा कोशिश करें", Toast.LENGTH_LONG).show()
                }
            }
        }.apply { (layoutParams as? LinearLayout.LayoutParams)?.topMargin = dp(12) })
    }

    // ---------------- मुख्य UI (एक्टिवेशन के बाद) ----------------

    private fun showMainUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(28), dp(16), dp(28))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(BG)
            addView(root)
        }
        setContentView(scroll)

        root.addView(tv("FUTURISTIC SOUNDS", 24f, CYAN, true))
        status = tv("", 14f, Color.WHITE).apply { setPadding(0, dp(8), 0, dp(8)) }
        root.addView(status)
        root.addView(Button(this).apply {
            text = "Accessibility सेटिंग्स खोलें"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(tv(
            "Android 13+ पर अगर सेवा चालू नहीं हो रही: सेटिंग्स > ऐप्स > Futuristic Sounds > ⋮ (तीन बिंदु) > 'Restricted settings की अनुमति दें', फिर दोबारा कोशिश करें।",
            12f, DIM
        ).apply { setPadding(0, dp(6), 0, dp(12)) })

        // मास्टर कंट्रोल
        val master = card()
        master.addView(Switch(this).apply {
            text = "सभी साउंड चालू"
            setTextColor(Color.WHITE)
            isChecked = Prefs.masterOn(this@MainActivity)
            setOnCheckedChangeListener { _, c -> Prefs.setMasterOn(this@MainActivity, c) }
        })
        master.addView(tv("मास्टर वॉल्यूम (सभी साउंड एक साथ)", 13f, DIM))
        master.addView(seek(Prefs.master(this), { Prefs.setMaster(this, it) },
            { SoundEngine.play(this, SoundEvent.TOUCH, force = true) }))
        root.addView(master)

        SoundEvent.values().forEach { root.addView(eventCard(it)) }

        root.addView(tv(
            "टिप: कस्टम साउंड छोटा (कुछ सेकंड का) रखें, WAV/MP3/OGG चलेगा।",
            12f, DIM
        ).apply { setPadding(0, dp(12), 0, 0) })
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(CARD)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) }
    }

    private fun eventCard(e: SoundEvent): View {
        val c = card()
        c.addView(Switch(this).apply {
            text = e.title
            setTextColor(CYAN)
            isChecked = Prefs.eventOn(this@MainActivity, e)
            setOnCheckedChangeListener { _, v -> Prefs.setEventOn(this@MainActivity, e, v) }
        })
        c.addView(tv(e.desc, 12f, DIM))
        val label = tv("", 12f, Color.WHITE).apply { setPadding(0, dp(4), 0, 0) }
        customLabels[e] = label
        c.addView(label)
        updateLabel(e)

        c.addView(tv("वॉल्यूम", 12f, DIM))
        c.addView(seek(Prefs.vol(this, e), { Prefs.setVol(this, e, it) },
            { SoundEngine.play(this, e, force = true) }))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun btn(t: String, click: () -> Unit) = Button(this).apply {
            text = t
            textSize = 12f
            setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(btn("▶ टेस्ट") { SoundEngine.play(this, e, force = true) })
        row.addView(btn("कस्टम साउंड") {
            pending = e
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
            }, 100)
        })
        row.addView(btn("डिफ़ॉल्ट") {
            Prefs.customPath(this, e)?.let { File(it).delete() }
            Prefs.clearCustom(this, e)
            SoundEngine.load(this, e)
            updateLabel(e)
        })
        c.addView(row)
        return c
    }

    private fun updateLabel(e: SoundEvent) {
        val n = Prefs.customName(this, e)
        customLabels[e]?.text = if (n != null) "साउंड: कस्टम ($n)" else "साउंड: डिफ़ॉल्ट फ्यूचरिस्टिक"
    }

    private fun queryName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) return c.getString(i)
        }
        return "audio"
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val e = pending ?: return
        val uri = data?.data
        if (requestCode != 100 || resultCode != RESULT_OK || uri == null) return
        try {
            val name = queryName(uri)
            val ext = name.substringAfterLast('.', "audio")
            Prefs.customPath(this, e)?.let { File(it).delete() }
            val out = File(filesDir, "custom_${e.name}_${System.currentTimeMillis()}.$ext")
            contentResolver.openInputStream(uri)?.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            Prefs.setCustom(this, e, out.absolutePath, name)
            SoundEngine.load(this, e)
            updateLabel(e)
            Handler(Looper.getMainLooper()).postDelayed({
                SoundEngine.play(this, e, force = true)
            }, 500)
        } catch (ex: Exception) {
            Toast.makeText(this, "साउंड जोड़ने में दिक्कत: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!this::status.isInitialized) return // एक्टिवेशन स्क्रीन पर है
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(packageName) == true
        status.text = if (enabled) "✅ सेवा चालू है, साउंड बज रहे हैं"
        else "⚠️ सेवा बंद है। नीचे बटन से Accessibility में 'Futuristic Sounds' चालू करें।"
    }
}
