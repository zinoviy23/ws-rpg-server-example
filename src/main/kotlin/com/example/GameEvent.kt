package com.example

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed interface GameEvent

@JsonTypeName("game_started")
object GameStarted : GameEvent

@JsonTypeName("game_finished")
object GameFinished : GameEvent

sealed interface UserEvent : GameEvent {
    val userId: String
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed interface ClientMessage

@JsonTypeName("user_connected")
data class UserConnected(override val userId: String): UserEvent, ClientMessage

enum class Direction {
    Forward,
    Back,
    Left,
    Right,
}

@JsonTypeName("user_movement")
data class UserMovement(
    override val userId: String,
    val direction: Direction,
    val delta: Double,
) : UserEvent, ClientMessage

@JsonTypeName("user_position")
data class UserPosition(
    override val userId: String,
    val direction: Direction,
    val x: Double,
    val y: Double,
) : UserEvent

enum class AttackType {
    Force,
    Fast;

    val radius: Double
        get() = when (this) {
            Force -> 5.0
            Fast -> 2.0
        }

    val damage: Long
        get() = when (this) {
            Force -> 20
            Fast -> 10
        }
}

@JsonTypeName("user_attack")
data class UserAttack(
    override val userId: String,
    val attackType: AttackType
) : UserEvent, ClientMessage

@JsonTypeName("user_damaged")
data class UserDamaged(
    override val userId: String,
    val damage: Long,
    val resultHP: Long,
    val userDied: Boolean
) : UserEvent

@JsonTypeName("experience_received")
data class ExperienceReceived(
    override val userId: String,
    val expDelta: Long,
    val resultExp: Long
) : UserEvent


