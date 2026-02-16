package com.dqc.cardsdp.listener;

import com.dqc.cardsdp.item.CardsItemService;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class DeckListener implements Listener {
    private final CardsItemService itemService;

    public DeckListener(CardsItemService itemService) {
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
            return;
        }
        event.setCancelled(true);

        List<ItemStack> cards = itemService.getDeckCards(main);
        if (cards.isEmpty()) {
            event.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            return;
        }

        if (event.getPlayer().isSprinting()) {
            shuffle(cards);
        } else {
            Collections.reverse(cards);
        }
        itemService.setDeckCards(main, cards);
        event.getPlayer().getInventory().setItemInMainHand(main);
    }

    private void shuffle(List<ItemStack> cards) {
        for (int i = cards.size() - 1; i >= 1; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            Collections.swap(cards, i, j);
        }
    }
}
