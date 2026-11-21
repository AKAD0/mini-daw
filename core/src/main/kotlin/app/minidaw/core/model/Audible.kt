package app.minidaw.core.model

/**
 * The flags "audible" of feature_specifications.md Section 3.2.3 (D-083). The compiler writes them into the
 * Snapshot. Mute and solo act after the fader; the Master channel has neither.
 */
object Audible {
    /**
     * `audible(c) = not muted(c) and (not anySolo or onSoloPath(c))`. A channel is on the solo path when it is
     * soloed, when it sends into a soloed channel (directly or through others), or when a soloed channel sends
     * through it. In the MVP every channel sends to the Master channel, so the path is the solo alone.
     */
    fun channels(mixer: Mixer): Map<Int, Boolean> {
        val targets = mixer.channels.associate { it.id to it.target }
        val soloed = mixer.channels.filter { it.solo }.map { it.id }.toSet()
        val downstream = mixer.channels.associate { it.id to path(it.id, targets) }
        return mixer.channels.associate { c ->
            val onPath = c.id in soloed ||
                downstream.getValue(c.id).any { it in soloed } ||
                soloed.any { s -> c.id in downstream.getValue(s) }
            c.id to (!c.mute && (soloed.isEmpty() || onPath))
        }
    }

    /** The mute of a Track acts on its sources. P1: its solo works among Tracks like the rule of the MVP. */
    fun tracks(tracks: List<Track>): Map<Int, Boolean> {
        val anySolo = tracks.any { it.solo }
        return tracks.associate { it.id to (!it.mute && (!anySolo || it.solo)) }
    }

    /** The mute of a Sampler instrument acts in every Pattern (FR-CR-04). P1: its solo among Sampler instruments. */
    fun instruments(instruments: List<SamplerInstrument>): Map<Int, Boolean> {
        val anySolo = instruments.any { it.solo }
        return instruments.associate { it.id to (!it.mute && (!anySolo || it.solo)) }
    }

    /** The channels after `start` on its way to the Master channel. The routing has no loop (Section 3.2.5). */
    private fun path(start: Int, targets: Map<Int, Int?>): List<Int> =
        generateSequence(targets[start]) { targets[it] }.take(targets.size).toList()
}
