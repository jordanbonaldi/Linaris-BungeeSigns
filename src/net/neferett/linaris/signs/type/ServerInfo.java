package net.neferett.linaris.signs.type;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import lombok.Data;
import net.minecraft.util.com.google.gson.JsonObject;
import net.minecraft.util.com.google.gson.JsonParser;

import org.bukkit.ChatColor;

@Data
public class ServerInfo {
    private String name;
    private String description;
    private InetSocketAddress address;
    private int onlinePlayers, maxPlayers;
    private boolean online;

    public ServerInfo() {
        this.resetFields();
    }

    public void ping() {
        if (address == null) {
            this.resetFields();
            return;
        }
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(100);
            socket.connect(address, 100);
            final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            final DataInputStream in = new DataInputStream(socket.getInputStream());
            final ByteArrayOutputStream frame = new ByteArrayOutputStream();
            final DataOutputStream frameOut = new DataOutputStream(frame);
            ServerInfo.writeVarInt(0, frameOut);
            ServerInfo.writeVarInt(4, frameOut);
            ServerInfo.writeString(address.getHostName(), frameOut);
            frameOut.writeShort(address.getPort());
            ServerInfo.writeVarInt(1, frameOut);
            ServerInfo.writeVarInt(frame.size(), out);
            frame.writeTo(out);
            frame.reset();
            ServerInfo.writeVarInt(0, frameOut);
            ServerInfo.writeVarInt(frame.size(), out);
            frame.writeTo(out);
            frame.reset();
            final int len = ServerInfo.readVarInt(in);
            final byte[] packet = new byte[len];
            in.readFully(packet);
            final ByteArrayInputStream inPacket = new ByteArrayInputStream(packet);
            final DataInputStream inFrame = new DataInputStream(inPacket);
            final int id = ServerInfo.readVarInt(inFrame);
            if (id != 0) { throw new IllegalStateException("Wrong ping response!"); }
            final JsonObject object = new JsonParser().parse(ServerInfo.readString(inFrame)).getAsJsonObject();
            final JsonObject players = object.get("players").getAsJsonObject();
            online = true;
            description = object.get("description").getAsString();
            onlinePlayers = players.get("online").getAsInt();
            maxPlayers = players.get("max").getAsInt();
        } catch (final Exception ex) {
            this.resetFields();
        }
    }

    private void resetFields() {
        online = false;
        description = ChatColor.RED + "Hors ligne";
        onlinePlayers = 0;
        maxPlayers = -1;
    }

    public boolean canJoin() {
        return online && address != null && description != null && !description.startsWith(ChatColor.RED.toString()) && !description.startsWith(ChatColor.YELLOW.toString()) && !description.startsWith(ChatColor.BLUE.toString()) && !description.startsWith(ChatColor.DARK_RED.toString());
    }

    public static void writeString(final String s, final DataOutput out) throws IOException {
        final byte[] b = s.getBytes("UTF-8");
        ServerInfo.writeVarInt(b.length, out);
        out.write(b);
    }

    public static String readString(final DataInput in) throws IOException {
        final int len = ServerInfo.readVarInt(in);
        final byte[] b = new byte[len];
        in.readFully(b);
        return new String(b, "UTF-8");
    }

    public static int readVarInt(final DataInput input) throws IOException {
        int out = 0;
        int bytes = 0;
        while (true) {
            final byte in = input.readByte();
            out |= (in & 0x7F) << bytes++ * 7;
            if (bytes > 32) { throw new RuntimeException("VarInt too big"); }
            if ((in & 0x80) != 128) {
                break;
            }
        }
        return out;
    }

    public static void writeVarInt(int value, final DataOutput output) throws IOException {
        while (true) {
            int part = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                part |= 0x80;
            }
            output.writeByte(part);
            if (value == 0) {
                break;
            }
        }
    }
}
