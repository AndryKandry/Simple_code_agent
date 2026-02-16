package ru.agent.core.handlers

import ru.agent.common.wrappers.ResultWrapper

interface NetworkErrorHandling {
    fun transformToResultWrapper(throwable: Throwable): ResultWrapper<Nothing>
}