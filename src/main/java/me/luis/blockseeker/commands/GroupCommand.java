package me.luis.blockseeker.commands;

import me.luis.blockseeker.BlockSeeker;
import me.luis.blockseeker.managers.InventoryManager;
import me.luis.blockseeker.utils.C;
import me.luis.blockseeker.utils.GameGroup;
import me.luis.blockseeker.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class GroupCommand implements CommandExecutor, TabCompleter {

    /**
     * Returns a list of online {@link Player} names, who are NOT in the {@link GameGroup} provided.
     * In other words: Only shows players that are not in the {@link GameGroup}
     */
    private static Function<GameGroup, List<String>> getOnlineNamesExcludingGroupMembers = (group) -> {
        var list = new ArrayList<String>();

        Bukkit.getOnlinePlayers().stream().filter(online -> !group.isAssociated.apply(online)).forEach(online -> list.add(online.getName()));

        return list;
    };

    /**
     * Returns a list of every member (excluding leader) in a {@link GameGroup}
     */
    private static Function<GameGroup, List<String>> getGroupMembersExcludingLeader = (group) -> {
        var list = new ArrayList<String>();

        Bukkit.getOnlinePlayers().stream().filter(online -> group.isAssociated.apply(online)).filter(online -> !group.isLeader.apply(online)).forEach(online -> list.add(online.getName()));

        return list;
    };

    private static final ArrayList<String> EMPTY_LIST = new ArrayList<>();
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return null;

        var player = (Player) sender;
        var groupOptional = BlockSeeker.getGroupManager().getGroup.apply(player);
        var inGroup = groupOptional.isPresent();

        /**
         * /group [tab]
         */
        if (args.length == 1) {
            if (inGroup) {
                var group = groupOptional.get();
                var list = new ArrayList<>(List.of("list", "open"));

                /**
                 * If {@link player} is the leader of {@link group}, then we add additional completions.
                 */
                if (group.isLeader.apply(player)) {
                    list.addAll(Arrays.asList("invite", "kick", "disband"));
                }

                /**
                 * Otherwise, if they're just a regular (non-leader) member, then we add only one completion...
                 */
                else {
                    list.add("leave");
                }

                return list;
            } else {
                return Arrays.asList("create", "join");
            }
        }

        /**
         * /group args[1] [tab]
         */
        else if (args.length == 2) {

            /**
             * If {@link player} is in a {@link GameGroup}
             */
            if (inGroup) {

                var group = groupOptional.get();

                /**
                 * If {@link player} is the leader of {@link group}
                 */
                if (group.isLeader.apply(player)) {

                    /**
                     * Suggests names of group members (excluding self), who they can kick from the group.
                     */
                    if (args[1].equalsIgnoreCase("kick")) {
                        return getGroupMembersExcludingLeader.apply(group);
                    }

                    /**
                     * Suggests names of non-group members (excluding ALL group members), that can be invited.
                     */
                    else if (args[1].equalsIgnoreCase("invite")) {
                        return getOnlineNamesExcludingGroupMembers.apply(group);
                    }
                }
            }

            return EMPTY_LIST;
        }

        /**
         * We don't want any null returns, we do empty lists instead.
         */
        return EMPTY_LIST;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;

        var player = (Player) sender;

        /**
         * /group
         */
        if (args.length == 0) {
            var optionalGroup = BlockSeeker.getGroupManager().getGroup.apply(player);

            /**
             * If {@link optionalGroup} is empty, then {@link player} is not a {@link GameGroup}.
             * Therefore, we open the GUI in order for them to create a {@link GameGroup}
             */
            if (optionalGroup.isEmpty()) {
                // TODO: 10/15/2023 temporary, have a menu for this.
                player.sendMessage(C.mess("&eYou are currently not in a group."));
                player.sendMessage(C.mess("&6If you wish to create one, use: &c/group create"));
//                player.sendMessage(C.mess("&c"));
                return true;
            }

            var group = optionalGroup.get();
            var inv = InventoryManager.createGroupInventory(player, group);

            if (inv.isEmpty()) {
                player.sendMessage("inventory not opening, something happened.");
                return true;
            }

            player.openInventory(inv.get());
            return true;
        }

        /**
         * /group args[0]
         */
        if (args.length == 1) {

            /**
             * /group create
             */
            if (args[0].equalsIgnoreCase("create")) {
                var groupOptional = BlockSeeker.getGroupManager().getGroup.apply(player);

                /**
                 * If {@link groupOptional} is empty. It means {@link player} is not in a {@link GameGroup}
                 */
                if (groupOptional.isEmpty()) {
                    player.sendMessage(Messages.CREATING_GROUP);

                    /**
                     * We attempt to create a {@link GameGroup}
                     * Note: If the creation fails, an error message is returned.
                     */
                    var groupCreationFuture = CompletableFuture.supplyAsync(() -> {
                        var group = BlockSeeker.getGroupManager().createGroup(player).join();

                        /**
                         * We sleep the async thread, we don't need to, however, it'll let the plugin relax for a bit.
                         */
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored) {}

                        return group;
                    }).thenAccept(group -> {
                        Bukkit.getConsoleSender().sendMessage(C.mess("&a" + player.getName() + " created a group with code: " + group.getCode()));

                        player.sendMessage(C.mess("&aSuccess! Group code: &b" + group.getCode()));
                        player.sendMessage(C.mess("&dShare your group code with your friends!"));
                    });

                    /**
                     * We cancel {@link groupCreationFuture} if it fails to create a {@link GameGroup} after 5 seconds.
                     */
                    var groupCreationCancellationFuture = CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(5_000);
                        } catch (InterruptedException ignored) { }

                        /**
                         * If {@link groupCreationFuture} is not done, we cancel it.
                         */
                        if (!groupCreationFuture.isDone()) {
                            if (groupCreationFuture.cancel(true)) {
                                player.sendMessage(Messages.GROUP_COULD_NOT_BE_CREATED);
                            } else {
                                Bukkit.getConsoleSender().sendMessage(C.mess("&c" + player.getName() + " was trying to create a group. However, a group was never created and the group creation thread was unable to be cancelled."));
                                player.sendMessage(Messages.SOMETHING_UNEXPECTED_HAPPENED);
                            }
                        }
                    });
                }

                /**
                 * Otherwise, if {@link player} is already in a {@link GameGroup}
                 */
                else {
                    player.sendMessage(Messages.ALREADY_IN_A_GROUP);
                    return true;
                }
            }


            /**
             * /group disband
             */
            else if (args[0].equalsIgnoreCase("disband")) {
                var group = BlockSeeker.getGroupManager().getGroup(player, true, true);

                /**
                 * We return if {@link group} is null.
                 */
                if (group == null) return true;

                group.disband();

                return true;
            }

            /**
             * /group open
             */
            else if (args[0].equalsIgnoreCase("open")) {

                var group = BlockSeeker.getGroupManager().getGroup(player, false, true);

                /**
                 * We return if {@link group} is null.
                 */
                if (group == null) return true;

                group.disband();
            }
        }

        return true;
    }
}
