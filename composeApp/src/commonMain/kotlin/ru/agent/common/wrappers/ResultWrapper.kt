package ru.agent.common.wrappers

sealed class ResultWrapper<out T> {

    data class Success<out T>(val value: T) : ResultWrapper<T>()

    data class Error(
        val throwable: Throwable,
        val statusCode: Int? = null,
        val message: String? = null
    ) : ResultWrapper<Nothing>()

    fun <T> ResultWrapper<T>.exceptionOrNull(): Error? =
        when (this) {
            is Error -> this
            else -> null
        }

    fun <T> ResultWrapper<T>.resultOrNull(): T? =
        when (this) {
            is Success -> this.value
            else -> null
        }
}