package com.dqc.cardsdp.datapack;

import java.util.List;
import java.util.Map;

public record DatapackDefinitions(
    Map<String, Integer> deckColors,
    Map<String, Integer> tableColors,
    Map<String, Integer> jokerColors,
    Map<String, List<CardDefinition>> decksByColor,
    List<String> redJokerOwners,
    List<String> blueJokerOwners
) {
    public int deckColor(String colorId) {
        return deckColors.getOrDefault(colorId, 0);
    }

    public int tableColor(String colorId) {
        return tableColors.getOrDefault(colorId, 0);
    }

    public int jokerColor(String colorId) {
        return jokerColors.getOrDefault(colorId, 0);
    }

    public List<CardDefinition> deckForColor(String colorId) {
        return decksByColor.getOrDefault(colorId, List.of());
    }
}
