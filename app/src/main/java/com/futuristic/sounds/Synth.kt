package com.futuristic.sounds

import android.content.Context
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** फ्यूचरिस्टिक साउंड कोड से ही बनते हैं, कोई ऑडियो फाइल जोड़ने की ज़रूरत नहीं। */
object Synth {
    private const val SR = 22050
    private const val VERSION = "v1"

    private class Buf(ms: Int) {
        val d = FloatArray(SR * ms / 1000)

        fun tone(
            startMs: Int, durMs: Int, f0: Double, f1: Double, amp: Double = 0.6,
            vibHz: Double = 0.0, vibDepth: Double = 0.0,
            fmRatio: Double = 0.0, fmIdx: Double = 0.0, harm: Double = 0.0
        ) {
            val s0 = SR * startMs / 1000
            val n = SR * durMs / 1000
            var phase = 0.0
            var mph = 0.0
            for (i in 0 until n) {
                val idx = s0 + i
                if (idx >= d.size) break
                val t = i.toDouble() / n
                var f = f0 + (f1 - f0) * t
                if (vibHz > 0) f += vibDepth * sin(2 * PI * vibHz * i / SR)
                phase += 2 * PI * f / SR
                var mod = 0.0
                if (fmIdx > 0) {
                    mph += 2 * PI * f * fmRatio / SR
                    mod = fmIdx * sin(mph)
                }
                var x = sin(phase + mod)
                if (harm > 0) x += harm * sin(3 * phase)
                val att = min(1.0, i / (SR * 0.004))
                val dec = (1.0 - t).pow(1.5)
                d[idx] += (x * amp * att * dec).toFloat()
            }
        }

        fun echo(delayMs: Int, decay: Float, times: Int) {
            val delay = SR * delayMs / 1000
            val src = d.copyOf()
            var g = decay
            for (k in 1..times) {
                val off = delay * k
                for (i in 0 until d.size - off) d[i + off] += src[i] * g
                g *= decay
            }
        }

        fun shorts(): ShortArray {
            var peak = 0f
            for (v in d) peak = max(peak, abs(v))
            val scale = if (peak > 0f) 0.9f / peak else 1f
            return ShortArray(d.size) { (d[it] * scale * 32767f).toInt().coerceIn(-32767, 32767).toShort() }
        }
    }

    private fun build(e: SoundEvent): ShortArray {
        val b: Buf
        when (e) {
            SoundEvent.TOUCH -> {
                b = Buf(120)
                b.tone(0, 55, 1900.0, 2700.0, 0.7, harm = 0.2)
                b.tone(30, 60, 3800.0, 4200.0, 0.2)
                b.echo(45, 0.3f, 1)
            }
            SoundEvent.SHADE -> {
                b = Buf(700)
                b.tone(0, 380, 1500.0, 450.0, 0.5, fmRatio = 2.0, fmIdx = 2.0)
                b.tone(0, 450, 220.0, 180.0, 0.4, vibHz = 18.0, vibDepth = 6.0)
                b.echo(90, 0.35f, 2)
            }
            SoundEvent.NOTIFICATION -> {
                b = Buf(720)
                b.tone(0, 140, 880.0, 880.0, 0.5, fmRatio = 2.0, fmIdx = 1.5)
                b.tone(150, 300, 1320.0, 1320.0, 0.5, fmRatio = 2.0, fmIdx = 1.5)
                b.echo(130, 0.3f, 2)
            }
            SoundEvent.APP_OPEN -> {
                b = Buf(650)
                b.tone(0, 90, 523.0, 523.0)
                b.tone(70, 90, 784.0, 784.0)
                b.tone(140, 90, 1047.0, 1047.0)
                b.tone(210, 200, 1568.0, 1568.0, 0.5, fmRatio = 1.5, fmIdx = 1.0)
                b.echo(110, 0.3f, 2)
            }
            SoundEvent.SWIPE_UP -> {
                b = Buf(300)
                b.tone(0, 230, 300.0, 1900.0, 0.55, fmRatio = 0.5, fmIdx = 1.5, harm = 0.15)
                b.echo(70, 0.3f, 1)
            }
            SoundEvent.SWIPE_DOWN -> {
                b = Buf(300)
                b.tone(0, 230, 1900.0, 300.0, 0.55, fmRatio = 0.5, fmIdx = 1.5, harm = 0.15)
                b.echo(70, 0.3f, 1)
            }
            SoundEvent.ZOOM -> {
                b = Buf(400)
                b.tone(0, 300, 700.0, 1400.0, 0.5, vibHz = 22.0, vibDepth = 120.0, fmRatio = 2.0, fmIdx = 1.5)
                b.echo(80, 0.3f, 1)
            }
            SoundEvent.SCROLL -> {
                b = Buf(50)
                b.tone(0, 22, 3400.0, 2800.0, 0.35)
            }
            SoundEvent.BACK -> {
                b = Buf(300)
                b.tone(0, 80, 1300.0, 1300.0, 0.55)
                b.tone(80, 130, 860.0, 700.0, 0.55, harm = 0.1)
                b.echo(70, 0.3f, 1)
            }
            SoundEvent.HOME -> {
                b = Buf(650)
                b.tone(0, 360, 1100.0, 260.0, 0.55, fmRatio = 1.0, fmIdx = 1.2)
                b.tone(0, 420, 130.0, 110.0, 0.45)
                b.echo(100, 0.3f, 2)
            }
            SoundEvent.LOCK -> {
                b = Buf(600)
                b.tone(0, 320, 1300.0, 140.0, 0.55, fmRatio = 1.5, fmIdx = 1.8)
                b.tone(0, 350, 90.0, 55.0, 0.45)
                b.tone(220, 80, 2200.0, 600.0, 0.3)
                b.echo(90, 0.3f, 2)
            }
            SoundEvent.RECENTS -> {
                b = Buf(460)
                b.tone(0, 50, 1600.0, 1600.0, 0.5)
                b.tone(70, 50, 2000.0, 2000.0, 0.5)
                b.tone(140, 50, 2400.0, 2400.0, 0.5)
                b.tone(210, 120, 1200.0, 2800.0, 0.4, fmRatio = 3.0, fmIdx = 1.0)
                b.echo(60, 0.25f, 2)
            }
        }
        return b.shorts()
    }

    private fun writeWav(f: File, s: ShortArray) {
        val dataLen = s.size * 2
        val bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray())
        bb.putInt(36 + dataLen)
        bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray())
        bb.putInt(16)
        bb.putShort(1)
        bb.putShort(1)
        bb.putInt(SR)
        bb.putInt(SR * 2)
        bb.putShort(2)
        bb.putShort(16)
        bb.put("data".toByteArray())
        bb.putInt(dataLen)
        for (v in s) bb.putShort(v)
        f.writeBytes(bb.array())
    }

    fun defaultFile(ctx: Context, e: SoundEvent): File {
        val f = File(ctx.filesDir, "default_${e.name}_$VERSION.wav")
        if (!f.exists()) writeWav(f, build(e))
        return f
    }
}
