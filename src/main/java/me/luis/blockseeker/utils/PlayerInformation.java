package me.luis.blockseeker.utils;

import me.luis.blockseeker.utils.enums.player.PlayerRole;
import me.luis.blockseeker.utils.enums.player.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Each {@link GameInstance} houses several {@link PlayerInformation} in order to keep
 * track of each individual player, their objective, and their current information.
 */
public class PlayerInformation {


    /**
     * The {@link Player} whom this information belongs to.
     */
    private Player player;

    /**
     * The {@link UUID} of {@link PlayerInformation#player}
     */
    private final UUID uuid;

    /**
     * The current state of the player
     */
    private PlayerState state;

    /**
     * The current role of the player
     * Note: Players are seekers by default.
     */
    private PlayerRole role = PlayerRole.SEEKER;

    /**
     * The {@link Material} that this player currently has.
     * In other words, the block this player will be disguising as.
     * Note: This will be null, if {@link PlayerInformation#role} is anything besides {@link PlayerRole#HIDER}
     * Note 2: Players will be hidden as {@link Material#DIRT} by default.
     */
    private Material material = Material.DIRT;

    public PlayerInformation(Player player) {
        this(player.getUniqueId());
    }

    public PlayerInformation(UUID uuid) {
        this.player = Bukkit.getPlayer(uuid);
        this.uuid = uuid;
    }

    /**
     * @return The {@link Material} this player is hiding as (if hiding) or null (if has any other role)
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Sets the new {@link Material} that this {@link PlayerInformation} can hide as.
     * @param material The new {@link Material}
     */
    public void setMaterial(Material material) {
        this.material = material;
    }

    /**
     * Sets the player's role.
     *
     * @param role The role to set for the player
     * @return Whether the role switch was successful
     */
    public boolean setRole(PlayerRole role) {
        if (this.role == role) return false;

        this.role = role;

        return true;
    }

    /**
     * @return The player's current state.
     */
    public PlayerState getState() {
        return state;
    }

    /**
     * @return The player's current role.
     */
    public PlayerRole getRole() {
        return role;
    }

    /**
     * @return The {@link UUID} of the player
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * @param refresh Whether the {@link PlayerInformation#player} should be regrabbed
     * @return The {@link Player} instance
     */
    public Player getPlayer(boolean refresh) {
        if (refresh) {
            player = Bukkit.getPlayer(uuid);
        }

        return player;
    }

    /**
     * @return The {@link Player} instance
     * Note: The {@link PlayerInformation#player} variable will NOT be refreshed when using this.
     */
    public Player getPlayer() {
        return getPlayer(false);
    }
}
