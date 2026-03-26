package com.cards.game.literature.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlaySheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                stringResource(Res.string.help_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val sections = listOf(
                Res.string.help_asking_title to Res.string.help_asking_body,
                Res.string.help_claiming_title to Res.string.help_claiming_body,
                Res.string.help_no_cards_title to Res.string.help_no_cards_body,
                Res.string.help_winning_title to Res.string.help_winning_body,
                Res.string.help_tip_title to Res.string.help_tip_body,
            )

            sections.forEach { (titleRes, bodyRes) ->
                RuleSection(titleRes = titleRes, bodyRes = bodyRes)
            }
        }
    }
}

@Composable
private fun RuleSection(titleRes: StringResource, bodyRes: StringResource) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
    )
    Text(
        stringResource(bodyRes),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 24.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
