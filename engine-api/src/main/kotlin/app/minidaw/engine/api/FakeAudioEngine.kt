package app.minidaw.engine.api

import app.minidaw.engine.api.snapshot.SampleHash
import app.minidaw.engine.api.snapshot.SnapshotCheck

/** A Sample that the fake engine holds. */
data class RegisteredSample(val path: String, val use: SampleUse)

/**
 * A fake of the engine for the tests on a host (architecture.md Section 12.1). It records every call, checks
 * each Snapshot with [SnapshotCheck], and returns the telemetry and the events that a test sets. It plays no
 * sound and is not thread-safe.
 */
class FakeAudioEngine : AudioEngine {
    /** The Live commands in the order of [send]. */
    val commands = mutableListOf<LiveCommand>()

    /** The Snapshots that the fake accepted, oldest first. */
    val snapshots = mutableListOf<ByteArray>()

    /** The Samples that are registered and not released. */
    val samples = mutableMapOf<SampleHash, RegisteredSample>()

    /** The Samples whose preview started. */
    val previews = mutableListOf<SampleHash>()

    /** The events that the next [readEvents] returns. */
    val pendingEvents = ArrayDeque<EngineEvent>()

    /** When set, the next [loadSnapshot] refuses with this result, as the engine does for checks 3 to 6. */
    var refuseNextSnapshot: EngineResult? = null

    /** The file of the running Take, or null. */
    var takePath: String? = null
        private set

    var streamsOpen: Boolean = false
        private set

    override var telemetry: Telemetry = Telemetry.IDLE

    override fun send(command: LiveCommand) {
        commands += command
    }

    override fun loadSnapshot(bytes: ByteArray): EngineResult {
        val result = refuseNextSnapshot ?: SnapshotCheck.check(bytes)
        refuseNextSnapshot = null
        if (result == EngineResult.OK) snapshots += bytes.copyOf()
        return result
    }

    override fun registerSample(hash: SampleHash, path: String, use: SampleUse): EngineResult {
        samples[hash] = RegisteredSample(path, use)
        return EngineResult.OK
    }

    override fun releaseSample(hash: SampleHash) {
        samples.remove(hash)
    }

    override fun startPreview(hash: SampleHash): EngineResult = if (hash in samples) {
        previews += hash
        EngineResult.OK
    } else {
        EngineResult.UNKNOWN_SAMPLE
    }

    override fun startTake(path: String): EngineResult = if (takePath == null) {
        takePath = path
        EngineResult.OK
    } else {
        EngineResult.BUSY
    }

    override fun stopTake(): EngineResult {
        takePath = null
        return EngineResult.OK
    }

    override fun openStreams(): EngineResult {
        streamsOpen = true
        return EngineResult.OK
    }

    override fun closeStreams() {
        streamsOpen = false
    }

    override fun readEvents(): List<EngineEvent> {
        val events = pendingEvents.toList()
        pendingEvents.clear()
        return events
    }
}
