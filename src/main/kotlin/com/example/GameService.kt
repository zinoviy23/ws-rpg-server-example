package com.example

import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

private data class Position(
    val x: Double,
    val y: Double,
) {
    fun hits(r: Double, other: Position): Boolean =
        (x - other.x) * (x - other.x) + (y - other.y) * (y - other.y) <= r * r

    fun shift(x: Double = 0.0, y: Double = 0.0): Position =
        copy(x = this.x + x, y = this.y + y)
}

private val START_POSITION = Position(0.0, 0.0)


private data class User(
    val userId: String,
    val hp: Long = 100L,
    val exp: Long = 0L,
    val position: Position = START_POSITION,
)

const val KILL_EXP = 100L

@Singleton
class GameService {
    private val users = ConcurrentHashMap<String, User>()

    fun handleEvent(message: ClientMessage): List<GameEvent> = when (message) {
        is UserAttack -> {
            val user = users[message.userId]
            requireNotNull(user)
            val r = message.attackType.radius
            val damage = message.attackType.damage

            val damaged = users.values
                .filter { it.userId != user.userId && it.hp > 0 && user.position.hits(r, it.position) }

            val events = mutableListOf<UserDamaged>()

            for (damagedUser in damaged) {
                val hp = damagedUser.hp - damage
                if (users.replace(damagedUser.userId, damagedUser, damagedUser.copy(hp = hp))) {
                    events += UserDamaged(damagedUser.userId, damage, hp, hp <= 0)
                }
            }


            val resultEvents = mutableListOf<GameEvent>()
            resultEvents += events
            if (events.isNotEmpty()) {
                val exp = events.count { it.userDied } * KILL_EXP
                if (exp > 0 && users.replace(user.userId, user, user.copy(exp = user.exp + exp))) {
                    resultEvents += ExperienceReceived(user.userId, exp, user.exp + exp)
                }

                if (users.values.count { it.hp > 0 } < 2) {
                    resultEvents += GameFinished
                }
            }

            resultEvents
        }

        is UserConnected -> {
            val user = User(message.userId)
            require((users.putIfAbsent(message.userId, user) == null)) { "Restart server or create new user" }
            val resultEvents = mutableListOf<GameEvent>()
            if (users.size == 1) {
                resultEvents += GameStarted
            }
            resultEvents += UserPosition(
                user.userId,
                Direction.Forward,
                user.position.x,
                user.position.y
            )
            resultEvents
        }
        is UserMovement -> {
            val user = users[message.userId]
            requireNotNull(user)
            require(user.hp > 0)

            val resultEvents = mutableListOf<GameEvent>()

            val delta = message.delta
            val newPosition = when (message.direction) {
                Direction.Forward -> user.position.shift(y = delta)
                Direction.Back -> user.position.shift(y = -delta)
                Direction.Left -> user.position.shift(x = -delta)
                Direction.Right -> user.position.shift(x = delta)
            }

            if (users.replace(user.userId, user, user.copy(position = newPosition))) {
                resultEvents += UserPosition(
                    user.userId,
                    message.direction,
                    newPosition.x,
                    newPosition.y,
                )
            }

            resultEvents
        }
    }
}