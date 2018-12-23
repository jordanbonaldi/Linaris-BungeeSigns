package net.neferett.linaris.signs;

import java.util.Collections;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import net.neferett.linaris.signs.type.BungeeType;

public class RefreshTask extends BukkitRunnable {
    private BungeeSignsPlugin plugin;
    private Queue<BungeeType> queue;
    private Random random;

    public RefreshTask(BungeeSignsPlugin plugin) {
        this.plugin = plugin;
        queue = new ConcurrentLinkedQueue<>();
        random = new Random();
    }

    private void update() {
        BungeeType bungeeType = null;
        int count = 0, max = 1;
        if (count < max && (bungeeType = queue.poll()) != null) {
            bungeeType.update(plugin, random);
            count++;
        }
        if (queue.size() == 0) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this, plugin.getRefreshTime());
        } else {
            new BukkitRunnable() {

                @Override
                public void run() {
                    RefreshTask.this.update();
                }
            }.runTaskLater(plugin, 1);
        }
    }

    @Override
    public void run() {
        plugin.getManager().pingAll();
        for (BungeeType bungeeType : Collections.unmodifiableList(plugin.getManager().getTypes())) {
            queue.add(bungeeType);
        }
        new BukkitRunnable() {

            @Override
            public void run() {
                RefreshTask.this.update();
            }
        }.runTask(plugin);
    }
}
