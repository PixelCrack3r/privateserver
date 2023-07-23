package me.pixelgames.pixelcrack3r.ps.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import me.pixelgames.pixelcrack3r.ps.configuration.ServerConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonObject;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import me.pixelgames.pixelcrack3r.ps.objects.PrivateServerService;
import me.pixelgames.pixelcrack3r.ps.utils.FormatHelper;
import gq.pixelgames.pixelcrack3r.utils.AdvancedEntityModifier;
import gq.pixelgames.pixelcrack3r.utils.ItemGenerator;

public class PrivateServerUIHandler {

	private final Location location;
	private final Entity villager;
	
	private final List<Inventory> serverlist;
	
	public PrivateServerUIHandler(Location location) {
		this.location = location;
		location.getWorld().getNearbyEntities(location, 3, 3, 3).forEach(Entity::remove);
		location.getWorld().loadChunk(location.getWorld().getChunkAt(location));
		
		this.villager = this.location.getWorld().spawnEntity(this.location, EntityType.VILLAGER);
		
		AdvancedEntityModifier.modify(this.villager, PrivateServer.getInstance().getPlugin())
			.setCanDespawn(false)
			.setCanPickUpLoot(false)
			.setDisplayName(PrivateServer.getInstance().getPSConfig().getString("villager.name"))
			.setDisplayNameVisible(true)
			.setHealth(20F)
			.setFireTicks(0)
			.setInvulnerable(true)
			.setSpeed(0.0F)
			.setNoAI(true)
		.build();
	
		this.serverlist = new ArrayList<>();
		
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 1));
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 2));
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 3));
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 4));
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 5));
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 6));
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 7));
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 8));
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 9));
		this.serverlist.add(Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.serverlist.name") + " " + 10));
	}
	
	public void openStartStopInv(Player player) {
		PrivateServerService server = PrivateServer.getInstance().getPrivateServerHandler().getPrivateServer(player.getName());
		
		List<String> lore = new ArrayList<>();
		if(server != null && server.isRunning()) {
			lore = Arrays.asList("§7- §6Mode§7: " + (!server.getServerConfiguration().getTemplate().equalsIgnoreCase("none") ? ChatColor.stripColor(PrivateServer.getInstance().getTemplateHandler().getTemplateName(server.getServerConfiguration().getTemplate())) : "Survival"), "§7- §6Players§7: " + server.getServiceInfo().readProperty(BridgeDocProperties.ONLINE_COUNT));
		}
		
		Inventory inv = Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.cp.name"));
		
		inv.setItem(13, ItemGenerator.modify().setItemStack(new ItemStack(Material.SKULL_ITEM, 1, (byte)3)).setOwner(player.getName()).setDisplayName((server == null || !server.isConnected() ? "§cOffline" : "§e" + server.getName())).setLore(lore).build());
		inv.setItem(29, ItemGenerator.modify().setItemStack(new ItemStack(Material.INK_SACK, 1, (byte)2)).setDisplayName("§aStart").build());
		inv.setItem(33, ItemGenerator.modify().setItemStack(new ItemStack(Material.INK_SACK, 1, (byte)1)).setDisplayName("§cStop").build());
		inv.setItem(40, ItemGenerator.modify().setItemStack(new ItemStack(Material.ANVIL, 1, (byte)0)).setDisplayName("§6Settings").build());
		
		if(PrivateServer.getInstance().getPrivateServerHandler().getPublicServices().size() > 0)
			inv.setItem(53, ItemGenerator.modify().setItemStack(new ItemStack(Material.PAPER, 1, (byte)0)).setDisplayName("§6Next Page").build());
		
		for(int i = 0; i < inv.getSize(); i++) {
			if(inv.getItem(i) == null) inv.setItem(i, ItemGenerator.generatePlaceHolder());
		}
		
		player.openInventory(inv);
	}
	
	public void openSettingsInv(Player player) {
		ServerConfiguration config = PrivateServer.getInstance().getPrivateServerHandler().getServerConfiguration(player);
		
		if(config == null) return;
		
		Inventory inv = Bukkit.createInventory(null, 9*3, PrivateServer.getInstance().getPSConfig().getString("inventory.settings.name"));
		
		inv.setItem(10, ItemGenerator.modify().setItemStack(new ItemStack((config.isStatic() ? Material.BUCKET : Material.LAVA_BUCKET), 1, (byte)0)).setDisplayName(config.isStatic() ? "§eStatic" : "§9Dynamic").setLore(Arrays.asList("§7Choose whether the server", "§7should be deleted after", "§7the stop or should", "§7be saved")).build());
		inv.setItem(12, ItemGenerator.modify().setItemStack(new ItemStack(Material.SKULL_ITEM, 1, (byte)3)).setOwner("mattijs").setDisplayName("§6Templates").setLore(Arrays.asList("§7Choose a template which", "§7should be load on your", "§7current private server.", "§cWarning: The template will", "§coverride your current", "§csettings and modify", "§cyour permissions!")).build());
		inv.setItem(14, ItemGenerator.modify().setItemStack(new ItemStack(Material.SKULL_ITEM, 1, (byte)3)).setOwner("Zen9400").setDisplayName("§6Plugins").setLore(Arrays.asList("§7Choose some plugins and", "§7install them on your", "§7server to improve your", "§7experience.")).build());
		inv.setItem(16, ItemGenerator.modify().setItemStack(new ItemStack(Material.SKULL_ITEM, 1, (byte)3)).setOwner("Kevos").setDisplayName("§6Gamerules").setLore(Arrays.asList("§7Change the gamerules on", "§7your server. This is", "§7possible while the server", "§7is running as well.")).build());
		
		for(int i = 0; i < inv.getSize(); i++) {
			if(inv.getItem(i) == null) inv.setItem(i, ItemGenerator.generatePlaceHolder());
		}
		
		this.addPageHome(inv);
	
		player.openInventory(inv);
	}
	
	public void openTemplatesInv(Player player) {
		Inventory inventory = Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.templates.name"));
	
		for(int j = 0; j < inventory.getSize(); j++) {
			inventory.setItem(j, ItemGenerator.generatePlaceHolder());
		}
		
		ServerConfiguration config = PrivateServer.getInstance().getPrivateServerHandler().getServerConfiguration(player);
		
		List<String> templates = new ArrayList<>(PrivateServer.getInstance().getTemplateHandler().getTemplates());
		
		for(int i = 0; i < templates.size(); i++) {
			String template = templates.get(i);
			
			String itemName = PrivateServer.getInstance().getPSConfig().getString("privateservers.templates." + template + ".name");
			Material material = Material.valueOf(PrivateServer.getInstance().getPSConfig().getString("privateservers.templates." + template + ".item"));
	
			List<String> lore = new ArrayList<>();
			lore.add("§7Enabled: " + (config.getTemplate().equalsIgnoreCase(template) ? "§aYes" : "§cNo"));
			lore.add("&8&m-----------------------");
			lore.addAll(FormatHelper.nextLine(PrivateServer.getInstance().getPSConfig().getString("privateservers.templates." + template + ".description"), 23));
			lore.add("&8&m-----------------------");
			
			inventory.setItem(PrivateServer.getInstance().getPSConfig().getInt("privateserver.templates." + template + ".slot", i + 9), ItemGenerator.modify().setItemStack(new ItemStack(material, 1, (byte) 0)).setDisplayName(itemName).setLore(lore).build());
			
			if(i >= inventory.getSize() - 10) break;
		}
		
		this.addPageSettings(inventory);
		
		player.openInventory(inventory);
	}
	
	public void openPluginsInv(Player player) {
		Inventory inventory = Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.plugins.name"));
		
		for(int j = 0; j < inventory.getSize(); j++) {
			inventory.setItem(j, ItemGenerator.generatePlaceHolder());
		}
		
		ServerConfiguration config = PrivateServer.getInstance().getPrivateServerHandler().getServerConfiguration(player);
		JsonObject cfgPlugins = config.getProperties().has("plugins") ? config.getProperties().get("plugins").getAsJsonObject() : new JsonObject();
		
		List<String> plugins = new ArrayList<>(PrivateServer.getInstance().getPluginHandler().getPlugins());
		for(int i = 0; i < plugins.size(); i++) {
			String plugin = plugins.get(i);
			
			String itemName = PrivateServer.getInstance().getPSConfig().getString("privateservers.plugins." + plugin + ".name");
			Material material = Material.valueOf(PrivateServer.getInstance().getPSConfig().getString("privateservers.plugins." + plugin + ".item"));
	
			List<String> lore = new ArrayList<>();
			lore.add("§7Installed: " + (cfgPlugins.has(plugin) && cfgPlugins.get(plugin).getAsJsonObject().get("installed").getAsBoolean() ? "§aYes" : "§cNo"));
			lore.add("&8&m-----------------------");
			lore.addAll(FormatHelper.nextLine(PrivateServer.getInstance().getPSConfig().getString("privateservers.plugins." + plugin + ".description"), 23));
			lore.add("&8&m-----------------------");
			
			inventory.setItem(PrivateServer.getInstance().getPSConfig().getInt("privateserver.plugins." + plugin + ".slot", i + 9), ItemGenerator.modify().setItemStack(new ItemStack(material, 1, (byte) 0)).setDisplayName(itemName).setLore(lore).build());
			
			if(i >= inventory.getSize() - 10) break;
		}
		
		this.addPageSettings(inventory);
		
		player.openInventory(inventory);
	}
	
	public void openGameruleInv(Player player) {
		Inventory inventory = Bukkit.createInventory(null, 9*6, PrivateServer.getInstance().getPSConfig().getString("inventory.gamerules.name"));
		
		for(int j = 0; j < inventory.getSize(); j++) {
			inventory.setItem(j, ItemGenerator.generatePlaceHolder());
		}
		
		this.addPageSettings(inventory);
		
		player.openInventory(inventory);
	}
	
	public void openAdminStartInv(Player player) {
		Inventory inventory = Bukkit.createInventory(null, 9, PrivateServer.getInstance().getPSConfig().getString("inventory.adminStartInv.name"));
		
		for(int i = 0; i < inventory.getSize(); i++) {
			inventory.setItem(i, ItemGenerator.generatePlaceHolder());
		}
		
		String targetService = PrivateServer.getInstance().getConfigurationOf(player).getOrDefault("server.start.owner", player.getName());
		
		inventory.setItem(2, ItemGenerator.modify().setItemStack(new ItemStack(Material.INK_SACK, 1, (byte)2)).setDisplayName("§aStart").build());
		inventory.setItem(4, ItemGenerator.modify().setItemStack(new ItemStack(Material.SKULL_ITEM)).setDisplayName("§7" + targetService).setOwner(targetService).build());
		
		this.addPageHome(inventory);
		
		player.openInventory(inventory);
	}
	
	public void updateServerList() {
		List<PrivateServerService> onlineServices = PrivateServer.getInstance().getPrivateServerHandler().getPublicServices();

		for (Inventory inventory : this.serverlist) {
			for (int j = 0; j < inventory.getSize(); j++) {
				inventory.setItem(j, ItemGenerator.generatePlaceHolder());
			}

			if (onlineServices.size() == 0)
				continue;

			for (int j = 9; j < inventory.getSize() - 9; j++) {
				if (onlineServices.size() == 0) break;

				PrivateServerService server = onlineServices.get(onlineServices.size() - 1);
				onlineServices.remove(server);
				if (server == null) continue;

				List<String> lore = new ArrayList<>();
				if (server.isRunning()) {
					lore = Arrays.asList("§7- §6Mode§7: " + (!server.getServerConfiguration().getTemplate().equalsIgnoreCase("none") ? ChatColor.stripColor(PrivateServer.getInstance().getTemplateHandler().getTemplateName(server.getServerConfiguration().getTemplate())) : "Survival"), "§7- §6Players§7: " + server.getServiceInfo().readProperty(BridgeDocProperties.ONLINE_COUNT));
				}

				inventory.setItem(j, ItemGenerator.modify().setItemStack(new ItemStack(Material.SKULL_ITEM, 1, (byte) 3)).setOwner(server.getOwner()).setDisplayName(!server.isConnected() ? "§cOffline" : "§e" + server.getName()).setLore(lore).build());
			}

			this.addPageBackwars(inventory);
			if (onlineServices.size() > 0) this.addPageForward(inventory);

		}
		
	}
	
	public void openServerlistPage(Player player, int page) {
		player.openInventory(this.serverlist.get(page));
	}
	
	public void openChooseService(Player player) {
		Inventory inventory = Bukkit.createInventory(null, InventoryType.ANVIL, "§7Type in a service name");
	
		inventory.setItem(2, ItemGenerator.modify().setItemStack(new ItemStack(Material.SKULL_ITEM)).setOwner("PixelCrack3r").build());
		
		player.openInventory(inventory);
	}
	
	public List<String> getPluginInventories() {
		List<String> inventories = new ArrayList<>();
		ConfigurationSection section = PrivateServer.getInstance().getPSConfig().getConfigExact().getConfigurationSection("inventory");
		for(String key : section.getKeys(false)) {
			inventories.add(ChatColor.translateAlternateColorCodes('&', section.getString(key + ".name")));
		}
		return inventories;
	}
	
	private void addPageForward(Inventory inventory) {
		inventory.setItem(inventory.getSize() - 1, ItemGenerator.modify().setItemStack(new ItemStack(Material.PAPER, 1, (byte)0)).setDisplayName("§6Next Page").build());
	}
	
	private void addPageBackwars(Inventory inventory) {
		inventory.setItem(inventory.getSize() - 9, ItemGenerator.modify().setItemStack(new ItemStack(Material.PAPER, 1, (byte)0)).setDisplayName("§6Previous Page").build());
	}
	
	private void addPageHome(Inventory inventory) {
		inventory.setItem(inventory.getSize() - 9, ItemGenerator.modify().setItemStack(new ItemStack(Material.PAPER, 1, (byte)0)).setDisplayName("§6Home").build());
	}
	
	private void addPageSettings(Inventory inventory) {
		inventory.setItem(inventory.getSize() - 9, ItemGenerator.modify().setItemStack(new ItemStack(Material.ANVIL, 1, (byte)0)).setDisplayName("§6Settings").build());		
	}
	
	public Entity getVillager() {
		return this.villager;
	}
	
	public Location getVillagerLocation() {
		return this.location;
	}
	
}
