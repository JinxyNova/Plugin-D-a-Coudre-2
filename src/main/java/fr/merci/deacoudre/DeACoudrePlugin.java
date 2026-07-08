package fr.merci.deacoudre;

import fr.merci.deacoudre.commands.DacCommand;
import fr.merci.deacoudre.game.ArenaConfig;
import fr.merci.deacoudre.game.GameManager;
import fr.merci.deacoudre.listeners.PlayerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class DeACoudrePlugin extends JavaPlugin {

    private ArenaConfig arenaConfig;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.arenaConfig = new ArenaConfig(this);
        this.gameManager = new GameManager(this, arenaConfig);

        PluginCommand cmd = getCommand("dac");
        if (cmd != null) {
            DacCommand executor = new DacCommand(this, gameManager, arenaConfig);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        } else {
            getLogger().severe("Impossible d'enregistrer la commande /dac (vérifie plugin.yml) !");
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(gameManager), this);

        getLogger().info("DeACoudre activé ! (" + arenaConfig.minJoueurs + "-" + arenaConfig.maxJoueurs + " joueurs)");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.arreter(getServer().getConsoleSender());
        }
        getLogger().info("DeACoudre désactivé.");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ArenaConfig getArenaConfig() {
        return arenaConfig;
    }
}
