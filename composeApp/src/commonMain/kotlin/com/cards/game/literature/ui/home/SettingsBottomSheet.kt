package com.cards.game.literature.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cards.game.literature.notifications.PuzzleReminderScheduler
import com.cards.game.literature.preferences.GamePrefs
import com.cards.game.literature.ui.theme.ThemeController
import com.cards.game.literature.ui.theme.ThemeMode
import com.cards.game.literature.ui.theme.isDynamicColorSupported
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.button_done
import literature.composeapp.generated.resources.settings_daily_reminder
import literature.composeapp.generated.resources.settings_haptic_feedback
import literature.composeapp.generated.resources.settings_material_you
import literature.composeapp.generated.resources.settings_material_you_desc
import literature.composeapp.generated.resources.settings_notifications
import literature.composeapp.generated.resources.settings_sound_effects
import literature.composeapp.generated.resources.settings_theme
import literature.composeapp.generated.resources.settings_title
import literature.composeapp.generated.resources.theme_dark
import literature.composeapp.generated.resources.theme_light
import literature.composeapp.generated.resources.theme_system
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(onDismiss: () -> Unit) {
    var soundEnabled by remember { mutableStateOf(GamePrefs.isSoundEnabled()) }
    var hapticsEnabled by remember { mutableStateOf(GamePrefs.isHapticsEnabled()) }
    var notificationsEnabled by remember { mutableStateOf(GamePrefs.isNotificationsEnabled()) }
    var puzzleReminderEnabled by remember { mutableStateOf(GamePrefs.isPuzzleReminderEnabled()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingsToggleRow(
                label = stringResource(Res.string.settings_sound_effects),
                checked = soundEnabled,
                onCheckedChange = {
                    soundEnabled = it
                    GamePrefs.setSoundEnabled(it)
                }
            )

            SettingsToggleRow(
                label = stringResource(Res.string.settings_haptic_feedback),
                checked = hapticsEnabled,
                onCheckedChange = {
                    hapticsEnabled = it
                    GamePrefs.setHapticsEnabled(it)
                }
            )

            SettingsToggleRow(
                label = stringResource(Res.string.settings_notifications),
                checked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                    GamePrefs.setNotificationsEnabled(it)
                }
            )

            SettingsToggleRow(
                label = stringResource(Res.string.settings_daily_reminder),
                checked = puzzleReminderEnabled,
                onCheckedChange = {
                    puzzleReminderEnabled = it
                    GamePrefs.setPuzzleReminderEnabled(it)
                    if (it) PuzzleReminderScheduler.schedule() else PuzzleReminderScheduler.cancel()
                }
            )

            // Theme: a value-picker row sized like the toggle rows — the current
            // choice reads inline, the three options live in a dropdown.
            ThemePickerRow()

            // Dynamic color — only offered where the OS supports it (Android 12+).
            if (isDynamicColorSupported) {
                SettingsToggleRow(
                    label = stringResource(Res.string.settings_material_you),
                    supportingText = stringResource(Res.string.settings_material_you_desc),
                    checked = ThemeController.dynamicColors,
                    onCheckedChange = { ThemeController.dynamicColors = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(stringResource(Res.string.button_done), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supportingText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supportingText != null) {
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    // Dimmed so the description reads as secondary to the label.
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Theme row: label left, current value + dropdown right — same rhythm as the switches. */
@Composable
private fun ThemePickerRow() {
    var menuOpen by remember { mutableStateOf(false) }
    val labelFor: @Composable (ThemeMode) -> String = { themeMode ->
        stringResource(
            when (themeMode) {
                ThemeMode.SYSTEM -> Res.string.theme_system
                ThemeMode.LIGHT -> Res.string.theme_light
                ThemeMode.DARK -> Res.string.theme_dark
            }
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(Res.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
        Box {
            // Plain clickable row (not a TextButton) so the value + arrow sit flush
            // with the row's right edge, exactly where the switches end.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { menuOpen = true }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    labelFor(ThemeController.mode),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                // Match the game's surfaces instead of M3's default menu container,
                // but lifted a step above the sheet: a 10% onSurface wash lightens
                // the panel in dark mode (where shadows can't separate same-colored
                // surfaces) and gently darkens it in light. The hairline outline
                // gives it a defined edge on the dark felt.
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                    .compositeOver(MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                ThemeMode.entries.forEach { themeMode ->
                    DropdownMenuItem(
                        text = { Text(labelFor(themeMode)) },
                        trailingIcon = {
                            if (ThemeController.mode == themeMode) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        },
                        onClick = {
                            ThemeController.mode = themeMode
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}
