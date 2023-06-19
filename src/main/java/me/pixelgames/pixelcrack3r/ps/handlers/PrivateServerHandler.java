package me.pixelgames.pixelcrack3r.ps.handlers;

import java.sql.ResultSet;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.*;
import eu.cloudnetservice.modules.bridge.BridgeServiceProperties;
import me.pixelgames.pixelcrack3r.ps.configuration.ServerConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import me.pixelgames.pixelcrack3r.ps.objects.PrivateServerService;

public class PrivateServerHandler {

	public final static List<PrivateServerService> PRIVATE_SERVERS = new ArrayList<PrivateServerService>();
	
	private final static Map<String, ServerConfiguration> LOADED_CONFIGS = new HashMap<String, ServerConfiguration>();

	private final CloudServiceProvider cloudServiceProvider;
	private final ServiceTaskProvider serviceTaskProvider;
	private final CloudServiceFactory cloudServiceFactory;

	public PrivateServerHandler(CloudServiceProvider cloudServiceProvider, ServiceTaskProvider serviceTaskProvider, CloudServiceFactory cloudServiceFactory) {
		this.cloudServiceProvider = cloudServiceProvider;
		this.serviceTaskProvider = serviceTaskProvider;
		this.cloudServiceFactory = cloudServiceFactory;
	}

	public void initialize() {
		for(ServiceInfoSnapshot snapshot : getRunningPSs()) {
			if(PrivateServer.getInstance().getPSConfig().getBoolean("server.registerStartedServers") ) {

				if(snapshot.propertyAbsent(BridgeServiceProperties.EXTRA)) {
					snapshot.provider().stopAsync();
					Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "§cThe service " + snapshot.name() + " is stopped because it could not be initialized! You can disable this feature in the config.");
					continue;
				}

				try {
					JsonDocument properties = JsonDocument.fromJsonString(snapshot.readProperty(BridgeServiceProperties.EXTRA));

					UUID ownerId = properties.contains("privateserver.owner") ? UUID.fromString(properties.getString("privateserver.owner")) : null;
					OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);

					PrivateServerService service = new PrivateServerService(owner, snapshot, this.getServerConfiguration(owner));
					service.cancelSending();
					service.setWrapperActive(true);
					service.setConnected(true);
					service.start();
					PRIVATE_SERVERS.add(service);
					service.sendMessage(JsonDocument.newDocument().append("action", "broadcast").append("message", "§7[§bPrivateServer§7] §9INFO: §7This private server has been registered on a new provider service!"));
					Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "§cThe service " + snapshot.name() + " has been initialized.");
				} catch(Exception e) {
					Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "§cThe service " + snapshot.name() + " is stopped because it could not be initialized because the extra property could not be parsed!");
					snapshot.provider().stopAsync();
				}
					
			}

			
			if(PrivateServer.getInstance().getPSConfig().getBoolean("server.stopWhenInitializing")) {
				snapshot.provider().stopAsync();
				Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "§cThe server " + snapshot.name() + " is stopped because the provider is initialized. You can disable this feature in the config.");
			}
		}
	}
	
	public void register(PrivateServerService service) {
		if(PRIVATE_SERVERS.contains(service)) return;
		PRIVATE_SERVERS.add(service);
	}
	
	public PrivateServerService buildExistingService(ServiceInfoSnapshot snapshot) {
		JsonObject properties = PrivateServer.getInstance().requestServiceProperties(snapshot);
		if(properties == null) return null;
		
		UUID ownerId = properties.has("privateserver.owner") ? UUID.fromString(properties.get("privateserver.owner").getAsString()) : null;
		OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
		
		PrivateServerService service = new PrivateServerService(owner, snapshot, this.getServerConfiguration(owner));
		service.cancelSending();
		service.setConnected(true);
		service.setWrapperActive(true);
		service.start();
		return service;
	}
	
	public JsonObject extractServiceProperties(ServiceInfoSnapshot snapshot) {
		if(snapshot.propertyAbsent(BridgeServiceProperties.EXTRA)) return null;
		return new JsonParser().parse(snapshot.readProperty(BridgeServiceProperties.EXTRA)).getAsJsonObject();
	}
	
	public boolean isServerRunning(OfflinePlayer player) {
		return getPrivateServer(player.getName()) != null;
	}
	
	public PrivateServerService getPrivateServer(String name) {
		for(PrivateServerService service : PRIVATE_SERVERS) {
			if(service.getOwner().equalsIgnoreCase(name) || service.getName().equalsIgnoreCase(name) || service.getId().toString().equalsIgnoreCase(name)) {
				return service;
			}
		}
		return null;
	}
	
	public Collection<ServiceInfoSnapshot> getRunningPSs() {
		return this.cloudServiceProvider.servicesByGroup("PS");
	}
	
	public ServiceInfoSnapshot prepareService(Player player) {
		return prepareService(player, this.getServerConfiguration(player));
	}
	
	public ServiceInfoSnapshot prepareService(Player player, ServerConfiguration config) {
		if(isServerRunning(player)) return null;
		ServiceTask task = this.serviceTaskProvider.serviceTask("PS");
		if(task == null) throw new RuntimeException("The task PS is not configured yet!");

		boolean isTemplate = false;
		if(PrivateServer.getInstance().getTemplateHandler().templateExists(config.getTemplate())) {
			config = PrivateServer.getInstance().getTemplateHandler().getServerConfiguration(config.getTemplate());
			isTemplate = true;
		}

		if(isTemplate) {
			task.templates().add(PrivateServer.getInstance().getTemplateHandler().getTemplate(config.getTemplate()));
		}

		List<ServiceRemoteInclusion> plugins = new ArrayList<ServiceRemoteInclusion>();
		if(config.getProperties().has("plugins")) {
			for(Map.Entry<String, JsonElement> plugin : config.getProperties().get("plugins").getAsJsonObject().entrySet()) {

				if(!plugin.getValue().getAsJsonObject().get("installed").getAsBoolean()) continue;
				if(!PrivateServer.getInstance().getPSConfig().getBoolean("server.allowRemovedPlugins") && !PrivateServer.getInstance().getPluginHandler().pluginExists(plugin.getKey())) continue;

				plugins.add(ServiceRemoteInclusion.builder()
						.url(plugin.getValue().getAsJsonObject().get("url").getAsString())
						.destination("plugins/" + plugin.getValue().getAsJsonObject().get("file").getAsString())
						.build());

			}
		}

		ServiceConfiguration.Builder serviceBuilder = ServiceConfiguration.builder(task)
				.taskName("PS-" + player.getName());

		if(config.getProperties().has("javaCommand")) serviceBuilder.javaCommand(config.getProperties().get("javaCommand").getAsString());
		if(config.getProperties().has("runtime")) serviceBuilder.runtime(config.getProperties().get("runtime").getAsString());
		if(config.getProperties().has("maxHeapMemory")) serviceBuilder.maxHeapMemory(config.getProperties().get("maxHeapMemory").getAsInt());

		ServiceConfiguration serviceConfiguration = serviceBuilder.build();

		Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "The task " + "PS-" + player.getName() + " has been prepared with the following configuration:");
		Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  Template: " + config.getTemplate());
		Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  Template Exists: " + PrivateServer.getInstance().getTemplateHandler().templateExists(config.getTemplate()));
		Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  Static: " + config.isStatic());
		serviceConfiguration.templates().forEach(template -> {
			Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  " + template.name() + ": " + template.prefix() + " | " + template.storageName() + " -> " + template.fullName());
		});
		plugins.forEach(inclusion -> {
			Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  " + inclusion.destination() + ": " + inclusion.url());
		});

		return serviceConfiguration.createNewService().serviceInfo();
	}
	
	public boolean start(Player player) {
		ServiceInfoSnapshot service = this.prepareService(player);
		if(service == null) return false;
		PRIVATE_SERVERS.add(new PrivateServerService(player, service));
		PrivateServerService server = this.getPrivateServer(player.getName());
		server.start();
		return true;
	}
	
	public ServerConfiguration getServerConfiguration(OfflinePlayer player) {
		if(!LOADED_CONFIGS.containsKey(player.getName())) return this.downloadServerConfiguration(player);
		return LOADED_CONFIGS.get(player.getName());
	}
	
	public void removeConfigOutOfCache(Player player) {
		LOADED_CONFIGS.remove(player.getName());
	}
	
	public ServerConfiguration downloadServerConfiguration(OfflinePlayer player) {
		try(ResultSet result = PrivateServer.getInstance().getMySQLInstance().query("SELECT * FROM `privateservers` WHERE `owner` = '" + player.getUniqueId().toString() + "';");) {
			if(result == null) {
				Bukkit.getConsoleSender().sendMessage("§7[§cERROR§7] The configuration of " + player.getName() + " couldn't be loaded!");
				return null;
			}
			
			while(result.next()) {
				String owner = result.getString("owner");
				String properties = result.getString("properties");
				String template = result.getString("templates");
				boolean isStatic = result.getBoolean("static");
				
				if(owner.equalsIgnoreCase(player.getUniqueId().toString())) {
					LOADED_CONFIGS.put(player.getName(), new ServerConfiguration(template, isStatic, new JsonParser().parse(properties).getAsJsonObject()));
					return LOADED_CONFIGS.get(player.getName());
				}
			}
			
			ServerConfiguration config = new ServerConfiguration();
			
			int isStatic = config.isStatic() ? 1 : 0;
			
			PrivateServer.getInstance().getMySQLInstance().update("INSERT INTO `privateservers` (`owner`, `properties`, `static`, `templates`) VALUES ('" + player.getUniqueId().toString() + "', '" + config.getProperties().toString() + "', " + isStatic + ", '" + config.getTemplate() + "');");
			return config;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void updateServerConfiguration(OfflinePlayer player, ServerConfiguration config) {
		int isStatic = config.isStatic() ? 1 : 0;
		
		PrivateServer.getInstance().getMySQLInstance().update("UPDATE `privateservers` SET `properties` = '" + config.getProperties().toString() + "', `templates` = '" + config.getTemplate() + "', `static` = " + isStatic + " WHERE `owner` = '" + player.getUniqueId().toString() + "';");
		LOADED_CONFIGS.put(player.getName(), config);
	}
	
	public List<PrivateServerService> getPublicServices() {
		return new ArrayList<PrivateServerService>(PrivateServerHandler.PRIVATE_SERVERS).stream()
				.filter(Objects::nonNull)
				.filter(PrivateServerService::isConnected)
				.filter(server -> !server.getServerConfiguration().getProperties().has("access") || server.getServerConfiguration().getProperties().get("access").getAsString().equalsIgnoreCase("public"))
				.collect(Collectors.toList());
	}

}
