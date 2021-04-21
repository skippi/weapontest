package io.github.skippi.weapontest;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.Iterables;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class WeaponTestPlugin extends JavaPlugin implements Listener {
    public static ProtocolManager PM;
    public static BowDrawOracle BDO = new BowDrawOracle();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(BDO, this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
                () -> Bukkit.getOnlinePlayers().forEach(this::updatePlayerActions), 0, 1);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
                () -> Bukkit.getOnlinePlayers().forEach(this::stepHurricane), 0, 2);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
                () -> Bukkit.getOnlinePlayers().forEach(this::updateMaxHealth), 0, 1);
        Bukkit.getOnlinePlayers().forEach(p -> p.getInventory().clear());
        Bukkit.getOnlinePlayers().forEach(this::giveLeapSkill);
        Bukkit.getOnlinePlayers().forEach(this::giveHurricaneSkill);
        for (int i = 0; i < 64; ++i)
            Bukkit.getOnlinePlayers().forEach(this::giveTomeSkill);
        PM = ProtocolLibrary.getProtocolManager();
        PM.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_DIG, PacketType.Play.Client.BLOCK_PLACE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                BDO.observe(event);
            }
        });
    }

    private final Map<UUID, Vector> playerLastPos = new HashMap<>();
    private final Map<UUID, ArrayDeque<Command>> playerActions = new HashMap<>();

    private void stepHurricane(Player player) {
        if (!Iterables.any(player.getInventory(), this::isHurricaneSkill)) return;
        if (!BDO.isDrawing(player.getUniqueId())) return;
        player.launchProjectile(Arrow.class);
    }

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

    private void updateMaxHealth(Player player) {
        int tomeCount = Math.min(StreamSupport.stream(player.getInventory().spliterator(), false)
                .filter(this::isTomeSkill)
                .mapToInt(ItemStack::getAmount)
                .sum(), 64);
        double newHealth = 20.0 + 2 * tomeCount;
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() != newHealth) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newHealth);
        }
        if (player.getHealth() > newHealth) {
            player.setHealth(newHealth);
        }
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

    private void giveHurricaneSkill(Player player) {
        ItemStack skill = new ItemStack(Material.BOOK);
        ItemMeta meta = skill.getItemMeta();
        meta.setCustomModelData(2);
        meta.displayName(Component.text("Hurricane"));
        meta.lore(Arrays.asList(Component.text(ChatColor.WHITE + "While drawing a bow, launch a continuous stream of arrows."),
                Component.text(ChatColor.WHITE + "For every second held, the user fires ten arrows per second.")));
        skill.setItemMeta(meta);
        player.getInventory().addItem(skill);
    }

    private void giveTomeSkill(Player player) {
        ItemStack skill = new ItemStack(Material.BOOK);
        ItemMeta meta = skill.getItemMeta();
        meta.setCustomModelData(3);
        meta.displayName(Component.text(ChatColor.GOLD + "Tome of Health"));
        meta.lore(Arrays.asList(Component.text(ChatColor.BLUE + "+2 Max Health"),
                Component.text(ChatColor.GREEN + "Stacks up to 64 times.")));
        skill.setItemMeta(meta);
        player.getInventory().addItem(skill);
    }

    private boolean isLeapSkill(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getData() == null) return false;
        return stack.getType().equals(Material.BOOK) && stack.getItemMeta().getCustomModelData() == 1;
    }

    private boolean isHurricaneSkill(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getData() == null) return false;
        return stack.getType().equals(Material.BOOK) && stack.getItemMeta().getCustomModelData() == 2;
    }

    private boolean isTomeSkill(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getData() == null) return false;
        return stack.getType().equals(Material.BOOK) && stack.getItemMeta().getCustomModelData() == 3;
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
            @NotNull Vector fwd = player.getLocation().getDirection().clone().setY(0).normalize().multiply(2);
            @NotNull Vector right = fwd.getCrossProduct(new Vector(0, 1, 0)).normalize();
            @NotNull Vector powerVec = event.getPlayer().getLocation().getDirection().clone().normalize().multiply(2);
            if (powerVec.getY() < fwd.getY() || powerVec.angle(fwd) < Math.toRadians(15)) {
                powerVec = fwd.clone().rotateAroundAxis(right, Math.toRadians(15));
            } else if (Math.toRadians(45) < powerVec.angle(fwd)) {
                powerVec = fwd.clone().rotateAroundAxis(right, Math.toRadians(45));
            }
            event.getPlayer().setVelocity(powerVec);
        }
    }
}
