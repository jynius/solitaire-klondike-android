package us.jyni.game.klondike.util

fun Int.toFormattedString(): String {
    return String.format("%02d", this)
}

fun String.isNotEmptyOrBlank(): Boolean {
    return this.isNotEmpty() && this.isNotBlank()
}

fun <T> List<T>.shuffle(): List<T> {
    return this.shuffled()
}