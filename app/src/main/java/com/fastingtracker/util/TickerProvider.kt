package com.fastingtracker.util

import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Provides a ticker flow that emits every [intervalMs] milliseconds.
 * Inject this into ViewModels so tests can substitute a no-op implementation.
 */
interface TickerProvider {
    fun ticker(intervalMs: Long): Flow<Unit>
}

class DefaultTickerProvider @Inject constructor() : TickerProvider {
    override fun ticker(intervalMs: Long): Flow<Unit> = flow {
        while (true) {
            delay(intervalMs)
            emit(Unit)
        }
    }
}

/** No-op implementation for tests — never emits, so no infinite loop. */
class NoOpTickerProvider : TickerProvider {
    override fun ticker(intervalMs: Long): Flow<Unit> = flow { /* never emits */ }
}
