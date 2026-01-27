plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "de.knabbiii.spawnelytra"
version = "2.4.0"

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

// === Dev Server Setup ===
val serverDir = file("${projectDir}/run")

tasks.register("setupDevServer") {
    group = "development"
    description = "Downloads Paper server JAR for dev testing"
    
    doLast {
        serverDir.mkdirs()
        val serverJar = file("${serverDir}/server.jar")
        
        if (!serverJar.exists()) {
            println("Downloading Paper 1.21.11...")
            val downloadUrl = "https://fill-data.papermc.io/v1/objects/84f4283253ae7e50a25b26ef3b03d57818145534fb0c8a27925b7bae59222ba6/paper-1.21.11-99.jar"
            ant.invokeMethod("get", mapOf(
                "src" to downloadUrl,
                "dest" to serverJar,
                "quiet" to false
            ))
            println("Paper server downloaded!")
        } else {
            println("Server JAR already exists")
        }
        
        // Create eula.txt
        file("${serverDir}/eula.txt").writeText("eula=true")
        
        // Create server.properties with optimized settings
        val serverProps = """
            online-mode=false
            level-type=flat
            spawn-protection=0
            max-players=10
            view-distance=6
            simulation-distance=4
            motd=SpawnElytra Dev Server
        """.trimIndent()
        file("${serverDir}/server.properties").writeText(serverProps)
        
        // Create plugins directory
        val pluginsDir = file("${serverDir}/plugins")
        pluginsDir.mkdirs()
        
        // Download Geyser plugin
        val geyserJar = file("${pluginsDir}/Geyser-Spigot.jar")
        if (!geyserJar.exists()) {
            println("Downloading Geyser...")
            ant.invokeMethod("get", mapOf(
                "src" to "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot",
                "dest" to geyserJar,
                "quiet" to false
            ))
            println("Geyser downloaded!")
        }
        
        // Download Floodgate plugin
        val floodgateJar = file("${pluginsDir}/floodgate-spigot.jar")
        if (!floodgateJar.exists()) {
            println("Downloading Floodgate...")
            ant.invokeMethod("get", mapOf(
                "src" to "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot",
                "dest" to floodgateJar,
                "quiet" to false
            ))
            println("Floodgate downloaded!")
        }
        
        println("Dev server setup complete in: ${serverDir}")
    }
}

tasks.register("copyPlugin") {
    group = "development"
    description = "Copies built plugin to dev server"
    dependsOn(tasks.shadowJar)
    
    doLast {
        val pluginJar = tasks.shadowJar.get().archiveFile.get().asFile
        val targetDir = file("${serverDir}/plugins")
        targetDir.mkdirs()
        
        // Delete old versions
        targetDir.listFiles()?.forEach {
            if (it.name.startsWith("SpawnElytra") && it.name.endsWith(".jar")) {
                it.delete()
            }
        }
        
        pluginJar.copyTo(file("${targetDir}/${pluginJar.name}"), overwrite = true)
        println("Plugin copied to: ${targetDir}/${pluginJar.name}")
    }
}

tasks.register<Exec>("runDevServer") {
    group = "development"
    description = "Builds plugin and runs dev server"
    dependsOn("setupDevServer", "copyPlugin")
    
    workingDir = serverDir
    commandLine("java", "-Xms1G", "-Xmx2G", "-jar", "server.jar", "--nogui")
    standardInput = System.`in`
    
    doFirst {
        println("=".repeat(50))
        println("Starting dev server with SpawnElytra...")
        println("Server directory: ${serverDir}")
        println("Stop server with: stop")
        println("=".repeat(50))
    }
}

tasks.register("cleanDevServer") {
    group = "development"
    description = "Deletes dev server directory"
    
    doLast {
        serverDir.deleteRecursively()
        println("Dev server directory deleted")
    }
}
