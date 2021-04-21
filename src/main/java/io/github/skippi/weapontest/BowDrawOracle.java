package io.github.skippi.weapontest;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BowDrawOracle implements Listener {
    private final Map<UUID, Boolean> playerPulls = new HashMap<>();

    public boolean isDrawing(UUID playerId) {
        return playerPulls.getOrDefault(playerId, false);
    }

    @EventHandler
    private void observe(PlayerItemHeldEvent event) {
        playerPulls.put(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    private void observe(PlayerInteractEvent event) {
        @NotNull Action action = event.getAction();
        if (!(action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK))) return;
        @Nullable EquipmentSlot hand = event.getHand();
        if (hand == null || !hand.equals(EquipmentSlot.HAND)) return;
        @Nullable ItemStack item = event.getItem();
        if (item == null || !item.getType().equals(Material.BOW)) return;
        playerPulls.put(event.getPlayer().getUniqueId(), true);
    }

    public void observe(PacketEvent event) {
        if (!event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.BOW)) return;
        if (!event.getPacketType().equals(PacketType.Play.Client.BLOCK_DIG)) return;
        try {
            EnumWrappers.PlayerDigType status = event.getPacket().getEnumModifier(EnumWrappers.PlayerDigType.class, 2).read(0);
            if (status.equals(EnumWrappers.PlayerDigType.RELEASE_USE_ITEM)) {
                playerPulls.put(event.getPlayer().getUniqueId(), false);
            }
        } catch (FieldAccessException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
