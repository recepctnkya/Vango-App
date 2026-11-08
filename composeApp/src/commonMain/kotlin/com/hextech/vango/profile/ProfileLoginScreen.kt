package com.hextech.vango.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Login
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.login_create_registration
import testvango.composeapp.generated.resources.login_email_or_username
import testvango.composeapp.generated.resources.login_error_invalid
import testvango.composeapp.generated.resources.login_forgot_password
import testvango.composeapp.generated.resources.login_forgot_unavailable_body
import testvango.composeapp.generated.resources.login_forgot_unavailable_ok
import testvango.composeapp.generated.resources.login_forgot_unavailable_title
import testvango.composeapp.generated.resources.login_password
import testvango.composeapp.generated.resources.login_remember_me
import testvango.composeapp.generated.resources.login_sign_in
import testvango.composeapp.generated.resources.login_subtitle
import testvango.composeapp.generated.resources.footer_menu
import testvango.composeapp.generated.resources.login_title

private const val REQUIRED_FIELD_MARK = " *"

@Composable
fun ProfileLoginScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var loginIdentifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var credErrorText by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }
    val invalidCredText = stringResource(Res.string.login_error_invalid)

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = {
                Text(stringResource(Res.string.login_forgot_unavailable_title))
            },
            text = {
                Text(stringResource(Res.string.login_forgot_unavailable_body))
            },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text(stringResource(Res.string.login_forgot_unavailable_ok))
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        rememberMe = LoginPreferences.isRememberMeEnabled()
        if (rememberMe) {
            LoginPreferences.getRememberedEmail()?.let { loginIdentifier = it }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        val isLight = MaterialTheme.colors.isLight
        val cardBg = if (isLight) MaterialTheme.colors.surface else Color(0xFF2B313D)
        val highContrastText = if (isLight) MaterialTheme.colors.onSurface else Color(0xFFF2F5FA)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colors.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.footer_menu),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.login_title),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(Res.string.login_subtitle),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 2.dp,
                backgroundColor = cardBg,
                contentColor = highContrastText,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = highContrastText,
                        backgroundColor = cardBg,
                        cursorColor = MaterialTheme.colors.primary,
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = highContrastText.copy(alpha = 0.45f),
                        focusedLabelColor = MaterialTheme.colors.primary,
                        unfocusedLabelColor = highContrastText.copy(alpha = 0.88f),
                    )
                    OutlinedTextField(
                        value = loginIdentifier,
                        onValueChange = { loginIdentifier = it; credErrorText = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.login_email_or_username) + REQUIRED_FIELD_MARK) },
                        singleLine = true,
                        colors = fieldColors,
                        enabled = !state.isLoading,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; credErrorText = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.login_password) + REQUIRED_FIELD_MARK) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = fieldColors,
                        enabled = !state.isLoading,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary),
                            )
                            Text(
                                stringResource(Res.string.login_remember_me),
                                style = MaterialTheme.typography.body2,
                                color = highContrastText,
                            )
                        }
                        TextButton(
                            onClick = { showForgotDialog = true },
                            enabled = !state.isLoading
                        ) {
                            Text(
                                stringResource(Res.string.login_forgot_password),
                                color = MaterialTheme.colors.primary,
                                style = MaterialTheme.typography.caption,
                            )
                        }
                    }

                    if (!credErrorText.isNullOrBlank()) {
                        Text(
                            text = credErrorText ?: stringResource(Res.string.login_error_invalid),
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                        )
                    }

                    Button(
                        onClick = {
                            LoginPreferences.setRememberMe(rememberMe)
                            if (rememberMe) LoginPreferences.setRememberedEmail(loginIdentifier.trim())
                            else {
                                LoginPreferences.setRememberedEmail(null)
                            }
                            viewModel.signInWithCredentials(loginIdentifier, password) { result ->
                                result.onSuccess { onLoginSuccess() }
                                result.onFailure {
                                    credErrorText = it.message ?: invalidCredText
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading && loginIdentifier.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary,
                            disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.42f),
                            disabledContentColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.85f)
                        ),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colors.onPrimary
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, tint = MaterialTheme.colors.onPrimary)
                                Text(
                                    stringResource(Res.string.login_sign_in),
                                    color = MaterialTheme.colors.onPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colors.primary,
                            disabledContentColor = highContrastText.copy(alpha = 0.55f)
                        ),
                    ) {
                        Text(stringResource(Res.string.login_create_registration))
                    }
                }
            }
        }
    }
}
