package com.dqc.cardsdp.listener;

import com.dqc.cardsdp.table.TableService;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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
    public void onTableBlockBreak(BlockBreakEvent event) {
        if (event.isAsynchronous()) {
            return;
        }
        boolean dropItem = event.getPlayer().getGameMode() != GameMode.CREATIVE;
        tableService.handleTableBlockBreak(event.getBlock(), dropItem);
    }
}
