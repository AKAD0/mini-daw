package app.minidaw.core.model

import app.minidaw.core.model.Fixtures.edit
import app.minidaw.core.model.Fixtures.refuses
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MixerEditsTest {
    private val start = Fixtures.newProject().edit(AddTrack()).edit(AddTrack())
    private val channels = start.mixer.channels.map { it.id }

    private fun Project.channel(id: Int): MixerChannel = mixer.channels.single { it.id == id }

    @Test
    fun theFlagsAudibleFollowSection323ForEveryCombinationOfThreeChannels() {
        // AC-MX-04: 4 states (nothing, mute, solo, both) for each of three channels: 64 combinations.
        assertEquals(3, channels.size)
        var combinations = 0
        for (code in 0 until 64) {
            val states = channels.mapIndexed { i, id -> id to ((code shr (2 * i)) and 3) }
            val mixer = start.mixer.copy(
                channels = start.mixer.channels.map { c ->
                    val s = states.toMap().getValue(c.id)
                    c.copy(mute = s and 1 != 0, solo = s and 2 != 0)
                }
            )
            val anySolo = mixer.channels.any { it.solo }
            val expected = mixer.channels.associate { it.id to (!it.mute && (!anySolo || it.solo)) }
            assertEquals(expected, Audible.channels(mixer), "combination $code")
            combinations++
        }
        assertEquals(64, combinations)
    }

    @Test
    fun theRowsOfTheTableOfSection323() {
        val (a, b, c) = channels
        fun audible(vararg changes: Pair<Int, ChannelValue>): Set<Int> {
            val state = changes.fold(start) { s, (id, v) -> s.edit(SetChannelValue(id, v)) }
            return Audible.channels(state.mixer).filterValues { it }.keys
        }
        val mute = ChannelValue.Mute(true)
        val solo = ChannelValue.Solo(true)
        assertEquals(setOf(a, b, c), audible())
        assertEquals(setOf(b, c), audible(a to mute))
        assertEquals(setOf(a), audible(a to solo))
        assertEquals(setOf(a, b), audible(a to solo, b to solo))
        assertEquals(emptySet(), audible(a to solo, a to mute))
        assertEquals(setOf(a), audible(a to solo, b to mute))
    }

    @Test
    fun theSoloPathOfVersion10FollowsTheTargets() {
        // A sends into the bus B; C sends to the Master channel. A solo of A keeps B audible, and the reverse.
        val (a, b, c) = channels
        val routed = start.edit(SetTarget(a, b))
        val soloA = routed.edit(SetChannelValue(a, ChannelValue.Solo(true)))
        assertEquals(mapOf(a to true, b to true, c to false), Audible.channels(soloA.mixer))
        val soloB = routed.edit(SetChannelValue(b, ChannelValue.Solo(true)))
        assertEquals(mapOf(a to true, b to true, c to false), Audible.channels(soloB.mixer))
        routed.refuses(SetTarget(b, a), Reason.CONFLICT)
        start.refuses(SetTarget(a, a), Reason.CONFLICT)
    }

    @Test
    fun tracksAndSamplerInstrumentsHaveTheirOwnMute() {
        val muted = start.edit(MuteTrack(start.tracks[0].id, true))
        assertEquals(listOf(false, true, true), Audible.tracks(muted.tracks).values.toList())
        val kick = start.instruments.single().id
        val silent = start.edit(SetInstrumentValue(kick, InstrumentValue.Mute(true)))
        assertEquals(mapOf(kick to false), Audible.instruments(silent.instruments))
    }

    @Test
    fun channelsCanBeAddedRenamedAndDeletedWhenNothingSendsToThem() {
        val next = start.edit(AddMixerChannel())
        val added = next.mixer.channels.last()
        assertEquals("Channel 1", added.name)
        val renamed = next.edit(RenameChannel(added.id, "Bus")).edit(RecolorChannel(added.id, 7))
        assertEquals("Bus" to 7, renamed.channel(added.id).let { it.name to it.color })
        assertEquals(start.mixer, next.edit(DeleteMixerChannel(added.id)).mixer)
        start.refuses(DeleteMixerChannel(channels[0]), Reason.CONFLICT)
        next.edit(SetTarget(channels[0], added.id)).refuses(DeleteMixerChannel(added.id), Reason.CONFLICT)
    }

    @Test
    fun aTrackCanMoveToAnotherOrToANewChannel() {
        val track = start.tracks[0]
        val shared = start.edit(AssignTrack(track.id, channels[1]))
        assertEquals(channels[1], shared.tracks[0].channel)
        val own = shared.edit(AssignTrack(track.id, null))
        val channel = own.channel(own.tracks[0].channel)
        assertEquals(track.name to track.color, channel.name to channel.color)
        start.refuses(AssignTrack(track.id, 999), Reason.NOT_FOUND)
    }

    @Test
    fun valuesOfAChannelAndOfTheMasterKeepTheRangesOfSection14() {
        val c = channels[0]
        val next = start.edit(SetChannelValue(c, ChannelValue.VolumeDb(-90f)))
            .edit(SetChannelValue(c, ChannelValue.Pan(-1f))).edit(SetMasterVolume(6f))
        assertEquals(-90f to -1f, next.channel(c).let { it.volumeDb to it.pan })
        assertEquals(6f, next.mixer.master.volumeDb)
        start.refuses(SetChannelValue(c, ChannelValue.VolumeDb(6.1f)), Reason.RANGE)
        start.refuses(SetChannelValue(c, ChannelValue.Pan(1.01f)), Reason.RANGE)
        start.refuses(SetMasterVolume(-91f), Reason.RANGE)
    }

    @Test
    fun anEffectStartsWithItsDefaultsAndHoldsItsValuesInRange() {
        val c = channels[0]
        val next = start.edit(AddEffect(c, 1, "delay"))
        val delay = assertNotNull(next.channel(c).slots[1])
        val defaults =
            mapOf("timeMs" to 350f, "feedbackPct" to 35f, "dampingHz" to 6000f, "mixPct" to 30f, "sync" to 0f)
        assertEquals(defaults, delay.params)
        val held = assertNotNull(next.edit(SetParameter(c, 1, "feedbackPct", 99f)).channel(c).slots[1])
        assertEquals(85f, held.params.getValue("feedbackPct"))
        next.refuses(SetParameter(c, 1, "sizePct", 10f), Reason.NOT_FOUND)
        next.refuses(AddEffect(c, 1, "reverb"), Reason.DUPLICATE)
        start.refuses(AddEffect(c, 4, "reverb"), Reason.RANGE)
        start.refuses(AddEffect(c, 0, "chorus"), Reason.RANGE)
        val chorus = start.edit(AddEffect(c, 0, "chorus"), Limits.VERSION_1_0).channel(c).slots[0]
        assertEquals("chorus", assertNotNull(chorus).kind)
    }

    @Test
    fun effectsCanBeReplacedBypassedMovedAndRemoved() {
        val c = channels[0]
        val one = start.edit(AddEffect(c, 0, "eq3"))
        val replaced = assertNotNull(one.edit(ReplaceEffect(c, 0, "reverb")).channel(c).slots[0])
        assertEquals("reverb" to 25f, replaced.kind to replaced.params.getValue("mixPct"))
        assertTrue(assertNotNull(one.edit(SetBypass(c, 0, true)).channel(c).slots[0]).bypass)
        val moved = one.edit(MoveEffect(c, 0, 3)).channel(c).slots
        assertNull(moved[0])
        assertEquals("eq3", assertNotNull(moved[3]).kind)
        assertTrue(one.edit(RemoveEffect(c, 0)).channel(c).slots.all { it == null })
        start.refuses(RemoveEffect(c, 0), Reason.NOT_FOUND)
        val master = start.edit(AddEffect(null, 0, "reverb"))
        assertEquals("reverb", assertNotNull(master.mixer.master.slots[0]).kind)
    }

    @Test
    fun theMvpHasAtMostEightActiveEffects() {
        var state = start
        for (i in 0 until 8) state = state.edit(AddEffect(if (i < 4) channels[0] else channels[1], i % 4, "eq3"))
        state.refuses(AddEffect(channels[2], 0, "eq3"), Reason.LIMIT)
        assertEquals(emptyList(), Invariants.check(state, Limits.MVP))
    }

    @Test
    fun aSamplerInstrumentCanHaveItsOwnChannel() {
        val kick = start.instruments.single().id
        val own = start.edit(SetOwnChannel(kick, channels[2]))
        assertEquals(channels[2], own.instruments.single().channel)
        own.refuses(DeleteMixerChannel(channels[2]), Reason.CONFLICT)
        assertNull(own.edit(SetOwnChannel(kick, null)).instruments.single().channel)
        start.refuses(SetOwnChannel(kick, 999), Reason.NOT_FOUND)
    }

    @Test
    fun theInvariantsRefuseAnEffectOutsideTheCatalog() {
        val wrong = Effect(id = start.nextId, kind = "eq3", bypass = false, params = mapOf("lowGainDb" to 0f))
        val state = start.copy(
            nextId = start.nextId + 1,
            mixer = start.mixer.copy(master = start.mixer.master.copy(slots = listOf(wrong, null, null, null)))
        )
        assertTrue(Invariants.check(state, Limits.MVP).any { "effect" in it })
    }
}
