package me.pixelgames.pixelcrack3r.ps.listeners;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonObject;

import me.pixelgames.pixelcrack3r.ps.configuration.ServerConfiguration;
import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import me.pixelgames.pixelcrack3r.ps.objects.PrivateServerService;
import gq.pixelgames.pixelcrack3r.utils.ActionBar;

public class OnPlayerInventoryClickListener implements Listener {

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		
		if(e.getClickedInventory() == null || e.getClickedInventory().getTitle() == null) return;
		
		String invname = e.getClickedInventory().getTitle();
		List<String> paramName = Arrays.asList(invname.split(" "));
		String modinvname = String.join(" ", paramName.subList(0, paramName.size() - 1));
		
		if(e.getWhoClicked() instanceof Player && e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta() && (PrivateServer.getInstance().getUi().getPluginInventories().contains(invname) || PrivateServer.getInstance().getUi().getPluginInventories().contains(modinvname))) {
			e.setCancelled(true);
			Player player = (Player) e.getWhoClicked();
			ItemStack clickedItem = e.getCurrentItem();
			PrivateServerService server = PrivateServer.getInstance().getPrivateServerHandler().getPrivateServer(player.getName());
			Inventory inventory = e.getClickedInventory();
			if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§aStart")) {
				player.closeInventory();
				if(server != null) {
					ActionBar.sendActionBar(player, "§cYour server has already been started.");
					player.sendMessage(PrivateServer.getInstance().getPrefix() + "§cYour server has already been started. Stop it first!");
					player.playSound(player.getLocation(), Sound.ANVIL_LAND, 20, 10);
					return;
				}
				player.sendMessage(PrivateServer.getInstance().getPrefix() + "Preparing private server for " + player.getName() + "...");
				if(!PrivateServer.getInstance().getPrivateServerHandler().start(player)) {
					player.sendMessage(PrivateServer.getInstance().getPrefix() + "Error while preparing a server! Please report this bug!");
					
				}
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§cStop")) {
				if(server == null || server.isConnected()) {
					player.closeInventory();
					ActionBar.sendActionBar(player, "§cNo private server could be found.");
					player.sendMessage(PrivateServer.getInstance().getPrefix() + "§cNo private server could be found.");
					player.playSound(player.getLocation(), Sound.ANVIL_LAND, 20, 10);
					return;
				}
				server.stop();
				player.sendMessage(PrivateServer.getInstance().getPrefix() + "Your private server is stopped.");
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§6Settings")) {
				PrivateServer.getInstance().getUi().openSettingsInv(player);
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§6Gamerules")) {
				PrivateServer.getInstance().getUi().openGameruleInv(player);
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§6Templates")) {
				PrivateServer.getInstance().getUi().openTemplatesInv(player);
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§6Plugins")) {
				PrivateServer.getInstance().getUi().openPluginsInv(player);
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§eStatic")) {
				ServerConfiguration config = PrivateServer.getInstance().getPrivateServerHandler().getServerConfiguration(player);
				config.setStatic(false);
				PrivateServer.getInstance().getPrivateServerHandler().updateServerConfiguration(player, config);
				PrivateServer.getInstance().getUi().openSettingsInv(player);
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§9Dynamic")) {
				ServerConfiguration config = PrivateServer.getInstance().getPrivateServerHandler().getServerConfiguration(player);
				config.setStatic(true);
				PrivateServer.getInstance().getPrivateServerHandler().updateServerConfiguration(player, config);
				PrivateServer.getInstance().getUi().openSettingsInv(player);
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§6Next Page")) {
				int page = 0;
				try {
					page = Integer.parseInt(inventory.getTitle().split(" ")[inventory.getTitle().split(" ").length - 1]) + 1;
				} catch (NumberFormatException ignored) {} finally {
					PrivateServer.getInstance().getUi().openServerlistPage(player, page);
					PrivateServer.getInstance().getUi().updateServerList();
				}
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§6Previous Page")) {
				int page = 1;
				try {
					page = Integer.parseInt(inventory.getTitle().split(" ")[inventory.getTitle().split(" ").length - 1]) - 2;
				} catch (Exception ignored) {} finally {
					if(page < 0)PrivateServer.getInstance().getUi().openStartStopInv(player);
					else {
						PrivateServer.getInstance().getUi().openServerlistPage(player, page);
						PrivateServer.getInstance().getUi().updateServerList();
					}
				}
			} else if(clickedItem.getItemMeta().getDisplayName().equalsIgnoreCase("§6Home")) {
				PrivateServer.getInstance().getUi().openStartStopInv(player);
			} else {
				if(clickedItem.getItemMeta().getDisplayName().startsWith("§e") && clickedItem.getType() == Material.SKULL_ITEM) {
					String serverName = clickedItem.getItemMeta().getDisplayName().substring(2);
					PrivateServer.connect(player, serverName);
					if(serverName.startsWith("PS-")) return;
				}
				
				String itemName = clickedItem.getItemMeta().getDisplayName(); // String.join(" ", Arrays.asList(clickedItem.getItemMeta().getDisplayName().split(" ")).subList(0, clickedItem.getItemMeta().getDisplayName().split(" ").length - 1));
				if(PrivateServer.getInstance().getTemplateHandler().templateExists(itemName)) {

					String templateKey = PrivateServer.getInstance().getTemplateHandler().getTemplateKey(itemName);
					
					if(server != null) {
						ActionBar.sendActionBar(player, "§cYour server has already been started.");
						player.sendMessage(PrivateServer.getInstance().getPrefix() + "§cYour server has already been started. Stop it first to make changes!");
						return;
					}
					
					ServerConfiguration config = PrivateServer.getInstance().getPrivateServerHandler().getServerConfiguration(player);
					if(!config.getTemplate().equalsIgnoreCase(templateKey)) {
						config.setTemplate(templateKey);	
						player.sendMessage(PrivateServer.getInstance().getPrefix() + "§7You changed the template to §r" + itemName + "§7.");
					} else {
						config.setTemplate("none");
						player.sendMessage(PrivateServer.getInstance().getPrefix() + "§7You removed your current template setting.");
					}
					PrivateServer.getInstance().getPrivateServerHandler().updateServerConfiguration(player, config);
					PrivateServer.getInstance().getUi().openTemplatesInv(player);
				}
				
				if(PrivateServer.getInstance().getPluginHandler().pluginExists(itemName)) {
					String pluginKey = PrivateServer.getInstance().getPluginHandler().getPluginKey(itemName);
					
					if(server != null) {
						ActionBar.sendActionBar(player, "§cYour server has already been started.");
						player.sendMessage(PrivateServer.getInstance().getPrefix() + "§cYour server has already been started. Stop it first to make changes!");
						return;
					}
					
					ServerConfiguration config = PrivateServer.getInstance().getPrivateServerHandler().getServerConfiguration(player);
					if(!config.getProperties().has("plugins")) config.getProperties().add("plugins", new JsonObject());
					JsonObject pluginConfig = config.getProperties().get("plugins").getAsJsonObject();
					
					JsonObject pluginData = PrivateServer.getInstance().getPluginHandler().getPluginData(pluginKey);
					
					if(pluginConfig.has(pluginKey) && pluginConfig.get(pluginKey).getAsJsonObject().get("installed").getAsBoolean()) {
						pluginData.addProperty("installed", false);
						pluginConfig.remove(pluginKey);
						pluginConfig.add(pluginKey, pluginData);
						player.sendMessage(PrivateServer.getInstance().getPrefix() + "§7You removed the " + itemName + " §7plugin from your server.");
					} else {
						pluginData.addProperty("installed", true);
						pluginConfig.remove(pluginKey);
						pluginConfig.add(pluginKey, pluginData);
						player.sendMessage(PrivateServer.getInstance().getPrefix() + "§7You installed the " + itemName + " §7plugin to your server.");
					}
					
					PrivateServer.getInstance().getPrivateServerHandler().updateServerConfiguration(player, config);
					PrivateServer.getInstance().getUi().openPluginsInv(player);

				}
			}
		} else if(e.getClickedInventory().getType() == InventoryType.ANVIL) {
			AnvilInventory inventory = (AnvilInventory) e.getClickedInventory();
			if(inventory.getName().equalsIgnoreCase("§7Type in a service name")) {
				e.setCancelled(true);
				Player player = (Player) e.getWhoClicked();
				ItemStack clickedItem = e.getCurrentItem();
				
				if(clickedItem.hasItemMeta() && clickedItem.getType() == Material.SKULL_ITEM) {
					PrivateServer.getInstance().getConfigurationOf(player).put("server.start.owner", clickedItem.getItemMeta().getDisplayName());
					PrivateServer.getInstance().getUi().openAdminStartInv(player);
				}
			}
		}
	}
}
