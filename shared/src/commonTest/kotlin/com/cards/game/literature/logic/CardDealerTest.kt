package com.cards.game.literature.logic

import kotlin.test.Test
import kotlin.test.assertEquals

class CardDealerTest {
    @Test
    fun dealTo4PlayersGives12Each() {
        val hands = CardDealer.dealCards(4)
        assertEquals(4, hands.size)
        hands.forEach { assertEquals(12, it.size) }
    }

    @Test
    fun dealTo6PlayersGives8Each() {
        val hands = CardDealer.dealCards(6)
        assertEquals(6, hands.size)
        hands.forEach { assertEquals(8, it.size) }
    }

    @Test
    fun dealTo8PlayersGives6Each() {
        val hands = CardDealer.dealCards(8)
        assertEquals(8, hands.size)
        hands.forEach { assertEquals(6, it.size) }
    }

    @Test
    fun allCardsDealt() {
        val hands = CardDealer.dealCards(6)
        val allCards = hands.flatten().toSet()
        assertEquals(48, allCards.size)
    }
}
