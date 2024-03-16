package me.luis.blockseeker.utils;

import me.luis.blockseeker.factories.ItemStackFactory;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class Items {

    public static final ItemStack EMPTY_META_BLACK_STAINED_GLASS_PANE = ItemStackFactory.create(Material.BLACK_STAINED_GLASS_PANE, 1,
            Strings.EMPTY,
            null
    );

//    public static final ItemStack INVITE_FRIENDS = ItemStackFactory.create(Skulls.STEVE_AND_ALEX, 1,
//            "&dGroup ",
//            Arrays.asList(
//                    "&r",
//                    "&7"
//            )
//    );

    public static final ItemStack HIDDEN_GROUP_CODE = ItemStackFactory.create(Material.PAPER, 1,
            "&aGroup code",
            Arrays.asList(
                    "&r",
                    "&7&i*HIDDEN*",
                    "&r",
                    "&eClick to reveal"
            )
    );

    public static final ItemStack SHOWN_GROUP_CODE = ItemStackFactory.create(Material.PAPER, 1,
            "&aGroup code",
            Arrays.asList(
                    "&r",
                    "&d&i%code%",
                    "&r",
                    "&eClick to hide"
            )
    );

    public static final ItemStack DISGUISE_COMPASS = ItemStackFactory.create(Material.RECOVERY_COMPASS, 1,
            "&aDisguise Compass",
            Arrays.asList(
                    "&r",
                    "&7A vital tool used to open a menu with",
                    "&7a collection of blocks to disguise as.",
                    "&r",
                    "&dBe quick! &cThe compass can only be used",
                    "&cbefore seekers start hunting!"
            )
    );
}
