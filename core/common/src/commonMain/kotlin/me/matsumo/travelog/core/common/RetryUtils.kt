package me.matsumo.travelog.core.common

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

/**
 * Retry utility with exponential backoff and optional jitter.
 *
 * @param maxRetries Maximum number of retry attempts (not counting the initial attempt).
 * @param initialDelayMs Initial delay in milliseconds before the first retry.
 * @param maxDelayMs Maximum delay in milliseconds between retries.
 * @param factor Multiplier for exponential backoff (delay *= factor on each retry).
 * @param jitter Add random jitter to delay (up to 20% of current delay).
 * @param retryIf Predicate to determine if a retry should be attempted based on the exception.
 * @param block The suspend function to execute with retry logic.
 * @return The result of the block if successful.
 * @throws Throwable The last exception if all retries fail or if retryIf returns false.
 */
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000L,
    maxDelayMs: Long = 10000L,
    factor: Double = 2.0,
    jitter: Boolean = true,
    retryIf: (Throwable) -> Boolean = { true },
    block: suspend () -> T,
): T {
    var currentDelay = initialDelayMs
    var lastException: Throwable? = null

    repeat(maxRetries + 1) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e

            if (attempt >= maxRetries || !retryIf(e)) {
                throw e
            }

            val delayWithJitter = if (jitter) {
                val jitterAmount = (currentDelay * 0.2 * Random.nextDouble()).toLong()
                currentDelay + jitterAmount
            } else {
                currentDelay
            }

            delay(delayWithJitter)

            currentDelay = min((currentDelay * factor).toLong(), maxDelayMs)
        }
    }

    throw lastException ?: error("Unexpected state in retryWithBackoff")
}
