package me.pixelgames.pixelcrack3r.ps.handlers;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import com.google.gson.JsonObject;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;

public class PluginHandler {
	
	public boolean pluginExists(String key) {
		return this.getPluginKey(key) != null;
	}
	
	public String getPluginKey(String plugin) {
		if(plugin == null || plugin.equalsIgnoreCase("none")) return null;
		if(this.getPlugins().contains(plugin)) return plugin;
		if(!PrivateServer.getInstance().getPSConfig().isSet("privateservers.plugins." + plugin + ".name")) {
			ConfigurationSection section = PrivateServer.getInstance().getPSConfig().getConfigExact().getConfigurationSection("privateservers.plugins");
			for(String key : section.getKeys(false)) {
				if(ChatColor.translateAlternateColorCodes('&', section.getString(key + ".name")).equalsIgnoreCase(plugin)) {
					return key;
				}
			}
		}
		return null;
	}
	
	public String getPluginName(String key) {
		return PrivateServer.getInstance().getPSConfig().getString("privateservers.plugins." + key + ".name");
	}
	
	public JsonObject getPluginData(String key) {
		String name = ChatColor.stripColor(this.getPluginName(key));
		String url = PrivateServer.getInstance().getPSConfig().getConfigExact().getString("privateservers.plugins." + key + ".url");
		String file = PrivateServer.getInstance().getPSConfig().getConfigExact().getString("privateservers.plugins." + key + ".file");
		
		JsonObject data = new JsonObject();
		data.addProperty("name", name);
		data.addProperty("url", url);
		data.addProperty("file", file);
		
		return data;
	}
	
	public Set<String> getPlugins() {
		ConfigurationSection section = PrivateServer.getInstance().getPSConfig().getConfigExact().getConfigurationSection("privateservers.plugins");
		return section.getKeys(false);
	}
	
}
