package app.minidaw.core.document

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

// Reading of single fields by the rules of feature_specifications.md Section 6.1: a missing field or a field of
// another type gets its default value, and a number outside its range is held inside.

internal fun JsonObject.field(key: String): JsonElement? = get(key)?.takeUnless { it is JsonNull }

internal fun JsonObject.int(key: String, default: Int, range: IntRange? = null): Int {
    val value = (field(key) as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull ?: default
    return if (range == null) value else value.coerceIn(range)
}

internal fun JsonObject.intOrNull(key: String): Int? =
    (field(key) as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

internal fun JsonObject.float(key: String, default: Float, range: ClosedFloatingPointRange<Float>? = null): Float {
    val value = (field(key) as? JsonPrimitive)?.takeUnless { it.isString }?.floatOrNull ?: default
    return if (range == null) value else value.coerceIn(range.start, range.endInclusive)
}

internal fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
    (field(key) as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: default

internal fun JsonObject.text(key: String, default: String = ""): String =
    (field(key) as? JsonPrimitive)?.takeIf { it.isString }?.content ?: default

internal fun JsonObject.textOrNull(key: String): String? =
    (field(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun JsonObject.objects(key: String): List<JsonObject> =
    (field(key) as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

internal fun JsonObject.obj(key: String): JsonObject? = field(key) as? JsonObject

internal fun JsonObject.array(key: String): JsonArray? = field(key) as? JsonArray
