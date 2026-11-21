package org.racedaydreams.coach.application

import kotlin.test.Test
import kotlin.test.assertEquals
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/** The iOS gym screen checkpoints on backgrounding through these app visibility changes. */
class AppleAppVisibilityTest {
    @Test
    fun backgroundAndForegroundAreReportedUntilClosed() {
        val center = NSNotificationCenter()
        val seen = mutableListOf<Boolean>()
        val visibility = AppleAppVisibility(center, queue = null) { seen += it }
        center.postNotificationName(UIApplicationDidEnterBackgroundNotification, null)
        center.postNotificationName(UIApplicationWillEnterForegroundNotification, null)
        assertEquals(listOf(false, true), seen)
        visibility.close()
        center.postNotificationName(UIApplicationDidEnterBackgroundNotification, null)
        assertEquals(listOf(false, true), seen, "no callbacks after the screen leaves")
    }
}
