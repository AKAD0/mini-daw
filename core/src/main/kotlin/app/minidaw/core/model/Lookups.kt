package app.minidaw.core.model

// Shared steps of the Edits. Each lookup refuses the Edit when its item does not exist.

internal fun Project.track(id: Int): Track =
    tracks.firstOrNull { it.id == id } ?: refuse(Reason.NOT_FOUND, "Track $id does not exist")

internal fun Project.pattern(id: Int): Pattern =
    patterns.firstOrNull { it.id == id } ?: refuse(Reason.NOT_FOUND, "Pattern $id does not exist")

internal fun Project.instrument(id: Int): SamplerInstrument =
    instruments.firstOrNull { it.id == id } ?: refuse(Reason.NOT_FOUND, "Sampler instrument $id does not exist")

internal fun Project.withTrack(track: Track): Project =
    copy(tracks = tracks.map { if (it.id == track.id) track else it })

internal fun Project.withPattern(pattern: Pattern): Project =
    copy(patterns = patterns.map { if (it.id == pattern.id) pattern else it })

/** The default name `"$base N"` with the smallest N from 1 that no item uses yet (ui_ux_specification.md 14.7). */
internal fun defaultName(base: String, taken: Collection<String>): String =
    generateSequence(1) { it + 1 }.map { "$base $it" }.first { it !in taken }

/** A new item takes the color after the color of the last item of its kind (ui_ux_specification.md 1.4). */
internal fun nextColor(colors: List<Int>): Int = if (colors.isEmpty()) 0 else (colors.last() + 1) % Limits.COLORS

internal fun checkName(name: String) {
    if (name.isBlank() || name.length > Limits.NAME_LENGTH) {
        refuse(Reason.RANGE, "a name has 1 to ${Limits.NAME_LENGTH} characters")
    }
}

internal fun checkColor(color: Int) {
    if (color !in 0 until Limits.COLORS) refuse(Reason.RANGE, "a color is a number from 0 to ${Limits.COLORS - 1}")
}
