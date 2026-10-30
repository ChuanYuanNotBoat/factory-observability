pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
    }
}

plugins {
    id("net.neoforged.moddev.repositories") version "2.0.147"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "factory-observability"

include(
    "core",
    "minecraft-common",
    "compat-api",
    "platform-fabric",
    "platform-neoforge",
    "compat-create",
    "compat-ae2",
    "compat-mekanism",
    "compat-tfc",
    "compat-gtceu",
    "integration-litemfinder",
    "test-fixtures",
)
