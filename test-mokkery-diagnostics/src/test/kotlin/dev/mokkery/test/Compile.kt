package dev.mokkery.test

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import dev.mokkery.plugin.MokkeryCompilerPluginRegistrar
import org.intellij.lang.annotations.Language
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun compileJvm(@Language("kotlin") file: String): JvmCompilationResult {
    val source = SourceFile.kotlin("main.kt", file)
    val compilation = KotlinCompilation().apply {
        sources = listOf(source)
        compilerPluginRegistrars = listOf(MokkeryCompilerPluginRegistrar())
        inheritClassPath = true
        messageOutputStream = System.out
        kotlincArguments += "-Xcontext-parameters"
    }
    return compilation.compile()
}

fun JvmCompilationResult.assertSingleError(message: String, level: String = "e:") {
    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, exitCode)
    val errorLines = messages
        .split("\n")
        .filter { it.startsWith(level) }
    assertEquals(1, errorLines.size, "Expected exactly one error line starting with '$level', but found ${errorLines.size}: $errorLines")
    assertContains(errorLines.single(), message)
}

fun JvmCompilationResult.assertNoErrors() {
    assertEquals(KotlinCompilation.ExitCode.OK, exitCode)
    val errorLines = messages
        .split("\n")
        .filter { it.startsWith("e:") }
    assertTrue(errorLines.isEmpty(), "Expected no errors, but found: $errorLines")
}
