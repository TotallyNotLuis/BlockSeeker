package me.luis.blockseeker.factories;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionFactory {

    public static PotionEffect createInfinite(PotionEffectType type) {
        return createInfinite(type, 1);
    }

    public static PotionEffect createInfinite(PotionEffectType type, int amplifier) {
        return new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier - 1, false, false, true);
    }
}
