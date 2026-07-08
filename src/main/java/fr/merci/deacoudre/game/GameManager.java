package fr.merci.deacoudre.game;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Gère l'état de la partie : lobby, countdown, montée de l'eau,
 * élimination des joueurs, victoire.
 */
public class GameManager {

    private final JavaPlugin plugin;
    private final ArenaConfig arenaConfig;

    private GameState state = GameState.LOBBY;

    private final Set<UUID> joueurs = new LinkedHashSet<>();
    private final Set<UUID> vivants = new HashSet<>();
    private final Set<UUID> spectateurs = new HashSet<>();

    private BukkitTask countdownTask;
    private BukkitTask waterTask;
    private int countdownRestant;
    private int niveauEauActuel;

    public GameManager(JavaPlugin plugin, ArenaConfig arenaConfig) {
        this.plugin = plugin;
        this.arenaConfig = arenaConfig;
    }

    public GameState getState() {
        return state;
    }

    public ArenaConfig getArenaConfig() {
        return arenaConfig;
    }

    public boolean estEnJeu(UUID uuid) {
        return joueurs.contains(uuid);
    }

    public int nombreJoueurs() {
        return joueurs.size();
    }

    // ---------------------------------------------------------------
    // Rejoindre / Quitter
    // ---------------------------------------------------------------

    public String rejoindre(Player player) {
        if (state == GameState.EN_COURS || state == GameState.TERMINEE) {
            return ChatColor.RED + "Une partie est déjà en cours, attends la prochaine !";
        }
        if (!arenaConfig.zoneEstConfiguree()) {
            return ChatColor.RED + "La zone de jeu n'est pas encore configurée (voir /dac setcenter).";
        }
        if (arenaConfig.lobby == null) {
            return ChatColor.RED + "Le lobby n'est pas encore configuré (voir /dac setlobby).";
        }
        if (joueurs.size() >= arenaConfig.maxJoueurs) {
            return ChatColor.RED + "La partie est complète (" + arenaConfig.maxJoueurs + " joueurs max).";
        }
        if (joueurs.contains(player.getUniqueId())) {
            return ChatColor.YELLOW + "Tu es déjà dans la partie !";
        }

        joueurs.add(player.getUniqueId());
        player.teleport(arenaConfig.lobby);
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);

        broadcast(ChatColor.AQUA + player.getName() + ChatColor.GRAY + " a rejoint la partie ! ("
                + joueurs.size() + "/" + arenaConfig.maxJoueurs + ")");

        verifierLancementCountdown();
        return ChatColor.GREEN + "Tu as rejoint la partie DeACoudre !";
    }

    public String quitter(Player player) {
        if (!joueurs.contains(player.getUniqueId())) {
            return ChatColor.YELLOW + "Tu n'es pas dans une partie.";
        }
        retirerJoueur(player.getUniqueId(), false);
        return ChatColor.GREEN + "Tu as quitté la partie.";
    }

    private void retirerJoueur(UUID uuid, boolean elimine) {
        joueurs.remove(uuid);
        vivants.remove(uuid);
        spectateurs.remove(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && !elimine) {
            if (arenaConfig.lobby != null) {
                player.teleport(arenaConfig.lobby.getWorld().getSpawnLocation());
            }
            player.setGameMode(GameMode.SURVIVAL);
        }

        if (state == GameState.EN_COURS) {
            verifierVictoire();
        } else if (state == GameState.COUNTDOWN && joueurs.size() < arenaConfig.minJoueurs) {
            annulerCountdown();
            broadcast(ChatColor.YELLOW + "Pas assez de joueurs, le compte à rebours est annulé.");
        }
    }

    // ---------------------------------------------------------------
    // Countdown du lobby
    // ---------------------------------------------------------------

    private void verifierLancementCountdown() {
        if (state == GameState.LOBBY && joueurs.size() >= arenaConfig.minJoueurs) {
            demarrerCountdown();
        }
    }

    private void demarrerCountdown() {
        state = GameState.COUNTDOWN;
        countdownRestant = arenaConfig.countdownSecondes;

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (joueurs.size() < arenaConfig.minJoueurs) {
                    broadcast(ChatColor.YELLOW + "Pas assez de joueurs, le compte à rebours est annulé.");
                    annulerCountdown();
                    return;
                }
                if (countdownRestant <= 0) {
                    demarrerPartie();
                    this.cancel();
                    return;
                }
                if (countdownRestant <= 5 || countdownRestant % 10 == 0) {
                    broadcast(ChatColor.GOLD + "Départ dans " + ChatColor.YELLOW + countdownRestant
                            + ChatColor.GOLD + " seconde(s) !");
                }
                countdownRestant--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void annulerCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        state = GameState.LOBBY;
    }

    public String forcerDepart() {
        if (state == GameState.EN_COURS) {
            return ChatColor.RED + "Une partie est déjà en cours.";
        }
        if (joueurs.isEmpty()) {
            return ChatColor.RED + "Aucun joueur dans le lobby.";
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        demarrerPartie();
        return ChatColor.GREEN + "Partie lancée de force !";
    }

    // ---------------------------------------------------------------
    // Déroulement de la partie
    // ---------------------------------------------------------------

    private void demarrerPartie() {
        World monde = arenaConfig.getZoneMonde();
        if (monde == null) {
            broadcast(ChatColor.RED + "Erreur : le monde de la zone n'existe pas. Partie annulée.");
            resetComplet();
            return;
        }

        state = GameState.EN_COURS;
        vivants.clear();
        vivants.addAll(joueurs);
        spectateurs.clear();
        niveauEauActuel = arenaConfig.hauteurDepart;

        teleporterJoueursSurSpawns(monde);

        broadcast(ChatColor.GREEN + "La partie commence ! L'eau va bientôt monter, ne te fais pas piéger !");

        waterTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.EN_COURS) {
                    this.cancel();
                    return;
                }
                monterEau(monde);
                verifierEliminations(monde);
                verifierVictoire();
            }
        }.runTaskTimer(plugin, arenaConfig.intervalleSecondes * 20L, arenaConfig.intervalleSecondes * 20L);
    }

    private void teleporterJoueursSurSpawns(World monde) {
        List<Location> spawns = arenaConfig.spawns;
        int i = 0;
        for (UUID uuid : joueurs) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            Location dest;
            if (!spawns.isEmpty()) {
                dest = spawns.get(i % spawns.size());
            } else {
                // pas de spawn défini : on place les joueurs en cercle au centre de la zone,
                // en hauteur, au-dessus du niveau de départ de l'eau
                double angle = (2 * Math.PI / Math.max(1, joueurs.size())) * i;
                double rayon = Math.max(1, arenaConfig.taille - 2);
                double x = arenaConfig.centreX + rayon * Math.cos(angle);
                double z = arenaConfig.centreZ + rayon * Math.sin(angle);
                dest = new Location(monde, x, arenaConfig.hauteurMax, z);
            }

            player.teleport(dest);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20);
            player.setFoodLevel(20);
            i++;
        }
    }

    private void monterEau(World monde) {
        int prochainNiveau = Math.min(niveauEauActuel + arenaConfig.blocsParMontee, arenaConfig.hauteurMax);
        if (prochainNiveau <= niveauEauActuel) {
            return; // déjà au maximum
        }

        for (int y = niveauEauActuel; y < prochainNiveau; y++) {
            for (int x = arenaConfig.minX(); x <= arenaConfig.maxX(); x++) {
                for (int z = arenaConfig.minZ(); z <= arenaConfig.maxZ(); z++) {
                    Block bloc = monde.getBlockAt(x, y, z);
                    if (bloc.getType() == Material.AIR || bloc.getType() == Material.CAVE_AIR) {
                        bloc.setType(Material.WATER, false);
                    }
                }
            }
        }
        niveauEauActuel = prochainNiveau;
        broadcast(ChatColor.BLUE + "L'eau monte... (niveau " + niveauEauActuel + ")");
    }

    private void verifierEliminations(World monde) {
        for (UUID uuid : new ArrayList<>(vivants)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (!player.getWorld().equals(monde)) {
                continue;
            }
            Block piedBloc = player.getLocation().getBlock();
            boolean submerge = piedBloc.getType() == Material.WATER
                    || player.getLocation().getY() < arenaConfig.hauteurDepart;

            if (submerge) {
                eliminer(player);
            }
        }
    }

    private void eliminer(Player player) {
        vivants.remove(player.getUniqueId());
        spectateurs.add(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        if (arenaConfig.lobby != null) {
            player.teleport(arenaConfig.lobby);
        }
        broadcast(ChatColor.RED + player.getName() + " a été englouti par l'eau ! ("
                + vivants.size() + " restant(s))");
    }

    private void verifierVictoire() {
        if (state != GameState.EN_COURS) return;

        if (vivants.size() == 1) {
            UUID gagnantId = vivants.iterator().next();
            Player gagnant = Bukkit.getPlayer(gagnantId);
            String nom = gagnant != null ? gagnant.getName() : "Inconnu";
            broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + nom + " a gagné la partie DeACoudre !");
            terminerPartie();
        } else if (vivants.isEmpty()) {
            broadcast(ChatColor.YELLOW + "Aucun survivant... la partie se termine sans vainqueur.");
            terminerPartie();
        }
    }

    private void terminerPartie() {
        state = GameState.TERMINEE;
        if (waterTask != null) {
            waterTask.cancel();
            waterTask = null;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                resetComplet();
            }
        }.runTaskLater(plugin, 100L); // 5 secondes avant reset du lobby
    }

    public String arreter(org.bukkit.command.CommandSender sender) {
        if (state == GameState.LOBBY) {
            return ChatColor.YELLOW + "Aucune partie en cours.";
        }
        broadcast(ChatColor.RED + "La partie a été arrêtée par un administrateur.");
        if (waterTask != null) {
            waterTask.cancel();
            waterTask = null;
        }
        resetComplet();
        return ChatColor.GREEN + "Partie arrêtée et réinitialisée.";
    }

    private void resetComplet() {
        World monde = arenaConfig.getZoneMonde();
        if (monde != null && niveauEauActuel > arenaConfig.hauteurDepart) {
            viderEau(monde);
        }

        for (UUID uuid : joueurs) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
                if (arenaConfig.lobby != null) {
                    player.teleport(arenaConfig.lobby.getWorld().getSpawnLocation());
                }
            }
        }

        joueurs.clear();
        vivants.clear();
        spectateurs.clear();
        state = GameState.LOBBY;
        niveauEauActuel = arenaConfig.hauteurDepart;
    }

    private void viderEau(World monde) {
        for (int y = arenaConfig.hauteurDepart; y <= niveauEauActuel; y++) {
            for (int x = arenaConfig.minX(); x <= arenaConfig.maxX(); x++) {
                for (int z = arenaConfig.minZ(); z <= arenaConfig.maxZ(); z++) {
                    Block bloc = monde.getBlockAt(x, y, z);
                    if (bloc.getType() == Material.WATER) {
                        bloc.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void broadcast(String message) {
        for (UUID uuid : joueurs) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(ChatColor.DARK_AQUA + "[DeACoudre] " + ChatColor.RESET + message);
            }
        }
    }

    /**
     * À appeler quand un joueur se déconnecte pendant qu'il est engagé dans une partie.
     */
    public void gererDeconnexion(UUID uuid) {
        if (!joueurs.contains(uuid)) return;
        boolean etaitEnJeu = state == GameState.EN_COURS;
        joueurs.remove(uuid);
        vivants.remove(uuid);
        spectateurs.remove(uuid);
        if (etaitEnJeu) {
            verifierVictoire();
        } else if (state == GameState.COUNTDOWN && joueurs.size() < arenaConfig.minJoueurs) {
            annulerCountdown();
        }
    }
}
