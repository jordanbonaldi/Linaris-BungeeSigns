package net.neferett.linaris.signs.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lombok.Getter;
import net.neferett.linaris.signs.BungeeSignsPlugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class BungeeSign extends BungeeType {
    @Getter
    protected ServerInfo serverInfo;

    public BungeeSign(Location location) {
        super(location);
    }

    public List<ServerInfo> getAvailableServers() {
        List<ServerInfo> servers = new ArrayList<>();
        for (ServerInfo serverInfo : this.servers) {
            if (serverInfo != this.serverInfo && serverInfo.canJoin()) {
                servers.add(serverInfo);
            }
        }
        return servers;
    }

    @Override
    public void update(BungeeSignsPlugin plugin, Random random) {
        List<ServerInfo> available = this.getAvailableServers();
        Block block = location.getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            if (available.size() == 0 && (serverInfo == null || !serverInfo.canJoin())) {
                serverInfo = null;
                sign.setLine(0, ChatColor.RED + "Tous les");
                sign.setLine(1, ChatColor.RED + "serveurs");
                sign.setLine(2, ChatColor.RED + "sont en jeu");
                sign.setLine(3, ChatColor.DARK_RED + "" + ChatColor.STRIKETHROUGH + "------------");
                sign.update();
            } else {
                if (serverInfo == null || !serverInfo.canJoin()) {
                    serverInfo = available.remove(random.nextInt(available.size()));
                }
                plugin.updateSign(sign, serverInfo);
            }
        }
    }
}
