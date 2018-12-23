package net.neferett.linaris.signs;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import lombok.Data;
import net.neferett.linaris.signs.type.BungeeNPC;
import net.neferett.linaris.signs.type.BungeeSign;
import net.neferett.linaris.signs.type.BungeeType;
import net.neferett.linaris.signs.type.ServerInfo;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

@Data
public class BungeeSigns {
    private BungeeSignsPlugin plugin;
    private Logger logger;
    private List<BungeeType> types;
    private List<ServerInfo> servers;
    private boolean requested = false;

    public BungeeNPC getNPCById(String id) {
        for (BungeeType type : types) {
            if (type instanceof BungeeNPC && ((BungeeNPC) type).getId().equals(id)) { return (BungeeNPC) type; }
        }
        return null;
    }

    public BungeeNPC getNPCByLocation(Location location) {
        for (BungeeType type : types) {
            if (type instanceof BungeeNPC && type.getLocation().distanceSquared(location) <= 0.5) { return (BungeeNPC) type; }
        }
        return null;
    }

    public BungeeSign getSignByLocation(Location location) {
        for (BungeeType sign : types) {
            if (sign instanceof BungeeSign && sign.getLocation().equals(location)) { return (BungeeSign) sign; }
        }
        return null;
    }

    public ServerInfo getServerInfo(String name) {
        for (ServerInfo serverInfo : servers) {
            if (serverInfo.getName().equals(name)) { return serverInfo; }
        }
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setName(name);
        if (Bukkit.getOnlinePlayers().length >= 1) {
            Player player = Bukkit.getOnlinePlayers()[0];
            this.request(player, serverInfo);
        }
        servers.add(serverInfo);
        return serverInfo;
    }

    public void pingAll() {
        synchronized (servers) {
            for (ServerInfo serverInfo : Collections.unmodifiableList(servers)) {
                serverInfo.ping();
            }
        }
    }

    public void request(Player player, BungeeType bungeeType) {
        for (ServerInfo serverInfo : bungeeType.getServers()) {
            this.request(player, serverInfo);
        }
    }

    public void request(Player player, ServerInfo serverInfo) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ServerIP");
        out.writeUTF(serverInfo.getName());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void requestAll() {
        Player player = Bukkit.getOnlinePlayers()[0];
        for (ServerInfo serverInfo : servers) {
            this.request(player, serverInfo);
        }
        requested = true;
    }
}
