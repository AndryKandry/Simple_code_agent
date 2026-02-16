package ru.agent.common.extensions

fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
}
