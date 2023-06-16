package me.pixelgames.pixelcrack3r.ps.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;

import org.bukkit.event.Listener;

public class OnPlayerQuitListener implements Listener {

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		PrivateServer.getInstance().getPrivateServerHandler().removeConfigOutOfCache(e.getPlayer());
	}
	
}
