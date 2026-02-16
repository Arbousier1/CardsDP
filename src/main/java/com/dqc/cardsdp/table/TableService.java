package com.dqc.cardsdp.table;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.dqc.cardsdp.i18n.I18nService;
import com.dqc.cardsdp.item.CardsItemService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TableService {
    private final JavaPlugin plugin;
    private final CardsItemService itemService;
    private final I18nService i18n;

    private final Map<UUID, TableState> tablesById = new HashMap<>();
    private final Map<UUID, TableState> tableInteractionToState = new HashMap<>();
    private final Map<UUID, StackState> stackInteractionToState = new HashMap<>();
    private final Map<BlockKey, UUID> tableByBlock = new HashMap<>();

    public TableService(JavaPlugin plugin, CardsItemService itemService, I18nService i18n) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.i18n = i18n;
    }

    public void startMaintenanceTask() {
        ensurePrimaryThread("startMaintenanceTask");
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupBrokenTables, 100L, 100L);
    }

    public boolean handleTablePlacement(BlockPlaceEvent event) {
        ensurePrimaryThread("handleTablePlacement");
        if (!itemService.isTable(event.getItemInHand())) {
            return false;
        }

        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        int tableColor = itemService.getItemColor(event.getItemInHand());
        block.setType(Material.END_PORTAL_FRAME, false);

        TableState state = spawnTable(block, player.getYaw(), tableColor);
        return state != null;
    }

    public boolean isTableInteraction(Entity entity) {
        return tableInteractionToState.containsKey(entity.getUniqueId());
    }

    public boolean isStackInteraction(Entity entity) {
        return stackInteractionToState.containsKey(entity.getUniqueId());
    }

    public void handleTableRightClick(Player player, Interaction tableInteraction) {
        ensurePrimaryThread("handleTableRightClick");
        TableState table = tableInteractionToState.get(tableInteraction.getUniqueId());
        if (table == null) {
            return;
        }

        int rotation = rotationFromYaw(player.getYaw());
        for (StackState stack : table.stacks) {
            if (stack.display.isValid()) {
                applyStackDisplayFacing(stack.display, rotation);
            }
        }
    }

    public void handleTableLeftClick(Player player, Interaction tableInteraction) {
        ensurePrimaryThread("handleTableLeftClick");
        TableState table = tableInteractionToState.get(tableInteraction.getUniqueId());
        if (table == null) {
            return;
        }

        table.health -= 1;
        if (table.health <= 0) {
            destroyTable(table.id, player.getGameMode() != GameMode.CREATIVE);
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (TableState state : tablesById.values()) {
                state.health = 2;
            }
        }, 5L);
    }

    public void handleTableBlockBreak(Block block, boolean dropItem) {
        ensurePrimaryThread("handleTableBlockBreak");
        UUID tableId = tableByBlock.get(BlockKey.from(block));
        if (tableId != null) {
            destroyTable(tableId, dropItem);
        }
    }

    public void handleStackRightClick(Player player, Interaction stackInteraction) {
        ensurePrimaryThread("handleStackRightClick");
        StackState stack = stackInteractionToState.get(stackInteraction.getUniqueId());
        if (stack == null) {
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        HeldItem held = classify(main);
        boolean sneak = player.isSneaking();

        if (sneak) {
            switch (held) {
                case CARD -> placeSingleCard(player, stack, true);
                case DECK -> placeFromDeck(player, stack, true);
                case HEAD -> setTopCardOwnerFromHead(player, stack);
                case EMPTY -> rightSneakEmpty(player, stack);
                default -> {
                }
            }
            return;
        }

        switch (held) {
            case CARD -> placeSingleCard(player, stack, false);
            case DECK -> placeFromDeck(player, stack, false);
            case EMPTY -> rightStandEmpty(player, stack);
            default -> {
            }
        }
    }

    public void handleStackLeftClick(Player player, Interaction stackInteraction) {
        ensurePrimaryThread("handleStackLeftClick");
        StackState stack = stackInteractionToState.get(stackInteraction.getUniqueId());
        if (stack == null) {
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        HeldItem held = classify(main);
        boolean sneak = player.isSneaking();

        if (sneak) {
            switch (held) {
                case CARD -> placeSingleCard(player, stack, true);
                case DECK -> placeEntireDeck(player, stack, true);
                case EMPTY -> leftEmptyToOffhand(player, stack);
                default -> {
                }
            }
            return;
        }

        switch (held) {
            case CARD -> placeSingleCard(player, stack, false);
            case DECK -> placeEntireDeck(player, stack, false);
            case EMPTY -> leftEmptyToMainHand(player, stack);
            default -> {
            }
        }
    }

    public void shutdown() {
        ensurePrimaryThread("shutdown");
        List<UUID> ids = new ArrayList<>(tablesById.keySet());
        for (UUID tableId : ids) {
            destroyTable(tableId, false);
        }
    }

    private TableState spawnTable(Block block, float playerYaw, int tableColor) {
        UUID tableId = UUID.randomUUID();
        int rotation = rotationFromYaw(playerYaw);

        Interaction tableInteraction = block.getWorld().spawn(
            block.getLocation().add(0.5, 0.825, 0.5),
            Interaction.class,
            entity -> {
                entity.setInteractionWidth(1.001F);
                entity.setInteractionHeight(-0.826F);
                entity.setResponsive(true);
                entity.setInvulnerable(true);
            }
        );

        ItemDisplay tableDisplay = block.getWorld().spawn(
            block.getLocation().add(0.5, 0.5, 0.5),
            ItemDisplay.class,
            entity -> {
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setInvulnerable(true);
                entity.setItemStack(createLargeTableDisplay(tableColor));
            }
        );

        TableState table = new TableState(tableId, block, tableColor, tableInteraction, tableDisplay);
        double[] offsets = {0.167, 0.5, 0.833};
        for (double x : offsets) {
            for (double z : offsets) {
                ItemDisplay stackDisplay = block.getWorld().spawn(
                    block.getLocation().add(x, 0.82, z),
                    ItemDisplay.class,
                    entity -> {
                        entity.setBillboard(Display.Billboard.FIXED);
                        entity.setInvulnerable(true);
                        entity.setTransformation(createStackTransformation(rotation, 0.3F));
                        entity.setItemStack(itemService.createEmptySlotItem(false));
                    }
                );

                Interaction stackInteraction = block.getWorld().spawn(
                    block.getLocation().add(x, 0.82, z),
                    Interaction.class,
                    entity -> {
                        entity.setInteractionWidth(0.3333F);
                        entity.setInteractionHeight(0.01F);
                        entity.setResponsive(true);
                        entity.setInvulnerable(true);
                    }
                );
                stackDisplay.addPassenger(stackInteraction);

                StackState stackState = new StackState(stackInteraction, stackDisplay);
                table.stacks.add(stackState);
                stackInteractionToState.put(stackInteraction.getUniqueId(), stackState);
            }
        }

        tablesById.put(tableId, table);
        tableInteractionToState.put(tableInteraction.getUniqueId(), table);
        tableByBlock.put(BlockKey.from(block), tableId);
        return table;
    }

    private ItemStack createLargeTableDisplay(int color) {
        ItemStack display = new ItemStack(Material.DARK_OAK_PLANKS);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            NamespacedKey model = NamespacedKey.fromString("dqc.cards:large_table");
            if (model != null) {
                meta.setItemModel(model);
            }
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setColors(List.of(rgbColor(color)));
            component.setFlags(List.of());
            component.setStrings(List.of());
            component.setFloats(List.of());
            meta.setCustomModelDataComponent(component);
            display.setItemMeta(meta);
        }
        return display;
    }

    private Color rgbColor(int rgb) {
        return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private Transformation createStackTransformation(int rotation, float depthScale) {
        return new Transformation(
            new Vector3f(0F, 0F, 0F),
            new Quaternionf().rotateX((float) (-Math.PI / 2.0)),
            new Vector3f(0.30F, 0.30F, depthScale),
            rightRotationFor(rotation)
        );
    }

    private Quaternionf rightRotationFor(int rotation) {
        float radians = switch (rotation) {
            case 180 -> (float) Math.PI;
            case 90 -> (float) (Math.PI / 2.0);
            case 0 -> 0F;
            default -> (float) (Math.PI * 1.5);
        };
        return new Quaternionf().rotateZ(radians);
    }

    private void applyStackDisplayFacing(ItemDisplay display, int rotation) {
        Transformation current = display.getTransformation();
        display.setTransformation(
            new Transformation(
                new Vector3f(current.getTranslation()),
                new Quaternionf(current.getLeftRotation()),
                new Vector3f(current.getScale()),
                rightRotationFor(rotation)
            )
        );
    }

    private void setStackDepth(ItemDisplay display, float depth) {
        Transformation current = display.getTransformation();
        Vector3f scale = new Vector3f(current.getScale());
        scale.z = depth;
        display.setTransformation(
            new Transformation(
                new Vector3f(current.getTranslation()),
                new Quaternionf(current.getLeftRotation()),
                scale,
                new Quaternionf(current.getRightRotation())
            )
        );
    }

    private void placeSingleCard(Player player, StackState stack, boolean faceDown) {
        if (isSlotDisabled(stack)) {
            fail(player, "gameplay.slot_locked");
            return;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (!itemService.isCard(main) || main.getAmount() <= 0) {
            return;
        }

        ItemStack card = main.clone();
        card.setAmount(1);
        itemService.setCardFaceDown(card, faceDown);
        stack.cards.add(card);

        if (main.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            main.setAmount(main.getAmount() - 1);
            player.getInventory().setItemInMainHand(main);
        }

        updateStackDisplay(stack);
        playInsert(player);
    }

    private void placeFromDeck(Player player, StackState stack, boolean faceDown) {
        if (isSlotDisabled(stack)) {
            fail(player, "gameplay.slot_locked");
            return;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (!itemService.isDeck(main)) {
            return;
        }

        List<ItemStack> deck = itemService.getDeckCards(main);
        if (deck.isEmpty()) {
            return;
        }
        ItemStack card = deck.remove(0);
        card.setAmount(1);
        itemService.setCardFaceDown(card, faceDown);
        stack.cards.add(card);

        if (deck.isEmpty()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            itemService.setDeckCards(main, deck);
            player.getInventory().setItemInMainHand(main);
        }

        updateStackDisplay(stack);
        playInsert(player);
    }

    private void placeEntireDeck(Player player, StackState stack, boolean faceDown) {
        if (isSlotDisabled(stack)) {
            fail(player, "gameplay.slot_locked");
            return;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (!itemService.isDeck(main)) {
            return;
        }
        List<ItemStack> deck = itemService.getDeckCards(main);
        if (deck.isEmpty()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            return;
        }

        for (ItemStack card : deck) {
            ItemStack clone = card.clone();
            clone.setAmount(1);
            itemService.setCardFaceDown(clone, faceDown);
            stack.cards.add(clone);
            playInsertQuiet(player);
        }
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        updateStackDisplay(stack);
    }

    private void rightStandEmpty(Player player, StackState stack) {
        if (stack.cards.isEmpty()) {
            if (isOffhandEmpty(player)) {
                stack.locked = false;
                updateStackDisplay(stack);
            }
            return;
        }

        ItemStack top = stack.cards.remove(stack.cards.size() - 1);
        sanitizeCard(top);
        player.getInventory().setItemInMainHand(top);
        updateStackDisplay(stack);
        playRemove(player);
    }

    private void rightSneakEmpty(Player player, StackState stack) {
        if (stack.cards.isEmpty()) {
            if (isOffhandEmpty(player)) {
                stack.locked = true;
                updateStackDisplay(stack);
            }
            return;
        }
        if (!offhandCanAcceptCards(player)) {
            fail(player, "gameplay.offhand_incompatible");
            return;
        }

        ItemStack top = stack.cards.remove(stack.cards.size() - 1);
        sanitizeCard(top);
        addCardToOffhand(player, top);
        updateStackDisplay(stack);
        playRemove(player);
    }

    private void leftEmptyToMainHand(Player player, StackState stack) {
        if (stack.cards.isEmpty()) {
            stack.locked = !stack.locked;
            updateStackDisplay(stack);
            return;
        }

        List<ItemStack> deck = new ArrayList<>(stack.cards.size());
        for (ItemStack card : stack.cards) {
            sanitizeCard(card);
            deck.add(card);
        }
        stack.cards.clear();
        updateStackDisplay(stack);

        int color = deck.isEmpty() ? 0 : itemService.getItemColor(deck.get(0));
        player.getInventory().setItemInMainHand(itemService.createDeck(deck, color));
    }

    private void leftEmptyToOffhand(Player player, StackState stack) {
        if (stack.cards.isEmpty()) {
            stack.locked = !stack.locked;
            updateStackDisplay(stack);
            return;
        }
        if (!offhandCanAcceptCards(player)) {
            fail(player, "gameplay.offhand_incompatible");
            return;
        }

        List<ItemStack> deck = new ArrayList<>();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && !offhand.getType().isAir()) {
            if (itemService.isCard(offhand)) {
                ItemStack old = offhand.clone();
                old.setAmount(1);
                sanitizeCard(old);
                deck.add(old);
            } else if (itemService.isDeck(offhand)) {
                deck.addAll(itemService.getDeckCards(offhand));
            }
        }

        for (ItemStack card : stack.cards) {
            sanitizeCard(card);
            deck.add(card);
        }
        stack.cards.clear();
        updateStackDisplay(stack);

        int color = deck.isEmpty() ? 0 : itemService.getItemColor(deck.get(0));
        player.getInventory().setItemInOffHand(itemService.createDeck(deck, color));
    }

    private void setTopCardOwnerFromHead(Player player, StackState stack) {
        ItemStack head = player.getInventory().getItemInMainHand();
        if (head == null || head.getType() != Material.PLAYER_HEAD) {
            return;
        }

        if (stack.cards.isEmpty()) {
            updateStackDisplay(stack);
            playInsert(player);
            return;
        }

        String owner = extractHeadOwner(head);
        PlayerProfile profile = extractHeadProfile(head);
        if (owner != null && !owner.isBlank()) {
            ItemStack top = stack.cards.get(stack.cards.size() - 1).clone();
            itemService.setCardOwner(top, owner);
            if (profile != null) {
                itemService.setCardProfile(top, profile);
            }
            stack.cards.set(stack.cards.size() - 1, top);
        }
        updateStackDisplay(stack);
        playInsert(player);
    }

    private PlayerProfile extractHeadProfile(ItemStack head) {
        ItemMeta meta = head.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) {
            return null;
        }
        PlayerProfile profile = skullMeta.getPlayerProfile();
        if (profile != null) {
            return profile;
        }
        if (skullMeta.getOwningPlayer() != null && skullMeta.getOwningPlayer().getName() != null) {
            return Bukkit.createProfile(skullMeta.getOwningPlayer().getName());
        }
        return null;
    }

    private String extractHeadOwner(ItemStack head) {
        PlayerProfile profile = extractHeadProfile(head);
        if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null) {
            return skullMeta.getOwningPlayer().getName();
        }
        return null;
    }

    private void addCardToOffhand(Player player, ItemStack card) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType().isAir()) {
            player.getInventory().setItemInOffHand(card);
            return;
        }
        if (itemService.isCard(offhand)) {
            List<ItemStack> cards = new ArrayList<>();
            cards.add(card);
            ItemStack old = offhand.clone();
            old.setAmount(1);
            sanitizeCard(old);
            cards.add(old);
            int color = itemService.getItemColor(cards.get(0));
            player.getInventory().setItemInOffHand(itemService.createDeck(cards, color));
            return;
        }
        if (itemService.isDeck(offhand)) {
            List<ItemStack> cards = itemService.getDeckCards(offhand);
            cards.add(0, card);
            itemService.setDeckCards(offhand, cards);
            player.getInventory().setItemInOffHand(offhand);
        }
    }

    private void updateStackDisplay(StackState stack) {
        if (!stack.display.isValid()) {
            return;
        }
        if (stack.cards.isEmpty()) {
            stack.display.setItemStack(itemService.createEmptySlotItem(stack.locked));
            setStackDepth(stack.display, 0.30F);
        } else {
            stack.display.setItemStack(stack.cards.get(stack.cards.size() - 1).clone());
            setStackDepth(stack.display, stack.cards.size() * 0.05F);
        }

        if (stack.interaction.isValid()) {
            if (stack.cards.size() >= 7) {
                stack.interaction.setInteractionHeight(Math.min(1.0F, stack.cards.size() * 0.0017F));
            } else {
                stack.interaction.setInteractionHeight(0.01F);
            }
        }
    }

    private void destroyTable(UUID tableId, boolean dropItem) {
        TableState table = tablesById.remove(tableId);
        if (table == null) {
            return;
        }

        for (StackState stack : table.stacks) {
            stackInteractionToState.remove(stack.interaction.getUniqueId());
            if (!stack.cards.isEmpty()) {
                List<ItemStack> deckCards = new ArrayList<>();
                for (ItemStack card : stack.cards) {
                    ItemStack clone = card.clone();
                    sanitizeCard(clone);
                    deckCards.add(clone);
                }
                ItemStack deck = itemService.createDeck(deckCards, 0);
                stack.display.getWorld().dropItemNaturally(stack.display.getLocation().add(0, 0.125, 0), deck);
            }
            stack.display.remove();
            stack.interaction.remove();
        }

        tableInteractionToState.remove(table.interaction.getUniqueId());
        table.display.remove();
        table.interaction.remove();
        tableByBlock.remove(BlockKey.from(table.block));

        if (table.block.getType() == Material.END_PORTAL_FRAME) {
            table.block.setType(Material.AIR, false);
        }
        if (dropItem) {
            table.block.getWorld().dropItemNaturally(
                table.block.getLocation().add(0.5, 0.5, 0.5),
                itemService.createTableItem(table.color)
            );
        }
    }

    private void cleanupBrokenTables() {
        ensurePrimaryThread("cleanupBrokenTables");
        List<UUID> toDestroy = new ArrayList<>();
        for (Map.Entry<BlockKey, UUID> entry : tableByBlock.entrySet()) {
            Block block = entry.getKey().resolve();
            if (block == null || block.getType() != Material.END_PORTAL_FRAME) {
                toDestroy.add(entry.getValue());
            }
        }
        for (UUID tableId : toDestroy) {
            destroyTable(tableId, false);
        }
    }

    private int rotationFromYaw(float yaw) {
        float normalized = yaw % 360F;
        if (normalized < 0F) {
            normalized += 360F;
        }
        if (normalized >= 315F || normalized < 45F) {
            return 180;
        }
        if (normalized < 135F) {
            return 90;
        }
        if (normalized < 225F) {
            return 0;
        }
        return -90;
    }

    private void sanitizeCard(ItemStack card) {
        card.setAmount(1);
        itemService.setCardFaceDown(card, false);
    }

    private boolean isSlotDisabled(StackState stack) {
        return stack.cards.isEmpty() && stack.locked;
    }

    private boolean offhandCanAcceptCards(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType().isAir()) {
            return true;
        }
        return itemService.isCard(offhand) || itemService.isDeck(offhand);
    }

    private boolean isOffhandEmpty(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        return offhand == null || offhand.getType().isAir();
    }

    private HeldItem classify(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return HeldItem.EMPTY;
        }
        if (itemService.isCard(item)) {
            return HeldItem.CARD;
        }
        if (itemService.isDeck(item)) {
            return HeldItem.DECK;
        }
        if (item.getType() == Material.PLAYER_HEAD) {
            return HeldItem.HEAD;
        }
        return HeldItem.OTHER;
    }

    private void playInsert(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1F, 1F);
    }

    private void playInsertQuiet(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 0.1F, 1F);
    }

    private void playRemove(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.PLAYERS, 1F, 1F);
    }

    private void fail(Player player, String key) {
        i18n.actionBar(player, key);
    }

    private void ensurePrimaryThread(String operation) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("CardsDP " + operation + " must run on the primary server thread.");
        }
    }

    private enum HeldItem {
        EMPTY, CARD, DECK, HEAD, OTHER
    }

    private static final class TableState {
        private final UUID id;
        private final Block block;
        private final int color;
        private final Interaction interaction;
        private final ItemDisplay display;
        private final List<StackState> stacks = new ArrayList<>();
        private int health = 2;

        private TableState(UUID id, Block block, int color, Interaction interaction, ItemDisplay display) {
            this.id = id;
            this.block = block;
            this.color = color;
            this.interaction = interaction;
            this.display = display;
        }
    }

    private static final class StackState {
        private final Interaction interaction;
        private final ItemDisplay display;
        private final List<ItemStack> cards = new ArrayList<>();
        private boolean locked = false;

        private StackState(Interaction interaction, ItemDisplay display) {
            this.interaction = interaction;
            this.display = display;
        }
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        static BlockKey from(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        Block resolve() {
            org.bukkit.World world = Bukkit.getWorld(worldId);
            if (world == null) {
                return null;
            }
            return world.getBlockAt(x, y, z);
        }
    }
}
