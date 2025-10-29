plugins {
    java
}

group = "de.knabbiii.spawnelytra"
version = "2.2"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    // Use 1.20.1 API for broader compatibility with 1.20.x and 1.21.x
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
}

java {
    // Use Java 21 LTS for optimal performance and security
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// Configure JAR naming
tasks.jar {
    archiveBaseName.set("SpawnElytra")
    archiveVersion.set("2.2")
}