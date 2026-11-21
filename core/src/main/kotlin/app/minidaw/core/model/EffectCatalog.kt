// The numbers of this file are the ranges and defaults of the tables of feature_specifications.md Section 3.10.
@file:Suppress("MagicNumber")

package app.minidaw.core.model

import app.minidaw.engine.api.snapshot.EffectKind

/** One parameter of an effect: its name in the document (Section 6.1), its range and its default. */
data class ParameterSpec(val name: String, val min: Float, val max: Float, val default: Float) {
    fun hold(value: Float): Float = value.coerceIn(min, max)
}

/** A kind of effect: its name in the code, its number in a Snapshot, its parameters in the order of Section 3.10. */
data class EffectSpec(val name: String, val code: Int, val parameters: List<ParameterSpec>) {
    fun defaults(): Map<String, Float> = parameters.associate { it.name to it.default }

    fun parameter(name: String): ParameterSpec? = parameters.firstOrNull { it.name == name }
}

/** The catalog of the effects (feature_specifications.md Section 3.10, AD-14, D-084). */
object EffectCatalog {
    /** The three kinds of the MVP. The other five are P1. */
    val MVP_KINDS = setOf("eq3", "delay", "reverb")

    /**
     * The note values of the delay's "follow the tempo" (P1): 0 is off, 1 to 12 count through 1/16, 1/8, 1/4 and
     * 1/2, each straight, dotted and as a triplet.
     */
    const val SYNC_VALUES = 12

    val KINDS: List<EffectSpec> = listOf(
        EffectSpec(
            "eq3",
            EffectKind.EQ3,
            listOf(
                ParameterSpec("lowGainDb", -15f, 15f, 0f),
                ParameterSpec("lowFreqHz", 40f, 400f, 120f),
                ParameterSpec("midGainDb", -15f, 15f, 0f),
                ParameterSpec("midFreqHz", 200f, 8000f, 1000f),
                ParameterSpec("midQ", 0.3f, 6f, 0.9f),
                ParameterSpec("highGainDb", -15f, 15f, 0f),
                ParameterSpec("highFreqHz", 2000f, 16_000f, 8000f)
            )
        ),
        EffectSpec(
            "delay",
            EffectKind.DELAY,
            listOf(
                ParameterSpec("timeMs", 1f, 1000f, 350f),
                ParameterSpec("feedbackPct", 0f, 85f, 35f),
                ParameterSpec("dampingHz", 1000f, 20_000f, 6000f),
                ParameterSpec("mixPct", 0f, 100f, 30f),
                ParameterSpec("sync", 0f, SYNC_VALUES.toFloat(), 0f)
            )
        ),
        EffectSpec(
            "reverb",
            EffectKind.REVERB,
            listOf(
                ParameterSpec("sizePct", 0f, 100f, 50f),
                ParameterSpec("dampingPct", 0f, 100f, 50f),
                ParameterSpec("widthPct", 0f, 100f, 100f),
                ParameterSpec("mixPct", 0f, 100f, 25f)
            )
        ),
        EffectSpec(
            "compressor",
            EffectKind.COMPRESSOR,
            listOf(
                ParameterSpec("thresholdDb", -60f, 0f, -18f),
                ParameterSpec("ratio", 1f, 20f, 4f),
                ParameterSpec("attackMs", 0.1f, 100f, 10f),
                ParameterSpec("releaseMs", 10f, 1000f, 150f),
                ParameterSpec("kneeDb", 0f, 12f, 6f),
                ParameterSpec("makeupDb", 0f, 24f, 0f)
            )
        ),
        EffectSpec(
            "limiter",
            EffectKind.LIMITER,
            listOf(
                ParameterSpec("inputDb", 0f, 18f, 0f),
                ParameterSpec("ceilingDb", -12f, 0f, -1f),
                ParameterSpec("releaseMs", 10f, 500f, 100f)
            )
        ),
        EffectSpec(
            "saturation",
            EffectKind.SATURATION,
            listOf(
                ParameterSpec("driveDb", 0f, 36f, 12f),
                ParameterSpec("outputDb", -24f, 0f, -6f),
                ParameterSpec("mixPct", 0f, 100f, 100f)
            )
        ),
        EffectSpec(
            "filter",
            EffectKind.FILTER,
            listOf(
                ParameterSpec("kind", 0f, 1f, 0f),
                ParameterSpec("freqHz", 20f, 20_000f, 1000f),
                ParameterSpec("q", 0.5f, 8f, 0.7f)
            )
        ),
        EffectSpec(
            "chorus",
            EffectKind.CHORUS,
            listOf(
                ParameterSpec("rateHz", 0.1f, 5f, 0.8f),
                ParameterSpec("depthMs", 0f, 10f, 3f),
                ParameterSpec("delayMs", 5f, 30f, 15f),
                ParameterSpec("mixPct", 0f, 100f, 50f)
            )
        )
    )

    fun spec(kind: String): EffectSpec? = KINDS.firstOrNull { it.name == kind }

    /** An effect of a known kind with exactly its parameters, each inside its range. */
    fun isValid(effect: Effect): Boolean {
        val spec = spec(effect.kind) ?: return false
        return effect.params.keys == spec.parameters.map { it.name }.toSet() &&
            spec.parameters.all { p -> effect.params.getValue(p.name) in p.min..p.max }
    }
}
