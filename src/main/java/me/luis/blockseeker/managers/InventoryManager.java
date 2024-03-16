package me.luis.blockseeker.managers;

import me.luis.blockseeker.utils.C;
import me.luis.blockseeker.utils.GameGroup;
import me.luis.blockseeker.utils.GameInstance;
import me.luis.blockseeker.utils.Items;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

public class InventoryManager {

    /**
     * Scans the {@link GameInstance} from bottom to top, and returns a {@link CopyOnWriteArrayList} with each
     * {@link Material} found during the scan.
     * Note: Duplicate entries, non-block types, and air, are ignored.
     */
    public static CompletableFuture<CopyOnWriteArrayList<Material>> getBlockMaterials(GameInstance gameInstance) {
        return CompletableFuture.supplyAsync(() -> {

            var center = gameInstance.getVectorCenter();
            var borderSize = gameInstance.getBorderSize();

            World world = gameInstance.getWorld();
            Set<Material> materials = ConcurrentHashMap.newKeySet();

            var halfOfBorderSize = (int) Math.floor(borderSize * .5);

            int minX = (center.getBlockX() - halfOfBorderSize);
            int maxX = (center.getBlockX() + halfOfBorderSize);

            int minZ = (center.getBlockZ() - halfOfBorderSize);
            int maxZ = (center.getBlockZ() + halfOfBorderSize);

            /**
             * We start scanning blocks from the minimum height to the maximum height.
             */
            IntStream.rangeClosed(world.getMinHeight(), world.getMaxHeight()).parallel().forEach(y -> {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        var block = world.getBlockAt(x, y, z);
                        var type = block.getType();

                        /**
                         * Skips over any non-block (or air) material.
                         * e.g. Air, Items, etc.
                         */
                        if (!type.isBlock() || type.isAir()) continue;

                        // We avoid any duplicate entries
                        if (materials.contains(type)) continue;

                        materials.add(type);
                    }
                }
            });

            return new CopyOnWriteArrayList<>(materials);
        });
    }

    public static Inventory createRoleSelectionInventory(GameGroup group) {
        var inv = Bukkit.createInventory(null, 54, C.mess("Your current selection: Random"));

        inv.setItem(27, Items.EMPTY_META_BLACK_STAINED_GLASS_PANE);

        return inv;
    }

    /**
     * todo: Give proper documentation
     */
    public static Inventory createInventory(GameInstance instance) {
        var inventory = Bukkit.createInventory(null, 54, C.mess("Choose a block to disguise as!"));

//        var alphabeticalOrderMaterials = instance.getMaterials().stream().sorted((mat1, mat2) -> mat1.name().compareTo(mat2.name()));
        var alphabeticalOrderMaterials = instance.getMaterials().stream().sorted(Comparator.comparing(Enum::name));

        alphabeticalOrderMaterials.forEach(mat -> {
            var item = new ItemStack(mat);
            var meta = item.getItemMeta();

            /**
             * {@link meta} should really only be null if {@link mat} is {@link Material#AIR}
             */
            if (meta != null) {
                meta.displayName(C.mess("&f" + WordUtils.capitalize(mat.name().toLowerCase().replace('_', ' ')))); // TODO: Format this correctly, this is just a temporary name
                item.setItemMeta(meta);
            } else {
                Bukkit.getConsoleSender().sendMessage(C.mess("&cSkipping ItemMeta for " + mat.name() + " because \"meta\" is null."));
            }

            // TODO: add a pages feature, with the last row having the back and forward arrows.

            inventory.addItem(item);
        });

        return inventory;
    }

    /**
     * Creates an inventory used to manipulate the group.
     * @param player The player attempting to open the menu (Used for the title and other features)
     * @param group The {@link GameGroup}
     * @return The {@link Inventory} for the group, or an empty {@link Optional}
     */
    public static Optional<Inventory> createGroupInventory(Player player, GameGroup group) {
        if (group == null) {
            return Optional.empty();
        }

        /**
         * The title for the inventory that will be created
         */
        var title = (group.isLeader.apply(player) ? "Your group" : player.getName() + "'s group");

        /**
         * The inventory
         */
        var inventory = Bukkit.createInventory(null, 54, C.mess(title));

        borderFill(inventory, Items.EMPTY_META_BLACK_STAINED_GLASS_PANE);

        return Optional.of(inventory);
    }

    /**
     * Fill the border of the inventory with a specific ItemStack
     * @param inventory The inventory
     * @param item The item used to make the border
     */
    private static void borderFill(Inventory inventory, ItemStack item) {
        if (inventory.getSize() % 9 != 0) return;

        int rows = inventory.getSize() / 9; // Assuming a standard 9-slot row inventory

        // Fill the first row
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, item);
        }

        // Fill the last row
        for (int i = inventory.getSize() - 9; i < inventory.getSize(); i++) {
            inventory.setItem(i, item);
        }

        // Fill the first column
        for (int i = 0; i < rows; i++) {
            inventory.setItem(i * 9, item);
        }

        // Fill the last column
        for (int i = 0; i < rows; i++) {
            inventory.setItem(i * 9 + 8, item);
        }
    }
}
