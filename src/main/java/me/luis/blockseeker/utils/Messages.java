package me.luis.blockseeker.utils;

import net.kyori.adventure.text.TextComponent;

public class Messages {

    public static final TextComponent SOMETHING_UNEXPECTED_HAPPENED = C.mess("&cSomething unexpected happened...");
    public static final TextComponent NOT_IN_A_GAME = C.mess("&cYou are currently not in a BlockSeeker game.");
    public static final TextComponent ALREADY_IN_A_GROUP = C.mess("&cYou are already in a group.");
    public static final TextComponent NOT_IN_A_GROUP = C.mess("&cYou are not in a group.");

    public static final TextComponent ONLY_GROUP_LEADERS_CAN_DO_THAT = C.mess("&cOnly group leaders can do that.");

    public static final TextComponent GROUP_DISBANDED = C.mess("&eYour group was disbanded.");

    public static final TextComponent TELEPORTED_BACK_TO_CENTER = C.mess("&dYou were teleported back to center");


    public static final TextComponent CREATING_GROUP = C.mess("&eCreating a group, please wait...");
    public static final TextComponent GROUP_COULD_NOT_BE_CREATED = C.mess("&cCould not create a group. Please try again later.");
}
