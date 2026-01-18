package me.matsumo.travelog.core.common

import io.github.aakira.napier.Napier
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (exception: Throwable) {
    Napier.w(exception) { "Failed to evaluate a suspendRunCatchingBlock." }
    Result.failure(exception)
}
