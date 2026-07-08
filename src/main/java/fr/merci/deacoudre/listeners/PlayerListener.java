package fr.merci.deacoudre.listeners;

import fr.merci.deacoudre.game.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class PlayerListener implements Listener {

    private final GameManager gameManager;

    public PlayerListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gameManager.gererDeconnexion(event.getPlayer().getUniqueId());
    }

    /**
     * On évite que les joueurs perdent de la faim pendant la partie (pas de survie longue durée).
     */
    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && gameManager.estEnJeu(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * On empêche la noyade de tuer directement le joueur : c'est le GameManager
     * qui gère l'élimination proprement (mode spectateur) plutôt que la mort/respawn.
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && gameManager.estEnJeu(player.getUniqueId())) {
            if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
                event.setCancelled(true);
            }
        }
    }
}
