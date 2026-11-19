package com.futuristic.sounds

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("futuristic", Context.MODE_PRIVATE)

    fun masterOn(c: Context) = sp(c).getBoolean("master_on", true)
    fun setMasterOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("master_on", v).apply()

    fun master(c: Context) = sp(c).getInt("master_vol", 80)
    fun setMaster(c: Context, v: Int) = sp(c).edit().putInt("master_vol", v).apply()

    fun eventOn(c: Context, e: SoundEvent) = sp(c).getBoolean("on_${e.name}", true)
    fun setEventOn(c: Context, e: SoundEvent, v: Boolean) =
        sp(c).edit().putBoolean("on_${e.name}", v).apply()

    fun vol(c: Context, e: SoundEvent) = sp(c).getInt("vol_${e.name}", 100)
    fun setVol(c: Context, e: SoundEvent, v: Int) = sp(c).edit().putInt("vol_${e.name}", v).apply()

    fun customPath(c: Context, e: SoundEvent): String? = sp(c).getString("cpath_${e.name}", null)
    fun customName(c: Context, e: SoundEvent): String? = sp(c).getString("cname_${e.name}", null)
    fun setCustom(c: Context, e: SoundEvent, path: String, name: String) =
        sp(c).edit().putString("cpath_${e.name}", path).putString("cname_${e.name}", name).apply()

    fun clearCustom(c: Context, e: SoundEvent) =
        sp(c).edit().remove("cpath_${e.name}").remove("cname_${e.name}").apply()

    // ---- Device-ID एक्टिवेशन ----
    fun activationCode(c: Context): String? = sp(c).getString("activation_code", null)
    fun setActivationCode(c: Context, code: String) =
        sp(c).edit().putString("activation_code", code).apply()
}
