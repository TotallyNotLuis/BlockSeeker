package me.luis.blockseeker.utils.enums.game;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.structure.Structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum SupportedStructure {

    /**
     * Stronghold
     * More info:
     */
    STRONGHOLD(Structure.STRONGHOLD, null),

    /**
     * Warm Ocean Ruin
     * More info: <a href="https://minecraft.fandom.com/wiki/Ocean_Ruins#Warm_ocean_ruins">...</a>
     */
    OCEAN_RUIN_WARM(Structure.OCEAN_RUIN_WARM, new Biome[] {
            Biome.WARM_OCEAN,
            Biome.LUKEWARM_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN
    }),

    /**
     * Cold Ocean Ruin
     * More info: <a href="https://minecraft.fandom.com/wiki/Ocean_Ruins#Cold_ocean_ruins">...</a>
     */
    OCEAN_RUIN_COLD(Structure.OCEAN_RUIN_COLD, new Biome[] {
            Biome.COLD_OCEAN,
            Biome.DEEP_COLD_OCEAN
    }),


    /**
     * Desert Pyramid
     * More info: <a href="https://minecraft.fandom.com/wiki/Desert_pyramid">...</a>
     */
    DESERT_PYRAMID(Structure.DESERT_PYRAMID, new Biome[] {
            Biome.DESERT
    }),

    /**
     * Jungle Pyramid
     * More info: <a href="https://minecraft.fandom.com/wiki/Jungle_pyramid">...</a>
     */
    JUNGLE_PYRAMID(Structure.JUNGLE_PYRAMID, new Biome[] {
            Biome.JUNGLE,
            Biome.BAMBOO_JUNGLE
    }),

    /**
     * Woodland Mansion
     * More info: <a href="https://minecraft.fandom.com/wiki/Woodland_Mansion">...</a>
     */
    WOODLAND_MANSION(Structure.MANSION, new Biome[] {
            Biome.DARK_FOREST
    }),

    /**
     * Ocean Monument
     * More info: <a href="https://minecraft.fandom.com/wiki/Ocean_Monument">...</a>
     */
    OCEAN_MONUMENT(Structure.MONUMENT, new Biome[] {
            Biome.DEEP_OCEAN,
            Biome.DEEP_FROZEN_OCEAN,
            Biome.DEEP_COLD_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN
    }),

    /**
     * Villages
     * More info: <a href="https://minecraft.fandom.com/wiki/Village">...</a>
     */

    VILLAGE_PLAINS(Structure.VILLAGE_PLAINS, new Biome[] {
            Biome.PLAINS,
//            Biome.SUNFLOWER_PLAINS, // BE only
            Biome.MEADOW
    }, Material.GRASS_BLOCK, Material.BELL),

    VILLAGE_DESERT(Structure.VILLAGE_DESERT, new Biome[] {
            Biome.DESERT
    }),
    VILLAGE_SAVANNA(Structure.VILLAGE_SAVANNA, new Biome[] {
            Biome.SAVANNA
    }),
    VILLAGE_SNOWY(Structure.VILLAGE_SNOWY, new Biome[] {
            Biome.SNOWY_PLAINS
    }),
    VILLAGE_TAIGA(Structure.VILLAGE_TAIGA, new Biome[] {
            Biome.TAIGA,
//            Biome.SNOWY_TAIGA // BE only
    }),


    ;

    private Structure bukkitStructure;

    /**
     * Array that holds every {@link Biome} that the {@link SupportedStructure} can support.
     * Note: If this is null, the {@link SupportedStructure} can spawn in WHATEVER {@link Biome}
     */
    private Biome[] biomes;
    private Material[] materials;

    SupportedStructure(Structure bukkitStructure, Biome[] biomes, Material... materials) {
        this.bukkitStructure = bukkitStructure;
        this.biomes = biomes;
        this.materials = materials;
    }

    /**
     * @return Whether the {@link SupportedStructure} supports all biomes (e.g. stronghold)
     */
    public boolean supportsAnyBiome() {
        return (this.biomes == null);
    }

    public boolean isSupportedBiome(Biome biome) {
        if (biome == null) return false;

        return (this.biomes == null) || (Arrays.stream(biomes).anyMatch(supportedBiome -> supportedBiome == biome));
    }

    /**
     * @return Array of biomes where this {@link SupportedStructure} can spawn in.
     */
    public Biome[] getBiomes() {
        return biomes;
    }

    /**
     * @return Array of materials that (together) are positive to match the specific structure.
     */
    public Material[] getMaterials() {
        return materials;
    }

    public Structure getBukkitStructure() {
        return bukkitStructure;
    }

    public List<String> getBiomesAsNames() {
        var list = new ArrayList<String>();

        Arrays.stream(biomes).forEach(biome -> list.add(biome.name()));

        return list;
    }

    public static Optional<SupportedStructure> getFromString(String str) {
        return Arrays.stream(values()).filter(ss -> ss.name().equalsIgnoreCase(str)).findAny();
    }
}
