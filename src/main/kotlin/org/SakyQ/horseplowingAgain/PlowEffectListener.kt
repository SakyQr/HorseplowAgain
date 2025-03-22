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
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class PlowEffectListener(private val plugin: HorseplowingAgain) : Listener {

    private val plowTag = NamespacedKey(plugin, "isPlow")
    private val cooldownMap = HashMap<Minecart, Long>()

    @EventHandler
    fun onMinecartMove(event: VehicleMoveEvent) {
        val vehicle = event.vehicle

        if (vehicle !is Minecart) {
            return
        }

        // Check if this is a plow minecart
        if (!vehicle.persistentDataContainer.has(plowTag, PersistentDataType.BYTE)) {
            return
        }

        // Check cooldown
        val currentTime = System.currentTimeMillis()
        val lastPlowTime = cooldownMap[vehicle] ?: 0L
        val cooldownMs = plugin.getConfigManager().getPlowCooldown() * 50L // Convert ticks to ms

        if (currentTime - lastPlowTime < cooldownMs) {
            return
        }

        // Plow nearby blocks
        cooldownMap[vehicle] = currentTime
        plowNearbyBlocks(vehicle)
    }

    private fun plowNearbyBlocks(minecart: Minecart) {
        val location = minecart.location
        val world = location.world
        val radius = plugin.getConfigManager().getPlowRadius()
        var blocksPlowed = 0
        val harvestedItems = mutableListOf<ItemStack>()

        // Find the horse associated with this minecart
        var horseId: java.util.UUID? = null
        for ((hId, mId) in plugin.getPlowHorses()) {
            if (mId == minecart.uniqueId) {
                horseId = hId
                break
            }
        }

        // If horse not found, return
        if (horseId == null) {
            return
        }

        // Plow blocks in radius
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                // Skip blocks too far away (create a circle rather than square)
                if (x * x + z * z > radius * radius) {
                    continue
                }

                val blockX = location.blockX + x
                val blockZ = location.blockZ + z

                // Check blocks at and below current Y position for potential farming
                for (y in 0 downTo -1) {
                    val blockY = location.blockY + y
                    val block = world.getBlockAt(blockX, blockY, blockZ)

                    // First, check if it's a harvestable crop and harvest if configured
                    if (plugin.getConfigManager().shouldHarvestCrops() && isHarvestableCrop(block)) {
                        val items = harvestCrop(block)
                        harvestedItems.addAll(items)
                        // After harvesting, we don't need to plow this block
                        break
                    }

                    // If not a crop, check if it's plowable
                    if (isPlowableBlock(block)) {
                        plowBlock(block)
                        blocksPlowed++
                        break
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

        // Play effects if any blocks were plowed
        if (blocksPlowed > 0 || harvestedItems.isNotEmpty()) {
            playPlowEffects(minecart.location)
        }
    }

    private fun isPlowableBlock(block: Block): Boolean {
        return plugin.getConfigManager().getPlowableBlocks().contains(block.type)
    }

    private fun plowBlock(block: Block) {
        block.type = Material.FARMLAND
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

        // Reset the crop if it's a replantable crop
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

        return harvestedItems
    }

    private fun playPlowEffects(location: org.bukkit.Location) {
        val configManager = plugin.getConfigManager()

        // Play particles
        if (configManager.areParticlesEnabled()) {
            location.world.spawnParticle(
                Particle.DUST,
                location,
                20,
                1.0, 0.1, 1.0,
                0.05,
                Particle.DustOptions(org.bukkit.Color.fromRGB(139, 69, 19), 1.0f)
            )
        }

        // Play sound
        if (configManager.isSoundEnabled()) {
            try {
                val soundName = configManager.getSoundType()
                val sound = Sound.valueOf(soundName)
                location.world.playSound(
                    location,
                    sound,
                    configManager.getSoundVolume(),
                    configManager.getSoundPitch()
                )
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid sound type in config: ${configManager.getSoundType()}")
                // Fallback to a default sound
                location.world.playSound(
                    location,
                    Sound.BLOCK_GRASS_BREAK,
                    configManager.getSoundVolume(),
                    configManager.getSoundPitch()
                )
            }
        }
    }
}