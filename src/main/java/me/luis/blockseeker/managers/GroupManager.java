package me.luis.blockseeker.managers;

import me.luis.blockseeker.BlockSeeker;
import me.luis.blockseeker.utils.GameGroup;
import me.luis.blockseeker.utils.Messages;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class GroupManager {

    /**
     * List of every {@link GameGroup}
     *
     * Note: This is thread safe.
     */
    private CopyOnWriteArrayList<GameGroup> groups = new CopyOnWriteArrayList<>();

    public GroupManager() {}

    /**
     * Whether a particular code is taken or not.
     */
    public Function<Integer, Boolean> isCodeTaken = (code) -> {
        var optional = groups.stream().filter(group -> group.getCode() == code);

        return optional.findAny().isPresent();
    };

    /**
     * Adds a {@link GameGroup} to {@link GroupManager#groups} and returns whether the addition was successful.
     */
    public Function<GameGroup, Boolean> addGroup = (group) -> {
        if (isCodeTaken.apply(group.getCode())) return false;

        groups.add(group);

        return true;
    };

    /**
     * Gets a {@link Player}'s {@link GameGroup}. If they are not in one. The {@link Optional<GameGroup>} will be empty, present otherwise.
     */
    public Function<Player, Optional<GameGroup>> getGroup = (player) -> {
        return groups.stream().filter(group -> group.isAssociated.apply(player)).findAny();
    };

    /**
     * Gets and returns the {@link GameGroup} a player is in
     * @param player The player
     * @param onlyLeaders Whether only to return the {@link GameGroup} if the player provided is the leader.
     * @param sendMessages Whether to send messages to the player if they are (not in a group/not the leader of their group)
     * @return The {@link GameGroup} or null
     */
    public GameGroup getGroup(Player player, boolean onlyLeaders, boolean sendMessages) {
        var optionalGroup = BlockSeeker.getGroupManager().getGroup.apply(player);

        /**
         * If {@link player} is not in a {@link GameGroup}, we inform them and return.
         */
        if (optionalGroup.isEmpty()) {
            if (sendMessages) player.sendMessage(Messages.NOT_IN_A_GROUP);
            return null;
        }

        var group = optionalGroup.get();

        if (onlyLeaders) {
            /**
             * If {@link player} is not the leader of {@link group}
             */
            if (!group.isLeader.apply(player)) {
                if (sendMessages) player.sendMessage(Messages.ONLY_GROUP_LEADERS_CAN_DO_THAT);
                return null;
            }
        }

        return group;
    }

    /**
     * Creates a {@link GameGroup} instance
     * @param groupLeader The leader of the group when it gets created.
     * @return {@link GameGroup} with a unique code and the player provided as the leader.
     * Note: The {@link GameGroup} created here will be added to {@link GroupManager#groups}.
     */
    public CompletableFuture<GameGroup> createGroup(Player groupLeader) {
        return CompletableFuture.supplyAsync(() -> {

            /**
             * Generates a unique and non-taken code for the {@link GameGroup} that is about to be created.
             */
            var codeGenerationFuture = CompletableFuture.supplyAsync(this::generateAvailableGroupCode);
            var code = codeGenerationFuture.join().join();

            /**
             * Create a {@link GameGroup}
             */
            var group = new GameGroup(groupLeader.getUniqueId(), code);

            /**
             * We make the {@link group} official.
             */
            groups.add(group);

            return group;
        });
    }

    /**
     * @return {@link CompletableFuture<Integer>} holding an available (non-taken) group code.
     */
    public CompletableFuture<Integer> generateAvailableGroupCode() {
        return CompletableFuture.supplyAsync(() -> {
            var random = ThreadLocalRandom.current();

            int code;

            do {
                code = random.nextInt(1000, 9999);
            } while (isCodeTaken.apply(code));

            return code;
        });
    }

    public CopyOnWriteArrayList<GameGroup> getGroups() {
        return groups;
    }
}
