package com.arlight.core.api;

import com.arlight.core.reward.RewardManager;
import com.arlight.core.xp.PlayerLevelManager;
import com.arlight.core.registry.MinigameRegistry;
import com.arlight.core.items.CoreItems;
import com.arlight.core.session.MinigameSessionManager;
import com.arlight.core.stats.PlayerStatsManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Punto de entrada estatico para que otros plugins (Bingo, SkyWars, etc.) interactuen
 * con ArlightCore: registrar su minijuego en el selector y otorgar XP al ganar.
 *
 * Antes de usar esta clase, el plugin llamador debe comprobar que ArlightCore este
 * instalado (ej. Bukkit.getPluginManager().getPlugin("ArlightCore") != null) para
 * evitar errores si no esta presente.
 */
public final class ArlightCoreAPI {

    private static MinigameRegistry registry;
    private static PlayerLevelManager levelManager;
    private static RewardManager rewardManager;
    private static MinigameSessionManager sessionManager;
    private static PlayerStatsManager statsManager;
    private static JavaPlugin plugin;
    private static int xpPerWin = 5;

    private ArlightCoreAPI() {
    }

    /** Uso interno del plugin ArlightCore -- no llamar desde otros plugins. */
    public static void init(MinigameRegistry registry, PlayerLevelManager levelManager,
                            RewardManager rewardManager, MinigameSessionManager sessionManager,
                            PlayerStatsManager statsManager, JavaPlugin plugin, int xpPerWin) {
        ArlightCoreAPI.registry = registry;
        ArlightCoreAPI.levelManager = levelManager;
        ArlightCoreAPI.rewardManager = rewardManager;
        ArlightCoreAPI.sessionManager = sessionManager;
        ArlightCoreAPI.statsManager = statsManager;
        ArlightCoreAPI.plugin = plugin;
        ArlightCoreAPI.xpPerWin = xpPerWin;
    }

    public static void registerMinigame(MinigameProvider provider) {
        if (registry != null) registry.register(provider);
    }

    public static void unregisterMinigame(String id) {
        if (registry != null) registry.unregister(id);
    }

    /** Da la XP configurada por ganar un minijuego (por defecto 5, ver config.yml de ArlightCore). */
    public static void addWinXp(Player player) {
        if (levelManager != null) {
            levelManager.addXp(player.getUniqueId(), xpPerWin);
        }
        if (statsManager != null && sessionManager != null) {
            String minigame = sessionManager.getMinigameId(player.getUniqueId());
            if (minigame != null) statsManager.recordWin(player.getUniqueId(), minigame);
        }
    }

    public static void addXp(Player player, int amount) {
        if (levelManager != null) {
            levelManager.addXp(player.getUniqueId(), amount);
        }
    }

    public static int getLevel(Player player) {
        return levelManager != null ? levelManager.getLevel(player.getUniqueId()) : 0;
    }

    public static int getXp(Player player) {
        return levelManager != null ? levelManager.getXp(player.getUniqueId()) : 0;
    }

    /** Guarda el inventario y reserva al jugador para un único minijuego. */
    public static boolean beginMinigameSession(Player player, String minigameId) {
        if (sessionManager == null) return false;
        boolean alreadyActive = sessionManager.hasSession(player.getUniqueId());
        boolean started = sessionManager.begin(player, minigameId);
        if (started && !alreadyActive && statsManager != null) {
            statsManager.recordPlayed(player.getUniqueId(), minigameId);
        }
        return started;
    }

    /** Finaliza la sesión y restaura el inventario que tenía antes de entrar. */
    public static boolean endMinigameSession(Player player) {
        String minigame = sessionManager == null ? null : sessionManager.getMinigameId(player.getUniqueId());
        boolean restored = sessionManager != null && sessionManager.end(player, true);
        if (restored && statsManager != null && minigame != null) {
            statsManager.recordCompleted(player.getUniqueId(), minigame);
        }
        if (restored && plugin != null) CoreItems.giveIfMissing(plugin, player);
        return restored;
    }

    public static boolean isInMinigame(Player player) {
        return sessionManager != null && sessionManager.hasSession(player.getUniqueId());
    }

    public static String getCurrentMinigame(Player player) {
        return sessionManager == null ? null : sessionManager.getMinigameId(player.getUniqueId());
    }

    public static void giveCoreItems(Player player) {
        if (plugin != null) CoreItems.giveIfMissing(plugin, player);
    }
}
