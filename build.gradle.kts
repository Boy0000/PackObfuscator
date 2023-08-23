import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0" // Generates plugin.yml
    //id("io.papermc.paperweight.userdev") version "1.4.0" // NMS
    alias(libs.plugins.mia.copyjar)
}

group = "com.boy0000"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("io.th0rgal:oraxen:1.160.0")

    //paperDevBundle("1.19.3-R0.1-SNAPSHOT") //NMS
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.idofront.commands)
    implementation(libs.idofront.util)
    implementation(libs.idofront.text.components)
    implementation(libs.idofront.logging)
}

copyJar {
    destPath.set(project.property("oraxen_plugin_path") as String)
    jarName.set("OraxenPackObfuscator.jar")
    excludePlatformDependencies.set(false)
}

tasks {

    /*assemble {
        dependsOn(reobfJar)
    }*/

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
        minecraftVersion("1.20.1")
    }

    shadowJar {
        archiveFileName.set("OraxenPackObfuscator.jar")
        val pluginLoc = project.property("oraxen_plugin_path") as String + "\\OraxenPackObfuscator.jar"
        //archiveFile.get().asFile.copyTo(layout.projectDirectory.file("run/plugins/ModernLightApi.jar").asFile, true)
        println("Copied to $pluginLoc")
    }

    copyJar.get().dependsOn(shadowJar)

    build {
        dependsOn(copyJar)
    }

    /*
    reobfJar {
      // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
      // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
      outputJar.set(layout.buildDirectory.file("libs/PaperweightTestPlugin-${project.version}.jar"))
    }
     */
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}


bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    main = "com.boy0000.pack_obfuscator.OraxenPackObfuscator"
    version = "${project.version}"
    apiVersion = "1.20"
    authors = listOf("boy0000")
    depend = listOf("Oraxen")
    commands.create("oraxen_obf")
}