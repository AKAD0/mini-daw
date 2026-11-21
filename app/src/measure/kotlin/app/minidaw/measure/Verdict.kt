package app.minidaw.measure

/** Verdict of the owner on a test judged by ear or by eye (feature_specifications.md Section 9, rule 3). */
enum class Verdict(val word: String) {
    PASS("pass"),
    FAIL("fail"),
    NOT_RUN("not run")
}
