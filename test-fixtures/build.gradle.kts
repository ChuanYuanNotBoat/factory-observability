plugins {
    application
}

dependencies {
    implementation(project(":minecraft-common"))
}

application {
    mainClass.set("dev.factoryobservability.fixtures.ObservationDemo")
}
