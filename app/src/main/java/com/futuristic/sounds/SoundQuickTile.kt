package com.futuristic.sounds

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * कंट्रोल पैनल (Quick Settings) में दिखने वाली टाइल — एक टैप से
 * सारे फ्यूचरिस्टिक साउंड ऑन/ऑफ कर देती है (Prefs.masterOn को ही बदलती है,
 * इसलिए ऐप के अंदर वाला "सभी साउंड चालू" स्विच भी इसी के साथ sync रहता है)।
 */
class SoundQuickTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val newState = !Prefs.masterOn(this)
        Prefs.setMasterOn(this, newState)
        refresh()
    }

    private fun refresh() {
        val on = Prefs.masterOn(this)
        qsTile?.let { t ->
            t.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            t.label = "Futuristic Sounds"
            t.updateTile()
        }
    }
}
