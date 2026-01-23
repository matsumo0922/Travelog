package me.matsumo.travelog.feature.setting.components.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.matsumo.travelog.core.model.AppSetting
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.setting_other
import me.matsumo.travelog.core.resource.setting_other_clear_cache
import me.matsumo.travelog.core.resource.setting_other_clear_cache_description
import me.matsumo.travelog.core.resource.setting_other_developer_mode
import me.matsumo.travelog.core.resource.setting_other_developer_mode_description
import me.matsumo.travelog.core.resource.setting_other_open_source_license
import me.matsumo.travelog.core.resource.setting_other_open_source_license_description
import me.matsumo.travelog.core.resource.setting_other_privacy_policy
import me.matsumo.travelog.core.resource.setting_other_team_of_service
import me.matsumo.travelog.feature.setting.components.SettingDeveloperModeDialog
import me.matsumo.travelog.feature.setting.components.SettingSwitchItem
import me.matsumo.travelog.feature.setting.components.SettingTextItem
import me.matsumo.travelog.feature.setting.components.SettingTitleItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingOthersSection(
    setting: AppSetting,
    cacheSize: Long?,
    onTeamsOfServiceClicked: () -> Unit,
    onPrivacyPolicyClicked: () -> Unit,
    onOpenSourceLicenseClicked: () -> Unit,
    onClearCacheClicked: () -> Unit,
    onDeveloperModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isShowDeveloperModeDialog by remember { mutableStateOf(false) }

    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_other,
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.setting_other_team_of_service),
            onClick = onTeamsOfServiceClicked,
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.setting_other_privacy_policy),
            onClick = onPrivacyPolicyClicked,
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = Res.string.setting_other_open_source_license,
            description = Res.string.setting_other_open_source_license_description,
            onClick = { onOpenSourceLicenseClicked.invoke() },
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.setting_other_clear_cache),
            description = stringResource(Res.string.setting_other_clear_cache_description, formatCacheSize(cacheSize)),
            onClick = onClearCacheClicked,
        )

        SettingSwitchItem(
            modifier = Modifier.fillMaxWidth(),
            title = Res.string.setting_other_developer_mode,
            description = Res.string.setting_other_developer_mode_description,
            value = setting.developerMode,
            onValueChanged = {
                if (it) {
                    isShowDeveloperModeDialog = true
                } else {
                    onDeveloperModeChanged.invoke(false)
                }
            },
        )
    }

    if (isShowDeveloperModeDialog) {
        SettingDeveloperModeDialog(
            onDeveloperModeEnabled = {
                onDeveloperModeChanged.invoke(true)
                isShowDeveloperModeDialog = false
            },
            onDismissRequest = {
                isShowDeveloperModeDialog = false
            },
        )
    }
}

private fun formatCacheSize(bytes: Long?): String {
    if (bytes == null) return "..."
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
