package org.racedaydreams.coach

import androidx.compose.runtime.Composable

/** Visual only; the authoritative recorder still owns hysteresis and evidence qualification. */
@Composable
fun PaceRange(fast: Double, slow: Double, current: Double?, imperial: Boolean) =
    SharedPaceRange(fast, slow, current, imperial, ::formatPaceSeconds)
