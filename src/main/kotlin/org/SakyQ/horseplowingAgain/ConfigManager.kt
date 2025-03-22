package org.SakyQ.horseplowingAgain

import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

class ConfigManager(private val plugin: JavaPlugin) {
    private var config: FileConfiguration

    // Default configuration values
    private val defaults = mapOf(
        "plow.radius" to 3,
        "plow.cooldown" to 20,
        "plow.equip_cooldown" to 5,
        "plow.durability" to 500,
        "plow.durability_warning" to 25,
        "plow.minecart_follow_distance" to 2.0, // Changed from original value
        "plow.minecart_offset_y" to -0.5,
        "plow.particles_enabled" to true,
        "plow.sound_enabled" to true,
        "plow.sound_type" to "BLOCK_FARMLAND_TILL",
        "plow.sound_volume" to 1.0,
        "plow.sound_pitch" to 1.0,
        "plow.only_horse_owner" to true,
        "plow.allow_minecart_damage" to false,
        "plow.harvest_crops" to true,

        // Harvester configuration
        "harvester.radius" to 3,
        "harvester.cooldown" to 30,
        "harvester.minecart_follow_distance" to 2.0,
        "harvester.minecart_offset_y" to -0.5,
        "harvester.particles_enabled" to true,
        "harvester.sound_enabled" to true,
        "harvester.sound_type" to "BLOCK_CROP_BREAK",
        "harvester.sound_volume" to 1.0,
        "harvester.sound_pitch" to 1.0,
        "harvester.replant_crops" to true,

        // Planter configuration
        "planter.radius" to 3,
        "planter.cooldown" to 40,
        "planter.minecart_follow_distance" to 2.0,
        "planter.minecart_offset_y" to -0.5,
        "planter.particles_enabled" to true,
        "planter.sound_enabled" to true,
        "planter.sound_type" to "BLOCK_CROP_PLANT",
        "planter.sound_volume" to 1.0,
        "planter.sound_pitch" to 1.0,
        "planter.only_on_farmland" to true
    )

    init {
        // Create and load the main config
        plugin.saveDefaultConfig()
        config = plugin.config

        // Set up default values if they don't exist
        for ((path, value) in defaults) {
            if (!config.contains(path)) {
                config.set(path, value)
            }
        }

        // Set up default plowable blocks if they don't exist
        if (!config.contains("plow.plowable_blocks")) {
            config.set("plow.plowable_blocks", listOf(
                "GRASS_BLOCK", "DIRT", "DIRT_PATH", "COARSE_DIRT"
            ))
        }

        // Set up default harvestable crops if they don't exist
        if (!config.contains("plow.harvestable_crops")) {
            config.set("plow.harvestable_crops", listOf(
                "WHEAT", "CARROTS", "POTATOES", "BEETROOTS"
            ))
        }

        // Set up default plantable seeds if they don't exist
        if (!config.contains("planter.plantable_seeds")) {
            config.set("planter.plantable_seeds", mapOf(
                "WHEAT_SEEDS" to "FARMLAND:WHEAT",
                "CARROT" to "FARMLAND:CARROTS",
                "POTATO" to "FARMLAND:POTATOES",
                "BEETROOT_SEEDS" to "FARMLAND:BEETROOTS",
                "PUMPKIN_SEEDS" to "FARMLAND:PUMPKIN_STEM",
                "MELON_SEEDS" to "FARMLAND:MELON_STEM"
            ))
        }

        plugin.saveConfig()
    }

    // Plow settings
    fun getPlowRadius(): Int {
        return config.getInt("plow.radius", 3)
    }

    fun getPlowCooldown(): Int {
        return config.getInt("plow.cooldown", 20)
    }

    fun getEquipCooldown(): Int {
        return config.getInt("plow.equip_cooldown", 5)
    }

    fun getPlowDurability(): Int {
        return config.getInt("plow.durability", 500)
    }

    fun getDurabilityWarningThreshold(): Int {
        return config.getInt("plow.durability_warning", 25)
    }

    fun getMinecartFollowDistance(): Double {
        // Always ensure the follow distance is at most 2.0
        return config.getDouble("plow.minecart_follow_distance", 2.0).coerceAtMost(2.0)
    }

    fun getMinecartOffsetY(): Double {
        return config.getDouble("plow.minecart_offset_y", -0.5)
    }

    fun areParticlesEnabled(): Boolean {
        return config.getBoolean("plow.particles_enabled", true)
    }

    fun isSoundEnabled(): Boolean {
        return config.getBoolean("plow.sound_enabled", true)
    }

    fun getSoundType(): String {
        return config.getString("plow.sound_type", "BLOCK_FARMLAND_TILL") ?: "BLOCK_FARMLAND_TILL"
    }

    fun getSoundVolume(): Float {
        return config.getDouble("plow.sound_volume", 1.0).toFloat()
    }

    fun getSoundPitch(): Float {
        return config.getDouble("plow.sound_pitch", 1.0).toFloat()
    }

    fun isHorseOwnerOnly(): Boolean {
        return config.getBoolean("plow.only_horse_owner", true)
    }

    fun allowMinecartDamage(): Boolean {
        return config.getBoolean("plow.allow_minecart_damage", false)
    }

    fun shouldHarvestCrops(): Boolean {
        return config.getBoolean("plow.harvest_crops", true)
    }

    fun getPlowableBlocks(): List<Material> {
        val materialList = mutableListOf<Material>()
        val configList = config.getStringList("plow.plowable_blocks")

        for (materialName in configList) {
            try {
                val material = Material.valueOf(materialName)
                materialList.add(material)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid material in config: $materialName")
            }
        }

        // Return default list if empty
        if (materialList.isEmpty()) {
            return listOf(Material.GRASS_BLOCK, Material.DIRT, Material.DIRT_PATH)
        }

        return materialList
    }

    fun getHarvestableCrops(): List<Material> {
        val materialList = mutableListOf<Material>()
        val configList = config.getStringList("plow.harvestable_crops")

        for (materialName in configList) {
            try {
                val material = Material.valueOf(materialName)
                materialList.add(material)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid material in config: $materialName")
            }
        }

        // Return default list if empty
        if (materialList.isEmpty()) {
            return listOf(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS)
        }

        return materialList
    }

    // Harvester settings
    fun getHarvesterRadius(): Int {
        return config.getInt("harvester.radius", 3)
    }

    fun getHarvesterCooldown(): Int {
        return config.getInt("harvester.cooldown", 30)
    }

    fun getHarvesterMinecartFollowDistance(): Double {
        return config.getDouble("harvester.minecart_follow_distance", 2.0).coerceAtMost(2.0)
    }

    fun getHarvesterMinecartOffsetY(): Double {
        return config.getDouble("harvester.minecart_offset_y", -0.5)
    }

    fun areHarvesterParticlesEnabled(): Boolean {
        return config.getBoolean("harvester.particles_enabled", true)
    }

    fun isHarvesterSoundEnabled(): Boolean {
        return config.getBoolean("harvester.sound_enabled", true)
    }

    fun getHarvesterSoundType(): String {
        return config.getString("harvester.sound_type", "BLOCK_CROP_BREAK") ?: "BLOCK_CROP_BREAK"
    }

    fun getHarvesterSoundVolume(): Float {
        return config.getDouble("harvester.sound_volume", 1.0).toFloat()
    }

    fun getHarvesterSoundPitch(): Float {
        return config.getDouble("harvester.sound_pitch", 1.0).toFloat()
    }

    fun shouldReplantCrops(): Boolean {
        return config.getBoolean("harvester.replant_crops", true)
    }

    // Planter settings
    fun getPlanterRadius(): Int {
        return config.getInt("planter.radius", 3)
    }

    fun getPlanterCooldown(): Int {
        return config.getInt("planter.cooldown", 40)
    }

    fun getPlanterMinecartFollowDistance(): Double {
        return config.getDouble("planter.minecart_follow_distance", 2.0).coerceAtMost(2.0)
    }

    fun getPlanterMinecartOffsetY(): Double {
        return config.getDouble("planter.minecart_offset_y", -0.5)
    }

    fun arePlanterParticlesEnabled(): Boolean {
        return config.getBoolean("planter.particles_enabled", true)
    }

    fun isPlanterSoundEnabled(): Boolean {
        return config.getBoolean("planter.sound_enabled", true)
    }

    fun getPlanterSoundType(): String {
        return config.getString("planter.sound_type", "BLOCK_CROP_PLANT") ?: "BLOCK_CROP_PLANT"
    }

    fun getPlanterSoundVolume(): Float {
        return config.getDouble("planter.sound_volume", 1.0).toFloat()
    }

    fun getPlanterSoundPitch(): Float {
        return config.getDouble("planter.sound_pitch", 1.0).toFloat()
    }

    fun isPlanterOnlyOnFarmland(): Boolean {
        return config.getBoolean("planter.only_on_farmland", true)
    }

    fun getPlantableSeeds(): Map<Material, Pair<Material, Material>> {
        val seedMap = mutableMapOf<Material, Pair<Material, Material>>()
        val configMap = config.getConfigurationSection("planter.plantable_seeds")?.getValues(false) ?: mapOf<String, Any>()

        for ((seedStr, targetStr) in configMap) {
            try {
                val seedMaterial = Material.valueOf(seedStr)
                val parts = (targetStr as String).split(":")
                if (parts.size == 2) {
                    val soilMaterial = Material.valueOf(parts[0])
                    val cropMaterial = Material.valueOf(parts[1])
                    seedMap[seedMaterial] = Pair(soilMaterial, cropMaterial)
                }
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid material in planter seeds config: $seedStr or $targetStr")
            }
        }

        // Return default mapping if empty
        if (seedMap.isEmpty()) {
            return mapOf(
                Material.WHEAT_SEEDS to Pair(Material.FARMLAND, Material.WHEAT),
                Material.CARROT to Pair(Material.FARMLAND, Material.CARROTS),
                Material.POTATO to Pair(Material.FARMLAND, Material.POTATOES),
                Material.BEETROOT_SEEDS to Pair(Material.FARMLAND, Material.BEETROOTS)
            )
        }

        return seedMap
    }

    fun reload() {
        plugin.reloadConfig()
        config = plugin.config
    }
}