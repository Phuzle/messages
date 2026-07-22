package com.phuzle.labs.messages.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Single home for every settings toggle from the prototype's `state` object. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_HEX = stringPreferencesKey("accent_hex")
        val SWIPE_LEFT = stringPreferencesKey("swipe_left")
        val SWIPE_RIGHT = stringPreferencesKey("swipe_right")
        val CH_PERSONAL = booleanPreferencesKey("ch_personal_enabled")
        val CH_OTP = booleanPreferencesKey("ch_otp_enabled")
        val CH_TRANSACT = booleanPreferencesKey("ch_transact_enabled")
        val CH_PROMO = booleanPreferencesKey("ch_promo_enabled")
        val NOTIFICATIONS_ALLOWED = booleanPreferencesKey("notifications_allowed")
        val WAKE_SCREEN = booleanPreferencesKey("wake_screen")
        val QUICK_ACTIONS = booleanPreferencesKey("quick_actions")
        val LOCK_SCREEN_VISIBILITY = stringPreferencesKey("lock_screen_visibility")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_METHOD = stringPreferencesKey("app_lock_method")
        val SIGNATURE = stringPreferencesKey("signature")
        val SHOW_CHAR_COUNT = booleanPreferencesKey("show_char_count")
        val IN_APP_BROWSER = booleanPreferencesKey("in_app_browser")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val CLOUD_BACKUP_CONNECTED = booleanPreferencesKey("cloud_backup_connected")
        val OTP_EVICTION_ENABLED = booleanPreferencesKey("otp_eviction_enabled")
        val LAST_LOCAL_BACKUP_AT = longPreferencesKey("last_local_backup_at")
        val LAST_LOCAL_RESTORE_AT = longPreferencesKey("last_local_restore_at")
        val HISTORY_IMPORTED = booleanPreferencesKey("history_imported")
        val CLOUD_FALLBACK_ENABLED = booleanPreferencesKey("cloud_fallback_enabled")
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE] ?: "system",
            accentHex = prefs[Keys.ACCENT_HEX] ?: "#3E6DF2",
            swipeLeftAction = prefs[Keys.SWIPE_LEFT] ?: "archive",
            swipeRightAction = prefs[Keys.SWIPE_RIGHT] ?: "toggleRead",
            channelPersonalEnabled = prefs[Keys.CH_PERSONAL] ?: true,
            channelOtpEnabled = prefs[Keys.CH_OTP] ?: true,
            channelTransactEnabled = prefs[Keys.CH_TRANSACT] ?: true,
            channelPromoEnabled = prefs[Keys.CH_PROMO] ?: true,
            notificationsAllowed = prefs[Keys.NOTIFICATIONS_ALLOWED] ?: true,
            wakeScreenForHighPriority = prefs[Keys.WAKE_SCREEN] ?: true,
            quickActionButtons = prefs[Keys.QUICK_ACTIONS] ?: true,
            lockScreenVisibility = prefs[Keys.LOCK_SCREEN_VISIBILITY] ?: "full",
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appLockMethod = prefs[Keys.APP_LOCK_METHOD] ?: "fingerprint",
            signature = prefs[Keys.SIGNATURE] ?: "",
            showCharCount = prefs[Keys.SHOW_CHAR_COUNT] ?: false,
            inAppBrowser = prefs[Keys.IN_APP_BROWSER] ?: true,
            backupFrequency = prefs[Keys.BACKUP_FREQUENCY] ?: "daily",
            cloudBackupConnected = prefs[Keys.CLOUD_BACKUP_CONNECTED] ?: false,
            otpEvictionEnabled = prefs[Keys.OTP_EVICTION_ENABLED] ?: false,
            lastLocalBackupAt = prefs[Keys.LAST_LOCAL_BACKUP_AT],
            lastLocalRestoreAt = prefs[Keys.LAST_LOCAL_RESTORE_AT],
            historyImported = prefs[Keys.HISTORY_IMPORTED] ?: false,
            cloudFallbackEnabled = prefs[Keys.CLOUD_FALLBACK_ENABLED] ?: false,
            serverBaseUrl = prefs[Keys.SERVER_BASE_URL] ?: "http://10.0.2.2:8080/",
        )
    }

    suspend fun setThemeMode(mode: String) = edit { it[Keys.THEME_MODE] = mode }
    suspend fun setAccentHex(hex: String) = edit { it[Keys.ACCENT_HEX] = hex }
    suspend fun setSwipeLeftAction(action: String) = edit { it[Keys.SWIPE_LEFT] = action }
    suspend fun setSwipeRightAction(action: String) = edit { it[Keys.SWIPE_RIGHT] = action }
    suspend fun setChannelEnabled(channelId: String, enabled: Boolean) = edit {
        val key = when (channelId) {
            "ch_personal" -> Keys.CH_PERSONAL
            "ch_otp" -> Keys.CH_OTP
            "ch_transact" -> Keys.CH_TRANSACT
            else -> Keys.CH_PROMO
        }
        it[key] = enabled
    }
    suspend fun setNotificationsAllowed(allowed: Boolean) = edit { it[Keys.NOTIFICATIONS_ALLOWED] = allowed }
    suspend fun setWakeScreen(enabled: Boolean) = edit { it[Keys.WAKE_SCREEN] = enabled }
    suspend fun setQuickActions(enabled: Boolean) = edit { it[Keys.QUICK_ACTIONS] = enabled }
    suspend fun setLockScreenVisibility(mode: String) = edit { it[Keys.LOCK_SCREEN_VISIBILITY] = mode }
    suspend fun setAppLockEnabled(enabled: Boolean) = edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    suspend fun setAppLockMethod(method: String) = edit { it[Keys.APP_LOCK_METHOD] = method }
    suspend fun setSignature(signature: String) = edit { it[Keys.SIGNATURE] = signature }
    suspend fun setShowCharCount(enabled: Boolean) = edit { it[Keys.SHOW_CHAR_COUNT] = enabled }
    suspend fun setInAppBrowser(enabled: Boolean) = edit { it[Keys.IN_APP_BROWSER] = enabled }
    suspend fun setBackupFrequency(frequency: String) = edit { it[Keys.BACKUP_FREQUENCY] = frequency }
    suspend fun setCloudBackupConnected(connected: Boolean) = edit { it[Keys.CLOUD_BACKUP_CONNECTED] = connected }
    suspend fun setOtpEvictionEnabled(enabled: Boolean) = edit { it[Keys.OTP_EVICTION_ENABLED] = enabled }
    suspend fun setLastLocalBackupAt(timestamp: Long) = edit { it[Keys.LAST_LOCAL_BACKUP_AT] = timestamp }
    suspend fun setLastLocalRestoreAt(timestamp: Long) = edit { it[Keys.LAST_LOCAL_RESTORE_AT] = timestamp }
    suspend fun setHistoryImported(imported: Boolean) = edit { it[Keys.HISTORY_IMPORTED] = imported }
    suspend fun setCloudFallbackEnabled(enabled: Boolean) = edit { it[Keys.CLOUD_FALLBACK_ENABLED] = enabled }
    suspend fun setServerBaseUrl(url: String) = edit { it[Keys.SERVER_BASE_URL] = url }

    private suspend fun edit(transform: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(transform)
    }
}
