package com.dqc.cardsdp.listener;

import com.dqc.cardsdp.table.TableService;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TablePacketListener extends PacketListenerAbstract {
    private final JavaPlugin plugin;
    private final TableService tableService;

    public TablePacketListener(JavaPlugin plugin, TableService tableService) {
        this.plugin = plugin;
        this.tableService = tableService;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        int entityId = packet.getEntityId();
        if (!tableService.isInteractionEntityId(entityId)) {
            return;
        }

        WrapperPlayClientInteractEntity.InteractAction action = packet.getAction();
        boolean attack = action == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        if (!attack && packet.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            tableService.handleInteractionPacket(player, entityId, attack);
        });
    }
}
