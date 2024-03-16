package me.luis.blockseeker.factories;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.joml.Math;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class SkullFactory {

    /**
     * Creates a {@link ItemStack} with a {@link Material#PLAYER_HEAD} and a certain texture
     * @param texture The texture
     * @param amount The amount for the {@link ItemStack}
     * @return The created {@link ItemStack}
     * Website: <a href="https://minecraft-heads.com/">...</a>
     */
    public static ItemStack createSkull(String texture, int amount) {
        var item = new ItemStack(Material.PLAYER_HEAD, Math.clamp(1, Material.PLAYER_HEAD.getMaxStackSize(), amount));
        var meta = (SkullMeta) item.getItemMeta();

        /**
         * We create a dummy {@link PlayerProfile}
         */
        var profile = Bukkit.createProfile(UUID.randomUUID());

        /**
         * We set the texture of the skull and set the profile.
         */
        profile.getProperties().add(new ProfileProperty("textures", texture));

        meta.setPlayerProfile(profile);

        /**
         * We set the item meta again
         */
        item.setItemMeta(meta);

        return item;
    }
}
