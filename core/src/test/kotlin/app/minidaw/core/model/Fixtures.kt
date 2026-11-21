package app.minidaw.core.model

import app.minidaw.engine.api.snapshot.SampleHash
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.fail

/** States and steps that several tests share. */
object Fixtures {
    val KICK = UsedSample(SampleHash("11".repeat(SampleHash.BYTES)), name = "Kick", frames = 24_000, channels = 1)
    val LOOP = UsedSample(SampleHash("22".repeat(SampleHash.BYTES)), name = "Loop", frames = 480_000, channels = 2)
    private val NOW: Instant = Instant.parse("2026-11-20T07:55:00Z")

    /** The new Project of feature_specifications.md Section 1.7, with the Kick as default Sampler instrument. */
    fun newProject(): Project = NewProject.create(
        id = "0123456789abcdef0123456789abcdef",
        now = NOW,
        zone = ZoneOffset.UTC,
        instrument = NewProject.DefaultInstrument(KICK, name = "Kick", rootNote = 60)
    ).let { it.copy(samples = it.samples + LOOP) }

    /** Applies the Edit and fails the test when the Reducer refuses it. */
    fun Project.edit(edit: Edit, limits: Limits = Limits.MVP): Project =
        when (val result = Reducer.reduce(this, edit, limits)) {
            is EditResult.Applied -> result.state
            is EditResult.Refused -> fail("refused $edit: ${result.refusal}")
        }

    /** Asserts that the Reducer refuses the Edit for the reason and leaves the state unchanged. */
    fun Project.refuses(edit: Edit, reason: Reason, limits: Limits = Limits.MVP) {
        val result = Reducer.reduce(this, edit, limits)
        assertIs<EditResult.Refused>(result, "the Edit $edit was applied")
        assertEquals(reason, result.refusal.reason, result.refusal.detail)
    }
}
