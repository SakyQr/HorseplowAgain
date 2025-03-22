package org.SakyQ.horseplowingAgain

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Minecart
import org.bukkit.entity.minecart.HopperMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class PlanterEffectListener(private val plugin: HorseplowingAgain) : Listener {

    private val planterTag = NamespacedKey(plugin, "isPlanter")
    private val cooldownMap = HashMap<Minecart, Long>()

    @EventHandler
    fun onMinecartMove(event: VehicleMoveEvent) {
        val vehicle = event.vehicle

        if (vehicle !is Minecart) {
            return
        }

        // Check if this is a planter minecart
        if (!vehicle.persistentDataContainer.has(planterTag, PersistentDataType.BYTE)) {
            return
        }

        // Check cooldown
        val currentTime = System.currentTimeMillis()
        val lastPlantTime = cooldownMap[vehicle] ?: 0L
        val cooldownMs = plugin.getConfigManager().getPlanterCooldown() * 50L // Convert ticks to ms

        if (currentTime - lastPlantTime < cooldownMs) {
            return
        }

        // Plant seeds in nearby blocks
        cooldownMap[vehicle] = currentTime
        plantNearbyBlocks(vehicle)
    }

    private fun plantNearbyBlocks(minecart: Minecart) {
        if (minecart !is HopperMinecart) {
            return
        }

        val location = minecart.location
        val world = location.world
        val radius = plugin.getConfigManager().getPlanterRadius()
        var blocksPlanted = 0
        val seedMap = plugin.getConfigManager().getPlantableSeeds()

        // Find the horse associated with this minecart
        var horseId: java.util.UUID? = null
        for ((hId, mId) in plugin.getPlanterHorses()) {
            if (mId == minecart.uniqueId) {
                horseId = hId
                break
            }
        }

        // If horse not found, return
        if (horseId == null) {
            return
        }

        // Get items from hopper minecart inventory
        val inventory = minecart.inventory
        val availableSeeds = mutableMapOf<Material, Int>()

        // Check what seeds are available
        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i)
            if (item != null && seedMap.containsKey(item.type)) {
                availableSeeds[item.type] = (availableSeeds[item.type] ?: 0) + item.amount
            }
        }

        // No seeds available, nothing to plant
        if (availableSeeds.isEmpty()) {
            return
        }

        // Check blocks in radius
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                // Skip blocks too far away (create a circle rather than square)
                if (x * x + z * z > radius * radius) {
                    continue
                }

                val blockX = location.blockX + x
                val blockZ = location.blockZ + z

                // Check at current Y level and one below for farmland
                for (y in -1..0) {
                    val blockY = location.blockY + y
                    val block = world.getBlockAt(blockX, blockY, blockZ)
                    val blockAbove = world.getBlockAt(blockX, blockY + 1, blockZ)

                    // Check if we can plant here
                    if (blockAbove.type == Material.AIR) {
                        // Check all available seeds to see if any can be planted here
                        for ((seedType, count) in availableSeeds.toMap()) { // Create copy to avoid concurrent modification
                            if (count <= 0) continue

                            val plantData = seedMap[seedType] ?: continue
                            val targetSoil = plantData.first
                            val targetCrop = plantData.second

                            // If only planting on farmland is enabled, check if the block is farmland
                            if (plugin.getConfigManager().isPlanterOnlyOnFarmland() &&
                                targetSoil == Material.FARMLAND && block.type != Material.FARMLAND) {
                                continue
                            }

                            // If block matches the required soil type
                            if (block.type == targetSoil) {
                                // Plant the crop
                                blockAbove.type = targetCrop

                                // Remove one seed from inventory
                                for (i in 0 until inventory.size) {
                                    val item = inventory.getItem(i)
                                    if (item != null && item.type == seedType) {
                                        if (item.amount > 1) {
                                            item.amount--
                                            inventory.setItem(i, item)
                                        } else {
                                            inventory.setItem(i, null)
                                        }

                                        // Update available seeds count
                                        availableSeeds[seedType] = (availableSeeds[seedType] ?: 1) - 1
                                        blocksPlanted++
                                        break
                                    }
                                }

                                // Move to next block after planting
                                break
                            }
                        }
                    }
                }
            }
        }

        // Play effects if any blocks were planted
        if (blocksPlanted > 0) {
            playPlantingEffects(minecart.location)
        }
    }

    private fun playPlantingEffects(location: org.bukkit.Location) {
        val configManager = plugin.getConfigManager()

        // Play particles
        if (configManager.arePlanterParticlesEnabled()) {
            location.world.spawnParticle(
                Particle.HAPPY_VILLAGER,
                location,
                20,
                1.0, 0.5, 1.0,
                0.05
            )
        }

        // Play sound
        if (configManager.isPlanterSoundEnabled()) {
            try {
                val soundName = configManager.getPlanterSoundType()
                val sound = Sound.valueOf(soundName)
                location.world.playSound(
                    location,
                    sound,
                    configManager.getPlanterSoundVolume(),
                    configManager.getPlanterSoundPitch()
                )
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid sound type in config: ${configManager.getPlanterSoundType()}")
                // Fallback to a default sound
                location.world.playSound(
                    location,
                    Sound.BLOCK_GRASS_PLACE,
                    configManager.getPlanterSoundVolume(),
                    configManager.getPlanterSoundPitch()
                )
            }
        }
    }
}