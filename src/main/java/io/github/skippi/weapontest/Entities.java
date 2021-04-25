package io.github.skippi.weapontest;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class Entities {
    public static void home(Entity self, Vector pos, double weight) {
        final double speed = self.getVelocity().length();
        Vector toPosDir = pos.clone().subtract(self.getLocation().toVector()).normalize();
        Vector selfDir = self.getVelocity().clone().normalize();
        self.setVelocity(selfDir.clone().add(toPosDir.clone().multiply(weight)).normalize().multiply(speed));
    }
}
