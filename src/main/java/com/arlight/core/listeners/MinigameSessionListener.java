package com.arlight.core.listeners;

import com.arlight.core.ArlightCorePlugin;
import com.arlight.core.api.MinigameProvider;
import com.arlight.core.items.CoreItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Recupera de forma segura las sesiones abandonadas o interrumpidas por reinicios. */
public class MinigameSessionListener implements Listener {

    private final ArlightCorePlugin plugin;

    public MinigameSessionListener(ArlightCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String minigameId = plugin.getSessionManager().getMinigameId(player.getUniqueId());
        if (minigameId == null) return;

        MinigameProvider provider = plugin.getMinigameRegistry().get(minigameId);
        if (provider != null) {
            try {
                provider.handleDisconnect(player);
            } catch (Exception error) {
                plugin.getLogger().warning("No se pudo notificar la desconexión a " + minigameId
                        + ": " + error.getMessage());
            }
        }

        // Se marca después de avisar al minijuego para que siempre quede pendiente.
        if (plugin.getSessionManager().markPendingRestore(player.getUniqueId())) {
            plugin.getStatsManager().recordAbandon(player.getUniqueId(), minigameId);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getSessionManager().isPendingRestore(player.getUniqueId())) return;

        String minigameId = plugin.getSessionManager().getMinigameId(player.getUniqueId());
        MinigameProvider provider = plugin.getMinigameRegistry().get(minigameId);
        if (provider != null) {
            try {
                // También limpia participantes que el minijuego haya recuperado de disco
                // después de un reinicio inesperado.
                provider.handleDisconnect(player);
            } catch (Exception error) {
                plugin.getLogger().warning("No se pudo limpiar la sesión interrumpida de "
                        + minigameId + ": " + error.getMessage());
            }
        }

        // Un tick después evita conflictos con plugins que preparan el inventario al entrar.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (!plugin.getSessionManager().restorePending(player)) return;

            if (provider != null) {
                try {
                    provider.cleanupAfterRecovery(player);
                } catch (Exception error) {
                    plugin.getLogger().warning("No se pudieron limpiar los objetos temporales de "
                            + minigameId + ": " + error.getMessage());
                }
            }

            String lobbyName = plugin.getLobbyWorld();
            org.bukkit.Location lobbySpawn = plugin.getLobbySpawn();
            if (lobbySpawn != null) {
                player.teleport(lobbySpawn);
            } else {
                plugin.getLogger().warning("No se pudo enviar a " + player.getName()
                        + " al lobby porque el mundo '" + lobbyName + "' no está cargado.");
            }

            CoreItems.giveIfMissing(plugin, player);
            player.sendMessage(ChatColor.RED + "Fuiste descalificado porque abandonaste la partida.");
            player.sendMessage(ChatColor.GREEN + "Tu inventario fue restaurado y regresaste al lobby.");
        });
    }
}
