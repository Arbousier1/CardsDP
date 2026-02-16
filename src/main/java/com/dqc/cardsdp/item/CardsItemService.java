package com.dqc.cardsdp.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.dqc.cardsdp.definitions.CardColor;
import com.dqc.cardsdp.definitions.CardDefinition;
import com.dqc.cardsdp.definitions.CardsDefinitions;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class CardsItemService {
    private static final byte TRUE = 1;
    private static final byte FALSE = 0;
    private static final Color RED_CARD_TONE = Color.fromRGB(102, 0, 0);
    private static final Color BLACK_CARD_TONE = Color.fromRGB(26, 77, 77);
    private static final float DATAPACK_DAMAGE_SCALE = 0.0185F;

    private final JavaPlugin plugin;
    private final CardsDefinitions definitions;

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

    public CardsItemService(JavaPlugin plugin, CardsDefinitions definitions) {
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
        meta.setMaxStackSize(54);
        meta.setEnchantmentGlintOverride(Boolean.FALSE);
        setCardModel(meta, definition.redTone(), definition.deckColor(), faceDown, definition.suit(), definition.rank());
        setCardOwnerLore(meta, definition.ownerName());
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
        stripCardConsumable(card);
        applyOwnerProfileByName(card, definition.ownerName());
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
        meta.setMaxStackSize(1);
        setSingleColorModel(meta, deckColor);
        applyItemModel(meta, "dqc.cards:deck");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyIsDeck, PersistentDataType.BYTE, TRUE);
        pdc.set(keyColor, PersistentDataType.INTEGER, deckColor);
        pdc.set(keyDeckCards, PersistentDataType.BYTE_ARRAY, ItemListCodec.encode(new ArrayList<>(cards)));
        if (meta instanceof Damageable damageable) {
            damageable.setMaxDamage(55);
            damageable.setDamage(resolveDeckDamage(cards.size()));
        }
        deck.setItemMeta(meta);
        deck.setData(
            DataComponentTypes.USE_COOLDOWN,
            UseCooldown.useCooldown(0.01F).cooldownGroup(Key.key("dqc.cards:deck"))
        );
        deck.setData(DataComponentTypes.BUNDLE_CONTENTS, BundleContents.bundleContents(new ArrayList<>(cards)));
        deck.setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.DAMAGE)
        );
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
        if (meta instanceof BlockDataMeta blockDataMeta) {
            blockDataMeta.setBlockData(Bukkit.createBlockData("minecraft:soul_campfire[lit=false,facing=south]"));
        }
        setSingleColorModel(meta, color);
        applyItemModel(meta, "dqc.cards:table");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyIsTable, PersistentDataType.BYTE, TRUE);
        pdc.set(keyColor, PersistentDataType.INTEGER, color);
        table.setItemMeta(meta);
        table.setData(
            DataComponentTypes.CONTAINER,
            ItemContainerContents.containerContents(List.of(createLargeTableCarrier(color)))
        );
        table.setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.CONTAINER)
        );
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
        setSlotModel(meta, locked);
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
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFlags(List.of(faceDown));
        meta.setCustomModelDataComponent(component);
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
        setCardOwnerLore(meta, owner);
        card.setItemMeta(meta);
        applyOwnerProfileByName(card, owner);
    }

    public void setCardProfile(ItemStack card, PlayerProfile profile) {
        if (!isCard(card)) {
            return;
        }
        if (profile == null || profile.getName() == null || profile.getName().isBlank()) {
            card.unsetData(DataComponentTypes.PROFILE);
            card.unsetData(DataComponentTypes.TOOLTIP_DISPLAY);
            return;
        }
        card.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(profile));
        card.setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.PROFILE)
        );
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
        if (deck.hasData(DataComponentTypes.BUNDLE_CONTENTS)) {
            BundleContents bundleContents = deck.getData(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleContents != null) {
                List<ItemStack> out = new ArrayList<>(bundleContents.contents().size());
                for (ItemStack card : bundleContents.contents()) {
                    out.add(card.clone());
                }
                return out;
            }
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
            damageable.setMaxDamage(55);
            damageable.setDamage(resolveDeckDamage(cards.size()));
        }
        deck.setItemMeta(meta);
        deck.setData(DataComponentTypes.BUNDLE_CONTENTS, BundleContents.bundleContents(new ArrayList<>(cards)));
    }

    public CardsDefinitions definitions() {
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
        recipe.setGroup("dqc.cards:deck");
        recipe.setCategory(CraftingBookCategory.MISC);
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
        recipe.setGroup("dqc.cards:table");
        recipe.setCategory(CraftingBookCategory.MISC);
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
        recipe.setGroup("dqc.cards:joker");
        recipe.setCategory(CraftingBookCategory.MISC);
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('R', color.dyeMaterial());
        recipe.setIngredient('B', color.dyeMaterial());
        recipe.setIngredient('C', Material.CYAN_DYE);
        Bukkit.addRecipe(recipe);
    }

    private void applyItemModel(ItemMeta meta, String modelPath) {
        NamespacedKey model = NamespacedKey.fromString(modelPath);
        if (model != null) {
            meta.setItemModel(model);
        }
    }

    private void setItemName(ItemMeta meta, String name) {
        meta.itemName(Component.text(name));
    }

    private void setSingleColorModel(ItemMeta meta, int rgb) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setColors(List.of(rgbColor(rgb)));
        component.setFlags(List.of());
        component.setStrings(List.of());
        component.setFloats(List.of());
        meta.setCustomModelDataComponent(component);
    }

    private void setSlotModel(ItemMeta meta, boolean locked) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFlags(List.of(locked));
        component.setColors(List.of());
        component.setStrings(List.of());
        component.setFloats(List.of());
        meta.setCustomModelDataComponent(component);
    }

    private void setCardModel(ItemMeta meta, boolean redTone, int deckColor, boolean faceDown, String suit, String rank) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setColors(List.of(redTone ? RED_CARD_TONE : BLACK_CARD_TONE, rgbColor(deckColor)));
        component.setFlags(List.of(faceDown));
        component.setStrings(List.of(suit, rank));
        component.setFloats(List.of());
        meta.setCustomModelDataComponent(component);
    }

    private void setCardOwnerLore(ItemMeta meta, String owner) {
        if (owner == null || owner.isBlank()) {
            meta.lore(null);
            return;
        }
        meta.lore(List.of(Component.text(owner).decoration(TextDecoration.ITALIC, false)));
    }

    private int resolveDeckDamage(int cards) {
        int damage = Math.round(cards * DATAPACK_DAMAGE_SCALE);
        return Math.max(0, Math.min(damage, 54));
    }

    private Color rgbColor(int rgb) {
        return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
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
        meta.setEnchantmentGlintOverride(Boolean.FALSE);
        setSingleColorModel(meta, color);
        applyItemModel(meta, "dqc.cards:joker");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyHasJokers, PersistentDataType.BYTE, TRUE);
        pdc.set(keyColor, PersistentDataType.INTEGER, color);
        bag.setItemMeta(meta);
        bag.unsetData(DataComponentTypes.FOOD);
        bag.setData(
            DataComponentTypes.CONSUMABLE,
            Consumable.consumable()
                .consumeSeconds(1.0F)
                .animation(ItemUseAnimation.BOW)
                .hasConsumeParticles(false)
                .sound(Key.key("minecraft:item.bundle.drop_contents"))
        );
        bag.setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.DAMAGE)
        );
        return bag;
    }

    private void stripCardConsumable(ItemStack card) {
        card.unsetData(DataComponentTypes.FOOD);
        card.unsetData(DataComponentTypes.CONSUMABLE);
    }

    private void applyOwnerProfileByName(ItemStack card, String owner) {
        if (owner == null || owner.isBlank()) {
            card.unsetData(DataComponentTypes.PROFILE);
            card.unsetData(DataComponentTypes.TOOLTIP_DISPLAY);
            return;
        }
        setCardProfile(card, Bukkit.createProfile(owner));
    }

    private ItemStack createLargeTableCarrier(int color) {
        ItemStack largeTable = new ItemStack(Material.STONE);
        ItemMeta meta = Objects.requireNonNull(largeTable.getItemMeta());
        applyItemModel(meta, "dqc.cards:large_table");
        setSingleColorModel(meta, color);
        largeTable.setItemMeta(meta);
        return largeTable;
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(plugin, value);
    }
}
