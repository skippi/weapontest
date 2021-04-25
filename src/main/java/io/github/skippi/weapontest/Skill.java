package io.github.skippi.weapontest;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public enum Skill {
    LEAP(1, "Leap", "Launches the user forward. The launch angle is capped between 15-45\u00b0.", ImmutableList.of(), ImmutableList.of()),
    HURRICANE(2, "Hurricane", "While drawing a bow, launch a continuous stream of arrows.", ImmutableList.of("+10 Bow Attack Speed", "Enables Bow Auto-Fire", "Disables Charge Shot"), ImmutableList.of()),
    TOME_OF_HEALTH(3, "Tome of Health", "", ImmutableList.of("+2 Max Health"), ImmutableList.of("Stacks up to 64 times.")),
    STATS(4, "Stats", "", ImmutableList.of(), ImmutableList.of()),
    TOME_OF_AGILITY(5, "Tome of Agility", "", ImmutableList.of("+2 Agility"), ImmutableList.of("Stacks up to 64 times")),
    ARROW_RAIN(6, "Arrow Rain", "Fires a tracer shot. Upon impact, launches a volley of arrows over 3 seconds within a 10m AoE.", ImmutableList.of(), ImmutableList.of()),
    TOME_OF_STRENGTH(7, "Tome of Strength", "", ImmutableList.of("+2 Strength"), ImmutableList.of("Stacks up to 64 times")),
    TOME_OF_INTELLIGENCE(8, "Tome of Intelligence", "", ImmutableList.of("+2 Intelligence"), ImmutableList.of("Stacks up to 64 times")),
    RECOIL_SHOT(9, "Recoil Shot", "Launches the user backwards. At the apex of the jump, fires 5 arrows in a 48\u00b0 cone.", ImmutableList.of(), ImmutableList.of()),
    GUIDING_SHOTS(10, "Guiding Shots", "", ImmutableList.of("+10% Arrow Homing", "-10% Bow Damage"), ImmutableList.of("Stacks up to 64 times")),
    GRAVITON_FIELD(11, "Graviton Field", "Pulls enemy units towards the user and consumes 3% current HP per half second until the user stops sneaking.", ImmutableList.of(), ImmutableList.of());

    private final int id;
    private final String name;
    private final String description;
    private final ImmutableList<String> attributes;
    private final ImmutableList<String> notes;

    Skill(int id, String name, String description, ImmutableList<String> attributes, ImmutableList<String> notes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.attributes = attributes;
        this.notes = notes;
    }

    public static int count(Inventory self, Skill skill) {
        return StreamSupport.stream(self.spliterator(), false)
                .filter(s -> isBook(s, skill))
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    public static boolean has(Inventory self, Skill skill) {
        return StreamSupport.stream(self.spliterator(), false).anyMatch(s -> isBook(s, skill));
    }

    public static boolean isBook(ItemStack self, Skill skill) {
        return self != null && self.getType().equals(Material.BOOK) && self.getItemMeta().getCustomModelData() == skill.id;
    }

    public static ItemStack makeBook(Skill skill, int count) {
        ItemStack stack = makeBook(skill);
        stack.setAmount(count);
        return stack;
    }

    public static ItemStack makeBook(Skill skill) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setCustomModelData(skill.id);
        meta.displayName(Component.text(ChatColor.GOLD + skill.name));
        List<Component> lore = new ArrayList<>();
        if (!skill.description.isEmpty()) {
            List<String> splitted = Arrays.asList(WordUtils.wrap(skill.description, 35).split("\\r?\\n"));
            lore.addAll(splitted.stream().map(s -> Component.text(ChatColor.WHITE + s)).collect(Collectors.toList()));
        }
        lore.addAll(skill.attributes.stream().map(s -> Component.text(ChatColor.BLUE + s)).collect(Collectors.toList()));
        lore.addAll(skill.notes.stream().map(s -> Component.text(ChatColor.GREEN + s)).collect(Collectors.toList()));
        meta.lore(lore);
        book.setItemMeta(meta);
        return book;
    }

    public String getName() {
        return name;
    }
}
