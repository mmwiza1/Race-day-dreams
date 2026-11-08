package org.racedaydreams.coach

import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.RunListener

/** CI-only identities and bounded stack frames: no training values or application log dump. */
class GateProgressListener : RunListener() {
    private data class Running(val identity: String, val started: Long, val thread: Thread)

    private val current = AtomicReference<Running?>()
    private val scheduler =
        Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "rdd-gate-diagnostics").apply { isDaemon = true }
        }
    private var diagnostic: ScheduledFuture<*>? = null

    override fun testStarted(description: Description) {
        diagnostic?.cancel(false)
        val identity = "${description.className}#${description.methodName?.substringBefore('[')}"
        val running = Running(identity, SystemClock.elapsedRealtime(), Thread.currentThread())
        current.set(running)
        Log.i("RddGate", "START $identity")
        var snapshots = 0
        diagnostic =
            scheduler.scheduleWithFixedDelay(
                {
                    if (current.get() === running && snapshots++ < 4) {
                        Log.i(
                            "RddGate",
                            "DIAGNOSTIC $identity after ${(SystemClock.elapsedRealtime()-running.started)/1000}s (not a test timeout)",
                        )
                        val main = Looper.getMainLooper().thread
                        Thread.getAllStackTraces()
                            .entries
                            .filter {
                                it.key === main ||
                                    it.key === running.thread ||
                                    it.key.name.contains("test", ignoreCase = true) ||
                                    it.key.name.contains("Instr")
                            }
                            .sortedBy {
                                if (it.key === main) 0 else if (it.key === running.thread) 1 else 2
                            }
                            .take(6)
                            .forEach { (thread, frames) ->
                                Log.i("RddGate", "THREAD ${thread.name.take(100)} ${thread.state}")
                                frames.take(24).forEach { Log.i("RddGate", "  at $it") }
                            }
                    }
                },
                120,
                120,
                TimeUnit.SECONDS,
            )
    }

    override fun testFinished(description: Description) {
        val running = current.getAndSet(null)
        diagnostic?.cancel(false)
        if (running != null)
            Log.i(
                "RddGate",
                "END ${running.identity} ${(SystemClock.elapsedRealtime()-running.started)/1000}s",
            )
    }

    override fun testRunFinished(result: Result) {
        current.set(null)
        diagnostic?.cancel(false)
        scheduler.shutdownNow()
    }
}
