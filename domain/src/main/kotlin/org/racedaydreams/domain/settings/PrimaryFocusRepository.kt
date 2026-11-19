package org.racedaydreams.domain.settings

import java.util.Optional

/** Original JVM preference boundary; never creates, deletes or changes a training programme. */
interface PrimaryFocusRepository {
    fun load(): Optional<PrimaryFocus>

    fun save(focus: PrimaryFocus)
}
