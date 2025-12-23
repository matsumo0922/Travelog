package me.matsumo.travelog.feature.setting.components.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.setting_account
import me.matsumo.travelog.core.resource.setting_account_delete
import me.matsumo.travelog.core.resource.setting_account_delete_confirm_message
import me.matsumo.travelog.core.resource.setting_account_delete_confirm_title
import me.matsumo.travelog.core.resource.setting_account_delete_description
import me.matsumo.travelog.core.resource.setting_account_logout
import me.matsumo.travelog.core.resource.setting_account_logout_confirm_message
import me.matsumo.travelog.core.resource.setting_account_logout_confirm_title
import me.matsumo.travelog.core.resource.setting_account_logout_description
import me.matsumo.travelog.feature.setting.components.SettingConfirmDialog
import me.matsumo.travelog.feature.setting.components.SettingTextItem
import me.matsumo.travelog.feature.setting.components.SettingTitleItem

@Composable
internal fun SettingAccountSection(
    onLogoutClicked: () -> Unit,
    onDeleteAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isShowLogoutDialog by remember { mutableStateOf(false) }
    var isShowDeleteAccountDialog by remember { mutableStateOf(false) }

    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_account,
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = Res.string.setting_account_logout,
            description = Res.string.setting_account_logout_description,
            onClick = { isShowLogoutDialog = true },
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = Res.string.setting_account_delete,
            description = Res.string.setting_account_delete_description,
            onClick = { isShowDeleteAccountDialog = true },
        )
    }

    if (isShowLogoutDialog) {
        SettingConfirmDialog(
            title = Res.string.setting_account_logout_confirm_title,
            message = Res.string.setting_account_logout_confirm_message,
            onConfirm = {
                onLogoutClicked()
                isShowLogoutDialog = false
            },
            onDismissRequest = { isShowLogoutDialog = false },
        )
    }

    if (isShowDeleteAccountDialog) {
        SettingConfirmDialog(
            title = Res.string.setting_account_delete_confirm_title,
            message = Res.string.setting_account_delete_confirm_message,
            onConfirm = {
                onDeleteAccountClicked()
                isShowDeleteAccountDialog = false
            },
            onDismissRequest = { isShowDeleteAccountDialog = false },
        )
    }
}
