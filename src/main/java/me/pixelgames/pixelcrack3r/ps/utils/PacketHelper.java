package me.pixelgames.pixelcrack3r.ps.utils;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_8_R3.Packet;

public class PacketHelper {

	private Packet<?> packet;
	private Player player;
	
	public PacketHelper setPacket(Packet<?> packet) {
		this.packet = packet;
		return this;
	}
	
	public PacketHelper setTarget(Player player) {
		this.player = player;
		return this;
	}
	
	public void send() {
		((CraftPlayer)this.player).getHandle().playerConnection.sendPacket(this.packet);
	}
	
	public static PacketHelper prepare(Packet<?> packet) {
		return new PacketHelper().setPacket(packet);
	}
	
}
