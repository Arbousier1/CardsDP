package com.dqc.cardsdp.definitions;

public record CardDefinition(
    String suit,
    String rank,
    int deckColor,
    boolean redTone,
    String ownerName
) {
}
