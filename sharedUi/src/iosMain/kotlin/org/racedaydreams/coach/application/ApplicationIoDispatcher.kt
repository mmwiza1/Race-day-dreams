package org.racedaydreams.coach.application

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** Native IO is a platform extension, not a common Dispatchers member. */
internal actual fun applicationIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
