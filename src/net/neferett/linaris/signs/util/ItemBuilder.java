package net.neferett.linaris.signs.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {
    private String title;
    private final int amount;
    private final short damage;
    private final Material material;
    private final List<String> lores = new ArrayList<>();
    private final Map<Enchantment, Integer> enchantments = new HashMap<>();

    public ItemBuilder(final ItemStack item) {
        this(item.getType(), item.getAmount(), item.getDurability());
    }

    public ItemBuilder(final Material material) {
        this(material, 1, (short) 0);
    }

    public ItemBuilder(final Material material, final int amount) {
        this(material, amount, (short) 0);
    }

    public ItemBuilder(final Material material, final int amount, final short damage) {
        this.material = material;
        this.amount = amount;
        this.damage = damage;
    }

    public ItemBuilder(final Material material, final short durability) {
        this(material, 1, durability);
    }

    public ItemBuilder addEnchantment(final Enchantment enchantment, final int level) {
        enchantments.put(enchantment, level);
        return this;
    }

    public ItemBuilder addLores(final String... lores) {
        this.lores.addAll(Arrays.asList(lores));
        return this;
    }

    public ItemStack build() {
        if (material == null) { throw new NullPointerException("Material cannot be null!"); }
        final ItemStack item = new ItemStack(material, amount, damage);
        if (!enchantments.isEmpty()) {
            item.addUnsafeEnchantments(enchantments);
        }
        final ItemMeta meta = item.getItemMeta();
        if (title != null) {
            meta.setDisplayName(title);
        }
        if (!lores.isEmpty()) {
            meta.setLore(lores);
        }
        item.setItemMeta(meta);
        return item;
    }

    public ItemBuilder setTitle(final String title) {
        this.title = title;
        return this;
    }
}
