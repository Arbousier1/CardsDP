package com.dqc.cardsdp.definitions;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.Material;

public enum CardColor {
    BLACK("black", Material.BLACK_DYE, Material.BLACK_CARPET),
    BLUE("blue", Material.BLUE_DYE, Material.BLUE_CARPET),
    BROWN("brown", Material.BROWN_DYE, Material.BROWN_CARPET),
    CYAN("cyan", Material.CYAN_DYE, Material.CYAN_CARPET),
    GRAY("gray", Material.GRAY_DYE, Material.GRAY_CARPET),
    GREEN("green", Material.GREEN_DYE, Material.GREEN_CARPET),
    LIGHT_BLUE("light_blue", Material.LIGHT_BLUE_DYE, Material.LIGHT_BLUE_CARPET),
    LIGHT_GRAY("light_gray", Material.LIGHT_GRAY_DYE, Material.LIGHT_GRAY_CARPET),
    LIME("lime", Material.LIME_DYE, Material.LIME_CARPET),
    MAGENTA("magenta", Material.MAGENTA_DYE, Material.MAGENTA_CARPET),
    ORANGE("orange", Material.ORANGE_DYE, Material.ORANGE_CARPET),
    PINK("pink", Material.PINK_DYE, Material.PINK_CARPET),
    PURPLE("purple", Material.PURPLE_DYE, Material.PURPLE_CARPET),
    RED("red", Material.RED_DYE, Material.RED_CARPET),
    WHITE("white", Material.WHITE_DYE, Material.WHITE_CARPET),
    YELLOW("yellow", Material.YELLOW_DYE, Material.YELLOW_CARPET);

    private static final Map<String, CardColor> BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(CardColor::id, Function.identity()));

    private final String id;
    private final Material dyeMaterial;
    private final Material carpetMaterial;

    CardColor(String id, Material dyeMaterial, Material carpetMaterial) {
        this.id = id;
        this.dyeMaterial = dyeMaterial;
        this.carpetMaterial = carpetMaterial;
    }

    public String id() {
        return id;
    }

    public Material dyeMaterial() {
        return dyeMaterial;
    }

    public Material carpetMaterial() {
        return carpetMaterial;
    }

    public static CardColor fromId(String id) {
        return BY_ID.get(id);
    }
}
