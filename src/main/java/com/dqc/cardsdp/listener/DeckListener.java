package com.dqc.cardsdp.listener;

import com.dqc.cardsdp.item.CardsItemService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class DeckListener implements Listener {
    private final JavaPlugin plugin;
    private final CardsItemService itemService;
    private final Map<UUID, Integer> useFallTicksByPlayer = new HashMap<>();
    private boolean rightCheckScheduled;

    public DeckListener(JavaPlugin plugin, CardsItemService itemService) {
        this.plugin = plugin;
        this.itemService = itemService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeckUse(PlayerInteractEvent event) {
        if (event.isAsynchronous()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack main = event.getPlayer().getInventory().getItemInMainHand();
        if (!itemService.isDeck(main)) {
            return;
        }

        if (!event.getPlayer().isSneaking()) {
            if (action == Action.RIGHT_CLICK_AIR) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        handleRightHeld(event.getPlayer(), main);
    }

    private void handleRightHeld(Player player, ItemStack mainDeck) {
        handleRightRise(player, mainDeck);
        useFallTicksByPlayer.put(player.getUniqueId(), 2);
        scheduleRightCheck();
    }

    private void handleRightRise(Player player, ItemStack mainDeck) {
        List<ItemStack> cards = itemService.getDeckCards(mainDeck);
        if (cards.isEmpty()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            return;
        }

        if (player.isSprinting()) {
            shuffle(cards);
        } else {
            Collections.reverse(cards);
        }
        itemService.setDeckCards(mainDeck, cards);
        player.getInventory().setItemInMainHand(mainDeck);
    }

    private void scheduleRightCheck() {
        if (rightCheckScheduled) {
            return;
        }
        rightCheckScheduled = true;
        Bukkit.getScheduler().runTaskLater(plugin, this::runRightCheck, 1L);
    }

    private void runRightCheck() {
        Iterator<Map.Entry<UUID, Integer>> iterator = useFallTicksByPlayer.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticks = entry.getValue();
            if (ticks == 1) {
                handleRightFall();
            }
            if (ticks <= 1) {
                iterator.remove();
                continue;
            }
            entry.setValue(ticks - 1);
        }
        if (useFallTicksByPlayer.isEmpty()) {
            rightCheckScheduled = false;
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::runRightCheck, 1L);
    }

    private void handleRightFall() {
        // Datapack right_fall currently has no active logic.
    }

    private void shuffle(List<ItemStack> cards) {
        for (int i = cards.size() - 1; i >= 1; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            Collections.swap(cards, i, j);
        }
    }
}
