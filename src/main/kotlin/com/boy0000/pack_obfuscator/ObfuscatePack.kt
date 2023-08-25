package com.boy0000.pack_obfuscator

import com.boy0000.pack_obfuscator.ObfuscatePack.substringBetween
import com.google.gson.JsonParser
import io.th0rgal.oraxen.OraxenPlugin
import io.th0rgal.oraxen.api.OraxenPack
import org.bukkit.Material
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class ObfuscatedModel(val modelPath: String, val obfuscatedModelName: String) {
    val resourcePackModelPack: String get() {
        val namespace = modelPath.substringBetween("assets/", "/models")
        val path = modelPath.substringAfter("$namespace/models/").replace(".json", "")
        return if (namespace == "minecraft") path else "$namespace:$path"
    }
}
data class ObfuscatedTexture(val texturePath: String, val obfuscatedTextureName: String)

object ObfuscatePack {

    public val tempPackDir: File = OraxenPlugin.get().dataFolder.resolve("obfuscatedPack")
    private val tempTextureDir = tempPackDir.resolve("assets/minecraft/textures")
    private val tempModelDir = tempPackDir.resolve("assets/minecraft/models")
    private val obfuscatedMap = mutableMapOf<ObfuscatedModel, MutableSet<ObfuscatedTexture>>()
    fun obfuscate(pack: File) {
        tempPackDir.deleteRecursively()
        tempPackDir.mkdirs()
        unzip(pack, tempPackDir)

        val packFiles = tempPackDir.listFilesRecursively()
        obfuscateModels(packFiles)
        obfuscateParentModels(packFiles.filter { it.isTexture })
        obfuscateBlockStateFiles()
        obfuscateAtlas()

        copyAndCleanup()
    }

    private fun copyAndCleanup() {
        // Delete empty subfolders
        obfuscatedMap.clear()
        zip(tempPackDir, OraxenPack.getPack())
    }

    private fun obfuscateBlockStateFiles() {
        tempPackDir.resolve("assets/minecraft/blockstates").listFiles()?.forEach { blockstate ->
            val blockstateJson = JsonParser.parseString(blockstate.readText()).asJsonObject
            val variants = blockstateJson.getAsJsonObject("variants") ?: return@forEach
            variants.entrySet().forEach variant@{ variant ->
                val model = variant.value.asJsonObject.getAsJsonPrimitive("model").asString
                val obfuscatedModel = obfuscatedMap.keys.find { it.resourcePackModelPack == model }?.obfuscatedModelName ?: return@variant
                variant.value.asJsonObject.addProperty("model", obfuscatedModel)
                variants.add(variant.key, variant.value)
            }
            blockstateJson.add("variants", variants)
            blockstate.writeText(blockstateJson.toString())
        }
    }

    private fun obfuscateAtlas() {
        val atlas = File(tempPackDir, "assets/minecraft/atlases/blocks.json")
        val atlasJson = JsonParser.parseString(atlas.readText()).asJsonObject
        val sources = atlasJson.getAsJsonArray("sources") ?: return
        val obfuscatedTextures = obfuscatedMap.values.flatten().map { ObfuscatedTexture(it.texturePath.substringBetween("textures/", ".png"), it.obfuscatedTextureName) }
        sources.map { it.asJsonObject }.forEach {
            if (it.get("type").asString != "single") return@forEach
            val resource = it.get("resource").asString.replace("minecraft:", "")
            val texture = obfuscatedTextures.find { it.texturePath == resource } ?: return@forEach
            if (resource == texture.texturePath) it.addProperty("resource", texture.obfuscatedTextureName)
            if (it.get("sprite").asString.replace("minecraft:", "") == texture.texturePath) it.addProperty("sprite", texture.obfuscatedTextureName)
            sources.add(it)
        }
        atlasJson.add("sources", sources)
        atlas.writeText(atlasJson.toString())
    }

    private fun obfuscateParentModels(packFiles: List<File>) {
        val obfuscatedModelKeys = obfuscatedMap.keys.map {
            val namespace = it.modelPath .substringBetween("assets/", "/models")
            val path = it.modelPath.substringBetween("/models/", ".json")
            ObfuscatedModel(if (namespace == "minecraft") path else "$namespace:$path", it.obfuscatedModelName)
        }
        packFiles.filter { it.isModel && it.isVanillaBaseModel }.forEach baseModel@{ vanillaBaseModel ->
            val baseModelJson = JsonParser.parseString(vanillaBaseModel.readText()).asJsonObject
            val overrides = baseModelJson.getAsJsonArray("overrides") ?: return@baseModel
            overrides.forEach overrides@{ override ->
                val overrideModel = override.asJsonObject.getAsJsonPrimitive("model").asString
                val obfuscatedModel = obfuscatedModelKeys.find { it.modelPath == overrideModel.replace("\"", "") }?.obfuscatedModelName ?: return@overrides
                override.asJsonObject.addProperty("model", obfuscatedModel)
            }
            baseModelJson.add("overrides", overrides)
            vanillaBaseModel.writeText(baseModelJson.toString())
        }
    }

    private fun obfuscateModels(packFiles: List<File>) {
        val models = packFiles.filter { it.isModel && !it.isVanillaBaseModel }
        val textures = packFiles.filter { it.isTexture }
        models.forEach models@{ model ->
            if (!model.exists()) return@models
            val modelJson = JsonParser.parseString(model.readText()).asJsonObject
            val modelTextures = modelJson.getAsJsonObject("textures")?.asMap()?.values?.map { it.asString } ?: return@models
            val obfuscatedModelName = obfuscatedMap.keys.find { it.modelPath == model.modelPath }?.obfuscatedModelName ?: UUID.randomUUID().toString().replace("-", "")

            modelTextures.forEach textures@{ texture ->
                val obfuscatedTextureName = obfuscatedMap.values.flatten().find { it.texturePath.substringBetween("textures/", ".png") == texture }?.obfuscatedTextureName?: UUID.randomUUID().toString().replace("-", "")
                val textureFile = textures.find { texture in it.texturePath } ?: return@textures
                textureFile.renameTo(File(tempTextureDir, "$obfuscatedTextureName.png"))
                File(textureFile.path.replace(".png", ".png.mcmeta")).let {
                    if (it.exists()) it.renameTo(File(tempTextureDir, "$obfuscatedTextureName.png.mcmeta"))
                }
                modelJson.getAsJsonObject("textures").let { it.addProperty(it.entrySet().find { it.value.asString == texture }!!.key, obfuscatedTextureName) }
                obfuscatedMap.computeIfAbsent(ObfuscatedModel(model.packPath, obfuscatedModelName)) { mutableSetOf() } += ObfuscatedTexture(textureFile.packPath, obfuscatedTextureName)
            }

            model.writeText(modelJson.toString())
            model.renameTo(File(tempModelDir, "$obfuscatedModelName.json"))
        }
    }

    private val File.packPath get() = this.path.removePrefix(tempPackDir.absolutePath).drop(1).replace("\\", "/").replace(".png", "")
    private val File.modelPath get(): String {
        val namespace = this.packPath.substringAfter("assets/").substringBefore("/")
        val path = this.packPath.substringAfter("$namespace/models/")
        return if (namespace == "minecraft") path else "$namespace:$path"
    }
    private val File.texturePath get(): String {
        val namespace = this.packPath.substringAfter("assets/").substringBefore("/")
        val path = this.packPath.substringAfter("$namespace/textures/")
        return if (namespace == "minecraft") path else "$namespace:$path"
    }
    private val File.isModel get() = this.extension == "json"
    private val File.isTexture get() = this.extension == "png" || this.extension == "mcmeta"
    private val File.isVanillaBaseModel get(): Boolean {
        val isBase = this.isModel && "assets\\minecraft\\models\\item" in this.path || "assets\\minecraft\\models\\block" in this.path
        return isBase && Material.matchMaterial(this.nameWithoutExtension) != null
    }

    internal fun String.substringBetween(start: String, end: String) = this.substringAfter(start).substringBefore(end)

    private fun File.listFilesRecursively() = mutableListOf<File>().apply {
        walkTopDown().forEach { this += it }
    }

    private fun unzip(zippedPack: File, destinationDirectory: File) {
        val zipFile = ZipFile(zippedPack)

        if (!destinationDirectory.exists()) destinationDirectory.mkdirs()

        val entries = zipFile.entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryDestination = File(destinationDirectory, entry.name)

            if (entry.isDirectory) {
                entryDestination.mkdirs()
            } else {
                entryDestination.parentFile.mkdirs()

                zipFile.getInputStream(entry).use { input ->
                    FileOutputStream(entryDestination).use { output ->
                        val buffer = ByteArray(1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
            }
        }
    }

    private fun zip(sourceDirectory: File, zipFile: File) {
        val outputStream = ZipOutputStream(FileOutputStream(zipFile))

        sourceDirectory.walkTopDown().forEach { file ->
            val entryName = sourceDirectory.toPath().relativize(file.toPath()).toString()
            val entry = ZipEntry(entryName)
            outputStream.putNextEntry(entry)

            if (file.isFile) Files.copy(file.toPath(), outputStream)

            outputStream.closeEntry()
        }
        outputStream.close()
    }

}
