package com.bankingsystem.mobile.features.profile.interfaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankingsystem.mobile.core.modules.common.storage.AuthStore
import com.bankingsystem.mobile.core.modules.common.util.checkPasswordStrength
import com.bankingsystem.mobile.core.modules.common.util.doPasswordsMatch
import com.bankingsystem.mobile.features.auth.domain.repository.UserRepository
import com.bankingsystem.mobile.features.profile.domain.model.UserProfile
import com.bankingsystem.mobile.features.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profile: UserProfile? = null,
    val editingField: EditableField? = null,
    val tempValue: String = "",
    val isCheckingUsername: Boolean = false,
    val isUsernameAvailable: Boolean? = null,
    val showPasswordDialog: Boolean = false,
    val pendingUsername: String? = null,
    val showChangePasswordDialog: Boolean = false,
    val isChangingPassword: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val authRepo: UserRepository,
    private val store: AuthStore
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui

    private var usernameCheckJob: Job? = null

    private val crashHandler = CoroutineExceptionHandler { _, e ->
        android.util.Log.e("ProfileVM", "Coroutine exception", e)
        toast(e.message ?: "Unexpected error")
    }

    init { loadProfile() }

    /* --------------------------- Data loading --------------------------- */

    fun loadProfile() = viewModelScope.launch(crashHandler) {
        _ui.value = _ui.value.copy(isLoading = true, errorMessage = null)
        val res = profileRepo.getMe()
        if (res.isSuccess) {
            val safe = res.getOrNull()?.toSanitized()
            _ui.value = _ui.value.copy(isLoading = false, profile = safe)
        } else {
            _ui.value = _ui.value.copy(isLoading = false, errorMessage = "Failed to load profile.")
        }
    }

    /* --------------------------- Inline editing ------------------------- */

    fun startEditing(field: EditableField) {
        val value = currentValue(field)
        _ui.value = _ui.value.copy(
            editingField = field,
            tempValue = value,
            isUsernameAvailable = null,
            isCheckingUsername = false
        )
        if (field == EditableField.USERNAME && value.isNotBlank()) {
            debounceUsername(value)
        }
    }

    fun cancelEditing() {
        _ui.value = _ui.value.copy(
            editingField = null,
            tempValue = "",
            isCheckingUsername = false,
            isUsernameAvailable = null
        )
    }

    fun onTempValueChange(new: String) {
        _ui.value = _ui.value.copy(tempValue = new)
        if (_ui.value.editingField == EditableField.USERNAME) {
            debounceUsername(new)
        }
    }

    private fun debounceUsername(value: String) {
        usernameCheckJob?.cancel()
        if (value.isBlank()) {
            _ui.value = _ui.value.copy(isCheckingUsername = false, isUsernameAvailable = null)
            return
        }
        usernameCheckJob = viewModelScope.launch(crashHandler) {
            _ui.value = _ui.value.copy(isCheckingUsername = true, isUsernameAvailable = null)
            delay(450)
            val ok = runCatching { authRepo.checkUsernameAvailability(value) }.getOrDefault(false)
            _ui.value = _ui.value.copy(isCheckingUsername = false, isUsernameAvailable = ok)
        }
    }

    fun saveEditing() = viewModelScope.launch(crashHandler) {
        val field = _ui.value.editingField ?: return@launch
        val temp = _ui.value.tempValue
        val prof = _ui.value.profile ?: return@launch

        if (field == EditableField.USERNAME) {
            val avail = _ui.value.isUsernameAvailable
            if (avail != true) { toast("Username is not available or too short."); return@launch }
            _ui.value = _ui.value.copy(showPasswordDialog = true, pendingUsername = temp)
            return@launch
        }

        val key = fieldKey(field)
        val res = profileRepo.updateMe(mapOf(key to temp))
        if (res.isSuccess) {
            val updated = prof.toSanitized().copyWith(field, temp)
            _ui.value = _ui.value.copy(
                profile = updated,
                editingField = null,
                tempValue = "",
                snackbarMessage = "Profile updated successfully"
            )
        } else {
            toast("Failed to update profile. Please try again.")
        }
    }

    /* ----------------------- Username change (verify) ------------------- */

    fun onPasswordVerified(password: String) = viewModelScope.launch(crashHandler) {
        val newUsername = _ui.value.pendingUsername ?: return@launch

        val update = profileRepo.updateMe(mapOf("username" to newUsername, "currentPassword" to password))
        if (update.isFailure) { toast("Failed to update username. Please try again."); return@launch }

        val login = authRepo.login(newUsername, password)
        if (login.isFailure) { toast("Login failed after username change."); return@launch }

        val auth = login.getOrNull()!!
        store.saveAuth(auth.token, auth.username, auth.role)

        val updated = _ui.value.profile?.toSanitized()?.copy(username = newUsername)
        _ui.value = _ui.value.copy(
            profile = updated,
            showPasswordDialog = false,
            pendingUsername = null,
            editingField = null,
            tempValue = "",
            snackbarMessage = "Username updated and re-authenticated"
        )
    }

    fun dismissPasswordDialog() {
        _ui.value = _ui.value.copy(showPasswordDialog = false, pendingUsername = null)
    }

    /* ------------------------ Change password flow ---------------------- */

    fun openChangePassword() {
        _ui.value = _ui.value.copy(showChangePasswordDialog = true)
    }

    fun dismissChangePassword() {
        _ui.value = _ui.value.copy(showChangePasswordDialog = false)
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmNewPassword: String) =
        viewModelScope.launch(crashHandler) {
            val strength = checkPasswordStrength(newPassword)
            val matches = doPasswordsMatch(newPassword, confirmNewPassword)

            if (strength.score < 4) {
                toast(if (strength.issues.isNotEmpty())
                    "New password too weak: ${strength.issues.joinToString("; ")}"
                else "New password too weak.")
                return@launch
            }
            if (!matches) { toast("New password and confirmation do not match."); return@launch }

            _ui.value = _ui.value.copy(isChangingPassword = true)
            val res = profileRepo.changePassword(currentPassword, newPassword, confirmNewPassword)

            if (res.isSuccess) {
                _ui.value = _ui.value.copy(
                    isChangingPassword = false,
                    showChangePasswordDialog = false,
                    snackbarMessage = "Password changed successfully"
                )
            } else {
                _ui.value = _ui.value.copy(isChangingPassword = false)
                toast(res.exceptionOrNull()?.message ?: "Failed to change password.")
            }
        }

    /* ------------------------------- UX -------------------------------- */

    fun snackbarShown() { _ui.value = _ui.value.copy(snackbarMessage = null) }
    private fun toast(msg: String) { _ui.value = _ui.value.copy(snackbarMessage = msg) }

    /* ----------------------------- Helpers ------------------------------ */

    private fun currentValue(field: EditableField): String =
        when (field) {
            EditableField.USERNAME      -> (_ui.value.profile?.username as String?)?.trim().orEmpty()
            EditableField.EMAIL         -> (_ui.value.profile?.email as String?)?.trim().orEmpty()
            EditableField.FIRST_NAME    -> (_ui.value.profile?.firstName as String?)?.trim().orEmpty()
            EditableField.LAST_NAME     -> (_ui.value.profile?.lastName as String?)?.trim().orEmpty()
            EditableField.ADDRESS       -> (_ui.value.profile?.address as String?)?.trim().orEmpty()
            EditableField.CITY          -> (_ui.value.profile?.city as String?)?.trim().orEmpty()
            EditableField.STATE         -> (_ui.value.profile?.state as String?)?.trim().orEmpty()
            EditableField.COUNTRY       -> (_ui.value.profile?.country as String?)?.trim().orEmpty()
            EditableField.POSTAL_CODE   -> (_ui.value.profile?.postalCode as String?)?.trim().orEmpty()
            EditableField.HOME_NUMBER   -> (_ui.value.profile?.homeNumber as String?)?.trim().orEmpty()
            EditableField.WORK_NUMBER   -> (_ui.value.profile?.workNumber as String?)?.trim().orEmpty()
            EditableField.OFFICE_NUMBER -> (_ui.value.profile?.officeNumber as String?)?.trim().orEmpty()
            EditableField.MOBILE_NUMBER -> (_ui.value.profile?.mobileNumber as String?)?.trim().orEmpty()
        }

    private fun fieldKey(field: EditableField): String = when (field) {
        EditableField.USERNAME      -> "username"
        EditableField.EMAIL         -> "email"
        EditableField.FIRST_NAME    -> "firstName"
        EditableField.LAST_NAME     -> "lastName"
        EditableField.ADDRESS       -> "address"
        EditableField.CITY          -> "city"
        EditableField.STATE         -> "state"
        EditableField.COUNTRY       -> "country"
        EditableField.POSTAL_CODE   -> "postalCode"
        EditableField.HOME_NUMBER   -> "homeNumber"
        EditableField.WORK_NUMBER   -> "workNumber"
        EditableField.OFFICE_NUMBER -> "officeNumber"
        EditableField.MOBILE_NUMBER -> "mobileNumber"
    }

    private fun UserProfile.copyWith(field: EditableField, value: String): UserProfile =
        when (field) {
            EditableField.USERNAME      -> this.copy(username = value)
            EditableField.EMAIL         -> this.copy(email = value)
            EditableField.FIRST_NAME    -> this.copy(firstName = value)
            EditableField.LAST_NAME     -> this.copy(lastName = value)
            EditableField.ADDRESS       -> this.copy(address = value)
            EditableField.CITY          -> this.copy(city = value)
            EditableField.STATE         -> this.copy(state = value)
            EditableField.COUNTRY       -> this.copy(country = value)
            EditableField.POSTAL_CODE   -> this.copy(postalCode = value)
            EditableField.HOME_NUMBER   -> this.copy(homeNumber = value)
            EditableField.WORK_NUMBER   -> this.copy(workNumber = value)
            EditableField.OFFICE_NUMBER -> this.copy(officeNumber = value)
            EditableField.MOBILE_NUMBER -> this.copy(mobileNumber = value)
        }

    private fun UserProfile.toSanitized(): UserProfile = UserProfile(
        userId       = (this.userId as String?)       ?: "",
        username     = (this.username as String?)     ?: "",
        firstName    = (this.firstName as String?)    ?: "",
        lastName     = (this.lastName as String?)     ?: "",
        address      = (this.address as String?)      ?: "",
        city         = (this.city as String?)         ?: "",
        state        = (this.state as String?)        ?: "",
        country      = (this.country as String?)      ?: "",
        postalCode   = (this.postalCode as String?)   ?: "",
        homeNumber   = (this.homeNumber as String?)   ?: "",
        workNumber   = (this.workNumber as String?)   ?: "",
        officeNumber = (this.officeNumber as String?) ?: "",
        mobileNumber = (this.mobileNumber as String?) ?: "",
        email        = (this.email as String?)        ?: "",
        roleName     = (this.roleName as String?)     ?: ""
    )
}
