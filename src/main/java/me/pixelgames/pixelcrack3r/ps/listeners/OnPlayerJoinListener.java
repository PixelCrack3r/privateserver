package me.pixelgames.pixelcrack3r.ps.listeners;

import java.lang.reflect.Field;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import me.pixelgames.pixelcrack3r.ps.utils.PacketHelper;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;

public class OnPlayerJoinListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		PrivateServer.getInstance().getPrivateServerHandler().downloadServerConfiguration(e.getPlayer());
		
		// set villager rotation
		PacketPlayOutEntityHeadRotation packet = new PacketPlayOutEntityHeadRotation();
		
		try {
			Field entityId = packet.getClass().getDeclaredField("a");
			entityId.setAccessible(true);
			entityId.setInt(packet, PrivateServer.getInstance().getUi().getVillager().getEntityId());
			
			Field yaw = packet.getClass().getDeclaredField("b");
			yaw.setAccessible(true);
			yaw.setByte(packet, (byte)(PrivateServer.getInstance().getUi().getVillagerLocation().getYaw() * 256.0F / 360.0F));
			
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		
		PacketHelper.prepare(packet).setTarget(e.getPlayer()).send();
	}
	
}
