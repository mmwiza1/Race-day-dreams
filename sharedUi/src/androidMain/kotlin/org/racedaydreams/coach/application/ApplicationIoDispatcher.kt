package org.racedaydreams.coach.application

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Preserve the established Android worker dispatcher. */
internal actual fun applicationIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
