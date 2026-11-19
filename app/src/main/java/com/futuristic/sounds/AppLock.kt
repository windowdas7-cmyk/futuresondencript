package com.futuristic.sounds

/**
 * ऐप हर बार background से वापस आने पर PIN माँगे — इसके लिए सिर्फ़ in-memory flag।
 * MainActivity.onStop() में false होता है, सही PIN डालने पर true होता है।
 * (यह process-memory में है, disk पर नहीं — ऐप पूरी तरह बंद/restart होने पर हमेशा false से शुरू होगा)
 */
object AppLock {
    var isUnlockedThisSession: Boolean = false
}
