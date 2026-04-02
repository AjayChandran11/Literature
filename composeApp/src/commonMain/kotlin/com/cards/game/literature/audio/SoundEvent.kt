package com.cards.game.literature.audio

enum class SoundEvent(val resName: String) {
    ASK_SUCCESS("sound_ask_success"),
    ASK_FAIL("sound_ask_fail"),
    CARD_TAKEN("sound_card_taken"),
    TEAM_CLAIM_SUCCESS("sound_team_claim_success"),
    OPPONENT_CLAIM_SUCCESS("sound_opponent_claim_success"),
    CLAIM_FAIL("sound_claim_fail"),
    YOUR_TURN("sound_your_turn"),
    GAME_WIN("sound_game_win"),
    GAME_LOSE("sound_game_lose")
}
