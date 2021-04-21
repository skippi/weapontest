package io.github.skippi.weapontest;

import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ArrowRainOracle implements Listener {
    private final Set<UUID> tracerIds = new HashSet<>();

    public void recordTracer(UUID projectileId) {
        tracerIds.add(projectileId);
    }

    @EventHandler
    private void observe(ProjectileHitEvent event) {
        @NotNull Projectile projectile = event.getEntity();
        if (!tracerIds.contains(projectile.getUniqueId())) return;
        tracerIds.remove(projectile.getUniqueId());
        @NotNull Location loc = projectile.getLocation();
        BukkitRunnable task = new BukkitRunnable() {
            int ticksElapsed = 0;
            @Override
            public void run() {
                if (ticksElapsed > 60) {
                    cancel();
                }
                ++ticksElapsed;
                if (ticksElapsed % 2 != 0) return;
                for (int i = 0; i < 12; ++i) {
                    @NotNull Location targetLoc = loc.clone().add(new Vector(RandomUtils.nextDouble() * 10 - 5, 0, RandomUtils.nextDouble() * 10 - 5));
                    @NotNull Location spawnLoc = targetLoc.clone().add(new Vector(-3, 15, -3));
                    @NotNull Vector travelDir = targetLoc.clone().subtract(spawnLoc).toVector().normalize();
                    @NotNull Arrow arrow = (Arrow) targetLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARROW);
                    arrow.setSilent(true);
                    arrow.setVelocity(travelDir.multiply(1.5));
                    WeaponTestPlugin.PCO.record(arrow.getUniqueId());
                }
            }
        };
        task.runTaskTimer(WeaponTestPlugin.INSTANCE, 0, 1);
        projectile.remove();
    }
}
