package com.arlight.core.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class CoreItems {

    private static final String SELECTOR_KEY = "core-selector-item";
    private static final String REWARDS_KEY = "core-rewards-item";
    private static final String PROFILE_KEY = "core-profile-item";

    private CoreItems() {
    }

    public static NamespacedKey selectorKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, SELECTOR_KEY);
    }

    public static NamespacedKey rewardsKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, REWARDS_KEY);
    }

    public static NamespacedKey profileKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, PROFILE_KEY);
    }

    public static ItemStack createSelectorItem(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.PINK_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Minijuegos");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click derecho para ver los minijuegos",
                    ChatColor.GRAY + "disponibles y unirte a uno."
            ));
            meta.getPersistentDataContainer().set(selectorKey(plugin), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createRewardsItem(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Nivel y Recompensas");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click derecho para ver tu nivel",
                    ChatColor.GRAY + "y reclamar recompensas."
            ));
            meta.getPersistentDataContainer().set(rewardsKey(plugin), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createProfileItem(JavaPlugin plugin, org.bukkit.entity.Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.AQUA + "Perfil de " + ChatColor.WHITE + player.getName());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click derecho para ver tu perfil,",
                    ChatColor.GRAY + "minipase y estadísticas."
            ));
            meta.getPersistentDataContainer().set(profileKey(plugin), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isSelectorItem(JavaPlugin plugin, ItemStack item) {
        return hasMarker(item, selectorKey(plugin));
    }

    public static boolean isRewardsItem(JavaPlugin plugin, ItemStack item) {
        return hasMarker(item, rewardsKey(plugin));
    }

    public static boolean isProfileItem(JavaPlugin plugin, ItemStack item) {
        return hasMarker(item, profileKey(plugin));
    }

    public static boolean isCoreItem(JavaPlugin plugin, ItemStack item) {
        return isSelectorItem(plugin, item) || isRewardsItem(plugin, item) || isProfileItem(plugin, item);
    }

    public static void giveIfMissing(JavaPlugin plugin, org.bukkit.entity.Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (isSelectorItem(plugin, item)) {
                player.getInventory().setItem(slot, null);
            }
            if (isRewardsItem(plugin, item) || isProfileItem(plugin, item)) {
                player.getInventory().setItem(slot, null);
            }
        }
        placeSafely(player, 0, createSelectorItem(plugin));
        placeSafely(player, 4, createRewardsItem(plugin));
        placeSafely(player, 8, createProfileItem(plugin, player));
        player.updateInventory();
    }

    public static void removeAll(JavaPlugin plugin, org.bukkit.entity.Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isCoreItem(plugin, contents[slot])) player.getInventory().setItem(slot, null);
        }
        player.updateInventory();
    }

    private static void placeSafely(org.bukkit.entity.Player player, int slot, ItemStack coreItem) {
        ItemStack displaced = player.getInventory().getItem(slot);
        if (displaced != null && displaced.getType() != Material.AIR) {
            player.getInventory().setItem(slot, null);
            java.util.Map<Integer, ItemStack> leftovers = player.getInventory().addItem(displaced.clone());
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.getInventory().setItem(slot, coreItem);
    }

    private static boolean hasMarker(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
