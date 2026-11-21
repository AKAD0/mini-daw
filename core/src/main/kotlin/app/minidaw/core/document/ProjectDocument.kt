package app.minidaw.core.document

import app.minidaw.core.model.AudioClip
import app.minidaw.core.model.Clip
import app.minidaw.core.model.Effect
import app.minidaw.core.model.EffectCatalog
import app.minidaw.core.model.Envelope
import app.minidaw.core.model.Invariants
import app.minidaw.core.model.Limits
import app.minidaw.core.model.LoopRegion
import app.minidaw.core.model.MasterChannel
import app.minidaw.core.model.Mixer
import app.minidaw.core.model.MixerChannel
import app.minidaw.core.model.NewProject
import app.minidaw.core.model.Note
import app.minidaw.core.model.Part
import app.minidaw.core.model.Pattern
import app.minidaw.core.model.PatternClip
import app.minidaw.core.model.Project
import app.minidaw.core.model.SCHEMA
import app.minidaw.core.model.SamplerInstrument
import app.minidaw.core.model.SetTimeSignature
import app.minidaw.core.model.TimeBase
import app.minidaw.core.model.TimeSignature
import app.minidaw.core.model.Track
import app.minidaw.core.model.UsedSample
import app.minidaw.engine.api.snapshot.PlayMode
import app.minidaw.engine.api.snapshot.SampleHash
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.time.DateTimeException
import java.time.Instant

/**
 * Reads the document of a Project (feature_specifications.md Section 6.1, architecture.md Section 4.4). A missing
 * field gets its default, an unknown field is ignored, a value outside its range is held inside, an older schema
 * is migrated one version at a time, and a newer schema is not opened. The invariants are checked once at the end.
 */
object ProjectDocument {
    sealed interface Result {
        data class Opened(val project: Project) : Result

        /** The document comes from a newer app. It is not opened, and its file is kept. */
        data class Newer(val schema: Int) : Result

        /** The document cannot be read; the store then reads the backup. */
        data class Broken(val reason: String) : Result
    }

    /** The migration from schema n to n + 1, by n (FR-PRJ-06). Schema 1 is the first, so none exists yet. */
    val migrations: Map<Int, (JsonObject) -> JsonObject> = emptyMap()

    fun write(project: Project): String = DocumentWriter.write(project)

    fun read(text: String, limits: Limits): Result {
        val parsed = runCatching { Json.parseToJsonElement(text) }
        val root = parsed.getOrNull() as? JsonObject
        val schema = root?.int("schema", 1) ?: 0
        return when {
            root == null -> Result.Broken(parsed.exceptionOrNull()?.message ?: "the document is no JSON object")
            schema > SCHEMA -> Result.Newer(schema)
            schema < 1 -> Result.Broken("the schema $schema does not exist")
            else -> open(root, schema, limits)
        }
    }

    private fun open(root: JsonObject, schema: Int, limits: Limits): Result = try {
        var current = root
        for (from in schema until SCHEMA) {
            val step = requireNotNull(migrations[from]) { "no migration from schema $from" }
            current = step(current)
        }
        val project = project(current)
        val broken = Invariants.check(project, limits)
        if (broken.isEmpty()) Result.Opened(project) else Result.Broken(broken.first())
    } catch (e: IllegalArgumentException) {
        Result.Broken(e.message ?: "a value cannot be read")
    } catch (e: DateTimeException) {
        Result.Broken(e.message ?: "a time cannot be read")
    }
}

private val COLOR = 0 until Limits.COLORS
private val MIDI = 0..Limits.MIDI_MAX
private val LEVEL = Limits.MIN_DB..Limits.MAX_DB
private val PAN = -1f..1f
private val GAIN = Limits.MIN_CLIP_DB..Limits.MAX_CLIP_DB
private const val FINE_CENTS = 100f
private const val NOTE_FIELDS = 4
private val TEMPO = TimeBase.MIN_TEMPO_MILLI_BPM..TimeBase.MAX_TEMPO_MILLI_BPM
private val NOT_NEGATIVE = 0..Int.MAX_VALUE

private fun JsonObject.name(): String = text("name").take(Limits.NAME_LENGTH)

private fun project(o: JsonObject): Project {
    val signature = o.array("timeSignature")?.mapNotNull { (it as? JsonPrimitive)?.intOrNull }
        ?.takeIf { it.size == 2 }?.let { TimeSignature(it[0], it[1]) }
        ?.takeIf { it in SetTimeSignature.ALLOWED } ?: TimeSignature.FOUR_FOUR
    val bar = TimeBase.barTicks(signature)
    val tracks = o.objects("tracks").map(::track)
    val patterns = o.objects("patterns").map { pattern(it, bar) }
    val instruments = o.objects("instruments").map(::instrument)
    val mixer = o.obj("mixer")
    val channels = mixer?.objects("channels")?.map(::channel) ?: emptyList()
    val master = mixer?.obj("master")
    val ids = tracks.map { it.id } + tracks.flatMap { t -> t.clips.map { it.id } } + patterns.map { it.id } +
        instruments.map { it.id } + channels.map { it.id }
    return Project(
        id = o.text("id"),
        name = o.name(),
        created = Instant.parse(o.text("created", Instant.EPOCH.toString())),
        modified = Instant.parse(o.text("modified", Instant.EPOCH.toString())),
        tempoMilliBpm = o.int("tempo", NewProject.TEMPO_MILLI_BPM, TEMPO),
        timeSignature = signature,
        nextId = o.int("nextId", (ids.maxOrNull() ?: 0) + 1),
        tracks = tracks,
        patterns = patterns,
        instruments = instruments,
        samples = o.objects("samples").map(::sample),
        mixer = Mixer(
            channels = channels,
            master = MasterChannel(
                volumeDb = master?.float("volumeDb", 0f, LEVEL) ?: 0f,
                slots = slots(master?.array("slots"))
            )
        )
    )
}

private fun track(o: JsonObject): Track = Track(
    id = o.int("id", 0),
    name = o.name(),
    color = o.int("color", 0, COLOR),
    channel = o.int("channel", 0),
    clips = o.objects("clips").mapNotNull(::clip).sortedBy { it.start },
    mute = o.bool("mute"),
    solo = o.bool("solo")
)

private fun clip(o: JsonObject): Clip? = when (o.text("type")) {
    "audio" -> AudioClip(
        id = o.int("id", 0),
        sample = SampleHash(o.text("sample")),
        start = o.int("start", 0, NOT_NEGATIVE),
        offset = o.int("offset", 0, NOT_NEGATIVE),
        length = o.int("length", Limits.MIN_AUDIO_FRAMES, Limits.MIN_AUDIO_FRAMES..Int.MAX_VALUE),
        gainDb = o.float("gainDb", 0f, GAIN)
    )

    "pattern" -> PatternClip(
        id = o.int("id", 0),
        pattern = o.int("pattern", 0),
        start = o.int("start", 0, NOT_NEGATIVE),
        length = o.int("length", 1, 1..Int.MAX_VALUE),
        offset = o.int("offset", 0, NOT_NEGATIVE),
        gainDb = o.float("gainDb", 0f, GAIN)
    )

    else -> null
}

private fun pattern(o: JsonObject, bar: Int): Pattern {
    val length = o.int("length", bar, bar * Limits.MIN_PATTERN_BARS..bar * Limits.MAX_PATTERN_BARS)
    return Pattern(
        id = o.int("id", 0),
        name = o.name(),
        color = o.int("color", 0, COLOR),
        length = length,
        parts = o.objects("parts").map { Part(it.int("instrument", 0), notes(it.array("notes"))) },
        take = o.textOrNull("take")?.let(::SampleHash)
    )
}

private fun notes(array: JsonArray?): List<Note> = (array ?: JsonArray(emptyList())).mapNotNull { entry ->
    val numbers = (entry as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.intOrNull }
    if (numbers == null || numbers.size != NOTE_FIELDS) {
        null
    } else {
        val (start, length, pitch, velocity) = numbers
        Note(
            start = start.coerceAtLeast(0),
            length = length.coerceAtLeast(Limits.MIN_NOTE_TICKS),
            pitch = pitch.coerceIn(MIDI),
            velocity = velocity.coerceIn(1, Limits.MIDI_MAX)
        )
    }
}.sortedWith(compareBy({ it.start }, { it.pitch })).distinctBy { it.start to it.pitch }

private fun instrument(o: JsonObject): SamplerInstrument {
    val envelope = o.obj("envelope")?.let {
        Envelope(
            attackMs = it.float("attackMs", 0f),
            decayMs = it.float("decayMs", 0f),
            sustain = it.float("sustain", 1f, 0f..1f),
            releaseMs = it.float("releaseMs", 0f)
        )
    }
    val loop = o.array("loop")?.mapNotNull { (it as? JsonPrimitive)?.intOrNull }?.takeIf { it.size == 2 }
    return SamplerInstrument(
        id = o.int("id", 0),
        name = o.name(),
        color = o.int("color", 0, COLOR),
        sample = SampleHash(o.text("sample")),
        rootNote = o.int("rootNote", Limits.DEFAULT_ROOT_NOTE, MIDI),
        playMode = if (o.text("playMode") == "oneShot") PlayMode.ONE_SHOT else PlayMode.GATE,
        volumeDb = o.float("volumeDb", 0f, LEVEL),
        mute = o.bool("mute"),
        pan = o.float("pan", 0f, PAN),
        solo = o.bool("solo"),
        fineCents = o.float("fineCents", 0f, -FINE_CENTS..FINE_CENTS),
        envelope = envelope,
        startFrame = o.intOrNull("startFrame"),
        endFrame = o.intOrNull("endFrame"),
        loop = loop?.let { LoopRegion(it[0], it[1]) },
        channel = o.intOrNull("channel")
    )
}

private fun channel(o: JsonObject): MixerChannel = MixerChannel(
    id = o.int("id", 0),
    name = o.name(),
    color = o.int("color", 0, COLOR),
    volumeDb = o.float("volumeDb", 0f, LEVEL),
    pan = o.float("pan", 0f, PAN),
    mute = o.bool("mute"),
    solo = o.bool("solo"),
    target = o.intOrNull("target"),
    slots = slots(o.array("slots"))
)

/** Four slots. An effect of an unknown kind leaves its slot empty; its parameters are held in range. */
private fun slots(array: JsonArray?): List<Effect?> {
    val read = (array ?: JsonArray(emptyList())).take(MixerChannel.SLOTS).map { entry ->
        val o = entry as? JsonObject
        val spec = o?.let { EffectCatalog.spec(it.text("kind")) }
        if (o == null || spec == null) {
            null
        } else {
            val params = o.obj("params")
            Effect(
                id = o.int("id", 0),
                kind = spec.name,
                bypass = o.bool("bypass"),
                params = spec.parameters.associate { p ->
                    p.name to p.hold(params?.float(p.name, p.default) ?: p.default)
                }
            )
        }
    }
    return read + List(MixerChannel.SLOTS - read.size) { null }
}

private fun sample(o: JsonObject): UsedSample = UsedSample(
    hash = SampleHash(o.text("hash")),
    name = o.text("name"),
    frames = o.int("frames", 0, NOT_NEGATIVE),
    channels = o.int("channels", 1, 1..2)
)
