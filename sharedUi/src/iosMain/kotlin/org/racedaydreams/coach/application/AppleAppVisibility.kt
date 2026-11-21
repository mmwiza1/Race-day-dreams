package org.racedaydreams.coach.application

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/**
 * Reports when the app moves to the background (false) and returns to the foreground (true), on the
 * main queue. The iOS counterpart of the Android lifecycle stop/start the gym workout screen uses
 * to checkpoint and pause its display clock. Call [close] when the screen leaves.
 */
internal class AppleAppVisibility(
    private val center: NSNotificationCenter = NSNotificationCenter.defaultCenter,
    queue: NSOperationQueue? = NSOperationQueue.mainQueue,
    onChange: (visible: Boolean) -> Unit,
) {
    private val observers =
        listOf(
            center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, queue) { _
                ->
                onChange(false)
            },
            center.addObserverForName(UIApplicationWillEnterForegroundNotification, null, queue) { _
                ->
                onChange(true)
            },
        )

    fun close() {
        for (observer in observers) center.removeObserver(observer)
    }
}
