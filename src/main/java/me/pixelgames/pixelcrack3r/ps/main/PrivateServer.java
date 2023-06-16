package me.pixelgames.pixelcrack3r.ps.main;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import me.pixelgames.pixelcrack3r.ps.cloudlisteners.ChannelMessageReceiveListener;
import me.pixelgames.pixelcrack3r.ps.cloudlisteners.ServiceConnectionListener;
import me.pixelgames.pixelcrack3r.ps.commands.CommandPrivateServer;
import me.pixelgames.pixelcrack3r.ps.configuration.PSConfiguration;
import me.pixelgames.pixelcrack3r.ps.listeners.OnPlayerInteractEntityListener;
import me.pixelgames.pixelcrack3r.ps.listeners.OnPlayerInventoryClickListener;
import me.pixelgames.pixelcrack3r.ps.listeners.OnPlayerJoinListener;
import me.pixelgames.pixelcrack3r.ps.listeners.OnPlayerQuitListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget.Type;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import gq.pixelgames.pixelcrack3r.database.MySQL;
import me.pixelgames.pixelcrack3r.ps.handlers.PluginHandler;
import me.pixelgames.pixelcrack3r.ps.handlers.PrivateServerHandler;
import me.pixelgames.pixelcrack3r.ps.handlers.PrivateServerUIHandler;
import me.pixelgames.pixelcrack3r.ps.handlers.TemplateHandler;
import me.pixelgames.pixelcrack3r.ps.handlers.TicketSystem;
import net.md_5.bungee.api.ChatColor;

public class PrivateServer extends JavaPlugin {

	private static PrivateServer plugin;
	
	private PSConfiguration config;
	
	private PrivateServerUIHandler ui;
	private TemplateHandler templateHandler;
	private PrivateServerHandler serverHandler;
	private PluginHandler pluginHandler;
	
	private TicketSystem ticketSystem;
	
	private MySQL mySQLProvider;
	
	private Map<String, Map<String, String>> playerConfiguration;
	
	@Override
	public void onEnable() {
		plugin = this;
		
		Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "loading file and database configurations...");
		this.loadConfig();
		this.initializeMySQL();
		
		Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "loading instances...");
		this.setupUi();
		this.templateHandler = new TemplateHandler();
		this.serverHandler = new PrivateServerHandler();
		this.pluginHandler = new PluginHandler();
		this.ticketSystem = new TicketSystem();
		
		Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "initialize cloud...");
		this.getPrivateServerHandler().initialize();
		CloudNetDriver.getInstance().getEventManager().registerListener(new ServiceConnectionListener());
		CloudNetDriver.getInstance().getEventManager().registerListener(new ChannelMessageReceiveListener());
		
		Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "register channels...");
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "register commands...");
		this.registerCommands();
		
		Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "register listeners...");
		this.registerListeners();
		
		Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "plugin loaded!");
		
	}
	
	@Override
	public void onDisable() {
		CloudNetDriver.getInstance().getEventManager().unregisterListener(this.getClassLoader());
	}
	
	private void registerCommands() {
		this.getCommand("privateserver").setExecutor(new CommandPrivateServer());
	}
	
	private void registerListeners() {
		Bukkit.getPluginManager().registerEvents(new OnPlayerJoinListener(), this);
		Bukkit.getPluginManager().registerEvents(new OnPlayerQuitListener(), this);
		Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEntityListener(), this);
		Bukkit.getPluginManager().registerEvents(new OnPlayerInventoryClickListener(), this);
	}
	
	private void loadConfig() {
		this.config = new PSConfiguration(new File("plugins/PrivateServer/config.yml"));
		
		this.config.getConfigExact().addDefault("mysql.host", "localhost");
		this.config.getConfigExact().addDefault("mysql.database", "privateserver");
		this.config.getConfigExact().addDefault("mysql.user", "root");
		this.config.getConfigExact().addDefault("mysql.password", "toor");
		
		this.config.getConfigExact().addDefault("plugin.prefix", "&7[&bPrivateServer&7]");
		
		this.config.getConfigExact().addDefault("ticketsystem.enabled", false);
		this.config.getConfigExact().addDefault("ticketsystem.purchases", false);
		
		this.config.getConfigExact().addDefault("server.stopAfter", 30);
		this.config.getConfigExact().addDefault("server.stopWhenInitializing", true);
		this.config.getConfigExact().addDefault("server.registerStartedServers", false);
		this.config.getConfigExact().addDefault("server.executeOnConnect", Arrays.asList("plugman unload plugin1", "plugman unload plugin2"));
		this.config.getConfigExact().addDefault("server.allowRemovedPlugins", false);
		
		this.config.getConfigExact().addDefault("villager.name", "&8» &6PrivateServer");
		
		this.config.getConfigExact().addDefault("inventory.cp.name", "&8» &6PrivateServer");
		this.config.getConfigExact().addDefault("inventory.serverlist.name", "&8» &6Serverlist");
		this.config.getConfigExact().addDefault("inventory.settings.name", "&8» &6Settings");
		this.config.getConfigExact().addDefault("inventory.templates.name", "&8» &6Templates");
		this.config.getConfigExact().addDefault("inventory.plugins.name", "&8» &6Plugins");
		this.config.getConfigExact().addDefault("inventory.gamerules.name", "&8» &6Gamerules");
		this.config.getConfigExact().addDefault("inventory.adminStartInv.name", "&8» &6Start Service");
		
		if(!new File("plugins/PrivateServer/config.yml").exists()) {
			this.config.getConfigExact().addDefault("privateservers.templates.manhunt.name", "&fMan&cHunt");
			this.config.getConfigExact().addDefault("privateservers.templates.manhunt.description", "Kill the speedrunner before he finishs the game!");
			this.config.getConfigExact().addDefault("privateservers.templates.manhunt.item", Material.EYE_OF_ENDER.name());
			this.config.getConfigExact().addDefault("privateservers.templates.manhunt.prefix", "ManHunt");
			this.config.getConfigExact().addDefault("privateservers.templates.manhunt.template_name", "default");
			this.config.getConfigExact().addDefault("privateservers.templates.manhunt.storage", "local");	
		
			this.config.getConfigExact().addDefault("privateservers.plugins.worldedit.name", "&4Fast Async World Edit");
			this.config.getConfigExact().addDefault("privateservers.plugins.worldedit.description", "FAWE is designed for efficient world editing.");
			this.config.getConfigExact().addDefault("privateservers.plugins.worldedit.item", Material.WOOD_PICKAXE.name());
			this.config.getConfigExact().addDefault("privateservers.plugins.worldedit.url", "https://ci.athion.net/job/FastAsyncWorldEdit/262/artifact/artifacts/FastAsyncWorldEdit-Bukkit-2.4.4-SNAPSHOT-262.jar");
			this.config.getConfigExact().addDefault("privateservers.plugins.worldedit.file", "FAWE.jar");
			
		}
		
		this.config.getConfigExact().options().copyDefaults(true);
		
		this.config.save();
	}
	
	private void initializeMySQL() {
		this.mySQLProvider = new MySQL(this.config.getString("mysql.host"), this.config.getString("mysql.database"), this.config.getString("mysql.user"), this.config.getString("mysql.password"));
		this.mySQLProvider.connect();
		
		this.getMySQLInstance().update("CREATE TABLE IF NOT EXISTS `privateservers` (`id` INT AUTO_INCREMENT, `owner` VARCHAR(128) NOT NULL, `properties` TEXT, `static` BOOLEAN, `templates` VARCHAR(128), PRIMARY KEY (`id`), UNIQUE (`owner`));");
		this.getMySQLInstance().update("CREATE TABLE IF NOT EXISTS `tickets` (`id` INT AUTO_INCREMENT, `player` VARCHAR(128) NOT NULL, `properties` TEXT, PRIMARY KEY (`id`), UNIQUE (`player`));");
		Bukkit.getConsoleSender().sendMessage(this.getPrefix() + "The mysql database was initialized.");
	}
	
	public void setupUi() {
		if(!this.config.getConfigExact().isSet("villager.world")) return;
		
		Location location = new Location(
				Bukkit.getWorld(this.config.getString("villager.world")),
				this.config.getDouble("villager.posX"),
				this.config.getDouble("villager.posY"),
				this.config.getDouble("villager.posZ"),
				(float) this.config.getDouble("villager.yaw"),
				(float) this.config.getDouble("villager.pitch"));
			
			this.setUi(new PrivateServerUIHandler(location));

	}
	
	public JsonObject requestServiceProperties(ServiceInfoSnapshot target) {
		JsonObject properties = null;
		List<ChannelMessage> responses = new ArrayList<>(this.sendQuery(JsonDocument.newDocument().append("request", "startup_properties").append("target", target.getName())));
		ChannelMessage msg = responses.size() > 0 ? responses.get(0) : null;
		if(msg != null && msg.getJson().contains("properties")) properties = JsonParser.parseString(msg.getJson().getString("properties")).getAsJsonObject();
		return properties;
	}
	
	private Collection<ChannelMessage> sendQuery(JsonDocument data) {
		return ChannelMessage.builder().targetAll(Type.SERVICE).channel("private_server").message("send_query").json(data).build().sendQuery();
	}
	
	public static void connect(Player player, String server) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		
		try {
			out.writeUTF("Connect");
			out.writeUTF(server);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
	}
	
	public PSConfiguration getPSConfig() {
		return this.config;
	}
	
	public String getPrefix() {
		if(this.config == null || !this.config.getConfigExact().isSet("plugin.prefix")) return "[PrivateServer] ";
		return ChatColor.translateAlternateColorCodes('&', this.getPSConfig().getString("plugin.prefix")) + " §7";
	}
	
	public PrivateServerUIHandler getUi() {
		return ui;
	}

	public void setUi(PrivateServerUIHandler ui) {
		this.ui = ui;
	}
	
	public PluginHandler getPluginHandler() {
		return this.pluginHandler;
	}

	public MySQL getMySQLInstance() {
		return this.mySQLProvider;
	}
	
	public TemplateHandler getTemplateHandler() {
		return this.templateHandler;
	}
	
	public PrivateServerHandler getPrivateServerHandler() {
		return this.serverHandler;
	}
	
	public TicketSystem getTicketSystem() {
		return this.ticketSystem;
	}
	
	public Map<String, String> getConfigurationOf(Player player) {
		if(this.playerConfiguration == null) this.playerConfiguration = new HashMap<>();
		if(!this.playerConfiguration.containsKey(player.getName())) this.playerConfiguration.put(player.getName(), new HashMap<>());
		return this.playerConfiguration.get(player.getName());
	}
	
	public static PrivateServer getInstance() {
		return plugin;
	}
	
}
