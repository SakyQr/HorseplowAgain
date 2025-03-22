package org.SakyQ.horseplowingAgain

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataType

class ItemManager(private val plugin: HorseplowingAgain) {

    companion object {
        private const val PLOW_ID = "horse_plow"
        private const val HARVESTER_ID = "horse_harvester"
        private const val PLANTER_ID = "horse_planter"
        private const val DURABILITY_KEY = "plow_durability"

        fun isPlowItem(item: ItemStack): Boolean {
            if (item.type != Material.IRON_HOE) {
                return false
            }

            val meta = item.itemMeta ?: return false
            val key = NamespacedKey(HorseplowingAgain.instance, PLOW_ID)
            return meta.persistentDataContainer.has(key, PersistentDataType.BYTE)
        }

        fun isHarvesterItem(item: ItemStack): Boolean {
            if (item.type != Material.CHEST) {
                return false
            }

            val meta = item.itemMeta ?: return false
            val key = NamespacedKey(HorseplowingAgain.instance, HARVESTER_ID)
            return meta.persistentDataContainer.has(key, PersistentDataType.BYTE)
        }

        fun isPlanterItem(item: ItemStack): Boolean {
            if (item.type != Material.HOPPER) {
                return false
            }

            val meta = item.itemMeta ?: return false
            val key = NamespacedKey(HorseplowingAgain.instance, PLANTER_ID)
            return meta.persistentDataContainer.has(key, PersistentDataType.BYTE)
        }

        fun createPlowItem(durability: Int = -1): ItemStack {
            val horsePlow = ItemStack(Material.IRON_HOE, 1)
            val meta = horsePlow.itemMeta ?: return horsePlow

            val configManager = HorseplowingAgain.instance.getConfigManager()
            val actualDurability = if (durability > 0) durability else configManager.getPlowDurability()

            meta.setDisplayName("§6Horse Plow")

            val lore = mutableListOf(
                "§7Right-click on a horse to equip",
                "§7The horse will pull a chest minecart behind it",
                "§7that tills soil and collects harvests",
                "§8Durability: $actualDurability/${configManager.getPlowDurability()}"
            )
            meta.lore = lore

            // Add glow effect
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

            // Add custom tags to identify this as a plow item
            val plowKey = NamespacedKey(HorseplowingAgain.instance, PLOW_ID)
            meta.persistentDataContainer.set(plowKey, PersistentDataType.BYTE, 1)

            val durabilityKey = NamespacedKey(HorseplowingAgain.instance, DURABILITY_KEY)
            meta.persistentDataContainer.set(durabilityKey, PersistentDataType.INTEGER, actualDurability)

            horsePlow.itemMeta = meta
            return horsePlow
        }

        fun createHarvesterItem(): ItemStack {
            val harvester = ItemStack(Material.CHEST, 1)
            val meta = harvester.itemMeta ?: return harvester

            meta.setDisplayName("§6Horse Harvester")

            val lore = mutableListOf(
                "§7Right-click on a horse to equip",
                "§7The horse will pull a chest minecart behind it",
                "§7that automatically harvests mature crops"
            )
            meta.lore = lore

            // Add glow effect
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

            // Add custom tags to identify this as a harvester item
            val harvesterKey = NamespacedKey(HorseplowingAgain.instance, HARVESTER_ID)
            meta.persistentDataContainer.set(harvesterKey, PersistentDataType.BYTE, 1)

            harvester.itemMeta = meta
            return harvester
        }

        fun createPlanterItem(): ItemStack {
            val planter = ItemStack(Material.HOPPER, 1)
            val meta = planter.itemMeta ?: return planter

            meta.setDisplayName("§6Horse Planter")

            val lore = mutableListOf(
                "§7Right-click on a horse to equip",
                "§7The horse will pull a hopper minecart behind it",
                "§7that automatically plants seeds on farmland"
            )
            meta.lore = lore

            // Add glow effect
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

            // Add custom tags to identify this as a planter item
            val planterKey = NamespacedKey(HorseplowingAgain.instance, PLANTER_ID)
            meta.persistentDataContainer.set(planterKey, PersistentDataType.BYTE, 1)

            planter.itemMeta = meta
            return planter
        }

        fun getPlowDurability(item: ItemStack): Int {
            if (!isPlowItem(item)) {
                return 0
            }

            val meta = item.itemMeta ?: return 0
            val durabilityKey = NamespacedKey(HorseplowingAgain.instance, DURABILITY_KEY)
            return meta.persistentDataContainer.getOrDefault(
                durabilityKey,
                PersistentDataType.INTEGER,
                HorseplowingAgain.instance.getConfigManager().getPlowDurability()
            )
        }
    }

    fun registerItems() {
        registerPlowRecipe()
        registerHarvesterRecipe()
        registerPlanterRecipe()
    }

    private fun registerPlowRecipe() {
        val key = NamespacedKey(plugin, "horse_plow")
        val recipe = ShapedRecipe(key, createPlowItem())

        recipe.shape("III", "IMC", " S ")
        recipe.setIngredient('I', Material.IRON_INGOT)
        recipe.setIngredient('M', Material.MINECART)
        recipe.setIngredient('C', Material.CHEST)
        recipe.setIngredient('S', Material.STICK)

        plugin.server.addRecipe(recipe)
    }

    private fun registerHarvesterRecipe() {
        val key = NamespacedKey(plugin, "horse_harvester")
        val recipe = ShapedRecipe(key, createHarvesterItem())

        recipe.shape("ISI", "IMC", " S ")
        recipe.setIngredient('I', Material.IRON_INGOT)
        recipe.setIngredient('S', Material.SHEARS)
        recipe.setIngredient('M', Material.MINECART)
        recipe.setIngredient('C', Material.CHEST)

        plugin.server.addRecipe(recipe)
    }

    private fun registerPlanterRecipe() {
        val key = NamespacedKey(plugin, "horse_planter")
        val recipe = ShapedRecipe(key, createPlanterItem())

        recipe.shape("ISI", "IMH", " S ")
        recipe.setIngredient('I', Material.IRON_INGOT)
        recipe.setIngredient('S', Material.WHEAT_SEEDS)
        recipe.setIngredient('M', Material.MINECART)
        recipe.setIngredient('H', Material.HOPPER)

        plugin.server.addRecipe(recipe)
    }
}