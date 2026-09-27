package com.cards.game.literature.ui.common

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.Suit

// Savers for the selections a game screen holds while a turn is in progress.
//
// MainActivity declares no configChanges, so the Activity is recreated on a rotation — and on the
// system flipping to dark mode at dusk, which is exactly when people play. Anything in a plain
// remember{} died with it: a half-built claim, a queue of cards to ask for, the open sheet itself,
// all while the server's turn clock kept running. These map each value to a plain string, so the
// same code restores on Android and on the web.

/** A card as "SPADES|TWO". '|' appears in no enum name, so it can never collide. */
internal fun Card.toKey(): String = "${suit.name}|${value.name}"

internal fun cardFromKey(key: String): Card? {
    val parts = key.split('|')
    if (parts.size != 2) return null
    val suit = Suit.entries.firstOrNull { it.name == parts[0] } ?: return null
    val value = CardValue.entries.firstOrNull { it.name == parts[1] } ?: return null
    return Card(suit, value)
}

/**
 * An enum stored by name. A value the restore can't resolve — a selection whose enum was renamed
 * by an update, or the "nothing selected" null — falls back to the caller's own initial value,
 * which is the same null those call sites start from.
 */
internal inline fun <reified T : Enum<T>> nullableEnumSaver(): Saver<T?, String> =
    Saver(
        save = { it?.name ?: "" },
        restore = { name -> enumValues<T>().firstOrNull { it.name == name } }
    )

/** The same, for a selection that always has a value. */
internal inline fun <reified T : Enum<T>> enumSaver(): Saver<T, String> =
    Saver(
        save = { it.name },
        restore = { name -> enumValues<T>().firstOrNull { it.name == name } }
    )

/** The queue of cards built up in the ask sheet, in the order they were picked. */
internal val cardListSaver: Saver<SnapshotStateList<Card>, Any> = listSaver(
    save = { cards -> cards.map { it.toKey() } },
    restore = { keys -> mutableStateListOf<Card>().apply { addAll(keys.mapNotNull(::cardFromKey)) } }
)

/** The claim wizard's card → player assignments, as "SPADES|TWO=player_2" entries. */
internal val cardAssignmentsSaver: Saver<MutableMap<Card, String>, Any> = listSaver(
    save = { assignments -> assignments.map { (card, playerId) -> "${card.toKey()}=$playerId" } },
    restore = { entries ->
        val restored = mutableMapOf<Card, String>()
        entries.forEach { entry ->
            val split = entry.indexOf('=')
            if (split > 0) {
                cardFromKey(entry.substring(0, split))?.let { restored[it] = entry.substring(split + 1) }
            }
        }
        restored
    }
)
