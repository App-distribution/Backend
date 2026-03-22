package com.appdist.core.common

import java.net.SocketException
import java.net.UnknownHostException

sealed interface AppError {
    data class Network(val code: Int?, val message: String) : AppError
    data object Unauthorized : AppError
    data object NoInternet : AppError
    data class ChecksumMismatch(val expected: String, val actual: String) : AppError
    data class IncompatibleDevice(val reason: String) : AppError
    data class SignatureMismatch(val installed: String, val available: String) : AppError
    data class Storage(val message: String) : AppError
    data class Unknown(val message: String) : AppError
}

fun Exception.toAppError(): AppError = when (this) {
    is UnknownHostException, is SocketException -> AppError.NoInternet
    else -> AppError.Unknown(message ?: "Unknown error")
}
