package me.luis.blockseeker.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class GlobalWorldListener implements Listener {

    /**
     * Stops natural-occurring rain in every {@link World}
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGlobalWorldListener(WeatherChangeEvent e) {
        if (e.getCause() == WeatherChangeEvent.Cause.NATURAL) {
            e.setCancelled(true);
            e.getWorld().setClearWeatherDuration(Integer.MAX_VALUE);
        }
    }
}
