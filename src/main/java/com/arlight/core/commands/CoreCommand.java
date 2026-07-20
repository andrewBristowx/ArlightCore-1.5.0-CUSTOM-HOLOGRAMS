package com.arlight.core.commands;

import com.arlight.core.ArlightCorePlugin;
import com.arlight.core.items.CoreItems;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CoreCommand implements CommandExecutor, TabCompleter {

    private final ArlightCorePlugin plugin;

    public CoreCommand(ArlightCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("items", "reward", "xp", "setlobby", "lobby", "leaderboard", "hologram", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reward")) {
            return filter(Arrays.asList("set", "remove", "list"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("xp")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
            return filter(Arrays.asList("create", "remove", "list", "refresh"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("leaderboard")
                && args[1].equalsIgnoreCase("create")) {
            return filter(Arrays.asList("levels", "wins"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("hologram")) {
            return filter(Arrays.asList("create", "addline", "setline", "removeline", "move", "remove", "list", "refresh"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("hologram")
                && Arrays.asList("addline", "setline", "removeline", "move", "remove").contains(args[1].toLowerCase())) {
            return filter(plugin.getHologramManager().ids(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("leaderboard")
                && args[1].equalsIgnoreCase("remove")) {
            return filter(plugin.getLeaderboardManager().ids(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("reward")
                && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("remove"))) {
            return filter(plugin.getRewardManager().getAll().keySet().stream().map(String::valueOf).collect(Collectors.toList()), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /core <items|reward|xp|setlobby|lobby|leaderboard|hologram|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "items": {
                if (!(sender instanceof Player)) return true;
                Player player = (Player) sender;
                CoreItems.giveIfMissing(plugin, player);
                sender.sendMessage(ChatColor.GREEN + "Tus items del Core están listos.");
                return true;
            }

            case "reward": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /core reward <set|remove|list> [nivel]");
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "set": {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "Solo un jugador sosteniendo el item puede hacer esto.");
                            return true;
                        }
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core reward set <nivel>");
                            return true;
                        }
                        int level;
                        try {
                            level = Integer.parseInt(args[2]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "El nivel tiene que ser un numero.");
                            return true;
                        }
                        Player player = (Player) sender;
                        ItemStack held = player.getInventory().getItemInMainHand();
                        if (held.getType().isAir()) {
                            sender.sendMessage(ChatColor.RED + "Tenes que sostener el item que queres dar de recompensa.");
                            return true;
                        }
                        plugin.getRewardManager().setReward(level, held);
                        sender.sendMessage(ChatColor.GREEN + "Recompensa del nivel " + level + " actualizada (" + held.getAmount() + "x " + held.getType() + ").");
                        return true;
                    }
                    case "remove": {
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core reward remove <nivel>");
                            return true;
                        }
                        try {
                            int level = Integer.parseInt(args[2]);
                            plugin.getRewardManager().removeReward(level);
                            sender.sendMessage(ChatColor.GREEN + "Recompensa del nivel " + level + " eliminada.");
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "El nivel tiene que ser un numero.");
                        }
                        return true;
                    }
                    case "list": {
                        sender.sendMessage(ChatColor.GOLD + "Niveles con recompensa: " + ChatColor.WHITE
                                + plugin.getRewardManager().getAll().keySet());
                        return true;
                    }
                    default:
                        sender.sendMessage(ChatColor.RED + "Subcomando de reward desconocido.");
                        return true;
                }
            }

            case "xp": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /core xp <jugador> <cantidad>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Jugador no encontrado (debe estar conectado).");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    plugin.getLevelManager().addXp(target.getUniqueId(), amount);
                    sender.sendMessage(ChatColor.GREEN + "Se le dio " + amount + " XP a " + target.getName() + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "La cantidad tiene que ser un numero.");
                }
                return true;
            }

            case "setlobby": {
                if (!checkAdmin(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Este comando debe usarse dentro del juego.");
                    return true;
                }
                plugin.setLobbySpawn(player.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Lobby principal establecido en "
                        + player.getWorld().getName() + " (" + player.getLocation().getBlockX() + ", "
                        + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ() + ").");
                return true;
            }

            case "lobby": {
                if (!(sender instanceof Player player)) return true;
                if (plugin.getSessionManager().hasSession(player.getUniqueId())) {
                    sender.sendMessage(ChatColor.RED + "No puedes usar /core lobby mientras participas en un minijuego.");
                    return true;
                }
                if (plugin.getLobbySpawn() == null) {
                    sender.sendMessage(ChatColor.RED + "El mundo del lobby no está cargado.");
                    return true;
                }
                player.teleport(plugin.getLobbySpawn());
                CoreItems.giveIfMissing(plugin, player);
                return true;
            }

            case "leaderboard": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /core leaderboard <create|remove|list|refresh>");
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "create": {
                        if (!(sender instanceof Player player) || args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core leaderboard create <levels|wins>");
                            return true;
                        }
                        String id = plugin.getLeaderboardManager().create(args[2], player.getLocation());
                        if (id == null) {
                            sender.sendMessage(ChatColor.RED + "Tipo inválido. Usa levels o wins.");
                        } else {
                            sender.sendMessage(ChatColor.GREEN + "Texto flotante creado con ID " + id + ".");
                        }
                        return true;
                    }
                    case "remove": {
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core leaderboard remove <id>");
                            return true;
                        }
                        sender.sendMessage(plugin.getLeaderboardManager().remove(args[2])
                                ? ChatColor.GREEN + "Texto flotante eliminado."
                                : ChatColor.RED + "No existe un texto flotante con ese ID.");
                        return true;
                    }
                    case "list":
                        sender.sendMessage(ChatColor.GOLD + "Textos flotantes: " + ChatColor.WHITE
                                + plugin.getLeaderboardManager().ids());
                        return true;
                    case "refresh":
                        plugin.getLeaderboardManager().refresh();
                        sender.sendMessage(ChatColor.GREEN + "Clasificaciones actualizadas.");
                        return true;
                    default:
                        sender.sendMessage(ChatColor.RED + "Subcomando desconocido.");
                        return true;
                }
            }

            case "hologram": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) {
                    sendHologramUsage(sender);
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "create": {
                        if (!(sender instanceof Player player) || args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core hologram create <id> <linea 1 | linea 2>");
                            return true;
                        }
                        List<String> lines = Arrays.stream(joinArgs(args, 3).split("\\s*\\|\\s*"))
                                .filter(line -> !line.isBlank()).collect(Collectors.toList());
                        boolean created = plugin.getHologramManager().create(args[2], player.getLocation(), lines);
                        sender.sendMessage(created
                                ? ChatColor.GREEN + "Holograma '" + args[2].toLowerCase() + "' creado."
                                : ChatColor.RED + "No se pudo crear. Revisa el ID, el texto o si ya existe.");
                        return true;
                    }
                    case "addline": {
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core hologram addline <id> <texto>");
                            return true;
                        }
                        sender.sendMessage(plugin.getHologramManager().addLine(args[2], joinArgs(args, 3))
                                ? ChatColor.GREEN + "Línea agregada."
                                : ChatColor.RED + "No existe ese holograma.");
                        return true;
                    }
                    case "setline": {
                        if (args.length < 5) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core hologram setline <id> <numero> <texto>");
                            return true;
                        }
                        Integer line = parsePositiveInt(args[3]);
                        if (line == null) {
                            sender.sendMessage(ChatColor.RED + "El número de línea no es válido.");
                            return true;
                        }
                        sender.sendMessage(plugin.getHologramManager().setLine(args[2], line, joinArgs(args, 4))
                                ? ChatColor.GREEN + "Línea actualizada."
                                : ChatColor.RED + "No existe el holograma o esa línea.");
                        return true;
                    }
                    case "removeline": {
                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core hologram removeline <id> <numero>");
                            return true;
                        }
                        Integer line = parsePositiveInt(args[3]);
                        sender.sendMessage(line != null && plugin.getHologramManager().removeLine(args[2], line)
                                ? ChatColor.GREEN + "Línea eliminada."
                                : ChatColor.RED + "No se pudo eliminar (debe quedar al menos una línea).");
                        return true;
                    }
                    case "move": {
                        if (!(sender instanceof Player player) || args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core hologram move <id>");
                            return true;
                        }
                        sender.sendMessage(plugin.getHologramManager().move(args[2], player.getLocation())
                                ? ChatColor.GREEN + "Holograma movido a tu posición."
                                : ChatColor.RED + "No existe ese holograma.");
                        return true;
                    }
                    case "remove": {
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Uso: /core hologram remove <id>");
                            return true;
                        }
                        sender.sendMessage(plugin.getHologramManager().remove(args[2])
                                ? ChatColor.GREEN + "Holograma eliminado."
                                : ChatColor.RED + "No existe ese holograma.");
                        return true;
                    }
                    case "list":
                        sender.sendMessage(ChatColor.GOLD + "Hologramas: " + ChatColor.WHITE
                                + plugin.getHologramManager().ids());
                        return true;
                    case "refresh":
                        plugin.getHologramManager().refresh();
                        sender.sendMessage(ChatColor.GREEN + "Hologramas actualizados.");
                        return true;
                    default:
                        sendHologramUsage(sender);
                        return true;
                }
            }

            case "reload": {
                if (!checkAdmin(sender)) return true;
                plugin.reloadCoreConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuracion recargada.");
                return true;
            }

            default:
                sender.sendMessage(ChatColor.RED + "Subcomando desconocido.");
                return true;
        }
    }

    private boolean checkAdmin(CommandSender sender) {
        if (!sender.hasPermission("arlightcore.admin")) {
            sender.sendMessage(ChatColor.RED + "No tenes permiso para hacer eso.");
            return false;
        }
        return true;
    }

    private String joinArgs(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void sendHologramUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Uso: /core hologram <create|addline|setline|removeline|move|remove|list|refresh>");
    }
}
