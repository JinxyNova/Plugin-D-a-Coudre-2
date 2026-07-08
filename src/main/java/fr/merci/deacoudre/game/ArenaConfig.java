package fr.merci.deacoudre.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Contient toutes les options configurables du minigame :
 * nombre de joueurs, taille de la zone, hauteurs, intervalle de montée d'eau, etc.
 */
public final class ArenaConfig {

    private final JavaPlugin plugin;

    public int minJoueurs;
    public int maxJoueurs;
    public int countdownSecondes;

    public Location lobby;

    public String zoneMonde;
    public int centreX;
    public int centreZ;
    public int taille; // rayon en blocs
    public int hauteurDepart;
    public int hauteurMax;

    public int intervalleSecondes;
    public int blocsParMontee;

    public final List<Location> spawns = new ArrayList<>();

    public ArenaConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        charger();
    }

    public void charger() {
        FileConfiguration cfg = plugin.getConfig();

        minJoueurs = cfg.getInt("joueurs.min", 2);
        maxJoueurs = cfg.getInt("joueurs.max", 16);
        countdownSecondes = cfg.getInt("lobby.countdown-secondes", 15);

        String lobbyMonde = cfg.getString("lobby.monde", "");
        if (lobbyMonde != null && !lobbyMonde.isEmpty() && Bukkit.getWorld(lobbyMonde) != null) {
            World w = Bukkit.getWorld(lobbyMonde);
            lobby = new Location(w,
                    cfg.getDouble("lobby.x"),
                    cfg.getDouble("lobby.y"),
                    cfg.getDouble("lobby.z"),
                    (float) cfg.getDouble("lobby.yaw"),
                    (float) cfg.getDouble("lobby.pitch"));
        } else {
            lobby = null;
        }

        zoneMonde = cfg.getString("zone.monde", "");
        centreX = cfg.getInt("zone.centre-x", 0);
        centreZ = cfg.getInt("zone.centre-z", 0);
        taille = cfg.getInt("zone.taille", 25);
        hauteurDepart = cfg.getInt("zone.hauteur-depart", 64);
        hauteurMax = cfg.getInt("zone.hauteur-max", 100);

        intervalleSecondes = cfg.getInt("eau.intervalle-secondes", 5);
        blocsParMontee = cfg.getInt("eau.blocs-par-montee", 1);

        spawns.clear();
        List<?> rawSpawns = cfg.getList("spawns");
        if (rawSpawns != null) {
            for (Object o : rawSpawns) {
                if (o instanceof java.util.Map<?, ?> map) {
                    try {
                        String monde = String.valueOf(map.get("monde"));
                        World w = Bukkit.getWorld(monde);
                        if (w == null) continue;
                        double x = Double.parseDouble(String.valueOf(map.get("x")));
                        double y = Double.parseDouble(String.valueOf(map.get("y")));
                        double z = Double.parseDouble(String.valueOf(map.get("z")));
                        float yaw = Float.parseFloat(String.valueOf(map.get("yaw")));
                        float pitch = Float.parseFloat(String.valueOf(map.get("pitch")));
                        spawns.add(new Location(w, x, y, z, yaw, pitch));
                    } catch (Exception ignored) {
                        // spawn mal formé dans le config.yml, on l'ignore
                    }
                }
            }
        }
    }

    public void sauvegarder() {
        FileConfiguration cfg = plugin.getConfig();

        cfg.set("joueurs.min", minJoueurs);
        cfg.set("joueurs.max", maxJoueurs);
        cfg.set("lobby.countdown-secondes", countdownSecondes);

        if (lobby != null) {
            cfg.set("lobby.monde", lobby.getWorld().getName());
            cfg.set("lobby.x", lobby.getX());
            cfg.set("lobby.y", lobby.getY());
            cfg.set("lobby.z", lobby.getZ());
            cfg.set("lobby.yaw", (double) lobby.getYaw());
            cfg.set("lobby.pitch", (double) lobby.getPitch());
        }

        cfg.set("zone.monde", zoneMonde);
        cfg.set("zone.centre-x", centreX);
        cfg.set("zone.centre-z", centreZ);
        cfg.set("zone.taille", taille);
        cfg.set("zone.hauteur-depart", hauteurDepart);
        cfg.set("zone.hauteur-max", hauteurMax);

        cfg.set("eau.intervalle-secondes", intervalleSecondes);
        cfg.set("eau.blocs-par-montee", blocsParMontee);

        List<Object> serialized = new ArrayList<>();
        for (Location loc : spawns) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("monde", loc.getWorld().getName());
            map.put("x", loc.getX());
            map.put("y", loc.getY());
            map.put("z", loc.getZ());
            map.put("yaw", (double) loc.getYaw());
            map.put("pitch", (double) loc.getPitch());
            serialized.add(map);
        }
        cfg.set("spawns", serialized);

        plugin.saveConfig();
    }

    public World getZoneMonde() {
        if (zoneMonde == null || zoneMonde.isEmpty()) return null;
        return Bukkit.getWorld(zoneMonde);
    }

    public boolean zoneEstConfiguree() {
        return getZoneMonde() != null;
    }

    public int minX() {
        return centreX - taille;
    }

    public int maxX() {
        return centreX + taille;
    }

    public int minZ() {
        return centreZ - taille;
    }

    public int maxZ() {
        return centreZ + taille;
    }
}
