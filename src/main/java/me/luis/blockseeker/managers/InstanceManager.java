package me.luis.blockseeker.managers;

import me.luis.blockseeker.utils.GameInstance;
import org.bukkit.entity.Player;
import org.bukkit.util.Consumer;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

public class InstanceManager {

    /**
     * List of every ongoing {@link GameInstance}
     */
    private ArrayList<GameInstance> games = new ArrayList<>();

    /**
     * Whether the player accepted is associated with any {@link GameInstance}
     */
    public Function<Player, Boolean> isAssociated = player -> games.stream().anyMatch(gi -> gi.isAssociated(player.getUniqueId()));

    /**
     * Gets the {@link Optional<GameInstance>} that the player accepted is associated with.
     */
    public Function<Player, Optional<GameInstance>> getInstance = player -> games.stream().filter(gi -> gi.isAssociated(player.getUniqueId())).findFirst();

    public Consumer<GameInstance> Add = instance -> {
        games.add(instance);
    };

    /**
     * @return Every ongoing {@link GameInstance}
     */
    public ArrayList<GameInstance> getGames() {
        return games;
    }
}
