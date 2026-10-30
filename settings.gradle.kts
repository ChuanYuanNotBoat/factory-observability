pluginManagement {
    repositories {
        gradlePluginPortal()
    }
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
