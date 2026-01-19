plugins {
    kotlin("jvm")
}

kotlin.compilerOptions.optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")

dependencies {
    testImplementation(project(":mokkery-core"))
    testImplementation(project(":mokkery-runtime"))
    testImplementation(project(":mokkery-plugin"))
    testImplementation(kotlin("test"))
    testImplementation(libs.kctesting) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }
    testImplementation(libs.kotlin.compiler.embeddable)
}
