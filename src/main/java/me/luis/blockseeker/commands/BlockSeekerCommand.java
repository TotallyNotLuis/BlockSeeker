package me.luis.blockseeker.commands;

import com.google.common.collect.Range;
import me.luis.blockseeker.BlockSeeker;
import me.luis.blockseeker.managers.InventoryManager;
import me.luis.blockseeker.utils.C;
import me.luis.blockseeker.utils.GameGroup;
import me.luis.blockseeker.utils.GameInstance;
import me.luis.blockseeker.utils.enums.game.GameState;
import me.luis.blockseeker.utils.enums.game.SupportedStructure;
import me.luis.blockseeker.utils.group.GameSearch;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;


public class BlockSeekerCommand implements CommandExecutor, TabCompleter {

    private static final List<String> EMPTY_LIST = new ArrayList<>();

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return null;

        /**
         * /bs [tab]
         */
        if (args.length == 1) {
            return Arrays.asList("search");
        }

        /**
         * /bs args[0] [tab]
         */
        else if (args.length == 2) {
            /**
             * /bs search [tab]
             * We name every {@link Biome} and {@link SupportedStructure}
             */
            if (args[0].equalsIgnoreCase("search")) {
                var options = new ArrayList<String>();

                // We get the current search (if any)
                var regex = (args[1].isEmpty() ? Strings.EMPTY : args[1].toUpperCase(Locale.ROOT));

                // Suggest biomes/structures that contain the regex.
                Arrays.stream(Biome.values()).filter(biome -> {
                    var biomeName = biome.name();
                    return (biomeName.contains(regex) || biomeName.equalsIgnoreCase(regex));
                }).forEach(biome -> options.add(biome.name()));

                Arrays.stream(SupportedStructure.values()).filter(structure -> {
                    var structureName = structure.name();
                    return (structureName.contains(regex) || structureName.equalsIgnoreCase(regex));
                }).forEach(ss -> options.add(ss.name()));

                return options;
            }
        }

        /**
         * /bs args[0] args[1] [tab]
         */
        else if (args.length == 3) {
            /**
             * /bs search args[1] [tab]
             * We give some default border sizes.
             */
            if (args[0].equalsIgnoreCase("search")) {
                return List.of("25", "50", "100", "250");
            }
        }

        /**
         * /bs args[0] args[1] args[2] [tab]
         */
        else if (args.length == 4) {

            /**
             * /bs search args[1] args[2] [tab]
             * If args[1] is a {@link SupportedStructure}, then the suggestion are the {@link SupportedStructure#biomes}
             */
            if (args[0].equalsIgnoreCase("search")) {

                var sso = SupportedStructure.getFromString(args[1]);

                /**
                 * If {@link sso} is present, then a {@link SupportedStructure} was provided.
                 * In which case, we return all its supported biomes.
                 */
                if (sso.isPresent()) {
                    var ss = sso.get();

                    List<String> options = new ArrayList<>();

                    // We get the current search (if any)
                    var regex = (args[3].isEmpty() ? Strings.EMPTY : args[3].toUpperCase(Locale.ROOT));

                    // Suggest supported biomes that contain the regex
                    Arrays.stream(ss.getBiomes()).filter(biome -> {
                        var biomeName = biome.name();
                        return (biomeName.contains(regex) || biomeName.equalsIgnoreCase(regex));
                    }).forEach(biome -> options.add(biome.name()));

                    return options;
                }
            }
        }

        return EMPTY_LIST;
    }

    private static final PotionEffect INFINITE_BLINDNESS = new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 0, false, false, false);
    private static final PotionEffect NO_BLINDNESS = new PotionEffect(PotionEffectType.BLINDNESS, 0, 0, false, false, false);

    private static final PotionEffect INFINITE_NIGHT_VISION = new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, false, false, false);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage(C.mess("&cThis is a player-only command."));
            return true;
        }

        var player = (Player) sender;
        var uuid = player.getUniqueId();

        /**
         * /bs args[0] args[1] args[2] args[3] args[4]
         * -- Following arguments only apply to: /bs search
         * /bs args[0] args[1] args{2} args{3} args{4}
         * {} = optional
         * args[1] = {@link Biome} OR {@link SupportedStructure}
         * args{2} = Border size.
         * args{3} (Only if a {@link SupportedStructure} is provided) = A specific {@link Biome} supported by the {@link SupportedStructure}
         * args{4} (Only if a {@link SupportedStructure} is provided) = Whether to force args{3} as a {@link Biome}, even if it isn't supported. ("yes" or "true")
         */
        if (args.length >= 1) {

            /**
             * /bs cancel
             */
            if (args[0].equalsIgnoreCase("cancel")) {
                var group = BlockSeeker.getGroupManager().getGroup(player, true, true);

                /**
                 * We return if {@link group} is null.
                 */
                if (group == null) return true;

                /**
                 * If the {@link group} is not searching, we let the leader know.
                 */
                if (group.isSearching()) {
                    player.sendMessage(C.mess("&cYou are not searching for a game."));
                    return true;
                }

                var search = group.getSearch();

                /**
                 * We stop the {@link CompletableFuture} AND the {@link StopWatch}
                 */
                search.stop();

                var time = getTimeString(search.getStopWatch());

                group.broadcast(C.mess("&e" + player.getName() + " stopped the search."));
                group.broadcast(C.mess("&dTime elapsed: &a" + time));
                return true;
            }

            /**
             * /bs search args[1] args{2} args{3}
             */
            else if (args[0].equalsIgnoreCase("search")) {

                var im = BlockSeeker.getGameInstanceManager();

                var hasAssociation = im.isAssociated.apply(player);

                /**
                 * We return if {@link player} is trying to execute this command while in a game.
                 */
                if (hasAssociation) {
                    player.sendMessage(C.mess("&cYou're currently in a game, finish first before doing this."));
                    return true;
                }

                var optionalGroup = BlockSeeker.getGroupManager().getGroup.apply(player);

                /**
                 * We inform {@link player} that they need to be
                 * in a {@link GameGroup} in order to start a game.
                 */
                if (optionalGroup.isEmpty()) {
                    player.sendMessage(C.mess("&cYou must be in a group in order to start a game!"));
                    player.sendMessage(C.mess("&cJoin a friend's group or create a group: &e/group create"));
                    return true;
                }

                var group = optionalGroup.get();

                World world = player.getWorld();

                /**
                 * Adjustable arguments.
                 */
                var criteria = new Object() {

                    /**
                     * By default, {@link Biome#PLAINS} is used.
                     */
                    Biome biome = Arrays.stream(Biome.values()).filter(bukkitBiome -> bukkitBiome.name().equalsIgnoreCase(args[1])).findFirst().orElse(Biome.PLAINS);

                    /**
                     * By default, 25 border size.
                     */
                    int borderSize = 25;

                    /**
                     * By default, no {@link SupportedStructure} is being looked for.
                     */
                    SupportedStructure structure = null;


                    /**
                     * By default, an unsupported {@link Biome} (by the {@link #structure}) is not force.
                     */
                    boolean forcedStructureBiome = false;
                };

                /**
                 * We check if a {@link SupportedStructure} was provided (instead of a {@link Biome}
                 * Note: This is optional, and most of the time a {@link SupportedStructure} will NOT be present.
                 */
                if (args.length >= 2) {
                    /**
                     * We check if the name of a {@link SupportedStructure} was passed as an argument.
                     */
                    criteria.structure = Arrays.stream(SupportedStructure.values()).filter(s -> s.name().equalsIgnoreCase(args[1])).findAny().orElse(null);

                    /**
                     * We set {@link criteria.biome} to null. This way, any {@link SupportedStructure#biomes} would work.
                     */
                    if (criteria.structure != null) criteria.biome = null;
                }

                /**
                 * We check if a valid border size was provided
                 */
                if (args.length >= 3) {
                    try {
                        criteria.borderSize = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {}
                }

                /**
                 * We check if a {@link Biome} supported by {@link criteria.structure} was provided.
                 * Note: This also checks if a {@link Biome} is forced, if it isn't normally supported.
                 */
                if (args.length >= 4) {
                    var parsedBiome = Arrays.stream(Biome.values()).filter(bukkitBiome -> bukkitBiome.name().equalsIgnoreCase(args[3])).findFirst().orElse(null);

                    /**
                     * We check if the {@link parsedBiome} will be forced.
                     */
                    if (args.length >= 5) {
                        criteria.forcedStructureBiome = (args[4].equalsIgnoreCase("true") || args[4].equalsIgnoreCase("yes"));
                    }

                    /**
                     * If {@link parsedBiome} is indeed supported (or forced) by {@link criteria.structure}
                     */
                    if (criteria.forcedStructureBiome || criteria.structure.isSupportedBiome(parsedBiome)) {
                        criteria.biome = parsedBiome;
                    }

                    /**
                     * A {@link Biome} NOT supported (or forced) by {@link criteria.structure} OR null was provided.
                     */
                    else {
                        if (!criteria.forcedStructureBiome && parsedBiome != null) player.sendMessage(C.mess("&c" + criteria.structure.name() + " does not support the " + parsedBiome.name() + " biome."));
                        else player.sendMessage(C.mess("&cYou provided a biome that doesn't even exist."));

                        return true;
                    }
                }

                /**
                 * Stop watch for calculating the time for the entire search.
                 */
                var stopwatch = new StopWatch();
                stopwatch.start(); // Starts the stopwatch

                /**
                 *
                 *  == START OF THE SEARCH
                 *
                 */

                /**
                 * Start of the {@link CompletableFuture} chain.
                 */
                var searcher = CompletableFuture.supplyAsync(() -> {

                    /**
                     * The range used for coordinates
                     * Note: This shouldn't be changed. Limiting the coordinates will lead to longer searches and sometimes (if small enough) yield no results.
                     */
                    var range = Range.closed(-25_000_000, 25_000_000);

                    /**
                     * The current random coordinates that have been generated in the do/while loop.
                     */
                    Vector randomCoords;
                    var searchResults = new Object() {
                        Location center;
                    };

                    int count = 0;
//
//                    if (true) {
//                        player.sendMessage(C.mess("criteria.structure: " + (criteria.structure == null ? "null" : criteria.structure.name())));
//                        player.sendMessage(C.mess("criteria.biome: " + (criteria.biome == null ? "null" : criteria.biome.name())));
//                        player.sendMessage(C.mess("criteria.borderSize: " + criteria.borderSize));
//                    }

                    player.sendMessage(C.mess("&eAttempting to locate a " + (criteria.structure != null ? criteria.structure.name() : criteria.biome.name()) + "... Please wait."));
                    if (criteria.structure != null && criteria.biome != null) {
                        if (criteria.forcedStructureBiome) {
                            player.sendMessage(C.mess("&cForced to be a " + criteria.biome.name() + " biome..."));
                            player.sendMessage(C.mess("&cNOTE: &nThe search WILL be rather extensive"));
                        }
                        else player.sendMessage(C.mess("&6Specifically in a " + criteria.biome.name() + " biome..."));
                    }

                    /**
                     * We keep shuffling coordinates until:
                     * A) {@link searchResults}'s criteria is equal to {@link criteria.biome}
                     * OR
                     * B) {@link criteria.structure} is found within our {@link criteria.borderSize}
                     */
                    var keepLooping = true;

                    do {

                        // Generate a random (2d) vector within the range.
                        randomCoords = generateRandom(range);

                        // We create a center location, for ease of access, plus we need this to actually check the biome.
                        searchResults.center = new Location(world, randomCoords.getBlockX(), 0, randomCoords.getBlockZ());
                        var centerBiome = world.getBiome(searchResults.center);

                        var structureCondition = false;

                        /**
                         * This if statement is a bit complicated. However, once {@link searchResults.center} is randomized, it can do either a couple of things to keep the search running, all are only possible if a {@link SupportedStructure} was provided:
                         * 1) {@link criteria.structure} supports any {@link Biome}
                         * 2) {@link criteria.structure} supports the {@link centerBiome} (e.g. {@link Biome.DESERT} for a {@link SupportedStructure.VILLAGE_DESERT})
                         * --
                         * Some notes about forcing biomes (or in general):
                         *   1) Keep in mind, that {@link centerBiome} will be any {@link Biome} supported by {@link criteria.structure}.
                         *   1.1) {@link criteria.biome} will be the {@link Biome} that we are looking for (this is only true if {@link criteria.forcedStructureBiome}
                         *   1.2) So, in other words, we will keep looking, UNTIL the {@link criteria.structure} happens to be on the {@link criteria.biome} biome.
                         */
                        if (criteria.structure != null && (criteria.structure.supportsAnyBiome() || criteria.structure.isSupportedBiome(centerBiome))) {
//                            player.sendMessage(C.mess("&dFound a supported criteria: " + centerBiome.name() + ", checking if it is suitable."));
//                            if (criteria.forcedStructureBiome) player.sendMessage(C.mess("debug: " + centerBiome.name() + ", " + criteria.structure.supportsAnyBiome() + " | " + criteria.structure.isSupportedBiome(centerBiome)));

                            /**
                             * We pause the async thread, and search for {@link criteria.structure}. Until the search is completed successfully, this is halted (async though).
                             * This variable is the result of the scan (null for none found in the area, not-null otherwise)
                             */
                            var structureSearchResult = C.ensureOnMainThread(() -> {
                                var result = world.locateNearestStructure(searchResults.center, criteria.structure.getBukkitStructure(), (criteria.borderSize / 16), false);

                                /**
                                 * If we are looking for a specific supported {@link Biome} for {@link SupportedStructure}
                                 * We make sure that the {@link Biome} matches here, otherwise, we keep searching.
                                 * ---
                                 * Otherwise, we do nothing, since any {@link Biome} supported by {@link SupportedStructure} will work just fine.
                                 */
                                if (result != null && criteria.biome != null) {
                                    var structureBiome = world.getBiome(result.getLocation());

//                                    player.sendMessage(C.mess("Center: " + centerBiome.name() + " | Criteria: " + criteria.biome + " | Structure: " + structureBiome.name()));

                                    /**
                                     * We return null, which keeps looping.
                                     */
                                    if (structureBiome != criteria.biome) {
//                                        player.sendMessage(C.mess("&cThe two biomes didn't match, returning null."));
                                        return null;
                                    }
                                }

                                return result;
                            }).join();

                            /**
                             * We sleep for 2 seconds before continuing.
                             */
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ignored) {}

                            /**
                             * If {@link structureSearchResult} is not null, we set {@link searchResults.center} to the
                             * {@link structureSearchResult}'s location where the {@link Structure} was found.
                             * This way, we don't have to keep looping until we get lucky. We just adapt.
                             */
                            if (structureSearchResult != null) {
                                searchResults.center = structureSearchResult.getLocation().toCenterLocation();
                                structureCondition = true;
                            } else {

//                                if (criteria.biome == null) player.sendMessage(C.mess("&cIt wasn't suitable, continuing scan..."));
                            }
                        }

                        /**
                         * If either:
                         * A) The {@link centerBiome} matches that of {@link criteria.biome} (and {@link criteria.structure} is null)
                         * OR
                         * B) {@link structureCondition} is true.
                         */
                        if ((world.getBiome(searchResults.center) == criteria.biome && criteria.structure == null) || (structureCondition)) {
                            /**
                             * We then check if certain blocks are present (only true if {@link ss} isn't null)
                             */
                            keepLooping = false;
                        }

                        count += 1;

                    } while (keepLooping);

                    /**
                     * We stop the {@link stopwatch}
                     */
                    stopwatch.stop();

                    var time = getTimeString(stopwatch);

                    player.sendMessage(C.mess("&aSearch took &d" + time));
                    player.sendMessage(C.mess("&aScanned &b" + C.addCommas(count) + " &atime" + (count > 1 ? "s" : Strings.EMPTY) + "."));
                    player.sendMessage(C.mess("&bRunning some background tasks, expect lag."));

                    /**
                     * We create a {@link GameInstance}
                     * Note: The {@link Vector} created here is a 2d one. We don't know the highest y level for the center of the game (yet), so for now, it will be 0.
                     * Note 2: We don't use +0.5 because we use the {@link Location#getBlockX()} and {@link Location#getBlockZ()} which automatically floors the value.
                     */
                    var gi = new GameInstance(world, range, new Vector(searchResults.center.getBlockX(), 0, searchResults.center.getBlockZ()), criteria.borderSize);
                    gi.setState(GameState.CREATING);

                    /**
                     * We scan for all the materials within the world border.
                     */
                    var materialScanResult = CompletableFuture.supplyAsync(() -> InventoryManager.getBlockMaterials(gi).join());

                    // We get the result of the scan.
                    var materials = materialScanResult.join();

                    /**
                     * We set {@link gi}'s materials to the newly scanned materials.
                     */
                    gi.setMaterials(materials);

                    player.sendMessage(C.mess("&cFound " + materials.size() + " materials."));

                    /**
                     * We make the game official.
                     */
                    BlockSeeker.getGameInstanceManager().Add.accept(gi);
                    gi.setState(GameState.WAITING);

                    /**
                     * We return the newly created {@link GameInstance}
                     */
                    return gi;
                }).thenApplyAsync(gi -> {
                    /**
                     * We then load the {@link gi}'s center {@link Location} through Paper (async), we then return the {@link gi} again
                     */
                    var chunkGeneration = CompletableFuture.supplyAsync(() -> {
                        var chunkX = (gi.getVectorCenter().getBlockX() / 16);
                        var chunkZ = (gi.getVectorCenter().getBlockZ() / 16);

                        return gi.getWorld().getChunkAtAsync(chunkX, chunkZ, true).join();
                    });

                    return gi;
                }).thenApplyAsync(gi -> {
                    /**
                     * After we load the {@link gi}'s center {@link Location}, we get the highest Y
                     */
                    var highestY = world.getHighestBlockYAt(gi.getCenter()) + 1;

                    /**
                     * We create the center {@link Location} where players will spawn at.
                     */
                    var newCenter = new Location(gi.getWorld(), gi.getVectorCenter().getBlockX() + .5, highestY, gi.getVectorCenter().getBlockZ() + .5);

                    /**
                     * We set the newly created {@link newCenter}
                     */
                    gi.setCenter(newCenter);

                    return gi;
                }).thenAccept(gi -> C.ensureOnMainThread(() -> {
                    /**
                     * Finally, we create a unique {@link WorldBorder} for every {@link Player}
                     */
                    var wb = Bukkit.createWorldBorder();
                    wb.setCenter(gi.getCenter());

                    /**
                     * Odd numbers will not have a "true" center, thus adding a +1, will make it uneven again.
                     */
                    var calculatedBorderSize = criteria.borderSize + (criteria.borderSize % 2 == 0 ? 1 : 0);

                    // Setting size
                    wb.setSize(calculatedBorderSize);

                    // Warning distance set to 0, to avoid a red "overlay" when near the edges of the border.
                    wb.setWarningDistance(0);

                    /**
                     * We set {@link wb} as the {@link WorldBorder} players will receive once they teleport into the game.
                     */
                    gi.setWorldBorder(wb);

                    /**
                     * We associate every member (including leader) with the {@link gi}
                     */
                    group.getAllMembersAsPlayers().forEach(member -> gi.associate.apply(member));

                    /**
                     * Finally, we set the {@link gi}'s {@link GameState} to {@link GameState#STARTING}
                     */
                    gi.setState(GameState.STARTING);

                    /**
                     * The rest is taken care by {@link GameInstance#run()}
                     */
                    gi.runTaskTimer(BlockSeeker.getInstance(), 20L, 20L);

                    //            player.spawnParticle(Particle.BLOCK_MARKER, center, 1);
                    /**
                     * Dummy variable, this can be anything.
                     */
                    return gi;
                }));

                group.setSearch(new GameSearch(searcher, stopwatch));
            }
        }

        return true;
    }


    private String getTimeString(StopWatch stopwatch) {
        long timeMillis = stopwatch.getTime(); // get the elapsed time in milliseconds
        long minutes = timeMillis / (60 * 1000); // calculate minutes
        long seconds = (timeMillis / 1000) % 60; // calculate seconds
        long millis = timeMillis % 1000; // calculate milliseconds
        return String.format("%02d:%02d:%03d", minutes, seconds, millis); // format the string
    }


    private Vector generateRandom(Range<Integer> range) {
        var random = ThreadLocalRandom.current();

        int lower = range.lowerEndpoint();
        int upper = range.upperEndpoint() + 1; // +1 because of exclusion

        return new Vector(random.nextInt(lower, upper), 0, random.nextInt(lower, upper));
    }
}
