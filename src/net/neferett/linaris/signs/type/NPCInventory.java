package net.neferett.linaris.signs.type;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@Data
@AllArgsConstructor
public class NPCInventory implements InventoryHolder {
    private BungeeNPC npc;

    @Override
    public Inventory getInventory() {
        return null;
    }

}
