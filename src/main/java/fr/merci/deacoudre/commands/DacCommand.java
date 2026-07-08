package fr.merci.deacoudre.commands;

import fr.merci.deacoudre.game.ArenaConfig;
import fr.merci.deacoudre.game.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class DacCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SOUS_COMMANDES = List.of(
            "join", "leave", "start", "stop", "info", "reload",
            "setlobby", "setcenter", "settaille", "sethauteurdepart", "sethauteurmax",
            "setintervalle", "setminjoueurs", "setmaxjoueurs", "setcountdown",
            "addspawn", "clearspawns"
    );

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final ArenaConfig arenaConfig;

    public DacCommand(JavaPlugin plugin, GameManager gameManager, ArenaConfig arenaConfig) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.arenaConfig = arenaConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Utilise /dac join, /dac leave, ou /dac info");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "join" -> {
                if (!requirePlayer(sender)) return true;
                sender.sendMessage(gameManager.rejoindre((Player) sender));
                return true;
            }
            case "leave" -> {
                if (!requirePlayer(sender)) return true;
                sender.sendMessage(gameManager.quitter((Player) sender));
                return true;
            }
            case "info" -> {
                envoyerInfo(sender);
                return true;
            }
            case "start" -> {
                if (!requireAdmin(sender)) return true;
                sender.sendMessage(gameManager.forcerDepart());
                return true;
            }
            case "stop" -> {
                if (!requireAdmin(sender)) return true;
                sender.sendMessage(gameManager.arreter(sender));
                return true;
            }
            case "reload" -> {
                if (!requireAdmin(sender)) return true;
                plugin.reloadConfig();
                arenaConfig.charger();
                sender.sendMessage(ChatColor.GREEN + "Configuration rechargée.");
                return true;
            }
            case "setlobby" -> {
                if (!requirePlayerAdmin(sender)) return true;
                Player p = (Player) sender;
                arenaConfig.lobby = p.getLocation();
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Lobby défini à ta position actuelle.");
                return true;
            }
            case "setcenter" -> {
                if (!requirePlayerAdmin(sender)) return true;
                Player p = (Player) sender;
                arenaConfig.zoneMonde = p.getWorld().getName();
                arenaConfig.centreX = p.getLocation().getBlockX();
                arenaConfig.centreZ = p.getLocation().getBlockZ();
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Centre de la zone défini à ta position ("
                        + arenaConfig.centreX + ", " + arenaConfig.centreZ + ") dans le monde "
                        + arenaConfig.zoneMonde + ".");
                return true;
            }
            case "settaille" -> {
                if (!requireAdmin(sender)) return true;
                Integer taille = parseInt(sender, args, 1);
                if (taille == null) return true;
                if (taille < 3) {
                    sender.sendMessage(ChatColor.RED + "La taille doit être d'au moins 3 blocs.");
                    return true;
                }
                arenaConfig.taille = taille;
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Taille de la zone définie à " + taille + " blocs de rayon.");
                return true;
            }
            case "sethauteurdepart" -> {
                if (!requireAdmin(sender)) return true;
                Integer y = parseInt(sender, args, 1);
                if (y == null) return true;
                arenaConfig.hauteurDepart = y;
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Hauteur de départ de l'eau définie à Y=" + y);
                return true;
            }
            case "sethauteurmax" -> {
                if (!requireAdmin(sender)) return true;
                Integer y = parseInt(sender, args, 1);
                if (y == null) return true;
                arenaConfig.hauteurMax = y;
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Hauteur max de l'eau définie à Y=" + y);
                return true;
            }
            case "setintervalle" -> {
                if (!requireAdmin(sender)) return true;
                Integer sec = parseInt(sender, args, 1);
                if (sec == null) return true;
                if (sec < 1) {
                    sender.sendMessage(ChatColor.RED + "L'intervalle doit être d'au moins 1 seconde.");
                    return true;
                }
                arenaConfig.intervalleSecondes = sec;
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Intervalle de montée de l'eau : " + sec + " seconde(s).");
                return true;
            }
            case "setminjoueurs" -> {
                if (!requireAdmin(sender)) return true;
                Integer n = parseInt(sender, args, 1);
                if (n == null) return true;
                if (n < 1) {
                    sender.sendMessage(ChatColor.RED + "Il faut au moins 1 joueur minimum.");
                    return true;
                }
                arenaConfig.minJoueurs = n;
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Nombre minimum de joueurs : " + n);
                return true;
            }
            case "setmaxjoueurs" -> {
                if (!requireAdmin(sender)) return true;
                Integer n = parseInt(sender, args, 1);
                if (n == null) return true;
                if (n < arenaConfig.minJoueurs) {
                    sender.sendMessage(ChatColor.RED + "Le maximum doit être supérieur ou égal au minimum ("
                            + arenaConfig.minJoueurs + ").");
                    return true;
                }
                arenaConfig.maxJoueurs = n;
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Nombre maximum de joueurs : " + n);
                return true;
            }
            case "setcountdown" -> {
                if (!requireAdmin(sender)) return true;
                Integer sec = parseInt(sender, args, 1);
                if (sec == null) return true;
                if (sec < 1) {
                    sender.sendMessage(ChatColor.RED + "Le compte à rebours doit durer au moins 1 seconde.");
                    return true;
                }
                arenaConfig.countdownSecondes = sec;
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Durée du compte à rebours : " + sec + " seconde(s).");
                return true;
            }
            case "addspawn" -> {
                if (!requirePlayerAdmin(sender)) return true;
                Player p = (Player) sender;
                arenaConfig.spawns.add(p.getLocation());
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Spawn ajouté (total : " + arenaConfig.spawns.size() + ").");
                return true;
            }
            case "clearspawns" -> {
                if (!requireAdmin(sender)) return true;
                arenaConfig.spawns.clear();
                arenaConfig.sauvegarder();
                sender.sendMessage(ChatColor.GREEN + "Tous les spawns ont été supprimés.");
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Sous-commande inconnue. Essaie /dac join ou /dac info.");
                return true;
            }
        }
    }

    private void envoyerInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== DeACoudre ===");
        sender.sendMessage(ChatColor.GRAY + "État : " + ChatColor.WHITE + gameManager.getState());
        sender.sendMessage(ChatColor.GRAY + "Joueurs : " + ChatColor.WHITE + gameManager.nombreJoueurs()
                + " (" + arenaConfig.minJoueurs + " min / " + arenaConfig.maxJoueurs + " max)");
        sender.sendMessage(ChatColor.GRAY + "Zone : " + ChatColor.WHITE
                + (arenaConfig.zoneEstConfiguree()
                    ? arenaConfig.zoneMonde + " centre(" + arenaConfig.centreX + "," + arenaConfig.centreZ
                        + ") rayon=" + arenaConfig.taille
                    : "non configurée"));
        sender.sendMessage(ChatColor.GRAY + "Hauteurs : " + ChatColor.WHITE
                + "départ Y=" + arenaConfig.hauteurDepart + " -> max Y=" + arenaConfig.hauteurMax);
        sender.sendMessage(ChatColor.GRAY + "Montée eau : " + ChatColor.WHITE
                + arenaConfig.blocsParMontee + " bloc(s) toutes les " + arenaConfig.intervalleSecondes + "s");
        sender.sendMessage(ChatColor.GRAY + "Spawns définis : " + ChatColor.WHITE + arenaConfig.spawns.size());
    }

    private Integer parseInt(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            sender.sendMessage(ChatColor.RED + "Il manque un nombre, ex : /dac " + args[0] + " 25");
            return null;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "\"" + args[index] + "\" n'est pas un nombre valide.");
            return null;
        }
    }

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return false;
        }
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("dac.admin")) {
            sender.sendMessage(ChatColor.RED + "Tu n'as pas la permission d'utiliser cette commande.");
            return false;
        }
        return true;
    }

    private boolean requirePlayerAdmin(CommandSender sender) {
        return requirePlayer(sender) && requireAdmin(sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> resultats = new ArrayList<>();
            for (String s : SOUS_COMMANDES) {
                if (s.startsWith(args[0].toLowerCase())) {
                    resultats.add(s);
                }
            }
            return resultats;
        }
        return List.of();
    }
}
