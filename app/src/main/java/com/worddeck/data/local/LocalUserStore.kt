package com.worddeck.data.local

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.worddeck.common.AppError
import com.worddeck.common.AppResult
import com.worddeck.domain.model.DisplayName
import com.worddeck.domain.model.EmailAddress
import com.worddeck.domain.model.User
import com.worddeck.domain.model.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Persists the single local profile. Passwords and Firebase credentials are never stored here. */
internal class LocalUserStore(
    private val preferences: SharedPreferences,
) {
    private val activeUser = MutableStateFlow(loadActiveUser())

    fun observeActiveUser(): StateFlow<AppResult<User?>> = activeUser

    fun findActiveUser(): AppResult<User?> = loadActiveUser()

    fun findStoredUser(): AppResult<User?> = loadUser(requireActive = false)

    @SuppressLint("UseKtx") // The boolean commit result is needed for AppResult.Failure.
    fun save(user: User): AppResult<Unit> {
        val saved = preferences.edit()
            .putString(KEY_ID, user.id.value)
            .putString(KEY_DISPLAY_NAME, user.displayName.value)
            .putNullableString(KEY_EMAIL, user.email?.value)
            .putNullableString(KEY_FIREBASE_UID, user.firebaseUid?.value)
            .putBoolean(KEY_ACTIVE, true)
            .commit()

        if (!saved) return profileUnavailable()
        activeUser.value = AppResult.Success(user)
        return AppResult.Success(Unit)
    }

    @SuppressLint("UseKtx") // The boolean commit result is needed for AppResult.Failure.
    fun deactivate(): AppResult<Unit> {
        if (!preferences.edit().putBoolean(KEY_ACTIVE, false).commit()) {
            return profileUnavailable()
        }
        activeUser.value = AppResult.Success(null)
        return AppResult.Success(Unit)
    }

    private fun loadActiveUser(): AppResult<User?> = loadUser(requireActive = true)

    private fun loadUser(requireActive: Boolean): AppResult<User?> {
        val rawId = preferences.getString(KEY_ID, null) ?: return AppResult.Success(null)
        if (requireActive && !preferences.getBoolean(KEY_ACTIVE, false)) {
            return AppResult.Success(null)
        }

        val id = when (val result = UserId.from(rawId)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val displayName = when (
            val result = DisplayName.from(preferences.getString(KEY_DISPLAY_NAME, null).orEmpty())
        ) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val email = preferences.getString(KEY_EMAIL, null)?.let { rawEmail ->
            when (val result = EmailAddress.from(rawEmail)) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> return result
            }
        }
        val firebaseUid = preferences.getString(KEY_FIREBASE_UID, null)?.let { rawUid ->
            when (val result = UserId.from(rawUid)) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> return result
            }
        }

        return AppResult.Success(User(id, email, displayName, firebaseUid))
    }
}

private fun SharedPreferences.Editor.putNullableString(
    key: String,
    value: String?,
): SharedPreferences.Editor = if (value == null) remove(key) else putString(key, value)

private fun profileUnavailable(): AppResult.Failure =
    AppResult.Failure(AppError.Unavailable("local profile"))

private const val KEY_ID = "user_id"
private const val KEY_DISPLAY_NAME = "display_name"
private const val KEY_EMAIL = "email"
private const val KEY_FIREBASE_UID = "firebase_uid"
private const val KEY_ACTIVE = "active"
