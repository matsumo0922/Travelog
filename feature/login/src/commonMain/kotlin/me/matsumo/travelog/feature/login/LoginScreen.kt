package me.matsumo.travelog.feature.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithApple
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.model.Platform
import me.matsumo.travelog.core.model.currentPlatform
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.account_auth_error
import me.matsumo.travelog.core.resource.app_name
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.view.LoadingView
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.core.ui.theme.center
import me.matsumo.travelog.feature.login.components.LoginButtonSection
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun LoginRoute(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()

    val navBackStack = LocalNavBackStack.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val composeAuth = koinInject<ComposeAuth>()

    val authCallback: (NativeSignInResult) -> Unit = { result ->
        scope.launch {
            when (result) {
                is NativeSignInResult.Success -> null
                is NativeSignInResult.NetworkError -> getString(Res.string.error_network)
                is NativeSignInResult.ClosedByUser -> null
                is NativeSignInResult.Error -> {
                    Napier.w("Native login failed.", result.exception)
                    getString(Res.string.account_auth_error)
                }
            }?.also {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    val googleAuthState = composeAuth.rememberSignInWithGoogle(authCallback)
    val appleAuthState = composeAuth.rememberSignInWithApple(authCallback)

    LaunchedEffect(Unit) {
        viewModel.authenticatedTrigger.collect {
            navBackStack.removeLastOrNull()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.authenticationFailedTrigger.collect {
            snackbarHostState.showSnackbar(getString(Res.string.account_auth_error))
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier.navigationBarsPadding(),
                hostState = snackbarHostState,
            )
        },
        contentWindowInsets = WindowInsets()
    ) { contentPadding ->
        AnimatedContent(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            targetState = sessionStatus,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "AccountRoute",
        ) {
            when (it) {
                is SessionStatus.Authenticated,
                is SessionStatus.NotAuthenticated -> {
                    LoginScreen(
                        onGoogleLogin = {
                            if (currentPlatform == Platform.Android) {
                                googleAuthState.startFlow()
                            } else {
                                viewModel.signInWithGoogleOAuth()
                            }
                        },
                        onAppleLogin = {
                            if (currentPlatform == Platform.IOS) {
                                appleAuthState.startFlow()
                            } else {
                                viewModel.signInWithAppleOAuth()
                            }
                        },
                    )
                }

                else -> {
                    LoadingView(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    onGoogleLogin: () -> Unit,
    onAppleLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.titleLarge.center(),
            )
        }

        LoginButtonSection(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .navigationBarsPadding(),
            onGoogleLogin = onGoogleLogin,
            onAppleLogin = onAppleLogin,
        )
    }
}
