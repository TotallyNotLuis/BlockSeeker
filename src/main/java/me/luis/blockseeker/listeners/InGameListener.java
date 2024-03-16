package me.luis.blockseeker.listeners;

import me.luis.blockseeker.BlockSeeker;
import me.luis.blockseeker.managers.InventoryManager;
import me.luis.blockseeker.utils.C;
import me.luis.blockseeker.utils.GameInstance;
import me.luis.blockseeker.utils.enums.player.PlayerRole;
import org.bukkit.Material;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class InGameListener implements Listener {

    /**
     * Stops players from losing hunger (and maxes it, if not already)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerHunger(FoodLevelChangeEvent e) {
        e.setCancelled(true);

        if (e.getFoodLevel() != 20) e.setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLivingEntityTargetPlayer(EntityTargetEvent e) {
        var target = e.getTarget();

        if (target != null && target.getType() == EntityType.PLAYER) {
            var player = (Player) target;

            if (BlockSeeker.getGameInstanceManager().isAssociated.apply(player)) e.setCancelled(true);
        }
    }

    /**
     * Stops blocks from being burnt by fire.
     * Note: This does not stop the block from catching fire.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        e.setCancelled(true);
    }

    /**
     * Stops any randomly spreading blocks. Perhaps this means {@link Material#MYCELIUM} and {@link Material#VINE}
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractWithItem(PlayerInteractEvent e) {
        var p = e.getPlayer();
        var action = e.getAction();

        /**
         * If {@link e} has a block and {@link action} was a left-click, then we check if it's a seeker punching a hider block.
         */
        if (e.hasBlock() && action.isLeftClick()) {
            // TODO: implementation

            return;
        }

        /**
         * Note: At this point, {@link action} is guaranteed to be a right-click.
         */

        if (e.hasItem() && e.getItem().getType() == Material.RECOVERY_COMPASS) {

            var optional = BlockSeeker.getGameInstanceManager().getInstance.apply(p);

            if (optional.isPresent()) {
                var inv = InventoryManager.createInventory(optional.get());
                p.openInventory(inv);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerSneak(PlayerToggleSneakEvent e) {
        var p = e.getPlayer();

//        if (true) return;

        var optionalGame = BlockSeeker.getGameInstanceManager().getInstance.apply(p);

        /**
         * We return, since {@link p} is not in a {@link GameInstance}
         */
        if (optionalGame.isEmpty()) {
            return;
        }

        var uuid = p.getUniqueId();
        var gi = optionalGame.get();

        var optionalPlayerInfo = gi.getPlayerInformation(uuid);

        /**
         * We return since {@link gi} hasn't even started yet.
         */
        if (!gi.inProgress()) {
            return;
        }

        /**
         * If {@link optionalPlayerInfo} is empty, even though the {@link gi} has started. We kick {@link p} to be safe.
         * todo: maybe instead just create a player information and associate the player?? Idk
         */
        if (optionalPlayerInfo.isEmpty()) {
//            gi.associate.
            p.kick(C.mess("&cSomething weird happened.\n&cYou were kicked to avoid an error."), PlayerKickEvent.Cause.PLUGIN);
            return;
        }

        var ppi = optionalPlayerInfo.get();

        /**
         * We return if {@link p} isn't even a hider.
         */
        if (ppi.getRole() != PlayerRole.HIDER) return;

        var nowSneaking = e.isSneaking();
        var loc = p.getLocation();
        var block = loc.getBlock();

        if (nowSneaking) {
            /**
             * The {@link Material} that {@link p} will be hiding as
             */
            var mat = ppi.getMaterial();

            var blockData = mat.createBlockData();

            /**
             * .filter(pi -> pi.getUUID() != uuid)
             */
            for (var pi : gi.getPlayers()) {
                var player = pi.getPlayer();

                if (mat.isBlock())  {
                    if (pi.getUUID().equals(ppi.getUUID())) {
                        continue;
                    }
                }

                /**
                 * What to do if it's a two-tall block.
                 * e.g. {@link Material#SUNFLOWER} {@link Material#TALL_GRASS}, etc.
                 * TODO: Needs MORE supported blocks like beds (in villages) and such
                 * TODO: Maybe create the blockdata before hand and not in the for each loop?
                 */
                if (blockData instanceof Bisected) {
                    ((Bisected) blockData).setHalf(Bisected.Half.BOTTOM);
                    player.sendBlockChange(loc, blockData);

                    var topHalf = mat.createBlockData();
                    ((Bisected) topHalf).setHalf(Bisected.Half.TOP);
                    player.sendBlockChange(loc.clone().add(0, 1, 0), topHalf);
                } else if (blockData instanceof Bed) {
                    ((Bed) blockData).setPart(Bed.Part.HEAD);
                    player.sendBlockChange(loc, blockData);

                    var footHalf = mat.createBlockData();
                    ((Bed) blockData).setPart(Bed.Part.FOOT);
                    // TODO: Somehow maybe towards the way the player is facing????
                }
            }
        } else {

        }
    }

    /**
     * Stops players from taking certain damage while in-game.
     *
     * TODO: make it so if taking a damage by another player, check if seeker, if it is, person hit loses
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamagedWhileInGame(EntityDamageEvent e) {
        /**
         * We return if the {@link EntityType} is not a {@link EntityType#PLAYER}
         */
        if (e.getEntityType() != EntityType.PLAYER) return;

        Player p = (Player) e.getEntity();
        var gi = BlockSeeker.getGameInstanceManager().getInstance.apply(p);

        /**
         *
         */
        if (gi.isPresent()) {

            final var cause = e.getCause();

            switch (cause) {
                case FALL: // Falling from heights
                case SUFFOCATION: // Suffocation in blocks
                case DROWNING: // Drowning, duh
                case STARVATION: // Damage from hunger
                case FIRE: // Standing on fire.
                case FIRE_TICK: // Fire tick (not standing on fire)
                case LAVA: // In lava
                case MAGIC: // Splash potions of harming? Possibly other stuff
                case POISON: // Spiders/Witches (if any), overall poison
                case WITHER: // Wither flower, etc.
                case CONTACT: // Cacti, bushes, etc.
                case HOT_FLOOR: // Exclusively for magma blocks.
                case PROJECTILE: // Projectiles
                case ENTITY_EXPLOSION: // Creeper
                case BLOCK_EXPLOSION: // TNT Maybe?
                case CRAMMING: // Cramming from too many entities.
                {
                    e.setCancelled(true);
                }

                /**
                 * We still allow the hitting animation to go through, but it'll deal no damage.
                 */
                default: {
                    e.setDamage(0.0F);
                }
            }
        }
    }
}
