package me.luis.blockseeker.utils.enums.game;

import me.luis.blockseeker.utils.GameInstance;

public enum GameState {

    /**
     * The {@link GameInstance} is being created.
     * Note: The {@link GameInstance} is not considering official here.
     */
    CREATING,

    /**
     * The {@link GameInstance} exists, but, variables
     * and other values are still being calculated.
     * Note: The {@link GameInstance} is considered official here.
     */
    WAITING,

    /**
     * The game is starting (this phase lasts about a second or two)
     * Note: Hiders have YET to be teleported.
     */
    STARTING,

    /**
     * The hiders are currently hiding, seekers have yet to be released.
     */
    WAITING_FOR_HIDERS_TO_HIDE,

    /**
     * The game is currently ongoing.
     */
    IN_PROGRESS,

    /**
     * The small brief cooldown period between round restarts.
     */
    ROUND_ENDING,

    /**
     * The game (not round) is ending.
     */
    GAME_ENDING,

    /**
     * The game (not round) has ended.
     * Note: It is advised to ignore and remove any {@link GameInstance} whose state matches this one.
     */
    ENDED
}
