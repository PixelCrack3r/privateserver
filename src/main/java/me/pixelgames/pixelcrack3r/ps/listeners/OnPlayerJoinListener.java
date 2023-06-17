package me.pixelgames.pixelcrack3r.ps.listeners;

import java.lang.reflect.Field;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
public class OnPlayerJoinListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		PrivateServer.getInstance().getPrivateServerHandler().downloadServerConfiguration(e.getPlayer());
	}
	
}
