package org.racedaydreams.domain.support

actual typealias DreamRandom = java.util.Random

actual object DreamRandoms {
    actual fun seeded(seed: Long): DreamRandom = java.util.Random(seed)
}
