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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
    private static final Map<String, UUID> BUILTIN_OWNER_PROFILE_IDS = createOwnerProfileIds();

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
        if (profile == null || (profile.getId() == null && (profile.getName() == null || profile.getName().isBlank()))) {
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
        Integer currentColor = pdc.get(keyColor, PersistentDataType.INTEGER);
        int nextColor = deckColorFromCards(cards, currentColor == null ? 0 : currentColor);
        pdc.set(keyColor, PersistentDataType.INTEGER, nextColor);
        setSingleColorModel(meta, nextColor);
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

    private int deckColorFromCards(List<ItemStack> cards, int fallback) {
        if (cards == null || cards.isEmpty()) {
            return fallback;
        }
        int color = getItemColor(cards.get(0));
        return color == 0 ? fallback : color;
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
            setCardProfile(card, null);
            return;
        }
        UUID fixedId = BUILTIN_OWNER_PROFILE_IDS.get(owner);
        if (fixedId != null) {
            setCardProfile(card, Bukkit.createProfile(fixedId));
            return;
        }
        if (isValidProfileName(owner)) {
            setCardProfile(card, Bukkit.createProfile(owner));
            return;
        }
        setCardProfile(card, null);
    }

    private boolean isValidProfileName(String name) {
        if (name == null || name.isBlank() || name.length() > 16) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static Map<String, UUID> createOwnerProfileIds() {
        Map<String, UUID> map = new HashMap<>(128);
        map.put("_1024", UUID.fromString("635d5dc3-b9be-4c5b-8474-a735d7afb93b"));
        map.put("100percentme", UUID.fromString("aa6caffc-e7e2-4a16-b182-f919c65c207b"));
        map.put("3njooo", UUID.fromString("9f2ed5ba-d2f6-4f68-a8b9-5775ce56109b"));
        map.put("ADAM_4644", UUID.fromString("ba483835-5da5-4b01-92c6-d286f3c4c8ff"));
        map.put("AddyTheNomad", UUID.fromString("b84afba7-4df1-4e77-8dae-1f6318df21c2"));
        map.put("Aicesnow", UUID.fromString("479d8b82-25a4-473b-af0e-83c26b779919"));
        map.put("ATOMICtheCrow", UUID.fromString("8958dc4b-55ab-49ee-aad6-befb83b7e4f0"));
        map.put("AylaMao117", UUID.fromString("c0d7fd2a-05be-4b32-b994-4ea24d3b57e4"));
        map.put("bbb651", UUID.fromString("95e69664-4b29-4a1a-bd10-bfca42deaa68"));
        map.put("BdoubleO100", UUID.fromString("7163fbce-39ac-4a02-b836-a991c45d2dd1"));
        map.put("BlobGames", UUID.fromString("f6d8473f-430c-4cb7-8fd0-8cdc614a1232"));
        map.put("bpendragon", UUID.fromString("1f15b0ef-e78c-411f-a791-c0a1b345c04c"));
        map.put("bronylike", UUID.fromString("0f6b8d98-3a95-49e0-b1b5-ac4514c4234b"));
        map.put("Caloob_", UUID.fromString("b2fb9e99-1d2b-4327-beee-f1ac6ee619a3"));
        map.put("Coalava", UUID.fromString("e3705aad-14c6-4179-8cd2-f12e02d0055b"));
        map.put("Conure512", UUID.fromString("89104a1d-cac7-4298-9e89-87204d72e378"));
        map.put("cubfan135", UUID.fromString("88e2afec-6f2e-4a34-a96a-de61730bd3ca"));
        map.put("DanielRH", UUID.fromString("9bda9ae7-deb8-46f6-97c5-78e9a5ed7569"));
        map.put("davie53", UUID.fromString("c36d201c-0f8f-4477-9f61-fc92941bd70f"));
        map.put("Deaths_Shad0w", UUID.fromString("0a915591-a3a1-47aa-8be5-1b28a7461fd3"));
        map.put("Docm77", UUID.fromString("05e88dce-714d-4218-be77-fade8b5dfa3c"));
        map.put("DysphoricPeach", UUID.fromString("a36d8eda-b099-4e08-82b2-5d09fb8a6aed"));
        map.put("Eikichirou", UUID.fromString("23ee1eed-15ad-45d3-82ae-77548ece95c8"));
        map.put("EthosLab", UUID.fromString("4f41dcda-449a-46b7-8635-88979061fdd2"));
        map.put("FakeZircon", UUID.fromString("0d008d9f-51df-460c-89f0-055e2f8b0077"));
        map.put("FalseSymmetry", UUID.fromString("87d91548-6f18-491f-a267-7833caa5d7d8"));
        map.put("FaultierLotus54", UUID.fromString("a39fb25e-3247-4c4d-b0f4-fa63dcf948ff"));
        map.put("FernandoDaniel", UUID.fromString("2a48e5b2-4972-4f9c-a23b-09f804cf9aea"));
        map.put("Flamesilk", UUID.fromString("7a52fa51-05c1-40fa-a21f-935151071656"));
        map.put("fuffypandauwu", UUID.fromString("b3523a0b-1031-45cd-823f-f71cfe983807"));
        map.put("fwics", UUID.fromString("0af5a2a4-9b02-4e02-9386-8243fe52d7ad"));
        map.put("G4B8O", UUID.fromString("4b5bcafb-5b5d-4b32-a032-acf8df4b9ea0"));
        map.put("GeminiTay", UUID.fromString("5a1839d2-cecc-4c85-aa08-b346f9f772a1"));
        map.put("Gezinski", UUID.fromString("1908389b-c010-4f41-bd74-c1dd67c0f0f1"));
        map.put("Golden_Wither", UUID.fromString("be055fce-32a6-471d-9dbc-f34f819c66b5"));
        map.put("GoldenRobot_II", UUID.fromString("2fbeab40-410e-4bf7-a37d-ccf186d82138"));
        map.put("Golem64", UUID.fromString("80596a14-7399-4403-8d9a-b630f5d74efd"));
        map.put("GoodTimesWithScar", UUID.fromString("cae9554c-31be-47e2-ba2b-4b8867adacc5"));
        map.put("Grian", UUID.fromString("5f8eb73b-25be-4c5a-a50f-d27d65e30ca0"));
        map.put("GunsAndChips", UUID.fromString("5e98469f-9a6b-43e9-a54d-e1f69436f31a"));
        map.put("HAMMMY7", UUID.fromString("b8002105-f785-4b3e-92a6-6059df98818d"));
        map.put("Hexasann", UUID.fromString("112677d6-766f-40f7-a9b4-242cb714bee6"));
        map.put("hypnotizd", UUID.fromString("b0015b93-8a5d-461d-9991-3cfa23e3296f"));
        map.put("iJevin", UUID.fromString("3f28c559-0898-4be1-9f20-9fd37ca9cd22"));
        map.put("impulseSV", UUID.fromString("f6fe2200-609d-4fe6-88b6-529d59ee5b71"));
        map.put("Jeringlyst", UUID.fromString("5587e26a-994f-401b-aa68-767a94117fa3"));
        map.put("joehillssays", UUID.fromString("53bae456-dbbb-4c2f-8c79-9e8ec26c8382"));
        map.put("Joelydsac", UUID.fromString("5e077fb0-96a9-4aae-b289-9a0d681d08bc"));
        map.put("Jollto", UUID.fromString("901fe7f3-b4fb-40ab-827b-3dd288d856cb"));
        map.put("Keralis", UUID.fromString("bcdc48a5-7d87-4331-a566-ac6deb99d21b"));
        map.put("LightWingUltra", UUID.fromString("187899d4-8a14-415e-81f3-8a4cf60947e7"));
        map.put("lilypadSquared", UUID.fromString("0c7b090a-ba4a-45a6-8459-aa2ce9ce433f"));
        map.put("Lniz", UUID.fromString("a9131278-5ede-4fbc-be32-356e1da1c995"));
        map.put("LordFlame", UUID.fromString("afe5e264-4e75-435e-9f8b-4c2e08b8bfdc"));
        map.put("LukeKr", UUID.fromString("20aeca12-2e4e-4405-83b7-447203ed78f8"));
        map.put("machasins", UUID.fromString("7bbcb791-3c40-48b4-a73c-c957becb9598"));
        map.put("Melancholy__", UUID.fromString("fd52ce5b-dd57-4bd3-9b45-c7c9ca9843c0"));
        map.put("Mister_Scheu", UUID.fromString("b2948ce0-4e36-4c8b-951f-26135dbb98fe"));
        map.put("MrVildy", UUID.fromString("ae45f4af-7ffd-4ee4-b73c-f99e9a07d172"));
        map.put("MumboJumbo", UUID.fromString("c7da90d5-6a05-4217-b94a-7d427cbbcad8"));
        map.put("Nehmne", UUID.fromString("ffb6cb4e-7e65-4c03-b1b5-1135f9b78c99"));
        map.put("Nonik_", UUID.fromString("08fdf1db-e0aa-41d9-944e-69ea247be867"));
        map.put("NotBoringName", UUID.fromString("5119a11c-3bfe-422c-8e14-a13743979bbd"));
        map.put("oga449", UUID.fromString("a360dc09-18f7-4eb6-bfcf-f7631567b258"));
        map.put("omerkb", UUID.fromString("d120c418-0333-40ff-95dd-5f6cc50d2749"));
        map.put("OnyxJS", UUID.fromString("96ef65bf-cf56-43b8-9602-6162f67ed90e"));
        map.put("OpenBagTwo", UUID.fromString("8bcf4ff6-07ab-4cdb-98d0-8fcfee524197"));
        map.put("OrbBoi", UUID.fromString("57919d6a-0758-41f8-8d4c-3fff60432d06"));
        map.put("Paniawesome", UUID.fromString("e71d85a9-de56-4c9b-9881-bbce1bac27ca"));
        map.put("PearlescentMoon", UUID.fromString("75c863ae-bb92-486d-911c-53030c552be0"));
        map.put("ped3strian", UUID.fromString("f69d91ab-65ba-4c03-a15e-f6d9275503c0"));
        map.put("Pepe20129", UUID.fromString("55f3aaa7-24cf-4678-a47a-721de8b19296"));
        map.put("Peril137", UUID.fromString("03a02daa-9d39-48b9-bfae-5e96ec8c2b96"));
        map.put("Popa_42", UUID.fromString("7dfd271e-ef8b-425f-a0ff-e7559d5728a3"));
        map.put("puppywader", UUID.fromString("86a82aa9-cd84-40df-b2aa-879987f7c6a5"));
        map.put("Queen_1405", UUID.fromString("6a56f6ab-ab9b-4c82-9115-b83a79fe2b87"));
        map.put("Ratman0813", UUID.fromString("cbd5ef1d-103c-499e-beb3-899ab920a882"));
        map.put("Rendog", UUID.fromString("adcfbe76-42a7-43c2-ac93-50de07a4a3f0"));
        map.put("RubberCrowy", UUID.fromString("8e17ecaf-9fe0-46b1-ac63-a18754047024"));
        map.put("Simonomi", UUID.fromString("641c2ed1-609f-488d-9731-678a34a06ae0"));
        map.put("Skizzleman", UUID.fromString("64de994d-a707-4d8c-8a48-3ffb786c9b99"));
        map.put("SlimyRedstone", UUID.fromString("386f9e6c-df89-4c1e-95a3-c368188cd7ab"));
        map.put("SmallishBeans", UUID.fromString("69b3107a-6d03-4122-b567-7652fcc3cdb2"));
        map.put("StealthStalker", UUID.fromString("31896eea-a989-4127-976e-d6e4117eef3c"));
        map.put("TangoTek", UUID.fromString("c2faba36-3f6e-4961-b834-5bcfe5657f72"));
        map.put("Technoblade", UUID.fromString("b876ec32-e396-476b-a115-8438d83c67d4"));
        map.put("TheOneTheory", UUID.fromString("8ca38f31-3424-483e-8eb8-e81b406813fd"));
        map.put("ThePoyoPal", UUID.fromString("8d0e2c69-b096-48d5-b971-a3b7d58d0cf3"));
        map.put("TimeBender25", UUID.fromString("646b4e12-6f5e-42cf-839c-0dbccfdbc9a5"));
        map.put("UnableToFindUser", UUID.fromString("e00143ab-47d8-4ae4-9359-9ec29293457b"));
        map.put("Veganwater45", UUID.fromString("2fe4e38e-2772-4be2-9736-d3dd5e40f8ac"));
        map.put("Vertigofy", UUID.fromString("3b38e642-d889-4156-b470-0e72069a66fd"));
        map.put("victhor003", UUID.fromString("abbfc60f-6de9-4bb3-9c6c-da836c7e34c3"));
        map.put("VintageBeef", UUID.fromString("2f723150-24de-44ff-aeee-87c75f7c7a9e"));
        map.put("Welsknight", UUID.fromString("8fc22d29-4bac-4abe-84d4-7920ed4afe47"));
        map.put("xBCrafted", UUID.fromString("826cdcff-ccb0-42c5-9104-fcd4bb4e7f73"));
        map.put("xisumavoid", UUID.fromString("8d86df19-fa5c-4939-ac7c-3b90b2b6abb6"));
        map.put("Y2Kun", UUID.fromString("577b341d-d072-4514-88cd-ceeadf9b4512"));
        map.put("Zedaph", UUID.fromString("f9c3c385-f403-403c-b5b7-867e012e9660"));
        map.put("zeppelans", UUID.fromString("4c16ed1d-98b4-405a-86f6-f23500d07e1d"));
        map.put("Zerahu", UUID.fromString("339d6a8c-4281-4c96-9ad3-0010d760c8df"));
        map.put("Zeyro_p", UUID.fromString("1d077a0d-bdd7-4ca1-8abe-db83f4b3f48e"));
        map.put("ZombieCleo", UUID.fromString("a3075fa7-ec13-49a2-aa47-6529e8b7daf2"));
        return Map.copyOf(map);
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

