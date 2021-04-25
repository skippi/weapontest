package io.github.skippi.weapontest;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public enum Command {
    FORWARD,
    RIGHT,
    BACK,
    LEFT,
    FORWARD_RIGHT,
    BACK_RIGHT,
    BACK_LEFT,
    FORWARD_LEFT,
    NEUTRAL;

    public static Command fromDisplacement(@NotNull Vector displacement, @NotNull Vector look) {
        if (displacement.length() < 0.001) return NEUTRAL;
        @NotNull Vector dnorm = displacement.clone().normalize();
        @NotNull Vector fwd = look.clone().setY(0).normalize();
        @NotNull Vector bwd = fwd.clone().multiply(-1);
        @NotNull Vector right = fwd.getCrossProduct(new Vector(0, 1, 0));
        @NotNull Vector left = right.clone().multiply(-1);
        List<Vector> controlDirs = Arrays.asList(fwd, right, bwd, left,
                fwd.clone().add(right).normalize(),
                bwd.clone().add(right).normalize(),
                bwd.clone().add(left).normalize(),
                fwd.clone().add(left).normalize());
        int minIndex = 0;
        for (int i = 0; i < controlDirs.size(); ++i) {
            if (controlDirs.get(i).distance(dnorm) < controlDirs.get(minIndex).distance(dnorm))
            {
                minIndex = i;
            }
        }
        return values()[minIndex];
    }
}
