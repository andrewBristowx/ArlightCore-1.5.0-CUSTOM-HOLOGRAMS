package com.arlight.core;

import com.arlight.core.api.ArlightCoreAPI;
import com.arlight.core.commands.CoreCommand;
import com.arlight.core.listeners.CoreGUIListener;
import com.arlight.core.listeners.CoreItemListener;
import com.arlight.core.listeners.MinigameSessionListener;
import com.arlight.core.listeners.LobbyProtectionListener;
import com.arlight.core.leaderboard.LeaderboardManager;
import com.arlight.core.hologram.HologramManager;
import com.arlight.core.registry.MinigameRegistry;
import com.arlight.core.reward.RewardManager;
import com.arlight.core.session.MinigameSessionManager;
import com.arlight.core.stats.PlayerStatsManager;
import com.arlight.core.xp.PlayerLevelManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class ArlightCorePlugin extends JavaPlugin {

    private MinigameRegistry minigameRegistry;
    private PlayerLevelManager levelManager;
    private RewardManager rewardManager;
    private MinigameSessionManager sessionManager;
    private PlayerStatsManager statsManager;
    private LeaderboardManager leaderboardManager;
    private HologramManager hologramManager;

    private int xpPerWin;
    private boolean giveItemsOnJoin;
    private String lobbyWorld;
    private boolean teleportToLobbyOnJoin;
    private double lobbyVoidY;
    private boolean lobbyBlockProtection;
    private List<String> claimWorlds = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.minigameRegistry = new MinigameRegistry();
        this.levelManager = new PlayerLevelManager(this);
        this.rewardManager = new RewardManager(this);
        this.sessionManager = new MinigameSessionManager(this);
        this.statsManager = new PlayerStatsManager(this);
        this.leaderboardManager = new LeaderboardManager(this);
        this.hologramManager = new HologramManager(this);

        loadCoreConfigValues();
        levelManager.load();
        rewardManager.load();
        sessionManager.load();
        statsManager.load();

        ArlightCoreAPI.init(minigameRegistry, levelManager, rewardManager, sessionManager,
                statsManager, this, xpPerWin);

        getServer().getPluginManager().registerEvents(new CoreItemListener(this), this);
        getServer().getPluginManager().registerEvents(new MinigameSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new CoreGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyProtectionListener(this), this);
        CoreCommand coreCommand = new CoreCommand(this);
        getCommand("core").setExecutor(coreCommand);
        getCommand("core").setTabCompleter(coreCommand);
        leaderboardManager.start();
        hologramManager.start();

        getLogger().info("ArlightCore habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        if (levelManager != null) levelManager.save();
        if (rewardManager != null) rewardManager.save();
        if (sessionManager != null) sessionManager.save();
        if (statsManager != null) statsManager.save();
        if (leaderboardManager != null) leaderboardManager.stop();
        if (hologramManager != null) hologramManager.stop();
        getLogger().info("ArlightCore deshabilitado.");
    }

    public void reloadCoreConfig() {
        reloadConfig();
        loadCoreConfigValues();
    }

    private void loadCoreConfigValues() {
        this.xpPerWin = getConfig().getInt("xp.xp-per-win", 5);
        int xpPerLevel = getConfig().getInt("xp.xp-per-level", 30);
        this.giveItemsOnJoin = getConfig().getBoolean("give-items-on-join", true);
        this.lobbyWorld = getConfig().getString("lobby-world", "legos");
        this.teleportToLobbyOnJoin = getConfig().getBoolean("lobby.teleport-on-join", true);
        this.lobbyVoidY = getConfig().getDouble("lobby.void-rescue-y", -10.0);
        this.lobbyBlockProtection = getConfig().getBoolean("lobby.protect-blocks", true);
        this.claimWorlds = getConfig().getStringList("claim-worlds");
        if (levelManager != null) {
            levelManager.setXpPerLevel(xpPerLevel);
        }
    }

    public MinigameRegistry getMinigameRegistry() {
        return minigameRegistry;
    }

    public PlayerLevelManager getLevelManager() {
        return levelManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public MinigameSessionManager getSessionManager() {
        return sessionManager;
    }

    public PlayerStatsManager getStatsManager() {
        return statsManager;
    }

    public boolean isGiveItemsOnJoin() {
        return giveItemsOnJoin;
    }

    public List<String> getClaimWorlds() {
        return claimWorlds;
    }

    public String getLobbyWorld() {
        return lobbyWorld;
    }

    public boolean isTeleportToLobbyOnJoin() {
        return teleportToLobbyOnJoin;
    }

    public double getLobbyVoidY() {
        return lobbyVoidY;
    }

    public boolean isLobbyBlockProtectionEnabled() {
        return lobbyBlockProtection;
    }

    public Location getLobbySpawn() {
        World world = Bukkit.getWorld(lobbyWorld);
        if (world == null) return null;
        if (!getConfig().isSet("lobby.spawn.x")) return world.getSpawnLocation();
        return new Location(world,
                getConfig().getDouble("lobby.spawn.x"),
                getConfig().getDouble("lobby.spawn.y"),
                getConfig().getDouble("lobby.spawn.z"),
                (float) getConfig().getDouble("lobby.spawn.yaw"),
                (float) getConfig().getDouble("lobby.spawn.pitch"));
    }

    public void setLobbySpawn(Location location) {
        if (location.getWorld() == null) return;
        this.lobbyWorld = location.getWorld().getName();
        getConfig().set("lobby-world", lobbyWorld);
        getConfig().set("lobby.spawn.x", location.getX());
        getConfig().set("lobby.spawn.y", location.getY());
        getConfig().set("lobby.spawn.z", location.getZ());
        getConfig().set("lobby.spawn.yaw", location.getYaw());
        getConfig().set("lobby.spawn.pitch", location.getPitch());
        saveConfig();
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}
