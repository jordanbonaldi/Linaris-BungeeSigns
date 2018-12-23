package net.neferett.linaris.signs.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lombok.Data;
import net.neferett.linaris.signs.BungeeSignsPlugin;

import org.bukkit.Location;

@Data
public abstract class BungeeType {
    protected List<ServerInfo> servers;
    protected Location location;

    public BungeeType(Location location) {
        this.location = location;
        servers = new ArrayList<>();
    }

    public abstract void update(BungeeSignsPlugin plugin, Random random);

    public List<String> getServerNames() {
        List<String> serverNames = new ArrayList<>();
        for (ServerInfo serverInfo : servers) {
            serverNames.add(serverInfo.getName());
        }
        return serverNames;
    }
}
