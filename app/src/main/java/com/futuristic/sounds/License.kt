package com.futuristic.sounds

import android.content.Context
import android.provider.Settings
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * सीधा Device-ID पर आधारित एक्टिवेशन चेक।
 * ज़रूरी: नीचे वाली SECRET स्ट्रिंग बिल्कुल वही होनी चाहिए जो license_generator.html में है,
 * वरना जनरेट किया कोड यहाँ मैच नहीं करेगा।
 *
 * ⚠️ यह सिर्फ़ आम यूज़र को बिना परमिशन ऐप इस्तेमाल करने से रोकता है (casual anti-piracy)।
 * असली सिक्योरिटी नहीं है — कोई भी APK को decompile करके SECRET और चेक निकाल सकता है।
 * अगर चाहें तो SECRET बदल दें (दोनों जगह, ऐप + HTML जनरेटर में एक साथ)।
 */
object License {

    // इसे बदलना चाहें तो बदल लें — बस HTML जनरेटर में भी वही बदलाव करें
    private const val SECRET = "FuturisticSounds-2026-SecretKey-ChangeThis"

    fun deviceId(ctx: Context): String =
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN-DEVICE"

    /** दिए गए deviceId के लिए सही एक्टिवेशन कोड निकालता है (XXXX-XXXX-XXXX-XXXX) */
    fun expectedCode(deviceId: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(deviceId.trim().toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02X".format(it) }.substring(0, 16)
        return hex.chunked(4).joinToString("-")
    }

    fun isActivated(ctx: Context): Boolean {
        val saved = Prefs.activationCode(ctx) ?: return false
        return normalize(saved) == expectedCode(deviceId(ctx))
    }

    /** यूज़र द्वारा डाले कोड को चेक करता है, सही होने पर सेव करके true लौटाता है */
    fun activate(ctx: Context, enteredCode: String): Boolean {
        val norm = normalize(enteredCode)
        if (norm.isEmpty()) return false
        val expected = expectedCode(deviceId(ctx))
        return if (norm == expected) {
            Prefs.setActivationCode(ctx, norm)
            true
        } else {
            false
        }
    }

    private fun normalize(code: String): String =
        code.trim().uppercase().replace(Regex("[^0-9A-F-]"), "")
}
