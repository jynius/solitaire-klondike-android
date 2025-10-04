package us.jyni.game.klondike.util.sync

import java.io.Serializable

enum class RecycleOrder : Serializable { KEEP, REVERSE }

data class Ruleset(
    val draw: Int = 1,           // 1-draw or 3-draw
    val redeals: Int = -1,       // -1 = unlimited
    val recycle: RecycleOrder = RecycleOrder.REVERSE,
    val allowFoundationToTableau: Boolean = true
) : Serializable
