package com.example

import io.micronaut.http.annotation.Body
import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketBroadcaster
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.OnMessage
import io.micronaut.websocket.annotation.OnOpen
import io.micronaut.websocket.annotation.ServerWebSocket

@ServerWebSocket("/ws-rpg-game")
class GameController(
    private val broadcaster: WebSocketBroadcaster,
    private val gameService: GameService,
) {
    @OnOpen
    fun onOpen() {
    }

    @OnMessage
    fun onMessage(@Body message: ClientMessage, session: WebSocketSession) {
        for (gameEvent in gameService.handleEvent(message)) {
            session.sendSync(gameEvent)
            broadcaster.broadcastSync(gameEvent) { it != session }

            if (gameEvent == GameFinished) {
                session.close(CloseReason.NORMAL)
            }
        }
    }
}