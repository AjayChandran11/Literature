package com.cards.game.literature.server

import com.cards.game.literature.protocol.ClientMessage
import com.cards.game.literature.protocol.ServerMessage
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("GameWebSocket")

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = false
}

fun Routing.gameWebSocket(roomManager: RoomManager) {
    webSocket("/game") {
        var currentRoom: GameRoom? = null
        var currentPlayerId: String? = null

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val message = try {
                        json.decodeFromString<ClientMessage>(text)
                    } catch (e: Exception) {
                        log.warn("Invalid message from {}: {}", currentPlayerId ?: "unknown", e.message)
                        sendError("Invalid message format: ${e.message}")
                        continue
                    }

                    when (message) {
                        is ClientMessage.CreateRoom -> {
                            if (currentRoom != null) {
                                sendError("Already in a room")
                                continue
                            }
                            val name = message.playerName.trim().take(20)
                            if (name.isBlank()) {
                                sendError("Player name cannot be empty")
                                continue
                            }
                            if (message.playerCount !in listOf(4, 6, 8)) {
                                sendError("Invalid player count")
                                continue
                            }
                            val (room, playerId) = roomManager.createRoom(
                                name,
                                message.playerCount
                            )
                            currentRoom = room
                            currentPlayerId = playerId

                            room.getPlayerSession(playerId)?.session = this

                            sendMessage(ServerMessage.RoomCreated(room.roomCode, playerId))
                            room.broadcastRoomUpdate()
                        }

                        is ClientMessage.JoinRoom -> {
                            if (currentRoom != null) {
                                sendError("Already in a room")
                                continue
                            }
                            val joinName = message.playerName.trim().take(20)
                            if (joinName.isBlank()) {
                                sendError("Player name cannot be empty")
                                continue
                            }
                            val room = roomManager.getRoom(message.roomCode)
                            if (room == null) {
                                sendError("Room not found")
                                continue
                            }
                            if (room.getHumanPlayerCount() >= room.targetPlayerCount) {
                                sendError("Room is full")
                                continue
                            }

                            val playerId = room.addPlayer(joinName)
                            currentRoom = room
                            currentPlayerId = playerId

                            room.getPlayerSession(playerId)?.session = this

                            sendMessage(ServerMessage.RoomCreated(room.roomCode, playerId))
                            room.broadcastRoomUpdate()
                        }

                        is ClientMessage.StartGame -> {
                            val room = currentRoom
                            val playerId = currentPlayerId
                            if (room == null || playerId == null) {
                                sendError("Not in a room")
                                continue
                            }
                            if (!room.isHost(playerId)) {
                                sendError("Only the host can start the game")
                                continue
                            }
                            val started = room.startGame(message.fillWithBots)
                            if (!started) {
                                sendError("Cannot start game")
                            }
                        }

                        is ClientMessage.AskCards -> {
                            val room = currentRoom
                            val playerId = currentPlayerId
                            if (room == null || playerId == null) {
                                sendError("Not in a room")
                                continue
                            }
                            try {
                                room.processAsk(playerId, message.targetId, message.cards)
                            } catch (e: Exception) {
                                log.warn("[{}] Ask failed for {}: {}", room.roomCode, playerId, e.message)
                                sendError(e.message ?: "Ask failed")
                            }
                        }

                        is ClientMessage.ClaimDeck -> {
                            val room = currentRoom
                            val playerId = currentPlayerId
                            if (room == null || playerId == null) {
                                sendError("Not in a room")
                                continue
                            }
                            try {
                                room.processClaim(playerId, message.declaration)
                            } catch (e: Exception) {
                                log.warn("[{}] Claim failed for {}: {}", room.roomCode, playerId, e.message)
                                sendError(e.message ?: "Claim failed")
                            }
                        }

                        is ClientMessage.LeaveRoom -> {
                            val room = currentRoom
                            val playerId = currentPlayerId
                            if (room != null && playerId != null) {
                                room.removePlayer(playerId)
                                room.broadcastRoomUpdate()
                                if (room.allDisconnected() && room.getHumanPlayerCount() == 0) {
                                    roomManager.removeRoom(room.roomCode)
                                }
                            }
                            currentRoom = null
                            currentPlayerId = null
                        }

                        is ClientMessage.Reconnect -> {
                            val room = roomManager.getRoom(message.roomCode)
                            if (room == null) {
                                sendError("Room not found")
                                continue
                            }
                            val session = room.getPlayerSession(message.playerId)
                            if (session == null) {
                                sendError("Player not found in room")
                                continue
                            }
                            session.session = this
                            currentRoom = room
                            currentPlayerId = message.playerId
                            room.handleReconnect(message.playerId)
                        }

                        is ClientMessage.SwitchTeam -> {
                            val room = currentRoom
                            val playerId = currentPlayerId
                            if (room == null || playerId == null) {
                                sendError("Not in a room")
                                continue
                            }
                            if (room.phase != com.cards.game.literature.protocol.RoomPhase.WAITING) {
                                sendError("Cannot switch teams after game has started")
                                continue
                            }
                            room.switchTeam(playerId)
                            room.broadcastRoomUpdate()
                        }

                        is ClientMessage.LeaveGame -> {
                            val room = currentRoom
                            val playerId = currentPlayerId
                            if (room != null && playerId != null) {
                                room.handleIntentionalLeave(playerId)
                            }
                            currentRoom = null
                            currentPlayerId = null
                            close(CloseReason(CloseReason.Codes.NORMAL, "Player left game"))
                        }
                    }
                }
            }
        } finally {
            // Connection closed
            val room = currentRoom
            val playerId = currentPlayerId
            log.info("WebSocket closed for player {}", playerId ?: "unknown")
            if (room != null && playerId != null) {
                room.handleDisconnect(playerId)
                if (room.phase == com.cards.game.literature.protocol.RoomPhase.WAITING) {
                    room.broadcastRoomUpdate()
                }
            }
        }
    }
}

private suspend fun WebSocketSession.sendMessage(message: ServerMessage) {
    send(Frame.Text(json.encodeToString(message)))
}

private suspend fun WebSocketSession.sendError(message: String) {
    sendMessage(ServerMessage.Error(message))
}
