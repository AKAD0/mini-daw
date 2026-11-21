package app.minidaw.core.model

// The Edits of the Mixer (feature_specifications.md Section 3.4). An Edit of an Effect slot names its channel by
// ID, or null for the Master channel. Move effect, set target and own Mixer channel are P1.

private fun Project.channel(id: Int): MixerChannel =
    mixer.channels.firstOrNull { it.id == id } ?: refuse(Reason.NOT_FOUND, "Mixer channel $id does not exist")

private fun Project.withChannel(channel: MixerChannel): Project =
    copy(mixer = mixer.copy(channels = mixer.channels.map { if (it.id == channel.id) channel else it }))

private fun Project.slots(channel: Int?): List<Effect?> =
    if (channel == null) mixer.master.slots else channel(channel).slots

private fun Project.withSlots(channel: Int?, slots: List<Effect?>): Project = if (channel == null) {
    copy(mixer = mixer.copy(master = mixer.master.copy(slots = slots)))
} else {
    withChannel(channel(channel).copy(slots = slots))
}

private fun Project.usedEffect(channel: Int?, slot: Int): Effect {
    val slots = slots(channel)
    if (slot !in slots.indices) refuse(Reason.RANGE, "a channel has the slots 0 to ${MixerChannel.SLOTS - 1}")
    return slots[slot] ?: refuse(Reason.NOT_FOUND, "slot $slot is empty")
}

private fun Project.newEffect(kind: String, limits: Limits): Effect {
    val spec = EffectCatalog.spec(kind)
    if (spec == null || kind !in limits.effectKinds) refuse(Reason.RANGE, "this version has no effect \"$kind\"")
    return Effect(id = nextId, kind = kind, bypass = false, params = spec.defaults())
}

private fun checkLevel(db: Float) {
    if (db !in Limits.MIN_DB..Limits.MAX_DB) refuse(Reason.RANGE, "a volume lies in −90 to +6 dB")
}

/** A new Mixer channel at the end of the Mixer (FR-MX-01). */
data class AddMixerChannel(val name: String? = null, val color: Int? = null) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val channels = state.mixer.channels
        if (channels.size >= limits.channels) {
            refuse(Reason.LIMIT, "a Project has at most ${limits.channels} Mixer channels")
        }
        val name = name ?: defaultName("Channel", channels.map { it.name })
        val color = color ?: nextColor(channels.map { it.color })
        checkName(name)
        checkColor(color)
        val channel = MixerChannel(id = state.nextId, name = name, color = color)
        return state.copy(nextId = state.nextId + 1, mixer = state.mixer.copy(channels = channels + channel))
    }
}

data class RenameChannel(val channel: Int, val name: String) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        checkName(name)
        return state.withChannel(state.channel(channel).copy(name = name))
    }
}

data class RecolorChannel(val channel: Int, val color: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        checkColor(color)
        return state.withChannel(state.channel(channel).copy(color = color))
    }
}

/** Refused while a Track, a Sampler instrument or another channel sends to it. */
data class DeleteMixerChannel(val channel: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        state.channel(channel)
        val used = state.tracks.any { it.channel == channel } || state.instruments.any { it.channel == channel } ||
            state.mixer.channels.any { it.target == channel }
        if (used) refuse(Reason.CONFLICT, "a Track, a Sampler instrument or a channel sends to Mixer channel $channel")
        return state.copy(mixer = state.mixer.copy(channels = state.mixer.channels.filter { it.id != channel }))
    }
}

/** Assigns a Track to a Mixer channel, or to a new one with the name and color of the Track when `channel` is null. */
data class AssignTrack(val track: Int, val channel: Int?) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val assigned = state.track(track)
        if (channel != null) return state.withTrack(assigned.copy(channel = state.channel(channel).id))
        val added = AddMixerChannel(assigned.name, assigned.color).applyTo(state, limits)
        return added.withTrack(assigned.copy(channel = added.mixer.channels.last().id))
    }
}

/** One value of a Mixer channel. */
sealed interface ChannelValue {
    data class VolumeDb(val db: Float) : ChannelValue

    data class Pan(val pan: Float) : ChannelValue

    data class Mute(val mute: Boolean) : ChannelValue

    data class Solo(val solo: Boolean) : ChannelValue
}

/** Volume, pan, mute or solo of a Mixer channel, inside the ranges of Section 1.4. */
data class SetChannelValue(val channel: Int, val value: ChannelValue) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val old = state.channel(channel)
        val changed = when (value) {
            is ChannelValue.VolumeDb -> old.copy(volumeDb = value.db.also(::checkLevel))

            is ChannelValue.Pan -> {
                if (value.pan !in -1f..1f) refuse(Reason.RANGE, "a pan lies in −1 to +1")
                old.copy(pan = value.pan)
            }

            is ChannelValue.Mute -> old.copy(mute = value.mute)

            is ChannelValue.Solo -> old.copy(solo = value.solo)
        }
        return state.withChannel(changed)
    }
}

/** The Master channel has a volume and no pan, mute or solo. */
data class SetMasterVolume(val db: Float) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        checkLevel(db)
        return state.copy(mixer = state.mixer.copy(master = state.mixer.master.copy(volumeDb = db)))
    }
}

/** An effect with its default values into an empty slot, inside the limit of active effects. */
data class AddEffect(val channel: Int?, val slot: Int, val kind: String) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val slots = state.slots(channel)
        if (slot !in slots.indices) refuse(Reason.RANGE, "a channel has the slots 0 to ${MixerChannel.SLOTS - 1}")
        if (slots[slot] != null) refuse(Reason.DUPLICATE, "slot $slot holds an effect")
        val active = state.mixer.channels.sumOf { c -> c.slots.count { it != null } } +
            state.mixer.master.slots.count { it != null }
        if (active >= limits.activeEffects) {
            refuse(Reason.LIMIT, "a Project has at most ${limits.activeEffects} effects")
        }
        val effect = state.newEffect(kind, limits)
        val changed = slots.toMutableList().also { it[slot] = effect }
        return state.copy(nextId = state.nextId + 1).withSlots(channel, changed)
    }
}

data class RemoveEffect(val channel: Int?, val slot: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        state.usedEffect(channel, slot)
        return state.withSlots(channel, state.slots(channel).toMutableList().also { it[slot] = null })
    }
}

/** Another kind of effect, with its default values, in a used slot. */
data class ReplaceEffect(val channel: Int?, val slot: Int, val kind: String) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        state.usedEffect(channel, slot)
        val effect = state.newEffect(kind, limits)
        return state.copy(nextId = state.nextId + 1)
            .withSlots(channel, state.slots(channel).toMutableList().also { it[slot] = effect })
    }
}

data class SetBypass(val channel: Int?, val slot: Int, val bypass: Boolean) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val effect = state.usedEffect(channel, slot).copy(bypass = bypass)
        return state.withSlots(channel, state.slots(channel).toMutableList().also { it[slot] = effect })
    }
}

/** A parameter of an effect. The value is held inside the range of Section 3.10. */
data class SetParameter(val channel: Int?, val slot: Int, val parameter: String, val value: Float) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val effect = state.usedEffect(channel, slot)
        val spec = EffectCatalog.spec(effect.kind)?.parameter(parameter)
            ?: refuse(Reason.NOT_FOUND, "the effect ${effect.kind} has no parameter $parameter")
        val changed = effect.copy(params = effect.params + (parameter to spec.hold(value)))
        return state.withSlots(channel, state.slots(channel).toMutableList().also { it[slot] = changed })
    }
}

/** P1: swaps the effects of two slots of one channel. */
data class MoveEffect(val channel: Int?, val from: Int, val to: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val slots = state.slots(channel).toMutableList()
        if (from !in slots.indices || to !in slots.indices) refuse(Reason.RANGE, "a channel has the slots 0 to 3")
        val moved = slots[from]
        slots[from] = slots[to]
        slots[to] = moved
        return state.withSlots(channel, slots)
    }
}

/** P1: the target of a channel, null for the Master channel. A target that would close a loop is refused. */
data class SetTarget(val channel: Int, val target: Int?) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val old = state.channel(channel)
        if (target != null) {
            state.channel(target)
            val targets = state.mixer.channels.associate { it.id to it.target }
            val path = generateSequence(target) { targets[it] }.take(targets.size + 1)
            if (path.any { it == channel }) refuse(Reason.CONFLICT, "this target would close a loop (FR-MX-11)")
        }
        return state.withChannel(old.copy(target = target))
    }
}

/** P1: an own Mixer channel of a Sampler instrument, or none (FR-MX-19). */
data class SetOwnChannel(val instrument: Int, val channel: Int?) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val old = state.instrument(instrument)
        channel?.let { state.channel(it) }
        val instruments = state.instruments.map { if (it.id == instrument) old.copy(channel = channel) else it }
        return state.copy(instruments = instruments)
    }
}
