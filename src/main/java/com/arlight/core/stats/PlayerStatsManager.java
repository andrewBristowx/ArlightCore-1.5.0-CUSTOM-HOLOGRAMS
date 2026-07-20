package com.arlight.core.stats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Estadísticas persistentes globales y separadas por minijuego. */
public final class PlayerStatsManager {

    public record Stats(int played, int wins, int losses, int abandons,
                        int currentStreak, int bestStreak) {
        public double winRate() {
            return played <= 0 ? 0.0 : (wins * 100.0) / played;
        }
    }

    private static final Stats EMPTY = new Stats(0, 0, 0, 0, 0, 0);
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Stats>> values = new HashMap<>();
    private final Set<UUID> winnersInCurrentSession = new HashSet<>();

    public PlayerStatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public Stats get(UUID uuid, String minigameId) {
        return values.getOrDefault(uuid, Map.of()).getOrDefault(normalize(minigameId), EMPTY);
    }

    public Stats getGlobal(UUID uuid) {
        int played = 0, wins = 0, losses = 0, abandons = 0, current = 0, best = 0;
        for (Stats stats : values.getOrDefault(uuid, Map.of()).values()) {
            played += stats.played();
            wins += stats.wins();
            losses += stats.losses();
            abandons += stats.abandons();
            current = Math.max(current, stats.currentStreak());
            best = Math.max(best, stats.bestStreak());
        }
        return new Stats(played, wins, losses, abandons, current, best);
    }

    public Set<String> getMinigames(UUID uuid) {
        return Set.copyOf(values.getOrDefault(uuid, Map.of()).keySet());
    }

    public Set<UUID> getKnownPlayers() {
        return Set.copyOf(values.keySet());
    }

    public void recordPlayed(UUID uuid, String minigameId) {
        Stats old = get(uuid, minigameId);
        put(uuid, minigameId, new Stats(old.played() + 1, old.wins(), old.losses(),
                old.abandons(), old.currentStreak(), old.bestStreak()));
    }

    public void recordWin(UUID uuid, String minigameId) {
        if (!winnersInCurrentSession.add(uuid)) return;
        Stats old = get(uuid, minigameId);
        int streak = old.currentStreak() + 1;
        put(uuid, minigameId, new Stats(old.played(), old.wins() + 1, old.losses(),
                old.abandons(), streak, Math.max(old.bestStreak(), streak)));
    }

    public void recordCompleted(UUID uuid, String minigameId) {
        if (winnersInCurrentSession.remove(uuid)) return;
        Stats old = get(uuid, minigameId);
        put(uuid, minigameId, new Stats(old.played(), old.wins(), old.losses() + 1,
                old.abandons(), 0, old.bestStreak()));
    }

    public void recordAbandon(UUID uuid, String minigameId) {
        winnersInCurrentSession.remove(uuid);
        Stats old = get(uuid, minigameId);
        put(uuid, minigameId, new Stats(old.played(), old.wins(), old.losses(),
                old.abandons() + 1, 0, old.bestStreak()));
    }

    private void put(UUID uuid, String minigameId, Stats stats) {
        values.computeIfAbsent(uuid, ignored -> new HashMap<>()).put(normalize(minigameId), stats);
        save();
    }

    private String normalize(String id) {
        return id == null || id.isBlank() ? "desconocido" : id.trim().toLowerCase();
    }

    public void load() {
        values.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) return;
        for (String rawUuid : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawUuid);
                ConfigurationSection games = players.getConfigurationSection(rawUuid + ".minigames");
                if (games == null) continue;
                for (String game : games.getKeys(false)) {
                    String base = game + ".";
                    values.computeIfAbsent(uuid, ignored -> new HashMap<>()).put(game,
                            new Stats(games.getInt(base + "played"), games.getInt(base + "wins"),
                                    games.getInt(base + "losses"), games.getInt(base + "abandons"),
                                    games.getInt(base + "current-streak"), games.getInt(base + "best-streak")));
                }
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        values.forEach((uuid, games) -> games.forEach((game, stats) -> {
            String base = "players." + uuid + ".minigames." + game + ".";
            yaml.set(base + "played", stats.played());
            yaml.set(base + "wins", stats.wins());
            yaml.set(base + "losses", stats.losses());
            yaml.set(base + "abandons", stats.abandons());
            yaml.set(base + "current-streak", stats.currentStreak());
            yaml.set(base + "best-streak", stats.bestStreak());
        }));
        try {
            yaml.save(file);
        } catch (IOException error) {
            plugin.getLogger().log(Level.WARNING, "No se pudo guardar stats.yml", error);
        }
    }
}
