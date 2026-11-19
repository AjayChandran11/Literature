package com.cards.game.literature.ui.common

import androidx.compose.runtime.Composable
import com.cards.game.literature.model.HalfSuit
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The half-suit's name in the app's language. [HalfSuit.displayName] is the engine's English and
 * stays that way (the server logs it, tests pin it); every screen shows this instead.
 */
@Composable
fun HalfSuit.localizedName(): String = stringResource(
    when (this) {
        HalfSuit.SPADES_LOW -> Res.string.half_suit_low_spades
        HalfSuit.SPADES_HIGH -> Res.string.half_suit_high_spades
        HalfSuit.HEARTS_LOW -> Res.string.half_suit_low_hearts
        HalfSuit.HEARTS_HIGH -> Res.string.half_suit_high_hearts
        HalfSuit.DIAMONDS_LOW -> Res.string.half_suit_low_diamonds
        HalfSuit.DIAMONDS_HIGH -> Res.string.half_suit_high_diamonds
        HalfSuit.CLUBS_LOW -> Res.string.half_suit_low_clubs
        HalfSuit.CLUBS_HIGH -> Res.string.half_suit_high_clubs
    }
)

/** Every half-suit's name at once, for text that is formatted outside composition (the event strip). */
@Composable
fun halfSuitNames(): Map<HalfSuit, String> = HalfSuit.entries.associateWith { it.localizedName() }
