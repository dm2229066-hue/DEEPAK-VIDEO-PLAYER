package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioEffectsState(
    val audioSessionId: Int = 0,
    val isEqualizerSupported: Boolean = false,
    val isBassBoostSupported: Boolean = false,
    val isVirtualizerSupported: Boolean = false,
    val isLoudnessSupported: Boolean = false,
    val presetNames: List<String> = listOf("Normal"),
    val selectedPreset: Int = 0,
    val bandCount: Int = 0,
    val bandMinLevelMb: Int = -1500,
    val bandMaxLevelMb: Int = 1500,
    val bandLevelsMb: List<Int> = emptyList(),
    val bandCenterFreqHz: List<Int> = emptyList(),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0
)

class AudioEffectsManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _state = MutableStateFlow(AudioEffectsState())
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    fun attachToSession(
        audioSessionId: Int,
        initialPreset: Int = 0,
        initialBass: Int = 0,
        initialVirtualizer: Int = 0,
        initialLoudnessMb: Int = 0
    ) {
        if (audioSessionId == 0 || audioSessionId == _state.value.audioSessionId) return
        release()

        var eqSupported = false
        var eqPresets = listOf("Normal")
        var bandCount = 0
        var minLvl = -1500
        var maxLvl = 1500
        val levels = mutableListOf<Int>()
        val freqs = mutableListOf<Int>()

        runCatching {
            val eq = Equalizer(0, audioSessionId).apply { enabled = true }
            equalizer = eq
            eqSupported = true
            val count = eq.numberOfPresets.toInt()
            if (count > 0) {
                eqPresets = (0 until count).map { eq.getPresetName(it.toShort()) }
                val safePreset = initialPreset.coerceIn(0, count - 1)
                eq.usePreset(safePreset.toShort())
            }
            bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            minLvl = range[0].toInt()
            maxLvl = range[1].toInt()
            for (b in 0 until bandCount) {
                levels.add(eq.getBandLevel(b.toShort()).toInt())
                freqs.add(eq.getCenterFreq(b.toShort()) / 1000)
            }
        }.onFailure {
            Log.w("AudioEffectsManager", "Equalizer unavailable: ${it.message}")
        }

        var bassSupported = false
        runCatching {
            val bb = BassBoost(0, audioSessionId)
            if (bb.strengthSupported) {
                bb.enabled = initialBass > 0
                bb.setStrength(initialBass.coerceIn(0, 1000).toShort())
                bassBoost = bb
                bassSupported = true
            } else {
                bb.release()
            }
        }.onFailure {
            Log.w("AudioEffectsManager", "BassBoost unavailable: ${it.message}")
        }

        var virtSupported = false
        runCatching {
            val virt = Virtualizer(0, audioSessionId)
            if (virt.strengthSupported) {
                virt.enabled = initialVirtualizer > 0
                virt.setStrength(initialVirtualizer.coerceIn(0, 1000).toShort())
                virtualizer = virt
                virtSupported = true
            } else {
                virt.release()
            }
        }.onFailure {
            Log.w("AudioEffectsManager", "Virtualizer unavailable: ${it.message}")
        }

        var loudSupported = false
        runCatching {
            val le = LoudnessEnhancer(audioSessionId)
            le.setTargetGain(initialLoudnessMb.coerceIn(0, 1500))
            le.enabled = initialLoudnessMb > 0
            loudnessEnhancer = le
            loudSupported = true
        }.onFailure {
            Log.w("AudioEffectsManager", "LoudnessEnhancer unavailable: ${it.message}")
        }

        _state.value = AudioEffectsState(
            audioSessionId = audioSessionId,
            isEqualizerSupported = eqSupported,
            isBassBoostSupported = bassSupported,
            isVirtualizerSupported = virtSupported,
            isLoudnessSupported = loudSupported,
            presetNames = eqPresets,
            selectedPreset = initialPreset.coerceIn(0, (eqPresets.size - 1).coerceAtLeast(0)),
            bandCount = bandCount,
            bandMinLevelMb = minLvl,
            bandMaxLevelMb = maxLvl,
            bandLevelsMb = levels,
            bandCenterFreqHz = freqs,
            bassBoostStrength = if (bassSupported) initialBass.coerceIn(0, 1000) else 0,
            virtualizerStrength = if (virtSupported) initialVirtualizer.coerceIn(0, 1000) else 0,
            loudnessGainMb = if (loudSupported) initialLoudnessMb.coerceIn(0, 1500) else 0
        )
    }

    fun selectPreset(presetIndex: Int) {
        val eq = equalizer ?: return
        runCatching {
            val safe = presetIndex.coerceIn(0, eq.numberOfPresets.toInt() - 1)
            eq.usePreset(safe.toShort())
            val newLevels = (0 until eq.numberOfBands.toInt()).map {
                eq.getBandLevel(it.toShort()).toInt()
            }
            _state.value = _state.value.copy(
                selectedPreset = safe,
                bandLevelsMb = newLevels
            )
        }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val eq = equalizer ?: return
        runCatching {
            val clamped = levelMb.coerceIn(_state.value.bandMinLevelMb, _state.value.bandMaxLevelMb)
            eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
            val updated = _state.value.bandLevelsMb.toMutableList()
            if (bandIndex in updated.indices) {
                updated[bandIndex] = clamped
            }
            _state.value = _state.value.copy(bandLevelsMb = updated)
        }
    }

    fun setBassBoost(strength: Int) {
        val bb = bassBoost ?: return
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            bb.enabled = clamped > 0
            bb.setStrength(clamped.toShort())
            _state.value = _state.value.copy(bassBoostStrength = clamped)
        }
    }

    fun setVirtualizer(strength: Int) {
        val virt = virtualizer ?: return
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            virt.enabled = clamped > 0
            virt.setStrength(clamped.toShort())
            _state.value = _state.value.copy(virtualizerStrength = clamped)
        }
    }

    fun setLoudnessGain(gainMb: Int) {
        val le = loudnessEnhancer ?: return
        val clamped = gainMb.coerceIn(0, 1500)
        runCatching {
            le.setTargetGain(clamped)
            le.enabled = clamped > 0
            _state.value = _state.value.copy(loudnessGainMb = clamped)
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        _state.value = AudioEffectsState()
    }
}
