package io.github.skippi.weapontest;

import com.google.common.collect.Iterables;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WeaponTestPlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
                () -> Bukkit.getOnlinePlayers().forEach(this::updatePlayerActions), 0, 1);
        Bukkit.getOnlinePlayers().forEach(p -> p.getInventory().clear());
        Bukkit.getOnlinePlayers().forEach(this::giveLeapSkill);
    }

    private final Map<UUID, Vector> playerLastPos = new HashMap<>();
    private final Map<UUID, ArrayDeque<Command>> playerActions = new HashMap<>();

    private void updatePlayerActions(Player player) {
        @NotNull Location playerLoc = player.getLocation();
        @NotNull Vector lastPos = playerLastPos
                .computeIfAbsent(player.getUniqueId(), u -> playerLoc.toVector());
        @NotNull ArrayDeque<Command> actions = playerActions
                .computeIfAbsent(player.getUniqueId(), u -> new ArrayDeque<>(3));
        @NotNull Vector displacement = playerLoc.toVector().subtract(lastPos);
        Command command = Command.fromDisplacement(displacement, playerLoc.getDirection());
        if (actions.isEmpty() || actions.getLast() != command)
        {
            actions.add(command);
            if (actions.size() > 3) {
                actions.remove();
            }
        }
        playerLastPos.put(player.getUniqueId(), playerLoc.toVector());
    }

    private void giveLeapSkill(Player player) {
        ItemStack skill = new ItemStack(Material.BOOK);
        ItemMeta meta = skill.getItemMeta();
        meta.setCustomModelData(1);
        meta.displayName(Component.text("Leap"));
        meta.lore(Arrays.asList(Component.text(ChatColor.WHITE + "Launches the user forward."),
                Component.text(ChatColor.WHITE + "The launch angle is capped between 15-45\u00b0.")));
        skill.setItemMeta(meta);
        player.getInventory().addItem(skill);
    }

    private boolean isLeapSkill(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getData() == null) return false;
        return stack.getType().equals(Material.BOOK) && stack.getItemMeta().getCustomModelData() == 1;
    }

    @EventHandler
    public void rightClickTest(PlayerInteractEvent event) {
        @NotNull Player player = event.getPlayer();
        if (!Iterables.any(player.getInventory(), this::isLeapSkill)) return;
        ArrayDeque<Command> actions = playerActions.computeIfAbsent(player.getUniqueId(), u -> new ArrayDeque<>());
        List<Command> pattern = actions.stream().skip(actions.size() - 3).collect(Collectors.toList());
        if (pattern.size() != 3) return;
        if (pattern.equals(Arrays.asList(Command.BACK, Command.NEUTRAL, Command.FORWARD)))
        {
            @NotNull Vector powerVec = event.getPlayer().getLocation().getDirection().clone().normalize().multiply(3);
            event.getPlayer().setVelocity(powerVec);
        }
    }
}
