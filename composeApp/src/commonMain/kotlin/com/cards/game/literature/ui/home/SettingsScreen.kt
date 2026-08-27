package com.cards.game.literature.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cards.game.literature.di.appVersionCode
import com.cards.game.literature.di.appVersionName
import com.cards.game.literature.notifications.PuzzleReminderScheduler
import com.cards.game.literature.isWebPlatform
import com.cards.game.literature.preferences.BotPacing
import com.cards.game.literature.preferences.GamePrefs
import kotlin.math.roundToInt
import com.cards.game.literature.ui.theme.ThemeController
import com.cards.game.literature.ui.theme.ThemeMode
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private const val PRIVACY_POLICY_URL = "https://ajaychandran11.github.io/Literature/privacy"
private const val FEEDBACK_EMAIL = "ajaychandran443@gmail.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var soundEnabled by remember { mutableStateOf(GamePrefs.isSoundEnabled()) }
    var hapticsEnabled by remember { mutableStateOf(GamePrefs.isHapticsEnabled()) }
    var notificationsEnabled by remember { mutableStateOf(GamePrefs.isNotificationsEnabled()) }
    var puzzleReminderEnabled by remember { mutableStateOf(GamePrefs.isPuzzleReminderEnabled()) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.settings_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_settings_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            SettingsToggleRow(
                label = stringResource(Res.string.settings_sound_effects),
                checked = soundEnabled,
                onCheckedChange = {
                    soundEnabled = it
                    GamePrefs.setSoundEnabled(it)
                }
            )
            // No haptics or notifications on web — hide toggles that would do nothing.
            if (!isWebPlatform()) {
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
            }

            BotSpeedSetting()

            // Theme: a value-picker row sized like the toggle rows — the current choice reads
            // inline, the three options live in a dropdown. (Material You dynamic color was
            // deliberately NOT exposed: the game's heavy use of direct brand colours means it only
            // half-repaints — the dormant plumbing lives in ThemeController.dynamicColors if wanted.)
            ThemePickerRow()

            // ── About ─────────────────────────────────────────────────────
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Text(
                text = stringResource(Res.string.settings_about),
                // Same size as the option rows (bodyLarge) so the header doesn't read as shrunken;
                // bold weight is what marks it as a section label instead.
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            SettingsInfoRow(
                label = stringResource(Res.string.settings_version),
                value = "$appVersionName ($appVersionCode)"
            )
            val feedbackSubject = stringResource(Res.string.feedback_email_subject, appVersionName)
            SettingsLinkRow(
                label = stringResource(Res.string.settings_privacy_policy),
                onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) }
            )
            SettingsLinkRow(
                label = stringResource(Res.string.settings_send_feedback),
                onClick = {
                    // Minimal percent-encoding for the subject so spaces/parens survive the mailto.
                    val subject = feedbackSubject
                        .replace(" ", "%20").replace("(", "%28").replace(")", "%29")
                    uriHandler.openUri("mailto:$FEEDBACK_EMAIL?subject=$subject")
                }
            )
            Text(
                text = stringResource(Res.string.settings_licenses),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
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

/** Read-only label/value row (e.g. app version), sized like the toggle rows. */
@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Tappable text row that opens an external target (privacy page, mail composer). It reads as an
 *  inline link, not a button, so it's clickable without ripple/indication (interactionSource + null
 *  indication) — a ripple wash on a bare text line looks out of place. */
@Composable
private fun SettingsLinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Bot pacing for offline games. The toggle keeps the stock pacing (identical to every
 *  release so far) until the player opts in — only then does the slider appear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BotSpeedSetting() {
    var customEnabled by remember { mutableStateOf(GamePrefs.isBotSpeedCustomEnabled()) }
    var seconds by remember { mutableStateOf(GamePrefs.getBotDelaySeconds()) }

    SettingsToggleRow(
        label = stringResource(Res.string.settings_bot_speed),
        checked = customEnabled,
        onCheckedChange = {
            customEnabled = it
            GamePrefs.setBotSpeedCustomEnabled(it)
        }
    )
    AnimatedVisibility(
        visible = customEnabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        // Snapped to halves, so whole values read clean ("4s", not "4.0s").
        val thumbLabel = if (seconds % 1f == 0f) "${seconds.toInt()}s" else "${seconds}s"
        Slider(
            value = seconds,
            // Snap to clean half-second values; float step math can land on 4.4999998.
            onValueChange = { seconds = (it * 2).roundToInt() / 2f },
            // Persist on release, not on every drag frame.
            onValueChangeFinished = { GamePrefs.setBotDelaySeconds(seconds) },
            valueRange = BotPacing.MIN_SECONDS..BotPacing.MAX_SECONDS,
            steps = BotPacing.SLIDER_STEPS,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 26.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        thumbLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            },
            track = { sliderState ->
                // The default M3 track is chunky; slim it to read as a row control.
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(6.dp)
                )
            }
        )
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
            // Plain clickable row (not a TextButton) so the value + arrow sit flush with the
            // row's right edge, exactly where the switches end.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { menuOpen = true }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 4.dp),
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
