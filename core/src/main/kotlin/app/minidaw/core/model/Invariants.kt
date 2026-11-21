package app.minidaw.core.model

/**
 * The invariants of every state (architecture.md Section 4.2): references, routing without a loop, limits, one
 * Part per Sampler instrument, ranges, sorted lists and unique IDs. [check] returns one text per broken rule.
 * Two Clips of one Track may overlap after a change of the tempo (D-006); the Edits of Clips check overlaps.
 */
object Invariants {
    fun check(state: Project, limits: Limits): List<String> = buildList {
        references(state)
        routing(state)
        counts(state, limits)
        patterns(state)
        tracks(state)
        values(state)
        ids(state)
    }

    private fun MutableList<String>.expect(ok: Boolean, problem: () -> String) {
        if (!ok) add(problem())
    }

    private fun MutableList<String>.references(state: Project) {
        val channels = state.mixer.channels.map { it.id }.toSet()
        val patterns = state.patterns.map { it.id }.toSet()
        val instruments = state.instruments.map { it.id }.toSet()
        val samples = state.samples.map { it.hash }.toSet()
        for (track in state.tracks) {
            expect(track.channel in channels) { "Track ${track.id} sends to a missing Mixer channel" }
            for (clip in track.clips) {
                when (clip) {
                    is PatternClip -> expect(clip.pattern in patterns) { "Clip ${clip.id} plays a missing Pattern" }
                    is AudioClip -> expect(clip.sample in samples) { "Clip ${clip.id} plays an unlisted Sample" }
                }
            }
        }
        for (pattern in state.patterns) {
            for (part in pattern.parts) {
                expect(part.instrument in instruments) { "Pattern ${pattern.id} has a Part of a missing instrument" }
            }
        }
        for (channel in state.mixer.channels) {
            expect(channel.target == null || channel.target in channels) { "Channel ${channel.id} sends nowhere" }
        }
        for (instrument in state.instruments) {
            expect(instrument.sample in samples) { "Instrument ${instrument.id} plays an unlisted Sample" }
            expect(instrument.channel == null || instrument.channel in channels) {
                "Instrument ${instrument.id} has a missing own channel"
            }
        }
    }

    private fun MutableList<String>.routing(state: Project) {
        val targets = state.mixer.channels.associate { it.id to it.target }
        for (start in targets.keys) {
            var current: Int? = start
            var steps = 0
            while (current != null && steps <= targets.size) {
                current = targets[current]
                steps++
            }
            expect(current == null) { "The routing of channel $start has a loop" }
        }
    }

    private fun MutableList<String>.counts(state: Project, limits: Limits) {
        expect(state.tracks.size <= limits.tracks) { "More than ${limits.tracks} Tracks" }
        expect(state.mixer.channels.size <= limits.channels) { "More than ${limits.channels} Mixer channels" }
        val effects = state.mixer.channels.sumOf { c -> c.slots.count { it != null } } +
            state.mixer.master.slots.count { it != null }
        expect(effects <= limits.activeEffects) { "More than ${limits.activeEffects} effects" }
        for (pattern in state.patterns) {
            expect(pattern.parts.size <= limits.instrumentsPerPattern) {
                "Pattern ${pattern.id} has more than ${limits.instrumentsPerPattern} Sampler instruments"
            }
            for (part in pattern.parts) {
                expect(part.notes.size <= Limits.NOTES_PER_PART) {
                    "A Part of Pattern ${pattern.id} has more than ${Limits.NOTES_PER_PART} Notes"
                }
            }
        }
    }

    private fun MutableList<String>.patterns(state: Project) {
        val bar = TimeBase.barTicks(state.timeSignature)
        for (pattern in state.patterns) {
            expect(pattern.length in bar * Limits.MIN_PATTERN_BARS..bar * Limits.MAX_PATTERN_BARS) {
                "Pattern ${pattern.id} has a length outside 1 to 256 bars"
            }
            expect(pattern.parts.map { it.instrument }.toSet().size == pattern.parts.size) {
                "Pattern ${pattern.id} has two Parts of one Sampler instrument"
            }
            for (part in pattern.parts) {
                expect(NoteRules.sortedAndUnique(part.notes)) {
                    "The Notes of a Part of Pattern ${pattern.id} are not sorted or not unique"
                }
                expect(part.notes.all { NoteRules.inRange(it) && it.start + it.length <= pattern.length }) {
                    "A Note of Pattern ${pattern.id} lies outside its ranges or behind the end"
                }
            }
        }
    }

    private fun MutableList<String>.tracks(state: Project) {
        val patternLengths = state.patterns.associate { it.id to it.length }
        val frames = state.samples.associate { it.hash to it.frames }
        for (track in state.tracks) {
            expect(track.clips.zipWithNext().all { (a, b) -> a.start <= b.start }) {
                "The Clips of Track ${track.id} are not sorted by start"
            }
            for (clip in track.clips) {
                val ok = when (clip) {
                    is AudioClip -> clip.offset >= 0 && clip.length >= Limits.MIN_AUDIO_FRAMES &&
                        clip.offset.toLong() + clip.length <= (frames[clip.sample] ?: 0)
                    is PatternClip -> clip.length > 0 && clip.offset in 0 until (patternLengths[clip.pattern] ?: 0)
                }
                expect(ok && clip.start >= 0 && clip.gainDb in Limits.MIN_CLIP_DB..Limits.MAX_CLIP_DB) {
                    "Clip ${clip.id} lies outside its ranges"
                }
            }
        }
    }

    private fun MutableList<String>.values(state: Project) {
        expect(state.tempoMilliBpm in TimeBase.MIN_TEMPO_MILLI_BPM..TimeBase.MAX_TEMPO_MILLI_BPM) {
            "The tempo lies outside 30 to 300 BPM"
        }
        val named = state.tracks.map { it.name to it.color } + state.patterns.map { it.name to it.color } +
            state.instruments.map { it.name to it.color } + state.mixer.channels.map { it.name to it.color }
        expect(named.all { (name, color) -> name.length <= Limits.NAME_LENGTH && color in 0 until Limits.COLORS }) {
            "A name is longer than 40 characters, or a color lies outside the palette"
        }
        val levels = state.mixer.channels.map { it.volumeDb } + state.instruments.map { it.volumeDb } +
            state.mixer.master.volumeDb
        expect(levels.all { it in Limits.MIN_DB..Limits.MAX_DB }) { "A volume lies outside −90 to +6 dB" }
        val pans = state.mixer.channels.map { it.pan } + state.instruments.map { it.pan }
        expect(pans.all { it in -1f..1f }) { "A pan lies outside −1 to +1" }
        expect(state.instruments.all { it.rootNote in 0..Limits.MIDI_MAX }) { "A Root note lies outside 0 to 127" }
    }

    private fun MutableList<String>.ids(state: Project) {
        val all = state.tracks.map { it.id } + state.tracks.flatMap { t -> t.clips.map { it.id } } +
            state.patterns.map { it.id } + state.instruments.map { it.id } + state.mixer.channels.map { it.id } +
            state.mixer.channels.flatMap { c -> c.slots.mapNotNull { it?.id } } +
            state.mixer.master.slots.mapNotNull { it?.id }
        expect(all.toSet().size == all.size) { "Two items share an ID" }
        expect(all.all { it in 1 until state.nextId }) { "An ID lies outside 1 to nextId − 1" }
    }
}

/** The ranges of a Note (FR-PT-03, feature_specifications.md Section 1.5). */
object NoteRules {
    fun inRange(note: Note): Boolean {
        val time = note.start >= 0 && note.length >= Limits.MIN_NOTE_TICKS
        return time && note.pitch in 0..Limits.MIDI_MAX && note.velocity in 1..Limits.MIDI_MAX
    }

    /** Notes sorted by start and then by pitch, without two at one start and pitch. */
    fun sortedAndUnique(notes: List<Note>): Boolean =
        notes.zipWithNext().all { (a, b) -> a.start < b.start || a.start == b.start && a.pitch < b.pitch }
}
