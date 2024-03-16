package me.luis.blockseeker;

import me.luis.blockseeker.commands.BlockSeekerCommand;
import me.luis.blockseeker.commands.GroupCommand;
import me.luis.blockseeker.commands.StuckCommand;
import me.luis.blockseeker.listeners.GlobalWorldListener;
import me.luis.blockseeker.listeners.InGameListener;
import me.luis.blockseeker.managers.GroupManager;
import me.luis.blockseeker.managers.InstanceManager;
import me.luis.blockseeker.utils.C;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockSeeker extends JavaPlugin {

    private static final PluginManager pm = Bukkit.getPluginManager();

    private static BlockSeeker instance;
    private static GroupManager groupManager;
    private static InstanceManager instanceManager;

    @Override
    public void onEnable() {
        instance = this;
        groupManager = new GroupManager();
        instanceManager = new InstanceManager();

        registerCommands();

        resetWorlds();

        pm.registerEvents(new InGameListener(), this);
        pm.registerEvents(new GlobalWorldListener(), this);
    }

    private void resetWorlds() {
        for (World world: Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                // We stop all rain (and thunder) in the world.
                world.setClearWeatherDuration(Integer.MAX_VALUE);

                // We stop daylight cycles.
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

                // We make it daytime.
                world.setTime(1800);
            }

            world.getWorldBorder().reset();
            Bukkit.getConsoleSender().sendMessage(C.mess("&aReset the worldborder for world: " + world.getName()));
        }
    }

    private void registerCommands() {
        var bs = getCommand("blockseeker");
        if (bs != null) {
            var bscmd = new BlockSeekerCommand();

            bs.setExecutor(bscmd);
            bs.setTabCompleter(bscmd);
        }

        var group = getCommand("group");
        if (group != null) {
            var groupcmd = new GroupCommand();
            group.setExecutor(groupcmd);
            group.setTabCompleter(groupcmd);
        }

        var stuck = getCommand("stuck");
        if (stuck != null) {
            stuck.setExecutor(new StuckCommand());
        }
    }

    @Override
    public void onDisable() {
//        for (var instance : InstanceManager.getGames()) {
//
//        }
    }

    public static BlockSeeker getInstance() {
        return instance;
    }

    public static InstanceManager getGameInstanceManager() {
        return instanceManager;
    }

    public static GroupManager getGroupManager() {
        return groupManager;
    }
}
