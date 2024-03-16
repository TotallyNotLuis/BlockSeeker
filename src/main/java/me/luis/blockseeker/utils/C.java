package me.luis.blockseeker.utils;

import me.luis.blockseeker.BlockSeeker;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class C {

    /**
     * Colors a non-colored {@link String} and returns the colored version as a {@link TextComponent}
     *
     * @param message The message to color
     * @return The translated colored version of the message
     */
    public static TextComponent mess(String message) {
        return LegacyComponentSerializer.legacy('&').deserialize("&r" + message);
    }

    /**
     * Colors every entry in a {@link List<String>} and returns it as a {@link List<TextComponent>}
     *
     * @param list The list to color
     * @return The translated colored version of the list
     */
    public static List<TextComponent> mess(List<String> list) {
        var colored = new ArrayList<TextComponent>();

        list.forEach(line -> colored.add(C.mess(line)));

        return colored;
    }


    public static String addCommas(int number) {
        return NumberFormat.getInstance(Locale.US).format(number);
    }

    public static <T> CompletableFuture<Void> acceptOnMainThread(Supplier<T> supplier) {

        var future = new CompletableFuture<Void>();

        // if on the main thread already, we simply run it like normal.
        if (Bukkit.getServer().isPrimaryThread()) {
            supplier.get();
            future.complete(null);
            return future;
        }

        /**
         * Otherwise, we ENSURE that this runs on the main thread, and not in ASYNC.
         */
        try {
            Bukkit.getScheduler().runTask(BlockSeeker.getInstance(), () -> {
                supplier.get();
                future.complete(null);
            });
        } catch (Throwable exc) {
            future.completeExceptionally(exc);
        }

        return future;
    }

    public static <T> CompletableFuture<T> ensureOnMainThread(Supplier<T> supplier) {

        var future = new CompletableFuture<T>();

        // if on the main thread already, we simply run and complete it like normal.
        if (Bukkit.getServer().isPrimaryThread()) {
            future.complete(supplier.get());
            return future;
        }

        /**
         * Otherwise, we ENSURE that this runs on the main thread, and not in ASYNC.
         */
        try {
            Bukkit.getScheduler().runTask(BlockSeeker.getInstance(), () -> {
                future.complete(supplier.get());
            });
        } catch (Throwable exc) {
            future.completeExceptionally(exc);
        }

        return future;
    }
}
