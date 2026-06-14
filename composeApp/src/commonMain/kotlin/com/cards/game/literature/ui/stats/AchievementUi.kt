package com.cards.game.literature.ui.stats

import com.cards.game.literature.stats.Achievement
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/** Display metadata for an achievement; the engine only knows IDs. */
data class AchievementUi(
    val emoji: String,
    val title: StringResource,
    val description: StringResource
)

val Achievement.ui: AchievementUi
    get() = when (this) {
        Achievement.FIRST_WIN -> AchievementUi("🏆", Res.string.ach_first_win, Res.string.ach_first_win_desc)
        Achievement.HAT_TRICK -> AchievementUi("🎩", Res.string.ach_hat_trick, Res.string.ach_hat_trick_desc)
        Achievement.ON_FIRE -> AchievementUi("🔥", Res.string.ach_on_fire, Res.string.ach_on_fire_desc)
        Achievement.VETERAN -> AchievementUi("🎖️", Res.string.ach_veteran, Res.string.ach_veteran_desc)
        Achievement.CENTURION -> AchievementUi("💯", Res.string.ach_centurion, Res.string.ach_centurion_desc)
        Achievement.SOCIALITE -> AchievementUi("🌐", Res.string.ach_socialite, Res.string.ach_socialite_desc)
        Achievement.ONLINE_CHAMP -> AchievementUi("👑", Res.string.ach_online_champ, Res.string.ach_online_champ_desc)
        Achievement.BOT_SLAYER -> AchievementUi("🤖", Res.string.ach_bot_slayer, Res.string.ach_bot_slayer_desc)
        Achievement.SHARP_CALLER -> AchievementUi("🧠", Res.string.ach_sharp_caller, Res.string.ach_sharp_caller_desc)
        Achievement.PERFECT_GAME -> AchievementUi("✨", Res.string.ach_perfect_game, Res.string.ach_perfect_game_desc)
        Achievement.CLAIM_MASTER -> AchievementUi("🃏", Res.string.ach_claim_master, Res.string.ach_claim_master_desc)
    }
