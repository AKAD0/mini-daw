package app.minidaw.core.document

import app.minidaw.core.model.AudioClip
import app.minidaw.core.model.Clip
import app.minidaw.core.model.Effect
import app.minidaw.core.model.MixerChannel
import app.minidaw.core.model.Pattern
import app.minidaw.core.model.PatternClip
import app.minidaw.core.model.Project
import app.minidaw.core.model.SamplerInstrument
import app.minidaw.core.model.Track
import app.minidaw.engine.api.snapshot.PlayMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Writes the document of a Project (feature_specifications.md Section 6.1, D-061): one JSON object in UTF-8, the
 * version of the schema first, the fields in the order of the table, readable by a person.
 */
object DocumentWriter {
    private val json = Json { prettyPrint = true }

    fun write(project: Project): String = json.encodeToString(JsonElement.serializer(), tree(project))

    fun tree(project: Project): JsonObject = buildJsonObject {
        put("schema", project.schema)
        put("id", project.id)
        put("name", project.name)
        put("created", project.created.toString())
        put("modified", project.modified.toString())
        put("tempo", project.tempoMilliBpm)
        putJsonArray("timeSignature") {
            add(project.timeSignature.numerator)
            add(project.timeSignature.denominator)
        }
        put("nextId", project.nextId)
        put("tracks", JsonArray(project.tracks.map(::track)))
        put("patterns", JsonArray(project.patterns.map(::pattern)))
        put("instruments", JsonArray(project.instruments.map(::instrument)))
        putJsonArray("samples") {
            for (s in project.samples) {
                add(
                    buildJsonObject {
                        put("hash", s.hash.hex)
                        put("name", s.name)
                        put("frames", s.frames)
                        put("channels", s.channels)
                    }
                )
            }
        }
        putJsonObject("mixer") {
            put("channels", JsonArray(project.mixer.channels.map(::channel)))
            putJsonObject("master") {
                put("volumeDb", project.mixer.master.volumeDb)
                put("slots", slots(project.mixer.master.slots))
            }
        }
    }
}

private fun track(t: Track): JsonObject = buildJsonObject {
    put("id", t.id)
    put("name", t.name)
    put("color", t.color)
    put("mute", t.mute)
    put("solo", t.solo)
    put("channel", t.channel)
    put("clips", JsonArray(t.clips.map(::clip)))
}

private fun clip(c: Clip): JsonObject = buildJsonObject {
    when (c) {
        is AudioClip -> {
            put("type", "audio")
            put("id", c.id)
            put("sample", c.sample.hex)
            put("start", c.start)
            put("offset", c.offset)
            put("length", c.length)
        }

        is PatternClip -> {
            put("type", "pattern")
            put("id", c.id)
            put("pattern", c.pattern)
            put("start", c.start)
            put("length", c.length)
            put("offset", c.offset)
        }
    }
    put("gainDb", c.gainDb)
}

private fun pattern(p: Pattern): JsonObject = buildJsonObject {
    put("id", p.id)
    put("name", p.name)
    put("color", p.color)
    put("length", p.length)
    putJsonArray("parts") {
        for (part in p.parts) {
            add(
                buildJsonObject {
                    put("instrument", part.instrument)
                    putJsonArray("notes") {
                        for (n in part.notes) {
                            addJsonArray {
                                add(n.start)
                                add(n.length)
                                add(n.pitch)
                                add(n.velocity)
                            }
                        }
                    }
                }
            )
        }
    }
    put("take", p.take?.hex)
}

private fun instrument(i: SamplerInstrument): JsonObject = buildJsonObject {
    put("id", i.id)
    put("name", i.name)
    put("color", i.color)
    put("sample", i.sample.hex)
    put("rootNote", i.rootNote)
    put("playMode", if (i.playMode == PlayMode.ONE_SHOT) "oneShot" else "gate")
    put("volumeDb", i.volumeDb)
    put("mute", i.mute)
    put("pan", i.pan)
    put("solo", i.solo)
    put("fineCents", i.fineCents)
    put(
        "envelope",
        i.envelope?.let { e ->
            buildJsonObject {
                put("attackMs", e.attackMs)
                put("decayMs", e.decayMs)
                put("sustain", e.sustain)
                put("releaseMs", e.releaseMs)
            }
        } ?: JsonNull
    )
    put("startFrame", i.startFrame)
    put("endFrame", i.endFrame)
    put(
        "loop",
        i.loop?.let { l ->
            buildJsonArray {
                add(l.startFrame)
                add(l.endFrame)
            }
        } ?: JsonNull
    )
    put("channel", i.channel)
}

private fun channel(c: MixerChannel): JsonObject = buildJsonObject {
    put("id", c.id)
    put("name", c.name)
    put("color", c.color)
    put("volumeDb", c.volumeDb)
    put("pan", c.pan)
    put("mute", c.mute)
    put("solo", c.solo)
    put("target", c.target)
    put("slots", slots(c.slots))
}

private fun slots(slots: List<Effect?>): JsonArray = JsonArray(slots.map { it?.let(::effect) ?: JsonNull })

private fun effect(e: Effect): JsonObject = buildJsonObject {
    put("id", e.id)
    put("kind", e.kind)
    put("bypass", e.bypass)
    putJsonObject("params") {
        for ((name, value) in e.params.toSortedMap()) put(name, value)
    }
}
