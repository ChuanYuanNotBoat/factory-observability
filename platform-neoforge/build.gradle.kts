plugins {
    `java-library`
    id("net.neoforged.moddev")
}

description = "Client-only factory observation for Minecraft 1.21.1 / NeoForge."

val modId = providers.gradleProperty("mod_id").get()
val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val neoVersion = providers.gradleProperty("neo_version").get()
val sharedProjects = listOf(":core", ":minecraft-common", ":compat-api").map { path ->
    evaluationDependsOn(path)
    project(path)
}
val sharedSources = sharedProjects.map { it.extensions.getByType<SourceSetContainer>().named("main") }
val smoke = sourceSets.create("smoke") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}
val smokeJar by tasks.registering(Jar::class) {
    from(smoke.output)
    archiveFileName.set("factory-observability-smoke.jar")
    destinationDirectory.set(layout.buildDirectory.dir("smoke"))
}
val prepareSmokeMods by tasks.registering(Sync::class) {
    dependsOn(tasks.jar, smokeJar)
    from(tasks.jar.flatMap { it.archiveFile })
    from(smokeJar.flatMap { it.archiveFile })
    into(layout.projectDirectory.dir("run-smoke/mods"))
}
val prepareSmokeOptions by tasks.registering {
    doLast {
        val options = file("run-smoke/options.txt")
        options.parentFile.mkdirs()
        options.writeText("onboardAccessibility:false\nlang:zh_cn\nguiScale:2\nrenderDistance:4\nsimulationDistance:5\nmaxFps:60\n")
    }
}

base { archivesName.set("factory-observability-neoforge-$minecraftVersion") }

neoForge {
    version = neoVersion
    addModdingDependenciesTo(smoke)
    runs {
        create("client") {
            client()
            gameDirectory = file("run")
        }
        create("smokeClient") {
            client()
            programArguments.addAll("--width", "1280", "--height", "720")
            gameDirectory = file("run-smoke")
            loadedMods.set(emptySet())
            taskBefore(prepareSmokeMods)
            taskBefore(prepareSmokeOptions)
        }
    }
    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sharedSources.forEach { sourceSet(it.get()) }
        }
    }
}

dependencies {
    sharedProjects.forEach { implementation(project(it.path)) }
}

val metadata = mapOf("mod_version" to version.toString(), "neo_version" to neoVersion)
tasks.processResources {
    inputs.properties(metadata)
    filesMatching("META-INF/neoforge.mods.toml") { expand(metadata) }
}

tasks.jar {
    sharedSources.forEach { from(it.map { source -> source.output }) }
    from(rootProject.file("THIRD_PARTY_NOTICES.md")) { into("META-INF") }
}

tasks.named<Jar>("sourcesJar") {
    sharedSources.forEach { from(it.map { source -> source.allSource }) }
}

val verifyModJar by tasks.registering {
    group = "verification"
    description = "Verifies the installable client JAR contains metadata and all shared runtime classes."
    dependsOn(tasks.jar)
    val archive = tasks.jar.flatMap { it.archiveFile }
    inputs.file(archive)
    doLast {
        val entries = zipTree(archive).matching { include("**/*") }.files.map { it.name }.toSet()
        listOf("neoforge.mods.toml", "FactoryObservabilityClient.class", "SnapshotMetricProjector.class",
                "MenuSessionTracker.class", "ProviderRegistry.class").forEach {
            check(it in entries) { "Missing runtime entry: $it" }
        }
        check(entries.none { it.startsWith("SmokeProbe") }) { "Smoke fixtures must not ship in the mod JAR" }
    }
}
tasks.check { dependsOn(verifyModJar) }

tasks.register("verifySmokeClient") {
    group = "verification"
    description = "Runs the packaged client through real menu/entity synchronization and renders both inspector views."
    dependsOn("runSmokeClient")
    doLast {
        val log = file("run-smoke/logs/latest.log").readText()
        listOf("FACTORY SMOKE: menu inventory verified", "FACTORY SMOKE: replacement verified",
                "FACTORY SMOKE: censored lifecycle verified", "FACTORY SMOKE PASSED").forEach {
            check(it in log) { "Missing smoke evidence: $it" }
        }
        check("/FATAL]" !in log && "Shutdown failure!" !in log) { "Smoke client failed to shut down cleanly" }
        check(fileTree("run-smoke/screenshots").matching { include("factoryobs-*.png") }.files.size >= 8) {
            "Missing inspector screenshots"
        }
    }
}
