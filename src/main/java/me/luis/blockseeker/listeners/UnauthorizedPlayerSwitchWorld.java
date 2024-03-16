package me.luis.blockseeker.listeners;

import me.luis.blockseeker.BlockSeeker;
import me.luis.blockseeker.utils.C;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class UnauthorizedPlayerSwitchWorld implements Listener {

    /**
     * todo: fix this whole class.
     */
//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUnauthorizedPlayerSwitchWorld(PlayerChangedWorldEvent e) {
        var p = e.getPlayer();
        var from = e.getFrom();

        // The game instance, as an optional.
        var gio = BlockSeeker.getGameInstanceManager().getInstance.apply(p);

        // If the player isn't associated with any game instance, we send them back.
        if (gio.isEmpty()) {
            sendBack(p, from);
            return;
        }

        // The game instance.
        var gi = gio.get();

        /**
         * If {@link gi}'s name equals to the of the world that the player changed to
         */
        if (gi.getWorld().getName().equals(p.getWorld().getName())) {
//            sendBack(p, );
        }

    }

    private void sendBack(Player player, World from) {
        player.teleport(from.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.sendMessage(C.mess("&cThis is strictly a BlockSeeker world, you may not join unless you are playing!"));
    }
}
