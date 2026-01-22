plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "de.knabbiii.spawnelytra"
version = "2.3.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.opencollab.dev/main/") // Floodgate/Geyser
    maven("https://repo.codemc.org/repository/maven-public/") // bStats
}

dependencies {
    // Use 1.20.1 API for broader compatibility with 1.20.x and 1.21.x
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    // Optional: Floodgate API for better Bedrock player detection
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
    // bStats metrics
    implementation("org.bstats:bstats-bukkit:3.0.2")
}

java {
    // Use Java 21 LTS for optimal performance and security
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// Configure JAR naming
tasks.jar {
    archiveBaseName.set("SpawnElytra")
    archiveVersion.set(version.toString())
}

// Configure shadow plugin for bStats
tasks.shadowJar {
    archiveBaseName.set("SpawnElytra")
    archiveVersion.set(version.toString())
    archiveClassifier.set("")
    
    // Relocate bStats to avoid conflicts
    relocate("org.bstats", "de.knabbiii.spawnelytra.metrics")
    
    minimize()
}

// Use shadowJar as the default build artifact
tasks.build {
    dependsOn(tasks.shadowJar)
}
