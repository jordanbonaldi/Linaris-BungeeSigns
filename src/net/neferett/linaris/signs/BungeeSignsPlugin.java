package net.neferett.linaris.signs;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import net.neferett.linaris.signs.type.BungeeNPC;
import net.neferett.linaris.signs.type.BungeeSign;
import net.neferett.linaris.signs.type.BungeeType;
import net.neferett.linaris.signs.type.NPCInventory;
import net.neferett.linaris.signs.type.ServerInfo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.Packets;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class BungeeSignsPlugin extends JavaPlugin implements PluginMessageListener, Listener {
    @Getter
    private static BungeeSignsPlugin instance;
    @Getter
    private BungeeSigns manager;
    private Map<Player, Long> signCooldowns;
    private long cooldownSign;
    @Getter
    private long refreshTime;

    @Override
    public void onEnable() {
        BungeeSignsPlugin.instance = this;
        manager = new BungeeSigns();
        manager.setPlugin(this);
        manager.setTypes(Collections.synchronizedList(new ArrayList<BungeeType>()));
        manager.setServers(Collections.synchronizedList(new ArrayList<ServerInfo>()));
        manager.setLogger(this.getLogger());
        signCooldowns = new HashMap<>();
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.saveDefaultConfig();
        cooldownSign = this.getConfig().getInt("cooldown-signs") * 1000;
        refreshTime = this.getConfig().getInt("refresh-time") * 20;
        ConfigurationSection section = this.getConfig().getConfigurationSection("bungeesigns");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection bungee = section.getConfigurationSection(key);
                String[] splitted = bungee.getString("location").split("_");
                World world = Bukkit.getWorld(splitted[0]);
                BungeeType type = null;
                if (world != null) {
                    Location location = new Location(world, Double.parseDouble(splitted[1]), Double.parseDouble(splitted[2]), Double.parseDouble(splitted[3]));
                    type = !bungee.contains("name") ? new BungeeSign(location) : new BungeeNPC(bungee.getString("id"), bungee.getStringList("lines"), Material.getMaterial(bungee.getString("material")), bungee.getString("title"), location.add(0.5, 0, 0.5));
                    if (type instanceof BungeeNPC && bungee.isSet("name") && bungee.isSet("subname")) {
                        ((BungeeNPC) type).addLine(-1, bungee.getString("name"));
                        ((BungeeNPC) type).addLine(-1, bungee.getString("subname"));
                    }
                    for (String serverName : bungee.getStringList("servers")) {
                        type.getServers().add(manager.getServerInfo(serverName));
                    }
                    manager.getTypes().add(type);
                }
            }
        }
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, ConnectionSide.SERVER_SIDE, Packets.Server.NAMED_SOUND_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                String soundName = event.getPacket().getStrings().read(0);
                if (soundName.contains("mob.villager")) {
                    event.setCancelled(true);
                }
            }
        });
        manager.requestAll();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, new RefreshTask(this), 20);
    }

    @Override
    public void onDisable() {
        this.getConfig().set("bungeesigns", null);
        ConfigurationSection bungeeSigns = this.getConfig().createSection("bungeesigns");
        int id = 1;
        for (BungeeType bungeeType : manager.getTypes()) {
            Location location = bungeeType.getLocation();
            ConfigurationSection signSection = bungeeSigns.createSection(id + "");
            signSection.set("servers", bungeeType.getServerNames());
            if (bungeeType instanceof BungeeNPC) {
                BungeeNPC npc = (BungeeNPC) bungeeType;
                signSection.set("id", npc.getId());
                signSection.set("lines", npc.getLines());
                signSection.set("title", npc.getTitle());
                signSection.set("material", npc.getMaterial().name());
            }
            signSection.set("location", location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ());
            if (bungeeType instanceof BungeeNPC) {
                ((BungeeNPC) bungeeType).removeAll();
            }
            id++;
        }
        this.saveConfig();
        signCooldowns.clear();
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
    }

    public void updateSign(Sign sign, ServerInfo serverInfo) {
        sign.setLine(0, "[" + ChatColor.DARK_AQUA + "CLIQUE" + ChatColor.BLACK + "]");
        sign.setLine(1, serverInfo.getName().contains("fk") ? Integer.parseInt(serverInfo.getName().replace("fk", "")) <= 20 ? "Map Cactus" : "Map Dryness" : serverInfo.getName().contains("towers") ? Integer.parseInt(serverInfo.getName().replace("towers", "")) <= 20 ? "Map Originale" : "Map Pacific" : serverInfo.getName().contains("sheep") ? "Map Buildings" : serverInfo.getName());
        sign.setLine(2, ChatColor.DARK_PURPLE + "" + serverInfo.getOnlinePlayers() + ChatColor.GRAY + "/" + ChatColor.DARK_PURPLE + serverInfo.getMaxPlayers());
        sign.setLine(3, ChatColor.AQUA + serverInfo.getDescription());
        sign.update();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        if (!manager.isRequested()) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    manager.requestAll();
                }
            }.runTaskLater(this, 5);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt) {
        signCooldowns.remove(evt.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent evt) {
        signCooldowns.remove(evt.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent evt) {
        if (evt.getPlayer().isOp() && evt.getBlock().getState() instanceof Sign) {
            Location location = evt.getBlock().getLocation();
            for (BungeeType bungeeType : manager.getTypes()) {
                if (!(bungeeType instanceof BungeeSign) || !bungeeType.getLocation().equals(location)) {
                    continue;
                }
                evt.getPlayer().sendMessage(ChatColor.RED + "BungeeSign cassé avec succès.");
                manager.getTypes().remove(bungeeType);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent evt) {
        Entity entity = evt.getRightClicked();
        if (entity instanceof Villager) {
            for (BungeeType bungeeType : manager.getTypes()) {
                if (bungeeType instanceof BungeeNPC && ((BungeeNPC) bungeeType).getNpc().getBukkitEntity() == entity) {
                    evt.setCancelled(true);
                    evt.getPlayer().openInventory(((BungeeNPC) bungeeType).getInventory());
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent evt) {
        if (evt.getInventory().getHolder() instanceof NPCInventory) {
            evt.setCancelled(true);
            if (evt.getRawSlot() == evt.getSlot()) {
                ItemStack currentItem = evt.getCurrentItem();
                if (currentItem != null && currentItem.hasItemMeta() && currentItem.getItemMeta().hasDisplayName()) {
                    this.sendPlayerTo((Player) evt.getWhoClicked(), ChatColor.stripColor(currentItem.getItemMeta().getDisplayName()));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent evt) {
        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK && evt.getClickedBlock() != null && evt.getClickedBlock().getState() instanceof Sign) {
            if (signCooldowns.containsKey(evt.getPlayer()) && System.currentTimeMillis() - signCooldowns.get(evt.getPlayer()) < cooldownSign) { return; }
            signCooldowns.put(evt.getPlayer(), System.currentTimeMillis());
            Location location = evt.getClickedBlock().getLocation();
            for (BungeeType bungeeType : manager.getTypes()) {
                if (bungeeType instanceof BungeeSign && bungeeType.getLocation().equals(location)) {
                    ServerInfo serverInfo = ((BungeeSign) bungeeType).getServerInfo();
                    if (serverInfo == null) {
                        evt.getPlayer().sendMessage(ChatColor.RED + "Désolé, tous les serveurs sont en jeu !");
                        continue;
                    }
                    this.sendPlayerTo(evt.getPlayer(), serverInfo.getName());
                    break;
                }
            }
        }
    }

    private void sendPlayerTo(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equalsIgnoreCase("BungeeCord")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if (subchannel.equals("ServerIP")) {
                String serverName = in.readUTF();
                String ip = in.readUTF();
                short port = in.readShort();
                manager.getServerInfo(serverName).setAddress(new InetSocketAddress(ip, port));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Vous devez être un joueur.");
            return true;
        }
        Player player = (Player) sender;
        if (command.getName().equals("bungeesigns")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Vous devez indiquer des serveurs.");
                sender.sendMessage(ChatColor.GRAY + "Exemple : " + ChatColor.DARK_AQUA + "/bungeesigns serveur1 serveur2...");
                return true;
            }
            Block block = player.getTargetBlock(null, 5);
            if (block == null || !block.getType().name().contains("SIGN")) {
                player.sendMessage(ChatColor.RED + "Vous devez viser une pancarte.");
                return true;
            } else if (manager.getSignByLocation(block.getLocation()) != null) {
                player.sendMessage(ChatColor.RED + "Un BungeeSign est déjà présent ici.");
                return true;
            }
            BungeeSign bungeeSign = new BungeeSign(block.getLocation());
            for (String serverName : args) {
                bungeeSign.getServers().add(manager.getServerInfo(serverName));
            }
            manager.getTypes().add(bungeeSign);
            manager.request(player, bungeeSign);
            player.sendMessage(ChatColor.GREEN + "Le BungeeSign a été créé avec succès.");
        } else if (command.getName().equals("bungeenpc")) {
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("create")) {
                    String id = args[1];
                    if (manager.getNPCById(id) != null) {
                        player.sendMessage(ChatColor.RED + "L'id est déjà utilisé par un autre NPC.");
                        return true;
                    }
                    BungeeNPC bungeeNPC = new BungeeNPC(id, new ArrayList<String>(), Material.TNT, "Maître du jeu", player.getLocation());
                    manager.getTypes().add(bungeeNPC);
                    player.sendMessage(ChatColor.GREEN + "NPC créé ! " + ChatColor.GRAY + "Vous pouvez définir son nom avec la commande " + ChatColor.AQUA + "/bungeenpc line " + id + " <name>");
                    player.sendMessage(ChatColor.GRAY + "Il est également possible de le supprimer avec " + ChatColor.RED + "/bungeenpc delete " + id);
                    return true;
                } else if (args[0].equalsIgnoreCase("delete")) {
                    String id = args[1];
                    BungeeNPC bungeeNPC = this.findOrFail(id, player);
                    if (bungeeNPC != null) {
                        bungeeNPC.removeAll();
                        manager.getTypes().remove(bungeeNPC);
                        player.sendMessage(ChatColor.GREEN + "Le NPC a bien été supprimé !");
                    }
                    return true;
                }
            } else if (args.length >= 3) {
                if (args[0].equalsIgnoreCase("line")) {
                    String id = args[1];
                    BungeeNPC bungeeNPC = this.findOrFail(id, player);
                    if (bungeeNPC != null) {
                        int line = !args[args.length - 1].matches("[0-9]+") ? 0 : Integer.parseInt(args[args.length - 1]);
                        if (line > bungeeNPC.getLines().size() || line < 1) {
                            line = -1;
                        }
                        bungeeNPC.addLine(line - 1, ChatColor.translateAlternateColorCodes('&', this.getStringWithArgs(line == -1 ? args : Arrays.copyOf(args, args.length - 1))));
                        player.sendMessage(ChatColor.GREEN + "La ligne a bien été définie ! " + ChatColor.AQUA + "Astuce :" + ChatColor.GRAY + " Vous pouvez utiliser les codeurs couleurs grâce au symbole " + ChatColor.YELLOW + "&" + ChatColor.GRAY + ".");
                        player.sendMessage(ChatColor.GRAY + "Pour supprimer une ligne tapez la commande : " + ChatColor.AQUA + "/bungeenpc dline " + id + " <line>");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("dline")) {
                    String id = args[1];
                    BungeeNPC bungeeNPC = this.findOrFail(id, player);
                    if (bungeeNPC != null) {
                        int line = !args[2].matches("[0-9]+") ? -1 : Integer.parseInt(args[2]);
                        if (line > bungeeNPC.getLines().size() || line < 1) {
                            player.sendMessage(ChatColor.RED + "La ligne " + line + " n'existe pas.");
                        }
                        bungeeNPC.removeLine(line - 1);
                        player.sendMessage(ChatColor.GREEN + "La ligne " + id + " a bien été supprimée.");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("title")) {
                    String id = args[1];
                    BungeeNPC bungeeNPC = this.findOrFail(id, player);
                    if (bungeeNPC != null) {
                        bungeeNPC.setTitle(ChatColor.translateAlternateColorCodes('&', this.getStringWithArgs(args)));
                        player.sendMessage(ChatColor.GREEN + "Le titre de l'inventaire a bien été défini ! " + ChatColor.AQUA + "Astuce :" + ChatColor.GRAY + " Vous pouvez utiliser les codeurs couleurs grâce au symbole " + ChatColor.YELLOW + "&" + ChatColor.GRAY + ".");
                        player.sendMessage(ChatColor.GRAY + "Pour définir l'item utilisé tapez : " + ChatColor.AQUA + "/bungeenpc material " + id + " <material>");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("material")) {
                    String id = args[1];
                    BungeeNPC bungeeNPC = this.findOrFail(id, player);
                    if (bungeeNPC != null) {
                        Material material = Material.getMaterial(args[2]);
                        if (material == null) {
                            player.sendMessage(ChatColor.RED + "Aucun matériel avec le nom " + args[2] + ".");
                        } else {
                            bungeeNPC.setMaterial(material);
                            player.sendMessage(ChatColor.GREEN + "L'item utilisé a bien été défini !");
                            player.sendMessage(ChatColor.GRAY + "Pour définir le titre de l'inventaire tapez : " + ChatColor.AQUA + "/bungeenpc title " + id + " <title>");
                        }
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("addservers")) {
                    String id = args[1];
                    BungeeNPC bungeeNPC = this.findOrFail(id, player);
                    if (bungeeNPC != null) {
                        String[] newArgs = Arrays.copyOfRange(args, 2, args.length);
                        for (String serverName : newArgs) {
                            bungeeNPC.getServers().add(manager.getServerInfo(serverName));
                        }
                        player.sendMessage(ChatColor.GREEN + "Les serveurs ont bien été ajoutés !");
                    }
                    return true;
                }
            }
            this.showNPCHelp(player);
        }
        return true;
    }

    private BungeeNPC findOrFail(String id, Player player) {
        BungeeNPC bungeeNPC = manager.getNPCById(id);
        if (bungeeNPC == null) {
            player.sendMessage(ChatColor.RED + "Aucun NPC trouvé avec l'id " + id + ".");
        }
        return bungeeNPC;
    }

    private String getStringWithArgs(String[] args) {
        String string = "";
        String[] newArgs = Arrays.copyOfRange(args, 2, args.length);
        for (String part : newArgs) {
            string += part + " ";
        }
        return string.substring(0, string.length() - 1);
    }

    private void showNPCHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "====== AIDE BUNGEE NPC ======");
        player.sendMessage(ChatColor.AQUA + "/bungeenpc create <id> " + ChatColor.GRAY + "- créé un NPC");
        player.sendMessage(ChatColor.RED + "/bungeenpc delete <id> " + ChatColor.GRAY + "- supprime un NPC");
        player.sendMessage(ChatColor.AQUA + "/bungeenpc line <id> <text> [line]" + ChatColor.GRAY + "- défini une ligne du nom");
        player.sendMessage(ChatColor.AQUA + "/bungeenpc dline <id> <line>" + ChatColor.GRAY + "- supprime une ligne du nom");
        player.sendMessage(ChatColor.AQUA + "/bungeenpc title <id> <title> " + ChatColor.GRAY + "- défini le nom de l'inventaire");
        player.sendMessage(ChatColor.AQUA + "/bungeenpc material <id> <material> " + ChatColor.GRAY + "- défini l'item utilisé");
        player.sendMessage(ChatColor.AQUA + "/bungeenpc addservers <id> <servers...> " + ChatColor.GRAY + "- ajoute des serveurs au NPC");
    }
}
