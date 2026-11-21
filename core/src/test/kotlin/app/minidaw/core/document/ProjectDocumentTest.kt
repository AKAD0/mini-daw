package app.minidaw.core.document

import app.minidaw.core.model.AddAudioClip
import app.minidaw.core.model.AddEffect
import app.minidaw.core.model.AddNote
import app.minidaw.core.model.AddPatternClip
import app.minidaw.core.model.AddSamplerInstrument
import app.minidaw.core.model.AddTrack
import app.minidaw.core.model.ChannelValue
import app.minidaw.core.model.Envelope
import app.minidaw.core.model.Fixtures
import app.minidaw.core.model.Fixtures.edit
import app.minidaw.core.model.InstrumentValue
import app.minidaw.core.model.Limits
import app.minidaw.core.model.LoopRegion
import app.minidaw.core.model.Note
import app.minidaw.core.model.Project
import app.minidaw.core.model.SCHEMA
import app.minidaw.core.model.SetBypass
import app.minidaw.core.model.SetChannelValue
import app.minidaw.core.model.SetInstrumentValue
import app.minidaw.core.model.SetMasterVolume
import app.minidaw.core.model.SetOwnChannel
import app.minidaw.core.model.SetParameter
import app.minidaw.core.model.SetTarget
import app.minidaw.core.model.SetTempo
import app.minidaw.core.model.SwitchStep
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProjectDocumentTest {
    private val start = Fixtures.newProject()

    /** A state that uses every field of Section 6.1, also the fields of version 1.0. */
    private val rich: Project = run {
        val track = start.tracks.single().id
        val pattern = start.patterns.single().id
        val kick = start.instruments.single().id
        var state = start.edit(AddTrack())
        val channels = state.mixer.channels.map { it.id }
        state = state.edit(AddPatternClip(track, pattern, start = 0, length = 7680, offset = 960))
            .edit(AddAudioClip(state.tracks[1].id, Fixtures.LOOP, start = 3840, offset = 4800, length = 96_000))
            .edit(AddNote(pattern, kick, Note(0, 240, 36, 127)))
            .edit(SwitchStep(pattern, kick, 8))
            .edit(AddSamplerInstrument(Fixtures.LOOP, name = "Loop"))
            .edit(SetInstrumentValue(kick, InstrumentValue.Pan(-0.5f)), Limits.VERSION_1_0)
            .edit(SetInstrumentValue(kick, InstrumentValue.FineCents(12.5f)), Limits.VERSION_1_0)
            .edit(SetInstrumentValue(kick, InstrumentValue.VolumeDb(-3.25f)))
            .edit(SetOwnChannel(kick, channels[1]))
            .edit(SetChannelValue(channels[0], ChannelValue.VolumeDb(-6f)))
            .edit(SetChannelValue(channels[0], ChannelValue.Pan(0.3f)))
            .edit(SetChannelValue(channels[1], ChannelValue.Solo(true)))
            .edit(SetTarget(channels[0], channels[1]))
            .edit(AddEffect(channels[0], 2, "delay"))
            .edit(SetParameter(channels[0], 2, "timeMs", 123.5f))
            .edit(SetBypass(channels[0], 2, true))
            .edit(AddEffect(null, 0, "reverb"))
            .edit(SetMasterVolume(-1.5f))
            .edit(SetTempo(97_500))
        val instruments = state.instruments.map {
            if (it.id != kick) {
                it
            } else {
                it.copy(
                    envelope = Envelope(5f, 100f, 0.8f, 250f),
                    startFrame = 100,
                    endFrame = 20_000,
                    loop = LoopRegion(200, 19_000)
                )
            }
        }
        state.copy(
            instruments = instruments,
            patterns = state.patterns.map { it.copy(take = Fixtures.LOOP.hash) }
        )
    }

    private fun read(text: String, limits: Limits = Limits.VERSION_1_0): Project {
        val result = ProjectDocument.read(text, limits)
        assertIs<ProjectDocument.Result.Opened>(result, "$result")
        return result.project
    }

    private fun changed(project: Project, change: (MutableMap<String, JsonElement>) -> Unit): String {
        val map = DocumentWriter.tree(project).toMutableMap()
        change(map)
        return JsonObject(map).toString()
    }

    @Test
    fun anyValidStateSurvivesWritingAndReading() {
        // AC-PRJ-10, first sentence; AC-MX-11 and AC-FX-08: the Mixer and the effects value by value.
        for (state in listOf(start, rich)) {
            val back = read(ProjectDocument.write(state))
            assertEquals(state, back)
            assertEquals(state.mixer, back.mixer)
            assertEquals(state.mixer.channels.map { it.slots }, back.mixer.channels.map { it.slots })
        }
    }

    @Test
    fun theDocumentOfSchema1OpensAsTheExpectedState() {
        // AC-PRJ-09: a test document of every schema up to the current one; every older schema has a migration.
        val text = checkNotNull(javaClass.getResource("/documents/schema-1.json")).readText()
        assertEquals(start, read(text))
        assertEquals(Json.parseToJsonElement(text), DocumentWriter.tree(start))
        assertEquals((1 until SCHEMA).toSet(), ProjectDocument.migrations.keys)
    }

    @Test
    fun aDocumentOfANewerSchemaIsRefused() {
        // AC-PRJ-09, second sentence. The store keeps the file.
        val newer = changed(start) { it["schema"] = JsonPrimitive(SCHEMA + 1) }
        assertEquals(ProjectDocument.Result.Newer(SCHEMA + 1), ProjectDocument.read(newer, Limits.MVP))
    }

    @Test
    fun unknownFieldsAreIgnored() {
        // AC-PRJ-10, second sentence.
        val text = changed(rich) {
            it["fromTheFuture"] = JsonPrimitive("yes")
            it["mixer"] = JsonObject((it["mixer"] as JsonObject) + ("busses" to JsonPrimitive(3)))
        }
        assertEquals(rich, read(text))
    }

    @Test
    fun valuesOutsideTheirRangeAreHeldInside() {
        // AC-PRJ-10, third sentence.
        val text = ProjectDocument.write(rich)
            .replace("\"tempo\": 97500", "\"tempo\": 500000")
            .replace("\"volumeDb\": -1.5", "\"volumeDb\": 20")
            .replace("\"timeMs\": 123.5", "\"timeMs\": 5000")
            .replace("\"pan\": 0.3", "\"pan\": -7")
        val back = read(text)
        assertEquals(300_000, back.tempoMilliBpm)
        assertEquals(6f, back.mixer.master.volumeDb)
        assertEquals(-1f, back.mixer.channels[0].pan)
        assertEquals(1000f, back.mixer.channels[0].slots[2]?.params?.get("timeMs"))
    }

    @Test
    fun missingFieldsGetTheirDefaults() {
        val text = ProjectDocument.write(start).replace(Regex("\"solo\": false,?\\s*"), "")
            .replace(Regex("\"fineCents\": 0.0,?\\s*"), "")
        assertEquals(start, read(text))
    }

    @Test
    fun aBrokenDocumentIsReported() {
        assertIs<ProjectDocument.Result.Broken>(ProjectDocument.read("{ not json", Limits.MVP))
        assertIs<ProjectDocument.Result.Broken>(ProjectDocument.read("[1, 2]", Limits.MVP))
        val lost = ProjectDocument.write(start).replace("\"channel\": 2,", "\"channel\": 999,")
        assertIs<ProjectDocument.Result.Broken>(ProjectDocument.read(lost, Limits.MVP))
        val badHash = ProjectDocument.write(start).replace("1".repeat(64), "xyz")
        assertIs<ProjectDocument.Result.Broken>(ProjectDocument.read(badHash, Limits.MVP))
    }
}
