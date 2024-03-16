package me.luis.blockseeker.utils;

import me.luis.blockseeker.BlockSeeker;
import me.luis.blockseeker.managers.GroupManager;
import me.luis.blockseeker.utils.group.GameSearch;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Represents a group of players with a unique code for their group.
 * The group's code can be used to allow players to join this group.
 * The group leader, can then start the game whenever they wish through a GUI.
 */
public class GameGroup {
    /**
     * The maximum amount of members per party.
     * Note: This INCLUDES the leader.
     */
    public static final int MAX_MEMBERS = 200;

    /**
     * The {@link GameSearch} for this group.
     */
    private GameSearch search;

    /**
     * The leader for this group.
     */
    private UUID leader;

    /**
     * The members (leader excluded)
     */
    private final List<UUID> members = new ArrayList<>();

    /**
     * The code for this group.
     */
    private final int code;

    /**
     * Creates a {@link GameGroup}
     * @param groupLeader The leader of the group (who can start games, kick people, etc)
     * @param groupCode The unique code for this group, it is recommended to use {@link GroupManager#generateAvailableGroupCode()} instead of manually providing it
     */
    public GameGroup(UUID groupLeader, int groupCode) {
        this.leader = groupLeader;
        this.code = groupCode;
    }

    /**
     * Whether a {@link Player} is associated with this {@link GameGroup}
     */
    public Function<Player, Boolean> isAssociated = (player) -> {
        var uuid = player.getUniqueId();

        return (leader.equals(uuid) || isAssociatedMemberUUID(uuid));
    };

    /**
     * Whether a {@link Player} is the leader for this {@link GameGroup}
     */
    public Function<Player, Boolean> isLeader = (player) -> {
        var uuid = player.getUniqueId();

        return (leader.equals(uuid));
    };

    /**
     * Removes a {@link UUID} from this {@link GameGroup}
     */
    public Function<UUID, Boolean> removeMember = (uuid) -> {
        var removal = members.removeIf(member -> member.equals(uuid));

        return removal;
    };

    /**
     * Disbands this {@link GameGroup} instantly.
     */
    public void disband() {
        // TODO: 5/17/2023 Check if this group is in a game before disbanding

        /**
         * Removes the {@link GameGroup} from the {@link GroupManager}
         * Note: If this is false, then obviously the group will still exist afterward.
         */
        var success = BlockSeeker.getGroupManager().getGroups().removeIf(group -> group.getCode() == code);

        /**
         * If the removal was successful.
         * We want to confirm that a group was actually removed.
         */
        if (success) {
            /**
             * We broadcast one final message to the group :(
             */
            broadcast(C.mess("&e" + getLeader().getName() + " &chas disbanded the group."));

            /**
             * We clear the {@link members} list.
             */
            members.clear();
        }
    }

    /**
     * Promotes a member of the group to the leader
     * @param newLeaderUUID The {@link UUID} of the member that will become the leader
     * @return Whether the promotion was successful
     */
    public boolean promote(UUID newLeaderUUID) {
        var inGroup = isAssociatedMemberUUID(newLeaderUUID);
        var oldLeader = getLeader();

        /**
         * If {@link newLeaderUUID} is not associated with the group, it'll return an error saying so.
         */
        if (!inGroup) {
            oldLeader.sendMessage(C.mess("&cThat player is not in your party. Check spelling!"));
            return false;
        }

        /**
         * We set the new leader here
         */
        this.leader = newLeaderUUID;

        /**
         * We remove the new leader as a regular member (since they became the leader)
         */
        members.removeIf(member -> member.equals(newLeaderUUID));

        /**
         * We add the old leader, as a regular member
         */
        members.add(oldLeader.getUniqueId());

        /**
         * The new leader (as a player)
         */
        var newLeader = getLeader();

        /**
         * We broadcast a message to the entire group, saying there was a change in leadership.
         */
        broadcast(C.mess("&b" + newLeader.getName() + " &eis now group leader. (Promoted by &d" + oldLeader.getName() + "&e)"));

        return true;
    }

    /**
     * @return Whether this {@link GameGroup} is searching.
     */
    public boolean isSearching() {
//        var cf = search.getCompletableFuture();

        return (search != null && !search.isDone());
    }

    /**
     * @param search The new {@link GameSearch} (or null if there is no search)
     */
    public void setSearch(GameSearch search) {
        /**
         * If {@link #search} is still searching and NOT done, we cancel it.
         */
        if (this.search != null && !this.search.isDone()) {
            this.search.stop();
        }

        this.search = search;
    }

    /**
     * @return The current {@link GameSearch}
     */
    public GameSearch getSearch() {
        return search;
    }

    /**
     * Whether a {@link UUID} is a member (excluding leader) of this {@link GameGroup}
     * @param uuid The {@link UUID} to search for
     * @return Whether it's a member of the group
     */
    private boolean isAssociatedMemberUUID(UUID uuid) {
        return (members.stream().anyMatch(memberUUID -> memberUUID.equals(uuid)));
    }

    /**
     * Broadcast a message to every {@link Player} in the {@link GameGroup}
     * @param message The message to broadcast.
     */
    public void broadcast(TextComponent message) {
        getAllMembersAsPlayers().forEach(player -> player.sendMessage(message));
    }

    /**
     * @return List of every (including leader) member as their respective {@link Player} object.
     */
    public List<Player> getAllMembersAsPlayers() {
        var list = new ArrayList<Player>();

        list.add(getLeader());
        members.forEach(member -> list.add(Bukkit.getPlayer(member)));

        return list;
    }

    /**
     * @return List of every (excluding leader) member as their respective {@link Player} object.
     * Note: If you need the leader as-well, use {@link GameGroup#getAllMembers()}
     */
    public List<Player> getMembersAsPlayers() {
        var list = new ArrayList<Player>();

        members.forEach(member -> list.add(Bukkit.getPlayer(member)));

        return list;
    }

    /**
     * @return List of every (including leader) member's {@link UUID}
     */
    public List<UUID> getAllMembers() {
        var list = new ArrayList<UUID>();
        list.add(leader);
        list.addAll(members);

        return list;
    }

    /**
     * @return List of every (excluding leader) member's {@link UUID}
     * Note: If you need the leader as-well, use {@link GameGroup#getAllMembers()}
     */
    public List<UUID> getMembers() {
        return members;
    }

    /**
     * @return The leader's {@link UUID}
     */
    public UUID getLeaderUUID() {
        return leader;
    }


    /**
     * @return The leader as a {@link Player}
     */
    public Player getLeader() {
        return Bukkit.getPlayer(leader);
    }

    /**
     * @return The unique code for the group.
     */
    public int getCode() {
        return code;
    }
}
