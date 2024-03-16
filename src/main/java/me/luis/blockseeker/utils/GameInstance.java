package me.luis.blockseeker.utils;

import com.google.common.collect.Range;
import me.luis.blockseeker.BlockSeeker;
import me.luis.blockseeker.commands.BlockSeekerCommand;
import me.luis.blockseeker.utils.enums.game.GameState;
import me.luis.blockseeker.utils.enums.player.PlayerRole;
import net.kyori.adventure.title.Title;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static me.luis.blockseeker.utils.enums.game.GameState.*;

/**
 * Represents an ongoing game of block seeker.
 */
public class GameInstance extends BukkitRunnable {

    /**
     * The current seekers/hiders/spectators.
     * Essentially, everyone that is associated.
     */
    private ArrayList<PlayerInformation> players = new ArrayList<>();

    /**
     * The {@link World} where this game instance is taking place.
     */
    private World world;

    /**
     * The range that we can generate coordinates from.
     * Note: This is used for a future feature that will switch the location automatically between rounds.
     */
    private Range<Integer> range;

    /**
     * The center of the current round (if any) (ignoring y)
     */
    private Vector vectorCenter;

    /**
     * The center of the current round (not ignoring y)
     * --
     * Note: This is also the {@link Location} where players spawn at the beginning of the game.
     */
    private Location center;

    /**
     * The current {@link GameState} of this instance.
     */
    private GameState state;

    /**
     * The size of the border for the current round (or game)
     */
    private int borderSize;

    /**
     * All the possible materials that can be used to disguise.
     */
    private CopyOnWriteArrayList<Material> materials;

    /**
     * Current time in seconds
     */
    private int seconds;

    /**
     * The {@link WorldBorder} players will receive.
     */
    private WorldBorder worldBorder;

    /**
     * Attempts to associate the given {@link Player} with the current {@link GameInstance}
     * The return value is whether the association attempt was successful.
     */
    public Function<Player, Boolean> associate = (player) -> {
        var uuid = player.getUniqueId();

        /**
         * If {@link uuid} is already associated with any {@link GameInstance} (including this one), we return.
         */
        if (BlockSeeker.getGameInstanceManager().isAssociated.apply(player)) return false;

        /**
         * We create a {@link PlayerInformation} for {@link player}
         * and add it to {@link players}
         */
        var pi = new PlayerInformation(uuid);

        players.add(pi);

        return true;
    };

    /**
     * Creates a new {@link GameInstance}
     * @param world The {@link World} where the game will take place
     * @param range Deprecated, unused
     * @param _2dCenter The center of the game (as a 2d vector) (y is calculated later)
     * @param borderSize The size of the border
     * --
     * Please Note: The {@link Location} for the center first provided WILL not be the one used, the appropriate "y" level is calculated after within {@link BlockSeekerCommand}
     */
    public GameInstance(World world, Range<Integer> range, Vector _2dCenter, int borderSize) {
        this.world = world;
        this.range = range;
        this.borderSize = borderSize;
        this.vectorCenter = new Vector(_2dCenter.getBlockX(), 0, _2dCenter.getBlockZ());
    }

    private void end() {

        if (state == WAITING_FOR_HIDERS_TO_HIDE) {
            var seekers = getSeekers();
            var hiders = getHiders();

            /**
             * We change the state.
             */
            setState(IN_PROGRESS);

            /**
             * We loop through all the seekers.
             */
            seekers.forEach(seeker -> {
                var player = seeker.getPlayer(true);

                /**
                 * We teleport all seekers to {@link center}
                 */
                player.teleport(center, PlayerTeleportEvent.TeleportCause.PLUGIN);

                /**
                 * Play a "warp" enderman teleport sound.
                 */
                player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5F, 0.9F);

                /**
                 * We set their {@link WorldBorder}
                 */
                player.setWorldBorder(worldBorder);

                /**
                 * We send a title/subtitle for them, saying that they need to find all the hiders!
                 */
                player.showTitle(Title.title(C.mess(""), C.mess("&bFind all &d" + hiders.size() + " &bhiders"), Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(2500), Duration.ofMillis(250))));
            });

            /**
             * We loop through all the hiders.
             */
            hiders.forEach(hider -> {
                var player = hider.getPlayer(true);

                /**
                 * We send a title/subtitle, saying to avoid seekers at all costs!
                 */
                player.showTitle(Title.title(C.mess(""), C.mess("&cSeekers have been released"), Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(2500), Duration.ofMillis(250))));

                /**
                 * Play a enderdragon sound.
                 */
                player.playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5F, 0.9F);
            });
        }
    }

    @Override
    public void run() {

        if (state == STARTING) {
            var hiders = getHiders();

            hiders.forEach(hider -> {
                var player = hider.getPlayer();

                player.sendMessage(C.mess("&dAttempting to teleport to game, please wait..."));

                /**
                 * We clear any potion effects, and then add night vision
                 */
                player.clearActivePotionEffects();
                player.addPotionEffect(Potions.INFINITE_NIGHTVISION);
            });

            var teleportationFuture = CompletableFuture.runAsync(() -> {
                hiders.forEach(hider -> hider.getPlayer().teleportAsync(this.getCenter()).join());
            }).thenRun(() -> {
                /**
                 * We give players a minute to hide.
                 */
                seconds = 60;

                /**
                 * We change the state, since we are now waiting for all hiders to hide.
                 */
                setState(WAITING_FOR_HIDERS_TO_HIDE);

                /**
                 * We create a title to show the hiders as soon as they get teleported.
                 */
                var title = Title.title(C.mess("&a&lHIDE!"), C.mess("&e" + getAppropriateSeekerNames(true) + " &cwill be released soon"));

                hiders.forEach(hider -> {
                    var player = hider.getPlayer();
                    var inv = player.getInventory();

                    /**
                     * Clears out {@link player}'s {@link org.bukkit.inventory.Inventory}
                     */
                    inv.clear();

                    /**
                     * We set their own personal {@link WorldBorder} to {@link worldBorder}
                     */
                    player.setWorldBorder(worldBorder);

                    /**
                     * We give each {@link hider} a disguise compass, so they can switch their disguise before seekers are teleported.
                     */
                    inv.setItem(0, Items.DISGUISE_COMPASS);

                    /**
                     * Messages.
                     */
                    player.showTitle(title);
                });
            });
        }

        if (state == WAITING_FOR_HIDERS_TO_HIDE || state == IN_PROGRESS) {

            /**
             * If {@link seconds} is equal to or less than 0, we end the round (or start the game. Depends on the {@link state})
             */
            if (seconds <= 0) {
                end();
                return;
            }

            if (state == WAITING_FOR_HIDERS_TO_HIDE) {

                /**
                 * Creates and sends an action bar to every hider
                 */
                var hiderActionBar = C.mess(getColorCodeForSeconds(PlayerRole.HIDER) + "You have " + seconds + " seconds to hide");
                getHiders().forEach(pi -> {
                    var hider = pi.getPlayer();

                    hider.sendActionBar(hiderActionBar);
                });

                /**
                 * Creates and sends an action bar to every seeker
                 */
                var seekerActionBar = C.mess("&cYou will be released in " + seconds + " seconds");
                getSeekers().forEach(pi -> {
                    var seeker = pi.getPlayer();

                    seeker.sendActionBar(seekerActionBar);
                });
            }

            seconds--;
        }

        /**
         * Finally, we set every (alive) hider's inventory with their hiding material.
         */
        if (state == WAITING_FOR_HIDERS_TO_HIDE || state == IN_PROGRESS) {
            players.stream().filter(pi -> pi.getRole() == PlayerRole.HIDER).forEach(pi -> {
                var inv = pi.getPlayer().getInventory();
                var material = pi.getMaterial();

                // Create the item stack.
                var item = new ItemStack(material, 1);

                // Loop through every slot, blah blah.
                for (int slot = 0; slot < inv.getSize(); slot++) {
//                    inv.setItem(slot, item);
                }
            });
        }
    }

    private String getColorCodeForSeconds(PlayerRole role) {
        if (role == PlayerRole.HIDER) {
            if (seconds > 30) return "&a";
            else if (seconds > 10) return "&e";
            else {
                return (seconds % 2 == 0 ? "&f" : "&c");
            }
        } else if (role == PlayerRole.SEEKER) {
            // TODO: needed
        }

        return Strings.EMPTY;
    }

    /**
     * Creates a string that says "seekers" (plural) or the IGN of the single seeker.
     * @param capitalSeeker Whether if "seekers" is capital 'S' or not.
     * @return The {@link String}
     */
    public String getAppropriateSeekerNames(boolean capitalSeeker) {
        var sb = new StringBuilder();

        var seekers = getSeekers();

        if (seekers.size() == 1) sb.append(seekers.get(0));
        else sb.append(capitalSeeker ? "S" : "s").append("eekers");

        return sb.toString();
    }

    /**
     * Sets the center {@link Location} for this {@link GameInstance}
     * @param center The {@link Location} that will be represented as the center.
     * Realistically, this can be any {@link Location} as long as the X/Z values are within the {@link WorldBorder}
     */
    public void setCenter(Location center) {
        this.center = center;
    }

    /**
     * @return The size of the border size of the current round (or game overall)
     */
    public int getBorderSize() {
        return borderSize;
    }

    /**
     * @return The center of the current round (not ignoring y)
     * Note: This is also the {@link Location} where players spawn at the beginning of the game.
     */
    public Location getCenter() {
        /**
         * If {@link center} is null or does not match the current round center, we update it.
         */
        if (center == null || center.getBlockX() != vectorCenter.getBlockX() || center.getBlockZ() != vectorCenter.getBlockZ()) {
            var highestY = world.getHighestBlockYAt(vectorCenter.getBlockX(), vectorCenter.getBlockZ());

            center = new Location(world, vectorCenter.getBlockX() + .5, highestY, vectorCenter.getBlockZ() + .5);
        }

        return center;
    }

    public Vector getVectorCenter() {
        return vectorCenter;
    }

    /**
     * @return The current {@link GameState} for this game.
     */
    public GameState getState() {
        return state;
    }

    /**
     * @return The {@link World} instance for this game (or round)
     */
    public World getWorld() {
        return world;
    }

    /**
     * Sets a new {@link WorldBorder}
     * @param wb The new {@link WorldBorder} to use
     * Note: If changed mid-game, all players will be affected (instantly) todo: unimplemented
     */
    public void setWorldBorder(WorldBorder wb) {
        this.worldBorder = wb;
    }

    /**
     * @return Whether this {@link GameInstance} has a valid {@link WorldBorder} or not.
     */
    public boolean hasWorldBorder() {
        return (this.worldBorder != null);
    }

    /**
     * @param uuid The {@link UUID} to look for.
     * @return The {@link Optional<PlayerInformation>} that belongs to the {@link UUID}, or null.
     */
    public Optional<PlayerInformation> getPlayerInformation(UUID uuid) {
        return players.stream().filter(pi -> pi.getUUID().equals(uuid)).findFirst();
    }

    /**
     * Whether a particular {@link UUID} matches with any {@link Player} currently associated with this game instance
     * @param uuid The {@link UUID} to compare against everyone associated with this game instance
     * @return Whether it is associated
     */
    public boolean isAssociated(UUID uuid) {
        return players.stream().anyMatch(pi -> pi.getUUID().equals(uuid));
    }

    /**
     * Gathers and creates a list of {@link PlayerInformation} whose {@link PlayerRole} matches the one provided.
     * @param role The {@link PlayerRole} to look for
     * @return List of {@link PlayerInformation} with the {@link PlayerRole} provided.
     */
    public List<PlayerInformation> getAllPlayerInformationWithRole(PlayerRole role) {
        return players.stream().filter(pi -> pi.getRole() == role).toList();
    }

    /**
     * @return List of {@link PlayerInformation} of every spectator
     */
    public List<PlayerInformation> getSpectators() {
        return getAllPlayerInformationWithRole(PlayerRole.SPECTATOR);
    }

    /**
     * @return List of {@link PlayerInformation} of every hider
     */
    public List<PlayerInformation> getHiders() {
        return getAllPlayerInformationWithRole(PlayerRole.HIDER);
    }

    /**
     * @return List of {@link PlayerInformation} of every seeker
     */
    public List<PlayerInformation> getSeekers() {
        return getAllPlayerInformationWithRole(PlayerRole.SEEKER);
    }

    /**
     * @return List of {@link PlayerInformation} that belongs to every {@link Player} that is involved in this {@link GameInstance}
     */
    public List<PlayerInformation> getPlayers() {
        return players;
    }

    public void addMaterial(Material material) {
        this.materials.add(material);
    }

    public void setMaterials(List<Material> materials) {
        this.materials = new CopyOnWriteArrayList<>(materials);
    }

    public CopyOnWriteArrayList<Material> getMaterials() {
        return materials;
    }

    /**
     * Sets the new {@link GameState}
     * @param state The new {@link GameState}
     */
    public void setState(GameState state) {
        this.state = state;
    }

    public boolean inProgress() {
        return (state == WAITING_FOR_HIDERS_TO_HIDE || state == IN_PROGRESS);
    }
}
