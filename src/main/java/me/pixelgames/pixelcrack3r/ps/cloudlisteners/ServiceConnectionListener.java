package me.pixelgames.pixelcrack3r.ps.cloudlisteners;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import me.pixelgames.pixelcrack3r.ps.objects.PrivateServerService;

public class ServiceConnectionListener {

	@EventListener
	public void onCloudServiceLifecycleChange(CloudServiceLifecycleChangeEvent e) {
		if(e.serviceInfo().name().startsWith("PS-")) {
			PrivateServerService server = PrivateServer.getInstance().getPrivateServerHandler().getPrivateServer(e.serviceInfo().name());

			if(e.newLifeCycle() == ServiceLifeCycle.RUNNING) {
				if(server == null) {
					server = PrivateServer.getInstance().getPrivateServerHandler().buildExistingService(e.serviceInfo());
					if(server != null) PrivateServer.getInstance().getPrivateServerHandler().register(server);
				}
				
				if(server == null) {
					Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "Stopping unregistered PS service....");
					e.serviceInfo().provider().stopAsync();
					return;
				}
				server.setConnected(true);
				PrivateServer.getInstance().getUi().updateServerList();
			} else if(e.newLifeCycle() == ServiceLifeCycle.STOPPED || e.newLifeCycle() == ServiceLifeCycle.DELETED) {
				Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "CloudService " + e.serviceInfo().name() + " is disconnected!");
				if(server == null) return;
				server.setConnected(false);
				server.stop();
				server.forceDelete();
				PrivateServer.getInstance().getUi().updateServerList();
			}
		}
	}

	@EventListener
	public void onCloudServiceUpdate(CloudServiceUpdateEvent e) {
		if(e.serviceInfo().name().startsWith("PS-") && e.serviceInfo().lifeCycle() == ServiceLifeCycle.RUNNING) {
			PrivateServerService server = PrivateServer.getInstance().getPrivateServerHandler().getPrivateServer(e.serviceInfo().name());
			if(server == null) {
				Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "Stopping unregistered PS service....");
				e.serviceInfo().provider().stopAsync();
				return;
			}
			PrivateServer.getInstance().getUi().updateServerList();
			OfflinePlayer owner = Bukkit.getOfflinePlayer(server.getOwnerId());
			if(owner != null && server.isWrapperActive()) {
				server.getServerConfiguration().setProperties(server.getProperties());
				PrivateServer.getInstance().getPrivateServerHandler().updateServerConfiguration(owner, server.getServerConfiguration());
			}
		}
	}
}
