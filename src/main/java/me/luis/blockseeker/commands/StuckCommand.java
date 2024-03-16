package me.luis.blockseeker.commands;

import me.luis.blockseeker.BlockSeeker;
import me.luis.blockseeker.utils.C;
import me.luis.blockseeker.utils.Messages;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class StuckCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage(C.mess("&cThis is a player-only command."));
            return true;
        }

        var player = (Player) sender;

        var optional = BlockSeeker.getGameInstanceManager().getInstance.apply(player);

        // TODO: 10/15/2023 add a check here to see if the game first started, we should really only teleport the player if the game has yet to start, regardless of how much time is left.

        if (optional.isPresent()) {
            var gi = optional.get();

            /**
             * We teleport the player async.
             */
            CompletableFuture.supplyAsync(() -> {
                return player.teleportAsync(gi.getCenter(), PlayerTeleportEvent.TeleportCause.PLUGIN).join();
            }).thenAccept(success -> {

                /**
                 * If the teleportation was successful...
                 */
                if (success) {

                    /**
                     * We play a chorus fruit teleport sound if the teleportation was successful
                     */
                    player.playSound(player, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.75F, 1.0F);
                }
            });
        } else {
            player.sendMessage(Messages.NOT_IN_A_GAME);
        }

        return true;
    }
}
