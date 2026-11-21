package app.minidaw.measure

/** One entry of the list "results" of the file of results (feature_specifications.md Section 6.12). */
data class ResultRow(
    val id: String,
    val kind: String,
    val title: String,
    val value: Any? = null,
    val unit: String = "",
    val note: String = ""
)
