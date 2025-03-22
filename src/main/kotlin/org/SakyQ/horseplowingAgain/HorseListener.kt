package org.SakyQ.horseplowingAgain

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.entity.minecart.HopperMinecart
import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class HorseListener(private val plugin: HorseplowingAgain) : Listener {

    private val plowTag = NamespacedKey(plugin, "isPlow")
    private val harvesterTag = NamespacedKey(plugin, "isHarvester")
    private val planterTag = NamespacedKey(plugin, "isPlanter")
    private val ownerTag = NamespacedKey(plugin, "plowOwner")

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        val player = event.player

        // Check if the entity is a horse
        if (entity.type != EntityType.HORSE) {
            return
        }

        val horse = entity as Horse

        // Check what item the player is holding
        val itemInHand = player.inventory.itemInMainHand

        if (ItemManager.isPlowItem(itemInHand)) {
            event.isCancelled = true
            handlePlowEquip(horse, player, itemInHand)
        } else if (ItemManager.isHarvesterItem(itemInHand)) {
            event.isCancelled = true
            handleHarvesterEquip(horse, player, itemInHand)
        } else if (ItemManager.isPlanterItem(itemInHand)) {
            event.isCancelled = true
            handlePlanterEquip(horse, player, itemInHand)
        } else if (player.isSneaking) {
            event.isCancelled = true

            // Check which equipment to remove
            when {
                horse.persistentDataContainer.has(plowTag, PersistentDataType.BYTE) -> {
                    handlePlowRemoval(horse, player)
                }
                horse.persistentDataContainer.has(harvesterTag, PersistentDataType.BYTE) -> {
                    handleHarvesterRemoval(horse, player)
                }
                horse.persistentDataContainer.has(planterTag, PersistentDataType.BYTE) -> {
                    handlePlanterRemoval(horse, player)
                }
            }
        }
    }

    @EventHandler
    fun onHorseDeath(event: EntityDeathEvent) {
        if (event.entityType != EntityType.HORSE) {
            return
        }

        val horse = event.entity as Horse

        // Check which equipment the horse had
        when {
            horse.persistentDataContainer.has(plowTag, PersistentDataType.BYTE) -> {
                // Remove minecart if it exists
                plugin.getPlowHorses()[horse.uniqueId]?.let { minecartId ->
                    val minecart = plugin.server.getEntity(minecartId)

                    // If it's a storage minecart, drop all items before removing
                    if (minecart is StorageMinecart) {
                        val items = minecart.inventory.contents
                        items.filterNotNull().forEach { item ->
                            horse.world.dropItemNaturally(horse.location, item)
                        }
                    }

                    minecart?.remove()
                    plugin.getPlowHorses().remove(horse.uniqueId)
                }

                // Drop the plow item
                event.drops.add(ItemManager.createPlowItem())
            }

            horse.persistentDataContainer.has(harvesterTag, PersistentDataType.BYTE) -> {
                // Remove minecart if it exists
                plugin.getHarvesterHorses()[horse.uniqueId]?.let { minecartId ->
                    val minecart = plugin.server.getEntity(minecartId)

                    // If it's a storage minecart, drop all items before removing
                    if (minecart is StorageMinecart) {
                        val items = minecart.inventory.contents
                        items.filterNotNull().forEach { item ->
                            horse.world.dropItemNaturally(horse.location, item)
                        }
                    }

                    minecart?.remove()
                    plugin.getHarvesterHorses().remove(horse.uniqueId)
                }

                // Drop the harvester item
                event.drops.add(ItemManager.createHarvesterItem())
            }

            horse.persistentDataContainer.has(planterTag, PersistentDataType.BYTE) -> {
                // Remove minecart if it exists
                plugin.getPlanterHorses()[horse.uniqueId]?.let { minecartId ->
                    val minecart = plugin.server.getEntity(minecartId)

                    // If it's a hopper minecart, drop all items before removing
                    if (minecart is HopperMinecart) {
                        val items = minecart.inventory.contents
                        items.filterNotNull().forEach { item ->
                            horse.world.dropItemNaturally(horse.location, item)
                        }
                    }

                    minecart?.remove()
                    plugin.getPlanterHorses().remove(horse.uniqueId)
                }

                // Drop the planter item
                event.drops.add(ItemManager.createPlanterItem())
            }
        }
    }

    private fun handlePlowEquip(horse: Horse, player: Player, plowItem: ItemStack) {
        // Check if the player owns the horse (if configured)
        if (plugin.getConfigManager().isHorseOwnerOnly() && !isHorseOwner(horse, player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("error.not_owner"))
            return
        }

        // Check if the horse already has equipment
        if (horse.persistentDataContainer.has(plowTag, PersistentDataType.BYTE) ||
            horse.persistentDataContainer.has(harvesterTag, PersistentDataType.BYTE) ||
            horse.persistentDataContainer.has(planterTag, PersistentDataType.BYTE)) {
            return
        }

        // Mark the horse as a plow horse
        horse.persistentDataContainer.set(plowTag, PersistentDataType.BYTE, 1)
        horse.persistentDataContainer.set(ownerTag, PersistentDataType.STRING, player.uniqueId.toString())

        // Spawn and attach minecart plow
        spawnPlowMinecart(horse)

        // Consume one plow item
        if (plowItem.amount > 1) {
            plowItem.amount--
        } else {
            player.inventory.setItemInMainHand(null)
        }

        player.sendMessage(plugin.getMessageManager().getMessage("plow.equipped"))
    }

    private fun handleHarvesterEquip(horse: Horse, player: Player, harvesterItem: ItemStack) {
        // Check if the player owns the horse (if configured)
        if (plugin.getConfigManager().isHorseOwnerOnly() && !isHorseOwner(horse, player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("error.not_owner"))
            return
        }

        // Check if the horse already has equipment
        if (horse.persistentDataContainer.has(plowTag, PersistentDataType.BYTE) ||
            horse.persistentDataContainer.has(harvesterTag, PersistentDataType.BYTE) ||
            horse.persistentDataContainer.has(planterTag, PersistentDataType.BYTE)) {
            return
        }

        // Mark the horse as a harvester horse
        horse.persistentDataContainer.set(harvesterTag, PersistentDataType.BYTE, 1)
        horse.persistentDataContainer.set(ownerTag, PersistentDataType.STRING, player.uniqueId.toString())

        // Spawn and attach minecart harvester
        spawnHarvesterMinecart(horse)

        // Consume one harvester item
        if (harvesterItem.amount > 1) {
            harvesterItem.amount--
        } else {
            player.inventory.setItemInMainHand(null)
        }

        player.sendMessage(plugin.getMessageManager().getMessage("harvester.equipped", mapOf("horse" to horse.name)))
    }

    private fun handlePlanterEquip(horse: Horse, player: Player, planterItem: ItemStack) {
        // Check if the player owns the horse (if configured)
        if (plugin.getConfigManager().isHorseOwnerOnly() && !isHorseOwner(horse, player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("error.not_owner"))
            return
        }

        // Check if the horse already has equipment
        if (horse.persistentDataContainer.has(plowTag, PersistentDataType.BYTE) ||
            horse.persistentDataContainer.has(harvesterTag, PersistentDataType.BYTE) ||
            horse.persistentDataContainer.has(planterTag, PersistentDataType.BYTE)) {
            return
        }

        // Mark the horse as a planter horse
        horse.persistentDataContainer.set(planterTag, PersistentDataType.BYTE, 1)
        horse.persistentDataContainer.set(ownerTag, PersistentDataType.STRING, player.uniqueId.toString())

        // Spawn and attach minecart planter
        spawnPlanterMinecart(horse)

        // Consume one planter item
        if (planterItem.amount > 1) {
            planterItem.amount--
        } else {
            player.inventory.setItemInMainHand(null)
        }

        player.sendMessage(plugin.getMessageManager().getMessage("planter.equipped", mapOf("horse" to horse.name)))
    }

    private fun handlePlowRemoval(horse: Horse, player: Player) {
        // Check if the player owns the horse (if configured)
        if (plugin.getConfigManager().isHorseOwnerOnly() && !isHorseOwner(horse, player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("error.not_owner"))
            return
        }

        // Remove plow metadata
        horse.persistentDataContainer.remove(plowTag)
        horse.persistentDataContainer.remove(ownerTag)

        // Remove minecart if it exists
        plugin.getPlowHorses()[horse.uniqueId]?.let { minecartId ->
            val minecart = plugin.server.getEntity(minecartId)

            // If it's a storage minecart, drop all items before removing
            if (minecart is StorageMinecart) {
                val items = minecart.inventory.contents
                items.filterNotNull().forEach { item ->
                    horse.world.dropItemNaturally(horse.location, item)
                }
            }

            minecart?.remove()
            plugin.getPlowHorses().remove(horse.uniqueId)
        }

        // Give player back a plow item
        player.inventory.addItem(ItemManager.createPlowItem())

        player.sendMessage(plugin.getMessageManager().getMessage("plow.unequipped"))
    }

    private fun handleHarvesterRemoval(horse: Horse, player: Player) {
        // Check if the player owns the horse (if configured)
        if (plugin.getConfigManager().isHorseOwnerOnly() && !isHorseOwner(horse, player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("error.not_owner"))
            return
        }

        // Remove harvester metadata
        horse.persistentDataContainer.remove(harvesterTag)
        horse.persistentDataContainer.remove(ownerTag)

        // Remove minecart if it exists
        plugin.getHarvesterHorses()[horse.uniqueId]?.let { minecartId ->
            val minecart = plugin.server.getEntity(minecartId)

            // If it's a storage minecart, drop all items before removing
            if (minecart is StorageMinecart) {
                val items = minecart.inventory.contents
                items.filterNotNull().forEach { item ->
                    horse.world.dropItemNaturally(horse.location, item)
                }
            }

            minecart?.remove()
            plugin.getHarvesterHorses().remove(horse.uniqueId)
        }

        // Give player back a harvester item
        player.inventory.addItem(ItemManager.createHarvesterItem())

        player.sendMessage(plugin.getMessageManager().getMessage("harvester.unequipped", mapOf("horse" to horse.name)))
    }

    private fun handlePlanterRemoval(horse: Horse, player: Player) {
        // Check if the player owns the horse (if configured)
        if (plugin.getConfigManager().isHorseOwnerOnly() && !isHorseOwner(horse, player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("error.not_owner"))
            return
        }

        // Remove planter metadata
        horse.persistentDataContainer.remove(planterTag)
        horse.persistentDataContainer.remove(ownerTag)

        // Remove minecart if it exists
        plugin.getPlanterHorses()[horse.uniqueId]?.let { minecartId ->
            val minecart = plugin.server.getEntity(minecartId)

            // If it's a hopper minecart, drop all items before removing
            if (minecart is HopperMinecart) {
                val items = minecart.inventory.contents
                items.filterNotNull().forEach { item ->
                    horse.world.dropItemNaturally(horse.location, item)
                }
            }

            minecart?.remove()
            plugin.getPlanterHorses().remove(horse.uniqueId)
        }

        // Give player back a planter item
        player.inventory.addItem(ItemManager.createPlanterItem())

        player.sendMessage(plugin.getMessageManager().getMessage("planter.unequipped", mapOf("horse" to horse.name)))
    }

    private fun spawnPlowMinecart(horse: Horse) {
        val location = horse.location.clone()

        // Move the minecart spawn location behind the horse
        val direction = horse.location.direction.clone().multiply(-1)
        val followDistance = plugin.getConfigManager().getMinecartFollowDistance().coerceAtMost(2.0)
        location.add(direction.multiply(followDistance))

        // Find the highest block at this location to prevent spawning in the ground
        val highestY = findSafeYPosition(location)
        location.y = highestY + 1.0 + plugin.getConfigManager().getMinecartOffsetY()

        // Spawn a chest minecart
        val minecart = horse.world.spawn(location, StorageMinecart::class.java)
        minecart.setGravity(false)
        minecart.persistentDataContainer.set(plowTag, PersistentDataType.BYTE, 1)

        // Add to tracking map
        plugin.getPlowHorses()[horse.uniqueId] = minecart.uniqueId

        // Start minecart following task
        object : BukkitRunnable() {
            override fun run() {
                // If horse or minecart is dead/removed, cancel task
                if (!horse.isValid || !minecart.isValid) {
                    cancel()
                    return
                }

                updateMinecartPosition(horse, minecart, plugin.getConfigManager().getMinecartFollowDistance(),
                    plugin.getConfigManager().getMinecartOffsetY())
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }

    private fun spawnHarvesterMinecart(horse: Horse) {
        val location = horse.location.clone()

        // Move the minecart spawn location behind the horse
        val direction = horse.location.direction.clone().multiply(-1)
        val followDistance = plugin.getConfigManager().getHarvesterMinecartFollowDistance().coerceAtMost(2.0)
        location.add(direction.multiply(followDistance))

        // Find the highest block at this location to prevent spawning in the ground
        val highestY = findSafeYPosition(location)
        location.y = highestY + 1.0 + plugin.getConfigManager().getHarvesterMinecartOffsetY()

        // Spawn a chest minecart
        val minecart = horse.world.spawn(location, StorageMinecart::class.java)
        minecart.setGravity(false)
        minecart.persistentDataContainer.set(harvesterTag, PersistentDataType.BYTE, 1)

        // Add to tracking map
        plugin.getHarvesterHorses()[horse.uniqueId] = minecart.uniqueId

        // Start minecart following task
        object : BukkitRunnable() {
            override fun run() {
                // If horse or minecart is dead/removed, cancel task
                if (!horse.isValid || !minecart.isValid) {
                    cancel()
                    return
                }

                updateMinecartPosition(horse, minecart, plugin.getConfigManager().getHarvesterMinecartFollowDistance(),
                    plugin.getConfigManager().getHarvesterMinecartOffsetY())
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }

    private fun spawnPlanterMinecart(horse: Horse) {
        val location = horse.location.clone()

        // Move the minecart spawn location behind the horse
        val direction = horse.location.direction.clone().multiply(-1)
        val followDistance = plugin.getConfigManager().getPlanterMinecartFollowDistance().coerceAtMost(2.0)
        location.add(direction.multiply(followDistance))

        // Find the highest block at this location to prevent spawning in the ground
        val highestY = findSafeYPosition(location)
        location.y = highestY + 1.0 + plugin.getConfigManager().getPlanterMinecartOffsetY()

        // Spawn a hopper minecart for planter
        val minecart = horse.world.spawn(location, HopperMinecart::class.java)
        minecart.setGravity(false)
        minecart.persistentDataContainer.set(planterTag, PersistentDataType.BYTE, 1)

        // Add to tracking map
        plugin.getPlanterHorses()[horse.uniqueId] = minecart.uniqueId

        // Start minecart following task
        object : BukkitRunnable() {
            override fun run() {
                // If horse or minecart is dead/removed, cancel task
                if (!horse.isValid || !minecart.isValid) {
                    cancel()
                    return
                }

                updateMinecartPosition(horse, minecart, plugin.getConfigManager().getPlanterMinecartFollowDistance(),
                    plugin.getConfigManager().getPlanterMinecartOffsetY())
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }

    private fun updateMinecartPosition(horse: Horse, minecart: org.bukkit.entity.Entity, followDistance: Double, offsetY: Double) {
        // Get horse facing direction
        val horseDirection = horse.location.direction.clone()

        // Calculate position behind the horse
        val targetLocation = horse.location.clone()
        targetLocation.add(horseDirection.clone().multiply(-1 * followDistance))

        // Adjust Y position
        val highestY = findSafeYPosition(targetLocation)
        targetLocation.y = highestY + 1.0 + offsetY

        // Smoothly move the minecart to target position
        val currentLocation = minecart.location.clone()
        val moveVector = targetLocation.clone().subtract(currentLocation).toVector()

        // Apply vector with a smoothing factor
        minecart.velocity = moveVector.multiply(0.5)

        // Ensure the minecart is looking in the same direction as the horse
        minecart.teleport(minecart.location.setDirection(horseDirection))
    }

    private fun findSafeYPosition(location: org.bukkit.Location): Double {
        val world = location.world
        val maxHeight = world.maxHeight - 2

        // Start from the location's y and go down until we find a solid block
        for (y in location.blockY downTo 0) {
            val checkLocation = location.clone()
            checkLocation.y = y.toDouble()

            val block = checkLocation.block
            if (block.type.isSolid) {
                return y.toDouble()
            }
        }

        // If no solid block is found, return a default value
        return 60.0
    }

    private fun isHorseOwner(horse: Horse, player: Player): Boolean {
        if (!horse.persistentDataContainer.has(ownerTag, PersistentDataType.STRING)) {
            // If no owner tag exists, the horse is not owned
            return true
        }

        val ownerUuid = horse.persistentDataContainer.get(ownerTag, PersistentDataType.STRING)
        return ownerUuid == player.uniqueId.toString()
    }
}