package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class SupabaseConfig(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("doorbly_supabase_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_URL = "custom_supabase_url"
        private const val KEY_CUSTOM_KEY = "custom_supabase_key"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_NAME = "user_name"
    }

    var projectUrl: String
        get() {
            val custom = prefs.getString(KEY_CUSTOM_URL, null)?.trim()
            if (!custom.isNullOrEmpty()) return cleanUrl(custom)
            return try {
                val configUrl = BuildConfig.SUPABASE_URL.trim()
                if (configUrl.isNotEmpty()) cleanUrl(configUrl) else ""
            } catch (e: Throwable) {
                ""
            }
        }
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_URL, cleanUrl(value)).apply()
        }

    var anonKey: String
        get() {
            val custom = prefs.getString(KEY_CUSTOM_KEY, null)?.trim()
            if (!custom.isNullOrEmpty()) return custom
            return try {
                val configKey = BuildConfig.SUPABASE_ANON_KEY.trim()
                configKey
            } catch (e: Throwable) {
                ""
            }
        }
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_KEY, value.trim()).apply()
        }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userPhone: String?
        get() = prefs.getString(KEY_USER_PHONE, null)
        set(value) = prefs.edit().putString(KEY_USER_PHONE, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    val isConfigured: Boolean
        get() = projectUrl.isNotEmpty() &&
                anonKey.isNotEmpty() &&
                !projectUrl.contains("your-project-id") &&
                !anonKey.contains("placeholder")

    val isAuthenticated: Boolean
        get() = !userId.isNullOrEmpty() && !accessToken.isNullOrEmpty()

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHONE)
            .remove(KEY_USER_NAME)
            .apply()
    }

    private fun cleanUrl(raw: String): String {
        var url = raw.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        return url
    }
}
