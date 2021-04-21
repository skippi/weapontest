package io.github.skippi.weapontest;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProjectileCleanupOracle implements Listener {
    private final Set<UUID> toDeleteIds = new HashSet<>();

    public void record(UUID projectileId) {
        toDeleteIds.add(projectileId);
    }

    @EventHandler
    public void observe(ProjectileHitEvent event) {
        if (!toDeleteIds.contains(event.getEntity().getUniqueId())) return;
        toDeleteIds.remove(event.getEntity().getUniqueId());
        event.getEntity().remove();
    }
}
