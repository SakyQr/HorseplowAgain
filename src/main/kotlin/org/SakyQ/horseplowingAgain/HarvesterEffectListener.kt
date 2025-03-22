package org.SakyQ.horseplowingAgain

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Minecart
import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class HarvesterEffectListener(private val plugin: HorseplowingAgain) : Listener {

    private val harvesterTag = NamespacedKey(plugin, "isHarvester")
    private val cooldownMap = HashMap<Minecart, Long>()

    @EventHandler
    fun onMinecartMove(event: VehicleMoveEvent) {
        val vehicle = event.vehicle

        if (vehicle !is Minecart) {
            return
        }

        // Check if this is a harvester minecart
        if (!vehicle.persistentDataContainer.has(harvesterTag, PersistentDataType.BYTE)) {
            return
        }

        // Check cooldown
        val currentTime = System.currentTimeMillis()
        val lastHarvestTime = cooldownMap[vehicle] ?: 0L
        val cooldownMs = plugin.getConfigManager().getHarvesterCooldown() * 50L // Convert ticks to ms

        if (currentTime - lastHarvestTime < cooldownMs) {
            return
        }

        // Harvest nearby crops
        cooldownMap[vehicle] = currentTime
        harvestNearbyCrops(vehicle)
    }

    private fun harvestNearbyCrops(minecart: Minecart) {
        val location = minecart.location
        val world = location.world
        val radius = plugin.getConfigManager().getHarvesterRadius()
        var cropsHarvested = 0
        val harvestedItems = mutableListOf<ItemStack>()

        // Find the horse associated with this minecart
        var horseId: java.util.UUID? = null
        for ((hId, mId) in plugin.getHarvesterHorses()) {
            if (mId == minecart.uniqueId) {
                horseId = hId
                break
            }
        }

        // If horse not found, return
        if (horseId == null) {
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

                // Check at current Y level and one above/below for crops
                for (y in -1..1) {
                    val blockY = location.blockY + y
                    val block = world.getBlockAt(blockX, blockY, blockZ)

                    if (isHarvestableCrop(block)) {
                        val items = harvestCrop(block)
                        harvestedItems.addAll(items)
                        cropsHarvested++
                    }
                }
            }
        }

        // Add harvested items to storage minecart if it's a storage minecart
        if (minecart is StorageMinecart && harvestedItems.isNotEmpty()) {
            for (item in harvestedItems) {
                // Try to add to existing stacks first
                val leftover = minecart.inventory.addItem(item)

                // Drop any items that couldn't fit in the minecart
                if (leftover.isNotEmpty()) {
                    for (dropItem in leftover.values) {
                        world.dropItemNaturally(minecart.location, dropItem)
                    }
                }
            }
        }

        // Play effects if any crops were harvested
        if (cropsHarvested > 0) {
            playHarvestEffects(minecart.location)
        }
    }

    private fun isHarvestableCrop(block: Block): Boolean {
        // Check if block type is in harvestable list
        if (!plugin.getConfigManager().getHarvestableCrops().contains(block.type)) {
            return false
        }

        // Check if the crop is fully grown
        val blockData = block.blockData
        if (blockData is Ageable) {
            return blockData.age >= blockData.maximumAge
        }

        return false
    }

    private fun harvestCrop(block: Block): List<ItemStack> {
        val harvestedItems = mutableListOf<ItemStack>()
        val blockType = block.type

        // Get the drops from the block
        val drops = block.getDrops()
        harvestedItems.addAll(drops)

        // Reset the crop if it's a replantable crop and configured to replant
        if (plugin.getConfigManager().shouldReplantCrops()) {
            when (blockType) {
                Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS -> {
                    // Get the blockdata and reset age to 0
                    val blockData = block.blockData as Ageable
                    blockData.age = 0
                    block.blockData = blockData
                }
                else -> {
                    // For other crops, just break the block
                    block.type = Material.AIR
                }
            }
        } else {
            // If not configured to replant, just break the block
            block.type = Material.AIR
        }

        return harvestedItems
    }

    private fun playHarvestEffects(location: org.bukkit.Location) {
        val configManager = plugin.getConfigManager()

        // Play particles
        if (configManager.areHarvesterParticlesEnabled()) {
            location.world.spawnParticle(
                Particle.DUST,
                location,
                30,
                1.0, 0.5, 1.0,
                0.1,
                Material.HAY_BLOCK.createBlockData()
            )
        }

        // Play sound
        if (configManager.isHarvesterSoundEnabled()) {
            try {
                val soundName = configManager.getHarvesterSoundType()
                val sound = Sound.valueOf(soundName)
                location.world.playSound(
                    location,
                    sound,
                    configManager.getHarvesterSoundVolume(),
                    configManager.getHarvesterSoundPitch()
                )
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid sound type in config: ${configManager.getHarvesterSoundType()}")
                // Fallback to a default sound
                location.world.playSound(
                    location,
                    Sound.BLOCK_CROP_BREAK,
                    configManager.getHarvesterSoundVolume(),
                    configManager.getHarvesterSoundPitch()
                )
            }
        }
    }
}