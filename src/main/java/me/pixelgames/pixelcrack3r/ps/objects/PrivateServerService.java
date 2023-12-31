package me.pixelgames.pixelcrack3r.ps.objects;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import me.pixelgames.pixelcrack3r.ps.configuration.ServerConfiguration;
import me.pixelgames.pixelcrack3r.ps.handlers.PrivateServerHandler;
import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import gq.pixelgames.pixelcrack3r.utils.Title;

public class PrivateServerService {

	private final String owner;
	private final String name;
	
	private final UUID uuid;
	private final UUID oid;
	
	private final AtomicInteger step;
	private final AtomicInteger schedulerId;
	private final AtomicInteger timeout;
	
	private boolean connected;
	private boolean started;
	private boolean sent;
	private boolean wrapperActive;
	
	private final ServiceInfoSnapshot snapshot;
	
	private final ServerConfiguration config;
	
	public PrivateServerService(Player player, ServiceInfoSnapshot snapshot) {
		this(player, snapshot, PrivateServer.getInstance().getPrivateServerHandler().getServerConfiguration(player));
	}
	
	public PrivateServerService(OfflinePlayer owner, ServiceInfoSnapshot snapshot, ServerConfiguration config) {
		this.owner = owner.getName();
		this.oid = owner.getUniqueId();
		this.snapshot = snapshot;
		this.name = snapshot.getName();
		this.uuid = snapshot.getServiceId().getUniqueId();
		this.step = new AtomicInteger(0);
		this.timeout = new AtomicInteger(0);
		this.schedulerId = new AtomicInteger();
		this.started = false;
		this.config = config;
	}
	
	public ServiceInfoSnapshot getServiceInfo() {
		return CloudNetDriver.getInstance().getCloudServiceProvider().getCloudService(this.uuid);
	}
	
	public ServiceInfoSnapshot getPreServiceInfo() {
		return this.snapshot;
	}
	
	public String getOwner() {
		return this.owner;
	}
	
	public String getName() {
		return this.name;
	}
	
	public UUID getId() {
		return this.uuid;
	}
	
	public void connect(Player player) {
		if(player == null) return;
		PrivateServer.connect(player, this.name);
	}
	
	public void execute(String command) {
		if(this.isRunning())
			this.getServiceInfo().provider().runCommand(command);
	}
	
	public void start() {
		if(this.started) return;
		this.started = true;
		
		this.schedulerId.set(Bukkit.getScheduler().scheduleSyncRepeatingTask(PrivateServer.getInstance(), () -> {
			
			if(!this.sent) {
				Player player = Bukkit.getPlayer(this.getOwner());
				
				if(player != null) {
					switch(this.step.getAndAdd(1) % 4) {
					case 1: case 3: {
						Title.sendTitle(player, "§cLoading", "§7⬤  §c⬤  §7⬤");
						break;
					}
					case 2: {
						Title.sendTitle(player, "§cLoading", "§7⬤  §7⬤  §c⬤");
						break;
					}
					default: {
						Title.sendTitle(player, "§cLoading", "§c⬤  §7⬤  §7⬤");
						break;
					}
					}
					if(this.step.get() == 15 + (7*this.getPreServiceInfo().getConfiguration().getIncludes().length)) {
						player.sendMessage(PrivateServer.getInstance().getPrefix() + "Starting the service takes longer than usual.");
					} else if(this.step.get() == 30 + (7*this.getPreServiceInfo().getConfiguration().getIncludes().length)) {
						player.sendMessage(PrivateServer.getInstance().getPrefix() + "It seems an error has occurred. Please report this incident to an administrator.");
						this.stop();
					}
				}
			}
			
			if(this.isRunning()) {
				if(!this.sent) {
					Player player = Bukkit.getPlayer(this.getOwner());
					
					if(!this.config.isTemplate() && !this.isWrapperActive()) this.execute("op " + this.getOwner());
					
					PrivateServer.getInstance().getPSConfig().getConfigExact().getStringList("server.executeOnConnect").forEach(command -> {
						this.execute(command
								.replaceAll("%owner%", this.getOwner())
								.replaceAll("%ownerId%", this.getOwnerId().toString()));
					});
				
					if(player != null) {
						player.sendMessage(PrivateServer.getInstance().getPrefix() + "Your server has been successfully connected. You will be sent!");
						this.connect(player);
					}
					this.sent = true;
				}

				if(this.getServiceInfo().getProperty(BridgeServiceProperty.ONLINE_COUNT).isPresent() && this.getServiceInfo().getProperty(BridgeServiceProperty.ONLINE_COUNT).get() <= 0) {
					int current = this.timeout.addAndGet(1);
					if(current > PrivateServer.getInstance().getPSConfig().getInt("server.stopAfter"))
						this.stop();
				} else this.timeout.set(0);
			}
			
		}, 0, 20));
		
		if(this.isStarting()) return; // the service is already started
		
		this.getPreServiceInfo().provider().startAsync().onComplete(c -> {
			Player player = Bukkit.getPlayer(this.getOwner());
			if(player != null) {
				if(this.getServiceInfo() != null) {
					player.sendMessage(PrivateServer.getInstance().getPrefix() + "Your server is started. You will be sent to your server soon.");									
				} else {
					player.sendMessage(PrivateServer.getInstance().getPrefix() + "§cAn error occurred while the server was starting.");											
					this.stop();
				}
			}
		});
	}
	
	public boolean isConnected() {
		return this.connected;
	}
	
	public boolean isRunning() {
		ServiceInfoSnapshot info = this.getServiceInfo();
		return this.isConnected() && info != null && info.getProperty(BridgeServiceProperty.IS_ONLINE).isPresent() && info.getProperty(BridgeServiceProperty.IS_ONLINE).get();
	}
	
	public boolean isStarting() {
		ServiceInfoSnapshot info = this.getServiceInfo();
		return this.isConnected() && info != null && info.getProperty(BridgeServiceProperty.IS_STARTING).isPresent() && info.getProperty(BridgeServiceProperty.IS_STARTING).get();
	}
	
	public void sendMessage(JsonDocument data) {
		ChannelMessage.builder().channel("private_server").message("send_data").json(data).targetService(this.getServiceInfo().getName()).build().send();
	}
	
	public Collection<ChannelMessage> query(JsonDocument data) {
		return ChannelMessage.builder().channel("private_server").message("send_query").json(data).targetService(this.getServiceInfo().getName()).build().sendQuery();
	}
	
	public void setConnected(boolean connected) {
		if(connected) this.timeout.set(0);
		this.connected = connected;
	}
	
	public boolean isWrapperActive() {
		return wrapperActive;
	}

	public void setWrapperActive(boolean wrapperActive) {
		this.wrapperActive = wrapperActive;
	}
	
	public void cancelSending() {
		this.sent = true;
	}

	public void stop() {
		Bukkit.getScheduler().cancelTask(this.schedulerId.get());
		if(this.getServiceInfo() != null) {
			this.getServiceInfo().provider().stopAsync();
		}
		if(!this.isConnected()) PrivateServerHandler.PRIVATE_SERVERS.remove(this);
	}
	
	public ServerConfiguration getServerConfiguration() {
		return this.config;
	}
	
	public JsonObject getProperties() {
		return JsonParser.parseString(this.getServiceInfo().getProperty(BridgeServiceProperty.EXTRA).orElse("{}")).getAsJsonObject();
	}
	
	public JsonObject buildStartupProperties() {
		JsonObject obj = this.getServerConfiguration().getProperties();
		obj.addProperty("privateserver.owner", this.getOwnerId().toString());
		obj.addProperty("privateserver.isTemplate", this.getServerConfiguration().isTemplate());
		obj.addProperty("privateserver.isStatic", this.getServerConfiguration().isStatic());
		return obj;
	}
	
	public UUID getOwnerId() {
		return this.oid;
	}
	
}
