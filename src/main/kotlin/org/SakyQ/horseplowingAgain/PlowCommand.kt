package org.SakyQ.horseplowingAgain

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class PlowCommand(private val plugin: HorseplowingAgain) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.help"))
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("horseplow.admin")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"))
                    return true
                }

                plugin.getConfigManager().reload()
                plugin.getMessageManager().reload()
                sender.sendMessage(plugin.getMessageManager().getMessage("command.reload"))
                return true
            }
            "give" -> {
                if (!sender.hasPermission("horseplow.give")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"))
                    return true
                }

                if (sender !is Player) {
                    sender.sendMessage("This command can only be used by players.")
                    return true
                }

                // Check if there's a second argument specifying the item
                if (args.size > 1) {
                    when (args[1].lowercase()) {
                        "plow" -> {
                            sender.inventory.addItem(ItemManager.createPlowItem())
                            sender.sendMessage("§aYou've been given a Horse Plow!")
                        }
                        "harvester" -> {
                            sender.inventory.addItem(ItemManager.createHarvesterItem())
                            sender.sendMessage("§aYou've been given a Horse Harvester!")
                        }
                        "planter" -> {
                            sender.inventory.addItem(ItemManager.createPlanterItem())
                            sender.sendMessage("§aYou've been given a Horse Planter!")
                        }
                        else -> {
                            sender.sendMessage("§cInvalid item type. Use: plow, harvester, or planter")
                        }
                    }
                } else {
                    // Default to giving a plow if no item specified
                    sender.inventory.addItem(ItemManager.createPlowItem())
                    sender.sendMessage("§aYou've been given a Horse Plow!")
                }
                return true
            }
            "help" -> {
                sender.sendMessage("§6Horse Plowing Again - Commands:")
                sender.sendMessage("§e/horseplow reload §7- Reload the plugin configuration")
                sender.sendMessage("§e/horseplow give [plow|harvester|planter] §7- Get a tool")
                sender.sendMessage("§e/horseplow help §7- Show this help message")
                return true
            }
            else -> {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.help"))
                return true
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            val subCommands = listOf("reload", "give", "help")

            for (subCommand in subCommands) {
                if (subCommand.startsWith(args[0].lowercase())) {
                    completions.add(subCommand)
                }
            }
        } else if (args.size == 2 && args[0].equals("give", ignoreCase = true)) {
            val items = listOf("plow", "harvester", "planter")

            for (item in items) {
                if (item.startsWith(args[1].lowercase())) {
                    completions.add(item)
                }
            }
        }

        return completions
    }
}