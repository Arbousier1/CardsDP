package com.dqc.cardsdp.datapack;

import com.dqc.cardsdp.i18n.I18nService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public final class DatapackLoader {
    private final Logger logger;
    private final I18nService i18n;

    public DatapackLoader(Logger logger, I18nService i18n) {
        this.logger = logger;
        this.i18n = i18n;
    }

    public DatapackDefinitions load(ClassLoader classLoader) {
        Map<String, Integer> deckColors = new HashMap<>();
        Map<String, Integer> tableColors = new HashMap<>();
        Map<String, Integer> jokerColors = new HashMap<>();
        Map<String, List<CardDefinition>> deckCards = new HashMap<>();

        for (CardColor color : EnumSet.allOf(CardColor.class)) {
            String colorId = color.id();
            JsonObject deckRecipe = readJson(classLoader, "datapack/data/dqc.cards/recipe/deck/" + colorId + ".json");
            if (deckRecipe != null) {
                ParsedDeck parsedDeck = parseDeckRecipe(deckRecipe);
                deckColors.put(colorId, parsedDeck.color());
                deckCards.put(colorId, parsedDeck.cards());
            }

            JsonObject tableRecipe = readJson(classLoader, "datapack/data/dqc.cards/recipe/table/" + colorId + ".json");
            if (tableRecipe != null) {
                tableColors.put(colorId, parseResultColor(tableRecipe));
            }

            JsonObject jokerRecipe = readJson(classLoader, "datapack/data/dqc.cards/recipe/joker/" + colorId + ".json");
            if (jokerRecipe != null) {
                jokerColors.put(colorId, parseResultColor(jokerRecipe));
            }
        }

        List<String> redJokerOwners = parseJokerOwners(readJson(classLoader, "datapack/data/dqc.cards/loot_table/joker_red.json"));
        List<String> blueJokerOwners = parseJokerOwners(readJson(classLoader, "datapack/data/dqc.cards/loot_table/joker_blue.json"));

        return new DatapackDefinitions(
            Map.copyOf(deckColors),
            Map.copyOf(tableColors),
            Map.copyOf(jokerColors),
            Map.copyOf(deckCards),
            List.copyOf(redJokerOwners),
            List.copyOf(blueJokerOwners)
        );
    }

    private ParsedDeck parseDeckRecipe(JsonObject deckRecipe) {
        JsonObject components = getObject(deckRecipe, "result", "components");
        int deckColor = parseColorFromComponents(components);
        List<CardDefinition> cards = new ArrayList<>();
        if (components == null) {
            return new ParsedDeck(deckColor, cards);
        }

        JsonArray bundle = components.getAsJsonArray("minecraft:bundle_contents");
        if (bundle == null) {
            return new ParsedDeck(deckColor, cards);
        }

        for (JsonElement cardElement : bundle) {
            if (!cardElement.isJsonObject()) {
                continue;
            }
            JsonObject cardObject = cardElement.getAsJsonObject();
            JsonObject cardComponents = cardObject.getAsJsonObject("components");
            if (cardComponents == null) {
                continue;
            }
            JsonObject cmd = cardComponents.getAsJsonObject("minecraft:custom_model_data");
            if (cmd == null) {
                continue;
            }

            String suit = "unknown";
            String rank = "?";
            JsonArray strings = cmd.getAsJsonArray("strings");
            if (strings != null && strings.size() >= 2) {
                suit = strings.get(0).getAsString();
                rank = strings.get(1).getAsString();
            }

            boolean redTone = true;
            int cardDeckColor = deckColor;
            JsonArray colors = cmd.getAsJsonArray("colors");
            if (colors != null && colors.size() >= 1) {
                JsonElement first = colors.get(0);
                if (first.isJsonArray()) {
                    JsonArray toneArray = first.getAsJsonArray();
                    double r = toneArray.size() > 0 ? toneArray.get(0).getAsDouble() : 0D;
                    double g = toneArray.size() > 1 ? toneArray.get(1).getAsDouble() : 0D;
                    double b = toneArray.size() > 2 ? toneArray.get(2).getAsDouble() : 0D;
                    redTone = r >= g && r >= b;
                }
                if (colors.size() >= 2 && colors.get(1).isJsonPrimitive()) {
                    cardDeckColor = colors.get(1).getAsInt();
                }
            }

            String owner = null;
            JsonArray lore = cardComponents.getAsJsonArray("lore");
            if (lore != null && lore.size() > 0) {
                JsonElement firstLore = lore.get(0);
                if (firstLore.isJsonObject()) {
                    JsonElement text = firstLore.getAsJsonObject().get("text");
                    owner = text == null ? null : text.getAsString();
                } else if (firstLore.isJsonPrimitive()) {
                    owner = firstLore.getAsString();
                }
            }

            cards.add(new CardDefinition(suit, rank, cardDeckColor, redTone, owner));
        }
        return new ParsedDeck(deckColor, cards);
    }

    private int parseResultColor(JsonObject recipe) {
        JsonObject components = getObject(recipe, "result", "components");
        return parseColorFromComponents(components);
    }

    private int parseColorFromComponents(JsonObject components) {
        if (components == null) {
            return 0;
        }
        JsonObject cmd = components.getAsJsonObject("minecraft:custom_model_data");
        if (cmd == null) {
            return 0;
        }
        JsonArray colors = cmd.getAsJsonArray("colors");
        if (colors == null || colors.size() == 0) {
            return 0;
        }
        return colors.get(0).getAsInt();
    }

    private List<String> parseJokerOwners(JsonObject lootTable) {
        if (lootTable == null) {
            return List.of();
        }
        List<String> owners = new ArrayList<>();
        JsonArray pools = lootTable.getAsJsonArray("pools");
        if (pools == null) {
            return owners;
        }
        for (JsonElement poolElement : pools) {
            if (!poolElement.isJsonObject()) {
                continue;
            }
            JsonObject pool = poolElement.getAsJsonObject();
            JsonArray entries = pool.getAsJsonArray("entries");
            if (entries == null) {
                continue;
            }
            for (JsonElement entryElement : entries) {
                if (!entryElement.isJsonObject()) {
                    continue;
                }
                JsonObject entry = entryElement.getAsJsonObject();
                JsonArray functions = entry.getAsJsonArray("functions");
                if (functions == null || functions.size() == 0) {
                    continue;
                }
                JsonObject firstFunction = functions.get(0).getAsJsonObject();
                JsonObject components = firstFunction.getAsJsonObject("components");
                if (components == null) {
                    continue;
                }
                JsonArray lore = components.getAsJsonArray("lore");
                if (lore == null || lore.size() == 0) {
                    continue;
                }
                JsonElement firstLore = lore.get(0);
                String owner = null;
                if (firstLore.isJsonObject()) {
                    JsonElement text = firstLore.getAsJsonObject().get("text");
                    owner = text == null ? null : text.getAsString();
                } else if (firstLore.isJsonPrimitive()) {
                    owner = firstLore.getAsString();
                }
                if (owner != null && !owner.isBlank()) {
                    owners.add(owner);
                }
            }
        }
        return owners;
    }

    private JsonObject readJson(ClassLoader classLoader, String path) {
        InputStream stream = classLoader.getResourceAsStream(path);
        if (stream == null) {
            i18n.warning(logger, "datapack.missing_resource", Placeholder.unparsed("path", path));
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ex) {
            i18n.warning(
                logger,
                "datapack.parse_failed",
                Placeholder.unparsed("path", path),
                Placeholder.unparsed("reason", ex.getMessage() == null ? "unknown" : ex.getMessage())
            );
            return null;
        }
    }

    private JsonObject getObject(JsonObject root, String... keys) {
        JsonObject current = root;
        for (String key : keys) {
            if (current == null) {
                return null;
            }
            JsonElement element = current.get(key);
            if (element == null || !element.isJsonObject()) {
                return null;
            }
            current = element.getAsJsonObject();
        }
        return current;
    }

    private record ParsedDeck(int color, List<CardDefinition> cards) {
    }
}
