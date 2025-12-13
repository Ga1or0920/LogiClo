package com.example.myapplication.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.UserPreferencesRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel(private val userPreferencesRepository: UserPreferencesRepository) : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _uiState.value = _uiState.value.copy(
                isAuthenticated = user != null,
                username = user?.email ?: ""
            )
        }
    }

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username, authError = null, emailSent = false)
    }

    fun sendEmailLink() {
        if (_uiState.value.username.isBlank()) {
            _uiState.value = _uiState.value.copy(authError = "メールアドレスを入力してください。")
            return
        }
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setAndroidPackageName("com.example.myapplication", true, null)
            .setHandleCodeInApp(true)
            .setUrl("https://logiclo-kobedenshi.web.app")
            .build()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, authError = null)
            try {
                userPreferencesRepository.setEmailForSignIn(_uiState.value.username)
                auth.sendSignInLinkToEmail(_uiState.value.username, actionCodeSettings).await()
                _uiState.value = _uiState.value.copy(isLoading = false, emailSent = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, authError = e.message)
            }
        }
    }

    fun handleGoogleSignInResult(intent: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            _uiState.value = _uiState.value.copy(authError = "Googleログインに失敗しました。")
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        if (account?.idToken == null) {
            _uiState.value = _uiState.value.copy(authError = "Googleアカウントのトークンが取得できませんでした。")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, authError = null)
            try {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).await()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, authError = e.message)
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    companion object {
        fun Factory(userPreferencesRepository: UserPreferencesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(userPreferencesRepository) as T
                }
            }
    }
}

data class SettingsUiState(
    val username: String = "",
    val isLoading: Boolean = false,
    val authError: String? = null,
    val isAuthenticated: Boolean = false,
    val emailSent: Boolean = false
)
