package com.appdist.core.common.ext

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.appdist.core.common.AppError
import com.appdist.core.common.Result

fun <T> Flow<T>.asResult(): Flow<Result<T>> = map<T, Result<T>> { Result.Success(it) }
    .catch { e ->
        if (e is CancellationException) throw e
        emit(Result.Error(AppError.Unknown(e.message ?: "Flow error")))
    }
