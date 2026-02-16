package com.dqc.cardsdp.datapack;

public record CardDefinition(
    String suit,
    String rank,
    int deckColor,
    boolean redTone,
    String ownerName
) {
}
