import io.papermc.paperweight.util.configureTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("xyz.jpenilla.run-paper") version "2.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    //id("net.minecrell.plugin-yml.paper") version "0.6.0" // Generates plugin.yml
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0" // Generates plugin.yml
    alias(libs.plugins.shadowjar)
    alias(libs.plugins.mia.copyjar)
    alias(libs.plugins.mia.autoversion)
    alias(libs.plugins.mia.publication)
    alias(libs.plugins.kotlinx.serialization)
}

group = "com.boy0000"
version = "0.1"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.oraxen.com/releases")
    maven("https://repo.oraxen.com/snapshots")
    maven("https://repo.unnamed.team/repository/unnamed-public/")
    maven("https://mvn.lumine.io/repository/maven-public/") { metadataSources { artifact() } }// MythicMobs
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("io.th0rgal:oraxen:1.170.0-SNAPSHOT")
    compileOnly("io.lumine:MythicCrucible:2.0.0-SNAPSHOT")
    compileOnly("io.lumine:Mythic-Dist:5.6.0")
    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.4")

    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.idofront.commands)
    implementation(libs.idofront.config)
    implementation(libs.idofront.di)
    implementation(libs.idofront.util)
    implementation(libs.idofront.text.components)
    implementation(libs.idofront.logging)
    implementation(libs.kotlinx.serialization.kaml)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.creative.api)
    implementation(libs.creative.serializer.minecraft)
}

copyJar {
    excludePlatformDependencies.set(false)
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, "seconds")
    }
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion("1.18.2")
    }

    shadowJar {
        archiveFileName.set("PackObfuscator.jar")
        relocate("kotlin", "com.mineinabyss.shaded.kotlin")
        relocate("kotlinx", "com.mineinabyss.shaded.kotlinx")
    }

    build {
        dependsOn(copyJar)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}


//paper {
//    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
//    main = "com.mineinabyss.pack_obfuscator.PackObfuscator"
//    version = "${project.version}"
//    apiVersion = "1.18"
//    authors = listOf("boy0000")
//    foliaSupported = true
//
//    serverDependencies {
//        register("Oraxen") {
//            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
//            required = false
//        }
//        register("MythicCrucible") {
//            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
//            required = false
//        }
//        register("ModelEngine") {
//            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
//            required = false
//        }
//    }
//}

bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    main = "com.mineinabyss.pack_obfuscator.PackObfuscator"
    version = "${project.version}"
    apiVersion = "1.18"
    authors = listOf("boy0000")
    foliaSupported = true
    softDepend = listOf("Oraxen", "MythicCrucible", "ModelEngine")
}
