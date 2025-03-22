package org.SakyQ.horseplowingAgain

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.entity.Horse
import org.bukkit.entity.Minecart
import java.util.HashMap
import java.util.UUID

class HorseplowingAgain : JavaPlugin() {

    companion object {
        lateinit var instance: HorseplowingAgain
            private set
    }

    private lateinit var configManager: ConfigManager
    private lateinit var messageManager: MessageManager
    private val plowHorses = HashMap<UUID, UUID>() // Maps horse UUID to minecart UUID
    private val harvesterHorses = HashMap<UUID, UUID>() // Maps horse UUID to harvester minecart UUID
    private val planterHorses = HashMap<UUID, UUID>() // Maps horse UUID to planter minecart UUID

    override fun onEnable() {
        // Set instance for static access
        instance = this

        // Initialize config managers
        configManager = ConfigManager(this)
        messageManager = MessageManager(this)

        // Register events
        server.pluginManager.registerEvents(HorseListener(this), this)
        server.pluginManager.registerEvents(MinecartListener(this), this)
        server.pluginManager.registerEvents(PlowEffectListener(this), this)
        server.pluginManager.registerEvents(HarvesterEffectListener(this), this)
        server.pluginManager.registerEvents(PlanterEffectListener(this), this)

        // Register commands
        getCommand("horseplow")?.setExecutor(PlowCommand(this))

        // Register custom items
        ItemManager(this).registerItems()

        logger.info(messageManager.getMessage("plugin.enabled"))
    }

    override fun onDisable() {
        // Clean up any existing plow minecarts
        plowHorses.forEach { (_, minecartId) ->
            val minecart = Bukkit.getEntity(minecartId)
            minecart?.remove()
        }

        // Clean up any existing harvester minecarts
        harvesterHorses.forEach { (_, minecartId) ->
            val minecart = Bukkit.getEntity(minecartId)
            minecart?.remove()
        }

        // Clean up any existing planter minecarts
        planterHorses.forEach { (_, minecartId) ->
            val minecart = Bukkit.getEntity(minecartId)
            minecart?.remove()
        }

        logger.info(messageManager.getMessage("plugin.disabled"))
    }

    fun getConfigManager(): ConfigManager {
        return configManager
    }

    fun getMessageManager(): MessageManager {
        return messageManager
    }

    fun getPlowHorses(): HashMap<UUID, UUID> {
        return plowHorses
    }

    fun getHarvesterHorses(): HashMap<UUID, UUID> {
        return harvesterHorses
    }

    fun getPlanterHorses(): HashMap<UUID, UUID> {
        return planterHorses
    }
}