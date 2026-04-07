package com.cards.game.literature.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.cards.game.literature.preferences.GamePrefs

actual object SoundPlayer {
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SoundEvent, Int>()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()
        loadSounds()
    }

    private fun loadSounds() {
        val ctx = appContext ?: return
        val pool = soundPool ?: return
        SoundEvent.entries.forEach { event ->
            val resId = ctx.resources.getIdentifier(event.resName, "raw", ctx.packageName)
            if (resId != 0) {
                soundIds[event] = pool.load(ctx, resId, 1)
            }
        }
    }

    actual fun play(event: SoundEvent) {
        if (!GamePrefs.isSoundEnabled()) return
        val ctx = appContext ?: return
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val pool = soundPool ?: return
        val id = soundIds[event] ?: return
        if (id != 0) pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    actual fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
    }
}
