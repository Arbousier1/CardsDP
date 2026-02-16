package com.dqc.cardsdp.listener;

import com.dqc.cardsdp.item.CardsItemService;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public final class JokerListener implements Listener {
    private final CardsItemService itemService;

    public JokerListener(CardsItemService itemService) {
        this.itemService = itemService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsumeJokerBag(PlayerItemConsumeEvent event) {
        if (event.isAsynchronous()) {
            return;
        }
        ItemStack consumed = event.getItem();
        if (!itemService.isJokerBag(consumed)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        removeOneJokerBag(player, consumed);

        int deckColor = itemService.getItemColor(consumed);
        String redOwner = randomOwner(itemService.definitions().redJokerOwners(), "joker_red");
        String blueOwner = randomOwner(itemService.definitions().blueJokerOwners(), "joker_blue");

        spawnJoker(player, itemService.createJokerCard(true, deckColor, redOwner), 0.25);
        spawnJoker(player, itemService.createJokerCard(false, deckColor, blueOwner), -0.25);
    }

    private void spawnJoker(Player player, ItemStack card, double sideways) {
        Location eye = player.getEyeLocation().clone();
        Location drop = eye.add(player.getLocation().getDirection().normalize().multiply(0.25));
        Item item = player.getWorld().dropItem(drop, card);
        item.setPickupDelay(0);
        item.setVelocity(player.getLocation().getDirection().normalize().multiply(0.15).setY(0.06 + sideways * 0.02));
    }

    private void removeOneJokerBag(Player player, ItemStack consumed) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (itemService.isJokerBag(main)) {
            decrement(main, player, true);
            return;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (itemService.isJokerBag(off)) {
            decrement(off, player, false);
            return;
        }
        decrement(consumed, player, true);
    }

    private void decrement(ItemStack item, Player player, boolean mainHand) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        int amount = item.getAmount();
        if (amount <= 1) {
            if (mainHand) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
            return;
        }
        item.setAmount(amount - 1);
        if (mainHand) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.getInventory().setItemInOffHand(item);
        }
    }

    private String randomOwner(List<String> owners, String fallback) {
        if (owners == null || owners.isEmpty()) {
            return fallback;
        }
        return owners.get(ThreadLocalRandom.current().nextInt(owners.size()));
    }
}
