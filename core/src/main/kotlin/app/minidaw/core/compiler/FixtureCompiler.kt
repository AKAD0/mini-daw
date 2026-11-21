package app.minidaw.core.compiler

import app.minidaw.core.document.ProjectDocument
import app.minidaw.core.model.Limits
import java.io.File

/**
 * The chain of the fixtures (architecture.md Section 15): every document `NAME.json` of the input folder becomes the
 * Snapshot `NAME.mdsn` in the output folder. The tests of engine-core load these Snapshots. A fixture that cannot
 * be read stops the build. Arguments: the folder of the fixtures, the folder of the Snapshots.
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "arguments: the folder of the fixtures and the folder of the Snapshots" }
    val output = File(args[1]).apply { mkdirs() }
    val fixtures = File(args[0]).listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
    check(fixtures.isNotEmpty()) { "no fixture in ${args[0]}" }
    for (fixture in fixtures) {
        val result = ProjectDocument.read(fixture.readText(), Limits.VERSION_1_0)
        check(result is ProjectDocument.Result.Opened) { "${fixture.name}: $result" }
        File(output, fixture.nameWithoutExtension + ".mdsn").writeBytes(SnapshotCompiler.bytes(result.project))
        println("${fixture.name}: ${result.project.name}")
    }
}
