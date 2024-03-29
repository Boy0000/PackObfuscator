package com.mineinabyss.pack_obfuscator.crucible

import com.mineinabyss.pack_obfuscator.PackSquash
import com.mineinabyss.pack_obfuscator.obfuscator
import com.mineinabyss.pack_obfuscator.unzip
import io.lumine.mythiccrucible.MythicCrucible

object CruciblePackSquash : PackSquash {

    override val inputDir = obfuscator.plugin.dataFolder.resolve("crucible/pack/")
    override val outputZip = obfuscator.plugin.dataFolder.resolve("crucible/pack.zip")

    fun squashCruciblePack() {
        unzip(MythicCrucible.inst().packGenerationManager.zippedOutput, inputDir)
        val crucibleSquash = obfuscator.config.crucible.packSquash
        super.squashPack(crucibleSquash)

        inputDir.deleteRecursively()
        outputZip.copyTo(MythicCrucible.inst().packGenerationManager.zippedOutput, true)
    }
}