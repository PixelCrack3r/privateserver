package me.pixelgames.pixelcrack3r.ps.handlers;

import java.sql.ResultSet;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import me.pixelgames.pixelcrack3r.ps.configuration.ServerConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import me.pixelgames.pixelcrack3r.ps.objects.PrivateServerService;

public class PrivateServerHandler {

	public final static List<PrivateServerService> PRIVATE_SERVERS = new ArrayList<PrivateServerService>();
	
	private final static Map<String, ServerConfiguration> LOADED_CONFIGS = new HashMap<String, ServerConfiguration>();
	
	public void initialize() {
		for(ServiceInfoSnapshot snapshot : getRunningPSs()) {
			if(PrivateServer.getInstance().getPSConfig().getBoolean("server.registerStartedServers") ) {
				
				if(!snapshot.getProperty(BridgeServiceProperty.EXTRA).isPresent()) {
					snapshot.provider().stopAsync();
					Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "§cThe server " + snapshot.getName() + " was closed because it could not be initialized! You can disable this feature in the config.");
					continue;
				}

				try {
					JsonObject properties = JsonParser.parseString(snapshot.getProperty(BridgeServiceProperty.EXTRA).get()).getAsJsonObject();

					UUID ownerId = properties.has("privateserver.owner") ? UUID.fromString(properties.get("privateserver.owner").getAsString()) : null;
					OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);

					PrivateServerService service = new PrivateServerService(owner, snapshot, this.getServerConfiguration(owner));
					service.cancelSending();
					service.setWrapperActive(true);
					service.setConnected(true);
					service.start();
					PRIVATE_SERVERS.add(service);
					service.sendMessage(JsonDocument.newDocument().append("action", "broadcast").append("message", "§7[§bPrivateServer§7] §9INFO: §7This private server has been registered on a new provider service!"));
					Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "§cThe server " + snapshot.getName() + " has been initialized.");
				} catch(Exception e) {
					Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "§cThe server " + snapshot.getName() + " was closed because it could not be initialized because the extra property of this server had an invalid format!");
					snapshot.provider().stopAsync();
				}
					
			}

			
			if(PrivateServer.getInstance().getPSConfig().getBoolean("server.stopWhenInitializing")) {
				snapshot.provider().stopAsync();
				Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "§cThe server " + snapshot.getName() + " was closed because the provider is initialized. You can disable this feature in the config.");
			}
		}
	}
	
	public void initialize(PrivateServerService service) {
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
		if(!snapshot.getProperty(BridgeServiceProperty.EXTRA).isPresent()) return null;
		return JsonParser.parseString(snapshot.getProperty(BridgeServiceProperty.EXTRA).get()).getAsJsonObject();
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
		return CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesByGroup("PS");
	}
	
	public ServiceInfoSnapshot prepareService(Player player) {
		return prepareService(player, this.getServerConfiguration(player));
	}
	
	public ServiceInfoSnapshot prepareService(Player player, ServerConfiguration config) {
		if(isServerRunning(player)) return null;
		if(CloudNetDriver.getInstance().getServiceTaskProvider().isServiceTaskPresent("PS")) {
			boolean isTemplate = false;
			if(PrivateServer.getInstance().getTemplateHandler().templateExists(config.getTemplate())) {
				config = PrivateServer.getInstance().getTemplateHandler().getServerConfiguration(config.getTemplate());				
				isTemplate = true;
			}
			
			ServiceTask task = CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTask("PS");
			if(task == null) throw new RuntimeException("The task PS is not configured yet!");
			ServiceTask psTask = new ServiceTask(task.getIncludes(), task.getTemplates(), task.getDeployments(), task.getName() + "-" + player.getName(), task.getRuntime(), task.isMaintenance(), task.isAutoDeleteOnStop(), task.isStaticServices(), task.getAssociatedNodes(), task.getGroups(), task.getDeletedFilesAfterStop(), task.getProcessConfiguration(), task.getStartPort(), 0, task.getJavaCommand());
			
			if(isTemplate) {
				List<ServiceTemplate> templates = new ArrayList<>(task.getTemplates());
				templates.add(PrivateServer.getInstance().getTemplateHandler().getTemplate(config.getTemplate()));	
				psTask.setTemplates(templates);
			}
			
			List<ServiceRemoteInclusion> plugins = new ArrayList<ServiceRemoteInclusion>();
			if(config.getProperties().has("plugins")) {
				for(Entry<String, JsonElement> plugin : config.getProperties().get("plugins").getAsJsonObject().entrySet()) {
			
					if(!plugin.getValue().getAsJsonObject().get("installed").getAsBoolean()) continue;
					if(!PrivateServer.getInstance().getPSConfig().getBoolean("server.allowRemovedPlugins") && !PrivateServer.getInstance().getPluginHandler().pluginExists(plugin.getKey())) continue;
					
					ServiceRemoteInclusion inclusion = new ServiceRemoteInclusion(plugin.getValue().getAsJsonObject().get("url").getAsString(), "plugins/" + plugin.getValue().getAsJsonObject().get("file").getAsString());
					plugins.add(inclusion);
					
				}
			}
			
			if(config.getProperties().has("javaCommand")) psTask.setJavaCommand(config.getProperties().get("javaCommand").getAsString());
			if(config.getProperties().has("runtime")) psTask.setRuntime(config.getProperties().get("runtime").getAsString());
			if(config.getProperties().has("maxHeapMemory")) psTask.getProcessConfiguration().setMaxHeapMemorySize(config.getProperties().get("maxHeapMemory").getAsInt());
			
			Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "The task " + psTask.getName() + " has been prepared with the following configuration:");
			Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  Template: " + config.getTemplate());
			Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  Template Exists: " + PrivateServer.getInstance().getTemplateHandler().templateExists(config.getTemplate()));
			Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  Static: " + config.isStatic());
			psTask.getTemplates().forEach(template -> {
				Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  " + template.getName() + ": " + template.getPrefix() + " | " + template.getStorage() + " -> " + template.getTemplatePath());
			});
			plugins.forEach(inclusion -> {
				Bukkit.getConsoleSender().sendMessage(PrivateServer.getInstance().getPrefix() + "  " + inclusion.getDestination() + ": " + inclusion.getUrl());
			});
			
			ServiceConfiguration serviceConfig = ServiceConfiguration.builder()
					.task(psTask)
					.inclusions(plugins)
					.addDeletedFilesAfterStop("plugins/")
					.build();

			serviceConfig.setStaticService(config.isStatic());

			return CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(serviceConfig);
		}
		return null;
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
					LOADED_CONFIGS.put(player.getName(), new ServerConfiguration(template, isStatic, JsonParser.parseString(properties).getAsJsonObject()));
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
