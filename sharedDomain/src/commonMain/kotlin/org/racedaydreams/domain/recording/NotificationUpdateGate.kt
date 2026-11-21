package org.racedaydreams.domain.recording

/**
 * Decides whether an ongoing notification must be posted again. The recorder asks on every tick and
 * GPS fix, but the system only needs a new post when what the notification shows changes, such as
 * Pause becoming Resume. Not thread-safe: use from the recorder's single owner thread.
 */
class NotificationUpdateGate<T : Any> {
    private var shown: T? = null

    /** True when [content] differs from what was last posted (or nothing has been posted). */
    fun shouldPost(content: T): Boolean = content != shown

    /** Records that [content] is now on screen, after a successful post or foreground start. */
    fun posted(content: T) {
        shown = content
    }

    /** Forget what is shown, so the next request posts again (for example after a dismissal). */
    fun invalidate() {
        shown = null
    }
}
