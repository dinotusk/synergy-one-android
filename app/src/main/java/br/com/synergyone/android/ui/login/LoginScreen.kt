package br.com.synergyone.android.ui.login

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.R
import br.com.synergyone.android.data.auth.model.UserRole
import br.com.synergyone.android.ui.components.PasswordField
import br.com.synergyone.android.ui.theme.SynergyOneTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onNavigateToHome: (UserRole) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is LoginNavigationEvent.NavigateToHome -> onNavigateToHome(event.role)
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onLoginClick = viewModel::onLoginClick,
    )
}

/** A abertura ocorre no login, sem uma segunda tela ou espera artificial. */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
) {
    var logoVisible by remember { mutableStateOf(false) }
    var showRecoveryNotice by remember { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "logoAlpha",
    )

    LaunchedEffect(Unit) { logoVisible = true }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(loginBackground()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BrandLogo(modifier = Modifier.alpha(logoAlpha))
                Spacer(modifier = Modifier.height(30.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(R.string.login_welcome),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.login_access_heading),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = stringResource(R.string.login_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
                        )

                        OutlinedTextField(
                            value = uiState.username,
                            onValueChange = onUsernameChange,
                            label = { Text(stringResource(R.string.login_username_label)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.AlternateEmail, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            enabled = !uiState.isLoading,
                            isError = uiState.errorMessage != null,
                            colors = loginTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        PasswordField(
                            value = uiState.password,
                            onValueChange = onPasswordChange,
                            label = stringResource(R.string.login_password_label),
                            isPasswordVisible = uiState.isPasswordVisible,
                            onToggleVisibility = onTogglePasswordVisibility,
                            showLabel = stringResource(R.string.login_password_show),
                            hideLabel = stringResource(R.string.login_password_hide),
                            isError = uiState.errorMessage != null,
                            enabled = !uiState.isLoading,
                            colors = loginTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )

                        TextButton(
                            onClick = { showRecoveryNotice = !showRecoveryNotice },
                            enabled = !uiState.isLoading,
                            modifier = Modifier.padding(top = 6.dp),
                        ) { Text(stringResource(R.string.login_forgot_password)) }

                        if (showRecoveryNotice) {
                            Text(
                                text = stringResource(R.string.login_recovery_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }

                        if (uiState.errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 10.dp),
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp),
                                )
                            }
                        }

                        Button(
                            onClick = onLoginClick,
                            enabled = uiState.isSubmitEnabled,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(54.dp),
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else Text(stringResource(R.string.login_button))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.login_security_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 22.dp),
                )
            }
        }
    }
}

@Composable
private fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_synergy),
        contentDescription = stringResource(R.string.login_brand_mark_description),
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxWidth().widthIn(max = 236.dp).height(92.dp),
    )
}

@Composable
private fun loginBackground(): Brush = Brush.radialGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        MaterialTheme.colorScheme.background,
    ),
)

@Composable
private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f),
)

@Preview(showBackground = true, backgroundColor = 0xFF121016)
@Composable
private fun LoginScreenPreview() {
    SynergyOneTheme {
        LoginScreen(
            uiState = LoginUiState(username = "ana@empresa.com", password = "123456"),
            onUsernameChange = {}, onPasswordChange = {}, onTogglePasswordVisibility = {}, onLoginClick = {},
        )
    }
}
