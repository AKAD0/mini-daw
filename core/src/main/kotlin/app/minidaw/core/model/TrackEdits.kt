package app.minidaw.core.model

// The Edits of Tracks (feature_specifications.md Section 2.2.1). Solo and move are P1.

/** A new Track and a new Mixer channel with the same name and color (FR-MX-02), at the end of the list. */
data class AddTrack(val name: String? = null, val color: Int? = null) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        if (state.tracks.size >= limits.tracks) refuse(Reason.LIMIT, "a Project has at most ${limits.tracks} Tracks")
        if (state.mixer.channels.size >= limits.channels) {
            refuse(Reason.LIMIT, "a Project has at most ${limits.channels} Mixer channels")
        }
        val name = name ?: defaultName("Track", state.tracks.map { it.name })
        val color = color ?: nextColor(state.tracks.map { it.color })
        checkName(name)
        checkColor(color)
        val trackId = state.nextId
        val channelId = trackId + 1
        return state.copy(
            nextId = channelId + 1,
            tracks = state.tracks + Track(id = trackId, name = name, color = color, channel = channelId),
            mixer = state.mixer.copy(channels = state.mixer.channels + MixerChannel(channelId, name, color))
        )
    }
}

data class RenameTrack(val track: Int, val name: String) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        checkName(name)
        return state.withTrack(state.track(track).copy(name = name))
    }
}

data class RecolorTrack(val track: Int, val color: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        checkColor(color)
        return state.withTrack(state.track(track).copy(color = color))
    }
}

data class MuteTrack(val track: Int, val mute: Boolean) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project =
        state.withTrack(state.track(track).copy(mute = mute))
}

data class SoloTrack(val track: Int, val solo: Boolean) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project =
        state.withTrack(state.track(track).copy(solo = solo))
}

/** Moves a Track to the place `index` of the list (P1). */
data class MoveTrack(val track: Int, val index: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        val moved = state.track(track)
        if (index !in state.tracks.indices) refuse(Reason.RANGE, "a place lies in 0 to ${state.tracks.size - 1}")
        val others = state.tracks.filter { it.id != track }
        return state.copy(tracks = others.take(index) + moved + others.drop(index))
    }
}

/** Deletes a Track with its Clips. Its Mixer channel stays. */
data class DeleteTrack(val track: Int) : Edit {
    override fun applyTo(state: Project, limits: Limits): Project {
        state.track(track)
        return state.copy(tracks = state.tracks.filter { it.id != track })
    }
}
