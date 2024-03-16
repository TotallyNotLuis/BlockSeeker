package me.luis.blockseeker.utils.enums.player;

public enum PlayerRole {

    /**
     * The game instance has yet to prompt the player
     * what role they want to play as.
     */
    WAITING,

    /**
     * The role is being selected and has yet to be decided.
     */
    PICKING,

    /**
     * The seeker role, mean't to scout out any hider(s)
     */
    SEEKER,

    /**
     * The hider role, mean't to hide and disguise and not get
     * caught by any {@link PlayerRole#SEEKER}
     */
    HIDER,

    /**
     * However, it is obviously mean't to spectate
     * an ongoing game of BlockSeeker.
     * * Note:
     */
    SPECTATOR
}
