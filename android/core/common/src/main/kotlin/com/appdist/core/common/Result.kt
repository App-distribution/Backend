package com.appdist.core.common

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val error: AppError) : Result<T>()

    fun getOrNull(): T? = if (this is Success) data else null

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(error)
    }

    val isSuccess get() = this is Success
    val isError get() = this is Error
}

inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e.toAppError())
}
