package com.futuristic.sounds

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File

object SoundEngine {
    private var pool: SoundPool? = null
    private val ids = HashMap<SoundEvent, Int>()

    @Synchronized
    fun init(ctx: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build()
        val app = ctx.applicationContext
        SoundEvent.values().forEach { load(app, it) }
    }

    @Synchronized
    fun load(ctx: Context, e: SoundEvent) {
        val p = pool ?: return
        ids.remove(e)?.let { p.unload(it) }
        val customPath = Prefs.customPath(ctx, e)
        val file = if (customPath != null && File(customPath).exists()) File(customPath)
        else Synth.defaultFile(ctx, e)
        ids[e] = p.load(file.absolutePath, 1)
    }

    fun play(ctx: Context, e: SoundEvent, rate: Float = 1f, force: Boolean = false) {
        if (!force && (!Prefs.masterOn(ctx) || !Prefs.eventOn(ctx, e))) return
        init(ctx)
        val id = ids[e] ?: return
        val v = (Prefs.master(ctx) / 100f) * (Prefs.vol(ctx, e) / 100f)
        pool?.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }
}
