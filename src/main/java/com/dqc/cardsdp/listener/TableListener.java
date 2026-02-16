package com.dqc.cardsdp.listener;

import com.dqc.cardsdp.table.TableService;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class TableListener implements Listener {
    private final TableService tableService;

    public TableListener(TableService tableService) {
        this.tableService = tableService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTablePlace(BlockPlaceEvent event) {
        if (event.isAsynchronous()) {
            return;
        }
        tableService.handleTablePlacement(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRightClick(PlayerInteractEntityEvent event) {
        if (event.isAsynchronous()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Interaction interaction)) {
            return;
        }
        if (tableService.isTableInteraction(interaction)) {
            event.setCancelled(true);
            tableService.handleTableRightClick(event.getPlayer(), interaction);
            return;
        }
        if (tableService.isStackInteraction(interaction)) {
            event.setCancelled(true);
            tableService.handleStackRightClick(event.getPlayer(), interaction);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityLeftClick(EntityDamageByEntityEvent event) {
        if (event.isAsynchronous()) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof Interaction interaction)) {
            return;
        }
        if (tableService.isTableInteraction(interaction)) {
            event.setCancelled(true);
            tableService.handleTableLeftClick(player, interaction);
            return;
        }
        if (tableService.isStackInteraction(interaction)) {
            event.setCancelled(true);
            tableService.handleStackLeftClick(player, interaction);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTableBlockBreak(BlockBreakEvent event) {
        if (event.isAsynchronous()) {
            return;
        }
        boolean dropItem = event.getPlayer().getGameMode() != GameMode.CREATIVE;
        tableService.handleTableBlockBreak(event.getBlock(), dropItem);
    }
}
