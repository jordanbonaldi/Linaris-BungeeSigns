package net.neferett.linaris.signs.type;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;
import me.dablakbandit.customentitiesapi.entities.CustomEntities;
import me.dablakbandit.customentitiesapi.entities.CustomEntityVillager;
import me.dablakbandit.customentitiesapi.entities.EntityName;
import net.neferett.linaris.signs.BungeeSignsPlugin;
import net.neferett.linaris.signs.util.ItemBuilder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

@Getter
public class BungeeNPC extends BungeeType {
    private CustomEntityVillager npc;
    private Inventory inventory;
    private String id;
    private List<String> lines;
    private Hologram hologram;
    @Setter
    private Material material;
    private String title;
    private int playersCount = 0;

    public BungeeNPC(String id, List<String> lines, Material material, String title, Location location) {
        super(location);
        this.id = id;
        this.lines = lines;
        this.material = material;
        this.title = title;
        inventory = Bukkit.createInventory(new NPCInventory(this), 54, title);
        this.setNPCAttributes();
        this.refresh();
    }

    protected void refresh() {
        Location location = this.location.clone().add(0, 2.1 + 0.25 * lines.size(), 0);
        if (hologram == null) {
            hologram = HologramsAPI.createHologram(BungeeSignsPlugin.getInstance(), location);
        } else {
            hologram.teleport(location);
            hologram.clearLines();
        }
        for (String line : lines) {
            hologram.appendTextLine(line.replace("%count%", playersCount + ""));
        }
    }

    public void removeAll() {
        npc.getBukkitEntity().remove();
        if (hologram != null) {
            hologram.delete();
        }
    }

    protected void setNPCAttributes() {
        npc = CustomEntities.getNewCustomEntityVillager(location);
        npc.setProfession(1);
        npc.setUnableToMove();
        npc.setUnpushable();
        npc.setUndamageable();
        npc.setUntradeable();
        npc.newGoalSelectorPathfinderGoalInteract(EntityName.ENTITYHUMAN, 3.0F, 1.0F);
    }

    public void setTitle(String title) {
        this.title = title;
        inventory = Bukkit.createInventory(new NPCInventory(this), 54, title);
    }

    public void addLine(int line, String text) {
        if (line < 0) {
            lines.add(text);
        } else {
            lines.set(line, text);
        }
        this.refresh();
    }

    public void removeLine(int line) {
        lines.remove(line);
        this.refresh();
    }

    @Override
    public void update(BungeeSignsPlugin plugin, Random random) {
        inventory.clear();
        if (!npc.getBukkitEntity().isDead()) {
            npc.setProfession(1);
        } else {
            npc.getBukkitEntity().remove();
            this.setNPCAttributes();
        }
        int size = (int) Math.ceil((double) servers.size() / (double) 9) * 9;
        if (inventory.getSize() != size) {
            inventory = Bukkit.createInventory(new NPCInventory(this), size, title);
        }
        playersCount = 0;
        Map<ItemStack, Integer> map = new HashMap<>();
        for (ServerInfo serverInfo : servers) {
            ItemStack item = new ItemBuilder(material, serverInfo.getOnlinePlayers()).setTitle(ChatColor.YELLOW + serverInfo.getName()).addLores("", ChatColor.AQUA + "Joueur(s) " + ChatColor.WHITE + ": " + ChatColor.GOLD + serverInfo.getOnlinePlayers() + ChatColor.WHITE + "/" + ChatColor.GOLD + serverInfo.getMaxPlayers(), ChatColor.AQUA + "Status " + ChatColor.WHITE + ": " + serverInfo.getDescription()).build();
            if (serverInfo.getDescription().startsWith(ChatColor.RED.toString()) || serverInfo.getDescription().startsWith(ChatColor.YELLOW.toString()) || serverInfo.getDescription().startsWith(ChatColor.BLUE.toString())) {
                item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            }
            map.put(item, serverInfo.getOnlinePlayers());
            playersCount += serverInfo.getOnlinePlayers();
        }
        this.refresh();
        List<Map.Entry<ItemStack, Integer>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<ItemStack, Integer>>() {
            @Override
            public int compare(Map.Entry<ItemStack, Integer> o1, Map.Entry<ItemStack, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        for (Entry<ItemStack, Integer> entry : list) {
            inventory.addItem(entry.getKey());
        }
    }
}
