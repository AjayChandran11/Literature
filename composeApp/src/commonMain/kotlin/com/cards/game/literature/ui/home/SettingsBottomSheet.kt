package com.cards.game.literature.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cards.game.literature.preferences.GamePrefs
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.button_done
import literature.composeapp.generated.resources.settings_haptic_feedback
import literature.composeapp.generated.resources.settings_sound_effects
import literature.composeapp.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(onDismiss: () -> Unit) {
    var soundEnabled by remember { mutableStateOf(GamePrefs.isSoundEnabled()) }
    var hapticsEnabled by remember { mutableStateOf(GamePrefs.isHapticsEnabled()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
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
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
