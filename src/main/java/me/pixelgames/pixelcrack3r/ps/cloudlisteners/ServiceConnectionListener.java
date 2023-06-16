package me.pixelgames.pixelcrack3r.ps.cloudlisteners;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import me.pixelgames.pixelcrack3r.ps.objects.PrivateServerService;

public class ServiceConnectionListener {

	@EventListener
	public void onServiceConnect(CloudServiceConnectNetworkEvent e) {
		if(e.getServiceInfo().getName().startsWith("PS-")) {
			if(e.getServiceInfo().getLifeCycle() == ServiceLifeCycle.RUNNING) {
				PrivateServerService server = PrivateServer.getInstance().getPrivateServerHandler().getPrivateServer(e.getServiceInfo().getName());
				if(server == null) {
					server = PrivateServer.getInstance().getPrivateServerHandler().buildExistingService(e.getServiceInfo());
					if(server != null) PrivateServer.getInstance().getPrivateServerHandler().initialize(server);
				}
				
				if(server == null) {
					Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "Stopping unregistered PS service....");
					e.getServiceInfo().provider().stopAsync();
					return;
				}
				server.setConnected(true);
				PrivateServer.getInstance().getUi().updateServerList();
			}
		}
	}
	
	@EventListener
	public void onServiceDisconnect(CloudServiceDisconnectNetworkEvent e) {
		if(e.getServiceInfo().getName().startsWith("PS-")) {
			PrivateServerService server = PrivateServer.getInstance().getPrivateServerHandler().getPrivateServer(e.getServiceInfo().getName());
			if(server == null) return;
			server.setConnected(false);
			server.stop();
			PrivateServer.getInstance().getUi().updateServerList();
		}
	}
	
	@EventListener
	public void onServiceInfoUpdate(CloudServiceInfoUpdateEvent e) {
		if(e.getServiceInfo().getName().startsWith("PS-") && e.getServiceInfo().getLifeCycle() == ServiceLifeCycle.RUNNING) {
			PrivateServerService server = PrivateServer.getInstance().getPrivateServerHandler().getPrivateServer(e.getServiceInfo().getName());
			if(server == null) {
				Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "Stopping unregistered PS service....");
				e.getServiceInfo().provider().stopAsync();
				return;
			}
			PrivateServer.getInstance().getUi().updateServerList();
			OfflinePlayer owner = Bukkit.getOfflinePlayer(server.getOwnerId());
			if(owner != null) {
				server.getServerConfiguration().setProperties(server.getProperties());
				PrivateServer.getInstance().getPrivateServerHandler().updateServerConfiguration(owner, server.getServerConfiguration());
			}
		}
	}
}
