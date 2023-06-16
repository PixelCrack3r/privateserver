package me.pixelgames.pixelcrack3r.ps.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;

public class OnPlayerInteractEntityListener implements Listener {

	@EventHandler
	public void onInteractEntity(PlayerInteractEntityEvent e) {
		if(e.getRightClicked() != null && e.getRightClicked().getCustomName().equalsIgnoreCase(PrivateServer.getInstance().getPSConfig().getString("villager.name"))) {
			e.setCancelled(true);
			PrivateServer.getInstance().getUi().openStartStopInv(e.getPlayer());
		}
	}
	
}
