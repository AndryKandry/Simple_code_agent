package ru.agent.core.handlers

import ru.agent.common.wrappers.ResultWrapper

internal class NetworkErrorHandlingImpl() : NetworkErrorHandling {

    override fun transformToResultWrapper(throwable: Throwable) =
        ResultWrapper.Error(
            throwable = throwable,
            message = throwable.message
        )
}

//= when (throwable) {
//        is HttpException -> {
//            val (code, message) = throwable.toError(gson)
//            ResultWrapper.Error(
//                throwable = throwable,
//                statusCode = code,
//                message = message
//            )
//        }
//        else -> {
//            ResultWrapper.Error(
//                throwable = throwable,
//                message = throwable.message
//            )
//        }
//    }
