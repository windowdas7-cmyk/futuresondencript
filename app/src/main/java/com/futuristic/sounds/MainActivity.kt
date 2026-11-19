package com.futuristic.sounds

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import android.net.Uri
import android.text.InputType
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
    private val ON_CYAN = Color.parseColor("#00212A") // cyan बटन पर टेक्स्ट का रंग

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

    // ---------------- मॉडर्न UI हेल्पर्स ----------------

    private fun roundedBg(radiusDp: Int, fill: Int, strokeColor: Int? = null, strokeWidthDp: Int = 0) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(fill)
            if (strokeColor != null) setStroke(dp(strokeWidthDp), strokeColor)
        }

    /** भरा हुआ (filled) मुख्य बटन */
    private fun pillButton(text: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(ON_CYAN)
            textSize = 15f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBg(14, CYAN)
            setPadding(dp(18), dp(15), dp(18), dp(15))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }

    /** बॉर्डर वाला हल्का (ghost) बटन */
    private fun ghostButton(text: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(CYAN)
            textSize = 13f
            gravity = Gravity.CENTER
            background = roundedBg(14, Color.TRANSPARENT, CYAN, 1)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }

    private fun modernInput(hintText: String, numericSecret: Boolean = false): EditText =
        EditText(this).apply {
            hint = hintText
            setHintTextColor(DIM)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 17f
            if (numericSecret) {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
            background = roundedBg(14, CARD, DIM, 1)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
        }

    private fun screenRoot(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(72), dp(28), dp(28))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(BG)
            addView(root)
        })
        return root
    }

    // ---------------- लाइफ़साइकल ----------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SoundEngine.init(this)
        renderInitial()
    }

    private fun renderInitial() {
        when {
            !License.isActivated(this) -> showActivationScreen()
            !AppLock.isUnlockedThisSession -> showLockScreen()
            else -> showMainUi()
        }
    }

    override fun onResume() {
        super.onResume()
        if (License.isActivated(this) && !AppLock.isUnlockedThisSession) {
            showLockScreen()
            return
        }
        refreshStatusText()
    }

    override fun onStop() {
        super.onStop()
        // ऐप background में जाते ही लॉक हो जाए, अगली बार खुलने पर PIN माँगे
        AppLock.isUnlockedThisSession = false
    }

    private fun refreshStatusText() {
        if (!this::status.isInitialized) return
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(packageName) == true
        status.text = if (enabled) "✅ सेवा चालू है, साउंड बज रहे हैं"
        else "⚠️ सेवा बंद है। नीचे बटन से Accessibility में 'Futuristic Sounds' चालू करें।"
    }

    // ---------------- एक्टिवेशन स्क्रीन (मॉडर्न, मिनिमल) ----------------

    private fun showActivationScreen() {
        val root = screenRoot()
        val deviceId = License.deviceId(this)

        root.addView(tv("FUTURISTIC SOUNDS", 20f, CYAN, true).apply { gravity = Gravity.CENTER })

        root.addView(TextView(this).apply {
            text = deviceId
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            background = roundedBg(14, CARD)
            setPadding(dp(16), dp(18), dp(16), dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(28) }
        })

        root.addView(ghostButton("Device ID कॉपी करें") {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("device_id", deviceId))
            Toast.makeText(this, "कॉपी हो गया", Toast.LENGTH_SHORT).show()
        })

        val codeInput = modernInput("XXXX-XXXX-XXXX-XXXX")
        root.addView(codeInput)

        root.addView(pillButton("एक्टिवेट करें") {
            if (License.activate(this, codeInput.text.toString())) {
                Toast.makeText(this, "✅ एक्टिवेट हो गया", Toast.LENGTH_SHORT).show()
                recreate()
            } else {
                Toast.makeText(this, "❌ गलत कोड", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ---------------- ऐप लॉक स्क्रीन ----------------

    private fun showLockScreen() {
        val root = screenRoot()

        root.addView(tv("🔒", 36f, CYAN).apply { gravity = Gravity.CENTER })
        root.addView(tv("FUTURISTIC SOUNDS", 16f, CYAN, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })

        val pinInput = modernInput("PIN डालें", numericSecret = true)
        root.addView(pinInput)

        root.addView(pillButton("अनलॉक करें") {
            if (pinInput.text.toString() == Prefs.appLockPin(this)) {
                AppLock.isUnlockedThisSession = true
                showMainUi()
            } else {
                Toast.makeText(this, "❌ गलत PIN", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(ghostButton("पासवर्ड भूल गए?") { showResetPinScreen() })
    }

    private fun showResetPinScreen() {
        val root = screenRoot()

        root.addView(tv("PIN रीसेट करें", 17f, CYAN, true).apply { gravity = Gravity.CENTER })
        root.addView(tv("अपना एक्टिवेशन कोड डालें", 12f, DIM).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })

        val codeInput = modernInput("XXXX-XXXX-XXXX-XXXX")
        root.addView(codeInput)

        root.addView(pillButton("रीसेट करें") {
            val typed = codeInput.text.toString().trim().uppercase()
            if (typed == License.expectedCode(License.deviceId(this))) {
                Prefs.setAppLockPin(this, Prefs.DEFAULT_APP_PIN)
                Toast.makeText(
                    this, "PIN डिफ़ॉल्ट (${Prefs.DEFAULT_APP_PIN}) पर रीसेट हो गया", Toast.LENGTH_LONG
                ).show()
                showLockScreen()
            } else {
                Toast.makeText(this, "❌ गलत कोड", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(ghostButton("वापस") { showLockScreen() })
    }

    private fun showChangePasswordScreen() {
        val root = screenRoot()

        root.addView(tv("पासवर्ड बदलें", 17f, CYAN, true).apply { gravity = Gravity.CENTER })

        val currentInput = modernInput("मौजूदा PIN", numericSecret = true)
        val newInput = modernInput("नया PIN", numericSecret = true)
        val confirmInput = modernInput("नया PIN दोबारा", numericSecret = true)
        root.addView(currentInput)
        root.addView(newInput)
        root.addView(confirmInput)

        root.addView(pillButton("सेव करें") {
            val cur = currentInput.text.toString()
            val n1 = newInput.text.toString()
            val n2 = confirmInput.text.toString()
            when {
                cur != Prefs.appLockPin(this) ->
                    Toast.makeText(this, "❌ मौजूदा PIN गलत है", Toast.LENGTH_SHORT).show()
                n1.isBlank() ->
                    Toast.makeText(this, "नया PIN डालें", Toast.LENGTH_SHORT).show()
                n1 != n2 ->
                    Toast.makeText(this, "❌ नया PIN मैच नहीं हुआ", Toast.LENGTH_SHORT).show()
                else -> {
                    Prefs.setAppLockPin(this, n1)
                    Toast.makeText(this, "✅ PIN बदल गया", Toast.LENGTH_SHORT).show()
                    showMainUi()
                }
            }
        })

        root.addView(ghostButton("वापस") { showMainUi() })
    }

    // ---------------- मुख्य UI (एक्टिवेशन + लॉक के बाद) ----------------

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

        root.addView(ghostButton("🔒 पासवर्ड बदलें") { showChangePasswordScreen() })

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

        refreshStatusText()
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
}
