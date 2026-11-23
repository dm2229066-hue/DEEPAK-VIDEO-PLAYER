package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SortOption
import com.example.data.model.ViewMode
import com.example.ui.theme.AccentColorOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "deepak_player_prefs")

enum class OrientationPreference(val id: String, val label: String) {
    AUTO("AUTO", "Auto (Sensor)"),
    LANDSCAPE("LANDSCAPE", "Landscape"),
    PORTRAIT("PORTRAIT", "Portrait");

    companion object {
        fun fromId(id: String): OrientationPreference =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी (Hindi)"),
    SPANISH("es", "Español (Spanish)");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

data class UserPreferences(
    val isDarkTheme: Boolean = true,
    val accentOption: AccentColorOption = AccentColorOption.GOLD,
    val unlockedAccents: Set<String> = setOf("gold", "amber", "lime"),
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultOrientation: OrientationPreference = OrientationPreference.AUTO,
    val gesturesEnabled: Boolean = true,
    val autoPlayNext: Boolean = true,
    val backgroundPlayback: Boolean = false,
    val subtitleFontSizeSp: Float = 18f,
    val subtitleColorArgb: Long = 0xFFFFFFFFL,
    val subtitlePositionFraction: Float = 0.08f,
    val audioMonoMode: Boolean = false,
    val equalizerPreset: Int = 0,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val showHiddenFolders: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOption: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val UNLOCKED_ACCENTS = stringSetPreferencesKey("unlocked_accents")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val DEFAULT_ORIENTATION = stringPreferencesKey("default_orientation")
        val GESTURES_ENABLED = booleanPreferencesKey("gestures_enabled")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val SUBTITLE_FONT_SIZE = floatPreferencesKey("subtitle_font_size")
        val SUBTITLE_COLOR = longPreferencesKey("subtitle_color")
        val SUBTITLE_POS = floatPreferencesKey("subtitle_pos")
        val AUDIO_MONO = booleanPreferencesKey("audio_mono")
        val EQ_PRESET = intPreferencesKey("eq_preset")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val LOUDNESS_GAIN = intPreferencesKey("loudness_gain")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val SORT_ASC = booleanPreferencesKey("sort_asc")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
            accentOption = AccentColorOption.fromId(prefs[Keys.ACCENT_COLOR] ?: "gold"),
            unlockedAccents = prefs[Keys.UNLOCKED_ACCENTS] ?: setOf("gold", "amber", "lime"),
            defaultPlaybackSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            defaultOrientation = OrientationPreference.fromId(prefs[Keys.DEFAULT_ORIENTATION] ?: "AUTO"),
            gesturesEnabled = prefs[Keys.GESTURES_ENABLED] ?: true,
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            backgroundPlayback = prefs[Keys.BACKGROUND_PLAYBACK] ?: false,
            subtitleFontSizeSp = prefs[Keys.SUBTITLE_FONT_SIZE] ?: 18f,
            subtitleColorArgb = prefs[Keys.SUBTITLE_COLOR] ?: 0xFFFFFFFFL,
            subtitlePositionFraction = prefs[Keys.SUBTITLE_POS] ?: 0.08f,
            audioMonoMode = prefs[Keys.AUDIO_MONO] ?: false,
            equalizerPreset = prefs[Keys.EQ_PRESET] ?: 0,
            bassBoostStrength = prefs[Keys.BASS_BOOST] ?: 0,
            virtualizerStrength = prefs[Keys.VIRTUALIZER] ?: 0,
            loudnessGainMb = prefs[Keys.LOUDNESS_GAIN] ?: 0,
            showHiddenFolders = prefs[Keys.SHOW_HIDDEN] ?: false,
            appLanguage = AppLanguage.fromCode(prefs[Keys.APP_LANGUAGE] ?: "en"),
            viewMode = ViewMode.fromId(prefs[Keys.VIEW_MODE] ?: "LIST"),
            sortOption = SortOption.fromId(prefs[Keys.SORT_OPTION] ?: "date"),
            sortAscending = prefs[Keys.SORT_ASC] ?: false
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_THEME] = enabled }
    }

    suspend fun setAccentOption(option: AccentColorOption) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = option.id }
    }

    suspend fun unlockAccent(accentId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UNLOCKED_ACCENTS] ?: setOf("gold", "amber", "lime")
            prefs[Keys.UNLOCKED_ACCENTS] = current + accentId
            prefs[Keys.ACCENT_COLOR] = accentId
        }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_SPEED] = speed.coerceIn(0.25f, 4.0f) }
    }

    suspend fun setDefaultOrientation(orientation: OrientationPreference) {
        context.dataStore.edit { it[Keys.DEFAULT_ORIENTATION] = orientation.id }
    }

    suspend fun setGesturesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GESTURES_ENABLED] = enabled }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setBackgroundPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    }

    suspend fun setSubtitlePreferences(fontSizeSp: Float, colorArgb: Long, positionFraction: Float) {
        context.dataStore.edit {
            it[Keys.SUBTITLE_FONT_SIZE] = fontSizeSp.coerceIn(12f, 34f)
            it[Keys.SUBTITLE_COLOR] = colorArgb
            it[Keys.SUBTITLE_POS] = positionFraction.coerceIn(0.02f, 0.35f)
        }
    }

    suspend fun setAudioPreferences(
        monoMode: Boolean,
        eqPreset: Int,
        bassBoost: Int,
        virtualizer: Int,
        loudnessGainMb: Int
    ) {
        context.dataStore.edit {
            it[Keys.AUDIO_MONO] = monoMode
            it[Keys.EQ_PRESET] = eqPreset
            it[Keys.BASS_BOOST] = bassBoost.coerceIn(0, 1000)
            it[Keys.VIRTUALIZER] = virtualizer.coerceIn(0, 1000)
            it[Keys.LOUDNESS_GAIN] = loudnessGainMb.coerceIn(0, 1500)
        }
    }

    suspend fun setShowHiddenFolders(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_HIDDEN] = show }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = language.code }
    }

    suspend fun setViewMode(viewMode: ViewMode) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = viewMode.id }
    }

    suspend fun setSort(option: SortOption, ascending: Boolean) {
        context.dataStore.edit {
            it[Keys.SORT_OPTION] = option.id
            it[Keys.SORT_ASC] = ascending
        }
    }
}
