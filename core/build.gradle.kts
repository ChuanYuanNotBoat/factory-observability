plugins {
    `java-library`
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

val verifyCoreIsolation by tasks.registering {
    group = "verification"
    description = "Verifies that production Core depends only on the JDK."
    val sources = fileTree("src/main/java") { include("**/*.java") }
    inputs.files(sources)
    doLast {
        val imports = Regex("(?m)^\\s*import\\s+(?:static\\s+)?([^;]+);")
        val violations = sources.files.flatMap { source ->
            imports.findAll(source.readText()).mapNotNull { match ->
                val imported = match.groupValues[1]
                if (imported.startsWith("java.") || imported.startsWith("dev.factoryobservability.core.")) {
                    null
                } else {
                    "${source.relativeTo(projectDir)}: $imported"
                }
            }.toList()
        }
        check(violations.isEmpty()) { "Core must remain JDK-only:\n${violations.joinToString("\n")}" }
        check(configurations.runtimeClasspath.get().files.isEmpty()) {
            "Core production runtime must not contain external dependencies."
        }
    }
}

tasks.check { dependsOn(verifyCoreIsolation) }
