package com.futuristic.sounds

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import kotlin.math.abs
import kotlin.random.Random

class FuturisticService : AccessibilityService() {

    companion object {
        // Recents/Overview स्क्रीन पहचानने के लिए अलग-अलग Android स्किन के जाने-पहचाने क्लास-नाम हिस्से
        private val RECENTS_CLASS_HINTS = listOf(
            "Recents", "Overview", "Taskbar", "QuickStep", "RecentTask", "RecentsView"
        )

        // नोटिफिकेशन पैनल / क्विक-सेटिंग्स पैनल — Android version और फ़ोन ब्रांड के हिसाब से
        // क्लास-नाम बदलता रहता है (Android 13+ में "CentralSurfaces"/"Shade*" नाम आया),
        // इसलिए पुराने और नए दोनों तरह के नाम शामिल किए हैं
        private val SHADE_CLASS_HINTS = listOf(
            "NotificationShade", "StatusBar", "NotificationPanel", "QuickSettings",
            "Shade", "CentralSurfaces", "QSPanel", "QSFragment", "ControlCenter",
            "Notification"
        )

        // होम स्क्रीन से ऊपर स्वाइप करने पर खुलने वाला "App Drawer" (सारे ऐप्स वाली लिस्ट)
        // ज़्यादातर लॉन्चर में एक अलग विंडो/एक्टिविटी के तौर पर खुलता है
        private val APP_DRAWER_CLASS_HINTS = listOf(
            "AllApps", "AppsCustomize", "AppDrawer", "Apps"
        )
    }

    private var lastPkg = ""
    private val lastFire = HashMap<SoundEvent, Long>()
    private var homePkgs: Set<String> = emptySet()
    private var imePkgs: Set<String> = emptySet()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                lastPkg = ""
                fire(SoundEvent.LOCK, 500)
            }
        }
    }
    private var receiverRegistered = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (!License.isActivated(this)) {
            // बिना एक्टिवेशन के सर्विस खुद को बंद कर देगी, कोई साउंड नहीं बजेगा
            disableSelf()
            return
        }
        SoundEngine.init(this)
        refreshPackages()
        if (!receiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(screenReceiver, filter)
            }
            receiverRegistered = true
        }
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
        super.onDestroy()
    }

    private fun refreshPackages() {
        try {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            homePkgs = packageManager.queryIntentActivities(i, PackageManager.MATCH_ALL)
                .map { it.activityInfo.packageName }.toSet()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imePkgs = imm.enabledInputMethodList.map { it.packageName }.toSet()
        } catch (_: Exception) {
        }
    }

    private fun fire(e: SoundEvent, minGapMs: Long, rate: Float = 1f) {
        val now = SystemClock.uptimeMillis()
        if (now - (lastFire[e] ?: 0L) < minGapMs) return
        lastFire[e] = now
        SoundEngine.play(this, e, rate)
    }

    override fun onAccessibilityEvent(ev: AccessibilityEvent) {
        val pkg = ev.packageName?.toString() ?: return
        when (ev.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> fire(SoundEvent.TOUCH, 60)

            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ->
                fire(SoundEvent.NOTIFICATION, 300)

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(ev, pkg)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindow(ev, pkg)
        }
    }

    private fun handleScroll(ev: AccessibilityEvent, pkg: String) {
        var dx = 0
        var dy = 0
        if (Build.VERSION.SDK_INT >= 28) {
            dx = ev.scrollDeltaX
            dy = ev.scrollDeltaY
        }
        if (pkg in homePkgs) {
            // होम स्क्रीन / ऐप ड्रॉअर: ऊपर या नीचे स्वाइप
            when {
                dy > 0 -> fire(SoundEvent.SWIPE_UP, 250)
                dy < 0 -> fire(SoundEvent.SWIPE_DOWN, 250)
                else -> fire(SoundEvent.SCROLL, 90)
            }
            return
        }
        // दोनों दिशाओं में एक साथ हलचल (ज़ूम किए हुए फोटो/मैप में) को ज़ूम माना गया है
        if (dx != 0 && dy != 0 && abs(dx) > 8 && abs(dy) > 8) {
            fire(SoundEvent.ZOOM, 300)
        } else {
            fire(SoundEvent.SCROLL, 90, 0.9f + Random.nextFloat() * 0.3f)
        }
    }

    private fun handleWindow(ev: AccessibilityEvent, pkg: String) {
        val cls = ev.className?.toString() ?: ""
        if (pkg == "android" || pkg in imePkgs || pkg == packageName && cls.contains("Dialog")) return

        // अलग-अलग फ़ोन ब्रांड (स्टॉक/Pixel, MIUI, OxygenOS, One UI वगैरह) में
        // Recents/Overview स्क्रीन की क्लास अलग-अलग नाम से आती है, इसलिए कई पैटर्न चेक करते हैं
        if (RECENTS_CLASS_HINTS.any { cls.contains(it, true) }) {
            fire(SoundEvent.RECENTS, 400)
            return
        }
        if (pkg == "com.android.systemui") {
            if (SHADE_CLASS_HINTS.any { cls.contains(it, true) }) fire(SoundEvent.SHADE, 400)
            return
        }
        if (pkg in homePkgs) {
            // होम स्क्रीन पर ऊपर स्वाइप करने से खुलने वाला App Drawer — कई लॉन्चर में यह एक
            // अलग विंडो की तरह खुलता है (स्क्रॉल इवेंट नहीं, विंडो-चेंज इवेंट के रूप में आता है)
            if (APP_DRAWER_CLASS_HINTS.any { cls.contains(it, true) }) {
                fire(SoundEvent.SWIPE_UP, 250)
                lastPkg = pkg
                return
            }
            if (lastPkg.isNotEmpty() && lastPkg != pkg) fire(SoundEvent.HOME, 400)
            lastPkg = pkg
            return
        }
        if (pkg != lastPkg) {
            lastPkg = pkg
            fire(SoundEvent.APP_OPEN, 300)
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // नोट: कुछ फ़ोन (खासकर gesture-navigation वाले कुछ OEM स्किन) back/recents gesture के लिए
        // key event भेजते ही नहीं — यह Android की सीमा है, बिना root ठीक नहीं हो सकती।
        // जहाँ key event आता है, वहाँ ACTION_DOWN और ACTION_UP दोनों चेक करने से पकड़ने की संभावना बढ़ती है।
        if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK -> fire(SoundEvent.BACK, 150)
                KeyEvent.KEYCODE_APP_SWITCH -> fire(SoundEvent.RECENTS, 400)
            }
        }
        return false // बटन को सामान्य रूप से काम करने दें
    }

    override fun onInterrupt() {}
}
