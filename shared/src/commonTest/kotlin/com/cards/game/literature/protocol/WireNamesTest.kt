package com.cards.game.literature.protocol

import com.cards.game.literature.model.GameEvent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.elementDescriptors
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The wire's `type` values, pinned. Every message and event on the socket is identified by a
 * string that used to be whatever the class happened to be called; a rename or a move between
 * packages would have changed it silently and left older clients unable to read the server.
 * The names are now written on each class, and this test is the list a rename has to answer to.
 */
@OptIn(ExperimentalSerializationApi::class)
class WireNamesTest {

    private fun wireNames(serializer: KSerializer<*>): Set<String> =
        // A sealed class serializes as {"type": ..., "value": ...}; the subclasses hang off "value".
        serializer.descriptor.getElementDescriptor(1).elementDescriptors.map { it.serialName }.toSet()

    @Test
    fun clientMessagesKeepTheirWireNames() {
        val p = "com.cards.game.literature.protocol.ClientMessage"
        assertEquals(
            setOf(
                "$p.CreateRoom", "$p.JoinRoom", "$p.StartGame", "$p.AskCards", "$p.ClaimDeck",
                "$p.SelectPassTarget", "$p.LeaveRoom", "$p.Reconnect", "$p.LeaveGame", "$p.SwitchTeam",
                "$p.SendReaction", "$p.Rematch",
            ),
            wireNames(ClientMessage.serializer()),
        )
    }

    @Test
    fun serverMessagesKeepTheirWireNames() {
        val p = "com.cards.game.literature.protocol.ServerMessage"
        assertEquals(
            setOf(
                "$p.RoomCreated", "$p.RoomUpdate", "$p.GameStarted", "$p.GameUpdate", "$p.GameEventOccurred",
                "$p.Error", "$p.RoomClosed", "$p.HostTransferred", "$p.ReactionReceived", "$p.RematchStarted",
            ),
            wireNames(ServerMessage.serializer()),
        )
    }

    @Test
    fun gameEventsKeepTheirWireNames() {
        val p = "com.cards.game.literature.model.GameEvent"
        assertEquals(
            setOf(
                "$p.CardAsked", "$p.DeckClaimed", "$p.TurnChanged", "$p.GameStarted", "$p.GameEnded",
                "$p.TurnTimedOut", "$p.PlayerDisconnected", "$p.PlayerReconnected", "$p.PlayerReplacedByBot",
            ),
            wireNames(GameEvent.serializer()),
        )
    }
}
