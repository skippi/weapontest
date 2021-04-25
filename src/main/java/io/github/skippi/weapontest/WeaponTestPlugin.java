package io.github.skippi.weapontest;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class WeaponTestPlugin extends JavaPlugin implements Listener {
    public static WeaponTestPlugin INSTANCE;
    public static ProtocolManager PM;
    public static ArrowRainOracle ARO = new ArrowRainOracle();
    public static BowDrawOracle BDO = new BowDrawOracle();
    public static ProjectileCleanupOracle PCO = new ProjectileCleanupOracle();

    public static @NotNull Merchant SKILL_DEBUG_SHOP = makeSkillDebugShop();

    @Override
    public void onEnable() {
        INSTANCE = this;
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(ARO, this);
        Bukkit.getPluginManager().registerEvents(BDO, this);
        Bukkit.getPluginManager().registerEvents(PCO, this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::step, 0, 1);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
                () -> Bukkit.getOnlinePlayers().forEach(this::stepHurricane), 0, 2);
        PM = ProtocolLibrary.getProtocolManager();
        PM.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_DIG, PacketType.Play.Client.BLOCK_PLACE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                BDO.observe(event);
            }
        });
        getCommand("skilldebugmenu").setExecutor(new SkillDebugMenuCommand());
    }

    private void step() {
        Bukkit.getOnlinePlayers().forEach(this::updatePlayerActions);
        Bukkit.getOnlinePlayers().forEach(this::updateMaxHealth);
        Bukkit.getOnlinePlayers().forEach(this::updateAgility);
        Bukkit.getOnlinePlayers().forEach(this::updateStatBook);
        Bukkit.getOnlinePlayers().forEach(this::updateStrength);
        Bukkit.getOnlinePlayers().forEach(this::updateIntelligence);
    }

    private static Merchant makeSkillDebugShop() {
        Merchant shop = Bukkit.createMerchant(Component.text("Debug Skills"));
        List<MerchantRecipe> options = Arrays.stream(Skill.values())
                .sorted(Comparator.comparing(Skill::getName))
                .map(Skill::makeBook)
                .map(b -> {
                    MerchantRecipe recipe = new MerchantRecipe(b, Integer.MAX_VALUE);
                    recipe.addIngredient(new ItemStack(Material.GOLD_NUGGET));
                    recipe.setExperienceReward(false);
                    return recipe;
                })
                .collect(Collectors.toList());
        shop.setRecipes(options);
        return shop;
    }

    private final Map<UUID, Vector> playerLastPos = new HashMap<>();
    private final Map<UUID, ArrayDeque<Command>> playerActions = new HashMap<>();
    private final Map<UUID, Double> playerAgility = new HashMap<>();
    private final Map<UUID, Double> playerStrengths = new HashMap<>();
    private final Map<UUID, Double> playerIntelligences = new HashMap<>();

    private void stepHurricane(Player player) {
        if (!Skill.has(player.getInventory(), Skill.HURRICANE)) return;
        if (!BDO.isDrawing(player.getUniqueId())) return;
        player.launchProjectile(Arrow.class);
    }

    @EventHandler
    private void damage(EntityDamageByEntityEvent event) {
        @NotNull Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return;
        LivingEntity livingEntity = (LivingEntity) entity;
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            livingEntity.setNoDamageTicks(0);
            livingEntity.setVelocity(new Vector());
        }, 1);
    }

    private void updatePlayerActions(Player player) {
        @NotNull Location playerLoc = player.getLocation();
        @NotNull Vector lastPos = playerLastPos
                .computeIfAbsent(player.getUniqueId(), u -> playerLoc.toVector());
        @NotNull ArrayDeque<Command> actions = playerActions
                .computeIfAbsent(player.getUniqueId(), u -> new ArrayDeque<>(3));
        @NotNull Vector displacement = playerLoc.toVector().subtract(lastPos);
        Command command = Command.fromDisplacement(displacement, playerLoc.getDirection());
        if (!command.equals(Command.NEUTRAL) && (actions.isEmpty() || actions.getLast() != command))
        {
            actions.add(command);
            if (actions.size() > 3) {
                actions.remove();
            }
        }
        playerLastPos.put(player.getUniqueId(), playerLoc.toVector());
    }

    private void showSkillDebugMenu(Player player) {
        Bukkit.createInventory(player, InventoryType.CHEST);
    }

    private void updateAgility(Player player) {
        int tomeCount = Math.min(64, Skill.count(player.getInventory(), Skill.TOME_OF_AGILITY));
        double newAgility = 2 * tomeCount;
        playerAgility.put(player.getUniqueId(), newAgility);
    }

    private void updateStrength(Player player) {
        int tomeCount = Math.min(64, Skill.count(player.getInventory(), Skill.TOME_OF_STRENGTH));
        double newStrength = 2 * tomeCount;
        playerStrengths.put(player.getUniqueId(), newStrength);
    }

    private void updateIntelligence(Player player) {
        int tomeCount = Math.min(64, Skill.count(player.getInventory(), Skill.TOME_OF_INTELLIGENCE));
        double newIntelligence = 2 * tomeCount;
        playerIntelligences.put(player.getUniqueId(), newIntelligence);
    }

    private void updateMaxHealth(Player player) {
        int tomeCount = Math.min(64, Skill.count(player.getInventory(), Skill.TOME_OF_HEALTH));
        double newHealth = 20.0 + 2 * tomeCount;
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() != newHealth) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newHealth);
        }
        if (player.getHealth() > newHealth) {
            player.setHealth(newHealth);
        }
    }

    private void updateStatBook(@NotNull Player player) {
        Optional<ItemStack> maybeBook = StreamSupport.stream(player.getInventory().spliterator(), false)
                .filter(Objects::nonNull)
                .filter(s -> s.getType().equals(Material.BOOK) && s.getItemMeta().getCustomModelData() == 4)
                .findFirst();
        if (!maybeBook.isPresent()) return;
        ItemStack book = maybeBook.get();
        ItemMeta meta = book.getItemMeta();
        meta.lore(Arrays.asList(
                Component.text(ChatColor.GREEN + String.format("Health: %.3f/%.3f", player.getHealth(), player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())),
                Component.text(ChatColor.RED + String.format("Attack: %.3f", player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue())),
                Component.text(ChatColor.WHITE + String.format("Movespeed: %.3f", player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue())),
                Component.text(ChatColor.DARK_RED + String.format("Strength: %.2f", playerStrengths.getOrDefault(player.getUniqueId(), 0.0))),
                Component.text(ChatColor.DARK_GREEN + String.format("Agility: %.2f", playerAgility.getOrDefault(player.getUniqueId(), 0.0))),
                Component.text(ChatColor.AQUA + String.format("Intelligence: %.2f", playerIntelligences.getOrDefault(player.getUniqueId(), 0.0)))
        ));
        book.setItemMeta(meta);
    }

    @EventHandler
    public void rightClickTest(PlayerInteractEvent event) {
        @NotNull Player player = event.getPlayer();
        if (!Skill.has(player.getInventory(), Skill.LEAP)) return;
        ArrayDeque<Command> actions = playerActions.computeIfAbsent(player.getUniqueId(), u -> new ArrayDeque<>());
        List<Command> pattern = actions.stream().skip(actions.size() - 2).collect(Collectors.toList());
        if (pattern.size() != 2) return;
        if (pattern.equals(Arrays.asList(Command.BACK, Command.FORWARD)))
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

    @EventHandler
    public void arrowRain(PlayerInteractEvent event) {
        if (!(event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK))) return;
        @NotNull Player player = event.getPlayer();
        if (!player.getInventory().getItemInMainHand().getType().equals(Material.BOW)) return;
        if (!Skill.has(player.getInventory(), Skill.ARROW_RAIN)) return;
        ArrayDeque<Command> actions = playerActions.computeIfAbsent(player.getUniqueId(), u -> new ArrayDeque<>());
        List<Command> pattern = actions.stream().skip(actions.size() - 3).collect(Collectors.toList());
        if (pattern.equals(Arrays.asList(Command.BACK, Command.BACK_RIGHT, Command.RIGHT)))
        {
            @NotNull Snowball tracer = event.getPlayer().launchProjectile(Snowball.class);
            ARO.recordTracer(tracer.getUniqueId());
        }
    }

    @EventHandler
    private void guidingShots(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Arrow)) return;
        Arrow arrow = (Arrow) entity;
        if (!(arrow.getShooter() instanceof Player)) return;
        Player player = (Player) arrow.getShooter();
        if (!Skill.has(player.getInventory(), Skill.GUIDING_SHOTS)) return;
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isInBlock()) {
                    cancel();
                    return;
                }
                @NotNull Optional<LivingEntity> maybeEntity = arrow.getWorld()
                        .getNearbyLivingEntities(arrow.getLocation(), 10, 10, 10)
                        .stream()
                        .filter(e -> !e.isDead() && !e.equals(player) && player.hasLineOfSight(e))
                        .min(Comparator.comparing(e -> e.getLocation().distance(arrow.getLocation())));
                if (!maybeEntity.isPresent()) return;
                Entities.home(arrow, maybeEntity.get().getLocation().toVector(), 0.10);
            }
        };
        task.runTaskTimer(this, 0, 1);
    }

    @EventHandler
    public void recoilShot(PlayerInteractEvent event) {
        if (!(event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK))) return;
        @NotNull Player player = event.getPlayer();
        if (!player.getInventory().getItemInMainHand().getType().equals(Material.BOW)) return;
        if (!Skill.has(player.getInventory(), Skill.RECOIL_SHOT)) return;
        ArrayDeque<Command> actions = playerActions.computeIfAbsent(player.getUniqueId(), u -> new ArrayDeque<>());
        List<Command> pattern = actions.stream().skip(actions.size() - 2).collect(Collectors.toList());
        if (pattern.equals(Arrays.asList(Command.FORWARD, Command.BACK)))
        {
            @NotNull final Vector up = new Vector(0, 1, 0);
            @NotNull Vector bwd = player.getLocation().getDirection().clone().setY(0).normalize().multiply(-1);
            @NotNull Vector left = bwd.getCrossProduct(new Vector(0, 1, 0)).normalize();
            @NotNull Vector powerVec = bwd.clone().rotateAroundAxis(left, Math.toRadians(30)).multiply(1.25);
            event.getPlayer().setVelocity(powerVec);
            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.getVelocity().getY() > 0) return;
                    @NotNull Vector mainVec = player.getLocation().getDirection().clone().multiply(2.5);
                    player.launchProjectile(Arrow.class, mainVec.clone().rotateAroundAxis(up, Math.toRadians(-24)));
                    player.launchProjectile(Arrow.class, mainVec.clone().rotateAroundAxis(up, Math.toRadians(-12)));
                    player.launchProjectile(Arrow.class, mainVec.clone().multiply(2.5));
                    player.launchProjectile(Arrow.class, mainVec.clone().rotateAroundAxis(up, Math.toRadians(12)));
                    player.launchProjectile(Arrow.class, mainVec.clone().rotateAroundAxis(up, Math.toRadians(24)));
                    cancel();
                }
            };
            task.runTaskTimer(this, 0, 1);
        }
    }

    @EventHandler
    public void lightning(PlayerInteractEvent event) {
        if (!(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) return;
        @NotNull Player player = event.getPlayer();
        if (!player.getInventory().getItemInMainHand().getType().equals(Material.BOOK)) return;
        if (!Skill.has(player.getInventory(), Skill.LIGHTNING)) return;
        ArrayDeque<Command> actions = playerActions.computeIfAbsent(player.getUniqueId(), u -> new ArrayDeque<>());
        List<Command> pattern = actions.stream().skip(actions.size() - 3).collect(Collectors.toList());
        if (pattern.equals(Arrays.asList(Command.BACK, Command.BACK_RIGHT, Command.RIGHT))) {
            BlockIterator iter = new BlockIterator(player.getEyeLocation(), 0, 50);
            while (iter.hasNext()) {
                @NotNull Block block = iter.next();
                if (block.isEmpty()) continue;
                block.getWorld().strikeLightning(block.getLocation());
                break;
            }
        }
    }

    @EventHandler
    public void poisonPool(PlayerInteractEvent event) {
        if (!(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) return;
        @NotNull Player player = event.getPlayer();
        if (!player.getInventory().getItemInMainHand().getType().equals(Material.BOOK)) return;
        if (!Skill.has(player.getInventory(), Skill.LIGHTNING)) return;
        ArrayDeque<Command> actions = playerActions.computeIfAbsent(player.getUniqueId(), u -> new ArrayDeque<>());
        List<Command> pattern = actions.stream().skip(actions.size() - 3).collect(Collectors.toList());
        if (pattern.equals(Arrays.asList(Command.BACK, Command.BACK_LEFT, Command.LEFT))) {
            BlockIterator iter = new BlockIterator(player.getEyeLocation(), 0, 50);
            while (iter.hasNext()) {
                @NotNull Block block = iter.next();
                if (block.isEmpty()) continue;
                BukkitRunnable task = new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks > 100) {
                            cancel();
                            return;
                        }
                        if (ticks % 2 == 0) {
                            for (int x = -4; x < 4; ++x) {
                                for (int z = -4; z < 4; ++z) {
                                    block.getLocation().getWorld().spawnParticle(Particle.PORTAL, block.getLocation().clone().add(x, 0, z), 5);
                                }
                            }
                        }
                        if (ticks % 10 == 0) {
                            @NotNull Collection<LivingEntity> entities = block.getWorld().getNearbyLivingEntities(block.getLocation(), 4, 2)
                                    .stream()
                                    .filter(e -> !e.equals(player) && !e.isDead())
                                    .collect(Collectors.toList());
                            for (LivingEntity entity : entities) {
                                entity.damage(2, player);
                            }
                        }
                        ++ticks;
                    }
                };
                task.runTaskTimer(this, 0, 1);
                break;
            }
        }
    }

    @EventHandler
    private void gravitonField(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!Skill.has(player.getInventory(), Skill.GRAVITON_FIELD)) return;
        ArrayDeque<Command> actions = playerActions.computeIfAbsent(player.getUniqueId(), u -> new ArrayDeque<>());
        List<Command> pattern = actions.stream().skip(actions.size() - 3).collect(Collectors.toList());
        if (pattern.equals(Arrays.asList(Command.LEFT, Command.BACK_LEFT, Command.BACK))) {
            BukkitRunnable task = new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (!player.isSneaking()) {
                        cancel();
                        return;
                    }
                    if (ticks % 10 == 0) {
                        player.setHealth(0.97 * player.getHealth());
                    }
                    if (ticks % 2 == 0) {
                        @NotNull Collection<LivingEntity> entities = player.getWorld().getNearbyLivingEntities(player.getLocation(), 8, 8, 8)
                                .stream()
                                .filter(e -> !e.equals(player) && !e.isDead() && player.hasLineOfSight(e) && e.getLocation().distance(player.getLocation()) > 1.5)
                                .collect(Collectors.toList());
                        for (LivingEntity entity : entities) {
                            Vector powerVec = player.getLocation().clone().subtract(entity.getLocation()).toVector().normalize().multiply(0.5);
                            entity.setVelocity(entity.getVelocity().clone().add(powerVec));
                        }
                    }
                    ++ticks;
                }
            };
            task.runTaskTimer(this, 0, 1);
        }
    }
}
