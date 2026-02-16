package com.dqc.cardsdp.item;

import com.dqc.cardsdp.datapack.CardColor;
import com.dqc.cardsdp.datapack.CardDefinition;
import com.dqc.cardsdp.datapack.DatapackDefinitions;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class CardsItemService {
    private static final byte TRUE = 1;
    private static final byte FALSE = 0;

    private final JavaPlugin plugin;
    private final DatapackDefinitions definitions;

    private final NamespacedKey keyIsCard;
    private final NamespacedKey keyIsDeck;
    private final NamespacedKey keyIsTable;
    private final NamespacedKey keyHasJokers;
    private final NamespacedKey keyColor;
    private final NamespacedKey keySuit;
    private final NamespacedKey keyRank;
    private final NamespacedKey keyFaceDown;
    private final NamespacedKey keyCardOwner;
    private final NamespacedKey keyDeckCards;
    private final NamespacedKey keySlotLocked;
    private final Map<String, ItemStack> deckPrototypeByColor = new ConcurrentHashMap<>();
    private final Map<String, ItemStack> tablePrototypeByColor = new ConcurrentHashMap<>();
    private final Map<String, ItemStack> jokerPrototypeByColor = new ConcurrentHashMap<>();

    public CardsItemService(JavaPlugin plugin, DatapackDefinitions definitions) {
        this.plugin = plugin;
        this.definitions = definitions;
        this.keyIsCard = key("is_card");
        this.keyIsDeck = key("is_deck");
        this.keyIsTable = key("is_table");
        this.keyHasJokers = key("has_jokers");
        this.keyColor = key("color");
        this.keySuit = key("suit");
        this.keyRank = key("rank");
        this.keyFaceDown = key("face_down");
        this.keyCardOwner = key("owner");
        this.keyDeckCards = key("deck_cards");
        this.keySlotLocked = key("slot_locked");
    }

    public void registerRecipes() {
        for (CardColor color : CardColor.values()) {
            registerDeckRecipe(color);
            registerTableRecipe(color);
            registerJokerRecipe(color);
        }
    }

    public ItemStack createCard(CardDefinition definition, boolean faceDown) {
        ItemStack card = new ItemStack(Material.POISONOUS_POTATO);
        ItemMeta meta = Objects.requireNonNull(card.getItemMeta());
        setItemName(meta, "Playing Card");
        setModelValue(meta, definition.deckColor());
        applyItemModel(meta, "dqc.cards:card");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyIsCard, PersistentDataType.BYTE, TRUE);
        pdc.set(keyColor, PersistentDataType.INTEGER, definition.deckColor());
        pdc.set(keySuit, PersistentDataType.STRING, definition.suit());
        pdc.set(keyRank, PersistentDataType.STRING, definition.rank());
        pdc.set(keyFaceDown, PersistentDataType.BYTE, faceDown ? TRUE : FALSE);
        if (definition.ownerName() != null && !definition.ownerName().isBlank()) {
            pdc.set(keyCardOwner, PersistentDataType.STRING, definition.ownerName());
        } else {
            pdc.remove(keyCardOwner);
        }
        card.setItemMeta(meta);
        return card;
    }

    public ItemStack createJokerCard(boolean redTone, int deckColor, String ownerName) {
        CardDefinition definition = new CardDefinition("joker", "Z", deckColor, redTone, ownerName);
        return createCard(definition, false);
    }

    public ItemStack createDeck(String colorId) {
        ItemStack prototype = deckPrototypeByColor.computeIfAbsent(colorId, this::buildDeckPrototype);
        return prototype.clone();
    }

    public ItemStack createDeck(List<ItemStack> cards, int deckColor) {
        ItemStack deck = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = Objects.requireNonNull(deck.getItemMeta());
        setItemName(meta, "Deck of Cards");
        setModelValue(meta, deckColor);
        applyItemModel(meta, "dqc.cards:deck");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyIsDeck, PersistentDataType.BYTE, TRUE);
        pdc.set(keyColor, PersistentDataType.INTEGER, deckColor);
        pdc.set(keyDeckCards, PersistentDataType.BYTE_ARRAY, ItemListCodec.encode(new ArrayList<>(cards)));
        if (meta instanceof Damageable damageable) {
            int size = Math.min(cards.size(), 54);
            damageable.setDamage(Math.max(0, 54 - size));
        }
        deck.setItemMeta(meta);
        return deck;
    }

    public ItemStack createTableItem(String colorId) {
        ItemStack prototype = tablePrototypeByColor.computeIfAbsent(colorId, this::buildTablePrototype);
        return prototype.clone();
    }

    public ItemStack createTableItem(int color) {
        ItemStack table = new ItemStack(Material.SOUL_CAMPFIRE);
        ItemMeta meta = Objects.requireNonNull(table.getItemMeta());
        setItemName(meta, "Card Table");
        setModelValue(meta, color);
        applyItemModel(meta, "dqc.cards:table");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyIsTable, PersistentDataType.BYTE, TRUE);
        pdc.set(keyColor, PersistentDataType.INTEGER, color);
        table.setItemMeta(meta);
        return table;
    }

    public ItemStack createJokerBag(String colorId, int amount) {
        ItemStack bag = jokerPrototypeByColor.computeIfAbsent(colorId, this::buildJokerPrototype).clone();
        bag.setAmount(Math.max(1, amount));
        return bag;
    }

    public ItemStack createEmptySlotItem(boolean locked) {
        ItemStack empty = new ItemStack(Material.PAPER);
        ItemMeta meta = Objects.requireNonNull(empty.getItemMeta());
        setItemName(meta, "Card Slot");
        setModelValue(meta, locked ? 1 : 0);
        applyItemModel(meta, "dqc.cards:empty_slot");
        meta.getPersistentDataContainer().set(keySlotLocked, PersistentDataType.BYTE, locked ? TRUE : FALSE);
        empty.setItemMeta(meta);
        return empty;
    }

    public boolean isCard(ItemStack item) {
        return hasByteFlag(item, keyIsCard);
    }

    public boolean isDeck(ItemStack item) {
        return hasByteFlag(item, keyIsDeck);
    }

    public boolean isTable(ItemStack item) {
        return hasByteFlag(item, keyIsTable);
    }

    public boolean isJokerBag(ItemStack item) {
        return hasByteFlag(item, keyHasJokers);
    }

    public boolean isCardFaceDown(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte value = meta.getPersistentDataContainer().get(keyFaceDown, PersistentDataType.BYTE);
        return value != null && value == TRUE;
    }

    public void setCardFaceDown(ItemStack card, boolean faceDown) {
        if (!isCard(card)) {
            return;
        }
        ItemMeta meta = card.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(keyFaceDown, PersistentDataType.BYTE, faceDown ? TRUE : FALSE);
        card.setItemMeta(meta);
    }

    public String getCardOwner(ItemStack card) {
        if (!isCard(card)) {
            return null;
        }
        ItemMeta meta = card.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(keyCardOwner, PersistentDataType.STRING);
    }

    public void setCardOwner(ItemStack card, String owner) {
        if (!isCard(card)) {
            return;
        }
        ItemMeta meta = card.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (owner == null || owner.isBlank()) {
            pdc.remove(keyCardOwner);
        } else {
            pdc.set(keyCardOwner, PersistentDataType.STRING, owner);
        }
        card.setItemMeta(meta);
    }

    public int getItemColor(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Integer color = meta.getPersistentDataContainer().get(keyColor, PersistentDataType.INTEGER);
        return color == null ? 0 : color;
    }

    public List<ItemStack> getDeckCards(ItemStack deck) {
        if (!isDeck(deck)) {
            return new ArrayList<>();
        }
        ItemMeta meta = deck.getItemMeta();
        if (meta == null) {
            return new ArrayList<>();
        }
        byte[] raw = meta.getPersistentDataContainer().get(keyDeckCards, PersistentDataType.BYTE_ARRAY);
        return ItemListCodec.decode(raw);
    }

    public void setDeckCards(ItemStack deck, List<ItemStack> cards) {
        if (!isDeck(deck)) {
            return;
        }
        ItemMeta meta = deck.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyDeckCards, PersistentDataType.BYTE_ARRAY, ItemListCodec.encode(cards));
        if (meta instanceof Damageable damageable) {
            int size = Math.min(cards.size(), 54);
            damageable.setDamage(Math.max(0, 54 - size));
        }
        deck.setItemMeta(meta);
    }

    public DatapackDefinitions definitions() {
        return definitions;
    }

    private boolean hasByteFlag(ItemStack item, NamespacedKey key) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte value = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value != null && value == TRUE;
    }

    private void registerDeckRecipe(CardColor color) {
        NamespacedKey recipeKey = key("recipe_deck_" + color.id());
        Bukkit.removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createDeck(color.id()));
        recipe.shape("PPP", "RBC", "PPP");
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('R', color.dyeMaterial());
        recipe.setIngredient('B', color.dyeMaterial());
        recipe.setIngredient('C', Material.CYAN_DYE);
        Bukkit.addRecipe(recipe);
    }

    private void registerTableRecipe(CardColor color) {
        NamespacedKey recipeKey = key("recipe_table_" + color.id());
        Bukkit.removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createTableItem(color.id()));
        recipe.shape("#", "c", "@");
        recipe.setIngredient('#', Material.ITEM_FRAME);
        recipe.setIngredient('c', color.carpetMaterial());
        recipe.setIngredient('@', new RecipeChoice.MaterialChoice(Tag.PLANKS));
        Bukkit.addRecipe(recipe);
    }

    private void registerJokerRecipe(CardColor color) {
        NamespacedKey recipeKey = key("recipe_joker_" + color.id());
        Bukkit.removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createJokerBag(color.id(), 4));
        recipe.shape(" P ", "RBC", " P ");
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('R', color.dyeMaterial());
        recipe.setIngredient('B', color.dyeMaterial());
        recipe.setIngredient('C', Material.CYAN_DYE);
        Bukkit.addRecipe(recipe);
    }

    private void applyItemModel(ItemMeta meta, String modelPath) {
        try {
            Method method = meta.getClass().getMethod("setItemModel", NamespacedKey.class);
            NamespacedKey model = NamespacedKey.fromString(modelPath.toLowerCase(Locale.ROOT));
            if (model != null) {
                method.invoke(meta, model);
            }
        } catch (Exception ignored) {
            // Older APIs do not expose item model directly; PDC still preserves logic.
        }
    }

    private void setItemName(ItemMeta meta, String name) {
        meta.itemName(Component.text(name));
    }

    private void setModelValue(ItemMeta meta, int value) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFloats(List.of((float) value));
        meta.setCustomModelDataComponent(component);
    }

    private ItemStack buildDeckPrototype(String colorId) {
        int deckColor = definitions.deckColor(colorId);
        List<ItemStack> cards = new ArrayList<>();
        for (CardDefinition definition : definitions.deckForColor(colorId)) {
            cards.add(createCard(definition, false));
        }
        return createDeck(cards, deckColor);
    }

    private ItemStack buildTablePrototype(String colorId) {
        return createTableItem(definitions.tableColor(colorId));
    }

    private ItemStack buildJokerPrototype(String colorId) {
        int color = definitions.jokerColor(colorId);
        ItemStack bag = new ItemStack(Material.POISONOUS_POTATO);
        ItemMeta meta = Objects.requireNonNull(bag.getItemMeta());
        setItemName(meta, "Joker Grab Bag");
        setModelValue(meta, color);
        applyItemModel(meta, "dqc.cards:joker");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyHasJokers, PersistentDataType.BYTE, TRUE);
        pdc.set(keyColor, PersistentDataType.INTEGER, color);
        bag.setItemMeta(meta);
        return bag;
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(plugin, value);
    }
}
