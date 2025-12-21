package me.matsumo.travelog.feature.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.account_continue_with_apple
import me.matsumo.travelog.core.resource.account_continue_with_google
import me.matsumo.travelog.core.resource.account_policy_agreement
import me.matsumo.travelog.core.resource.account_privacy_policy
import me.matsumo.travelog.core.resource.account_team_of_service
import me.matsumo.travelog.core.ui.theme.center
import me.matsumo.travelog.core.ui.theme.semiBold
import me.matsumo.zencall.core.ui.icon.Apple
import me.matsumo.zencall.core.ui.icon.Google
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoginButtonSection(
    onGoogleLogin: () -> Unit,
    onAppleLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = ButtonDefaults.MediumContainerHeight

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onGoogleLogin,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            contentPadding = ButtonDefaults.contentPaddingFor(height)
        ) {
            Image(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Filled.Google,
                contentDescription = null,
            )

            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(Res.string.account_continue_with_google),
                style = MaterialTheme.typography.bodyMedium.semiBold(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onAppleLogin,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            contentPadding = ButtonDefaults.contentPaddingFor(height)
        ) {
            Image(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Filled.Apple,
                contentDescription = null,
            )

            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(Res.string.account_continue_with_apple),
                style = MaterialTheme.typography.bodyMedium.semiBold(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = buildAnnotatedString {
                val policyAgreement = stringResource(Res.string.account_policy_agreement)
                val teamOfService = stringResource(Res.string.account_team_of_service)
                val privacyPolicy = stringResource(Res.string.account_privacy_policy)

                append(policyAgreement)

                policyAgreement.indexOf(teamOfService).also {
                    addLink(
                        url = LinkAnnotation.Url("https://www.matsumo.me/application/pixiview/team_of_service"),
                        start = it,
                        end = it + teamOfService.length,
                    )
                }

                policyAgreement.indexOf(privacyPolicy).also {
                    addLink(
                        url = LinkAnnotation.Url("https://www.matsumo.me/application/pixiview/privacy_policy"),
                        start = it,
                        end = it + privacyPolicy.length,
                    )
                }
            },
            style = MaterialTheme.typography.bodyMedium.center(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}