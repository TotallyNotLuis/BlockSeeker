package me.luis.blockseeker.factories;

import me.luis.blockseeker.utils.C;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.joml.Math;

import java.util.List;

public class ItemStackFactory {

    public static ItemStack create(Material material, int amount) {
        return create(material, amount, null);
    }

    public static ItemStack create(ItemStack item, int amount) {
        return create(item, 1, null);
    }

    public static ItemStack create(Material material, int amount, String displayName) {
        return create(material, amount, displayName, null);
    }

    public static ItemStack create(ItemStack item, int amount, String displayName) {
        return create(item, amount, displayName, null);
    }

    public static ItemStack create(Material material, int amount, String displayName, List<String> lore) {
        return create(new ItemStack(material), amount, displayName, lore);
    }

    public static ItemStack create(ItemStack item, int amount, String displayName, List<String> lore) {
        var type = item.getType();

        /**
         * We clamp the amount (safety measure)
         */
        var actualAmount = Math.clamp(1, type.getMaxStackSize(), amount);

        item.setAmount(amount);

        if (displayName != null || lore != null) {
            var meta = item.getItemMeta();

            if (displayName != null) meta.displayName(C.mess(displayName));
            if (lore != null && !lore.isEmpty()) meta.lore(C.mess(lore));

            item.setItemMeta(meta);
        }

        return item;
    }
}
