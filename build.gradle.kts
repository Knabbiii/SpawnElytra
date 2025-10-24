plugins {
    java
}

group = "de.knabbiii.spawnelytra"
version = "2.1"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.10-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// Configure JAR naming
tasks.jar {
    archiveBaseName.set("SpawnElytra")
    archiveVersion.set("2.1")
}