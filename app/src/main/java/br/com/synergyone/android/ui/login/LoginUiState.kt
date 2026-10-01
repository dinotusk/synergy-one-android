package br.com.synergyone.android.ui.login

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isSubmitEnabled: Boolean get() = !isLoading && username.isNotBlank() && password.isNotBlank()
}
