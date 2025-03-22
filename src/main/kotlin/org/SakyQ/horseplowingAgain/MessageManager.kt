// Message Manager Class (Fixed)
package org.SakyQ.horseplowingAgain

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class MessageManager(private val plugin: JavaPlugin) {
    private var messagesConfig: YamlConfiguration

    // Default messages
    private val defaultMessages = mapOf(
        "plugin.enabled" to "&aHorseplowingAgain plugin has been enabled!",
        "plugin.disabled" to "&cHorseplowingAgain plugin has been disabled!",
        "plow.equipped" to "&aYou equipped a plow to this horse!",
        "plow.unequipped" to "&cYou removed the plow from this horse.",
        "plow.in_use" to "&ePlowing the fields...",
        "plow.cooldown" to "&cThe plow is cooling down!",
        "plow.equip_cooldown" to "&cYou must wait %time% seconds before equipping/unequipping a plow again.",
        "plow.durability_warning" to "&eYour plow is getting worn out! (%durability% uses left)",
        "plow.broken" to "&cYour plow has broken!",
        "command.reload" to "&aConfiguration reloaded!",
        "command.help" to "&6HorsePlowing Commands:\n&e/horseplow reload &7- Reload configuration\n&e/horseplow give &7- Give yourself a horse plow",
        "error.not_owner" to "&cYou don't own this horse!",
        "error.not_horse" to "&cThis isn't a horse!",
        "error.no_permission" to "&cYou don't have permission to use this command!"
    )

    init {
        // Create and load the messages config
        val messagesFile = File(plugin.dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            messagesFile.parentFile.mkdirs()

            // Create the file with default messages
            messagesFile.createNewFile()
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile)

            // Set default messages
            for ((path, value) in defaultMessages) {
                messagesConfig.set(path, value)
            }

            messagesConfig.save(messagesFile)
        } else {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile)

            // Add any missing default messages
            var shouldSave = false
            for ((path, value) in defaultMessages) {
                if (!messagesConfig.contains(path)) {
                    messagesConfig.set(path, value)
                    shouldSave = true
                }
            }

            if (shouldSave) {
                messagesConfig.save(messagesFile)
            }
        }
    }

    fun getMessage(path: String, replacements: Map<String, String> = emptyMap()): String {
        var message = messagesConfig.getString(path) ?: defaultMessages[path] ?: "Missing message: $path"
        message = message.replace("&", "§")

        // Apply any replacements
        for ((key, value) in replacements) {
            message = message.replace("%$key%", value)
        }

        return message
    }

    fun reload() {
        val messagesFile = File(plugin.dataFolder, "messages.yml")
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile)
    }
}