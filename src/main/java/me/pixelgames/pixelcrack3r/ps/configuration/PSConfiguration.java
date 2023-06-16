package me.pixelgames.pixelcrack3r.ps.configuration;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PSConfiguration {

	private FileConfiguration config;
	private final File file;
	
	public PSConfiguration(File file) {
		this.file = file;
		this.reload();
	}
	
	public String getString(String key) {
		return this.getConfigExact().isSet(key) ? ChatColor.translateAlternateColorCodes('&', this.getConfigExact().getString(key, "null")) : null;
	}
	
	public boolean getBoolean(String key) {
		return this.getConfigExact().getBoolean(key, false);
	}
	
	public boolean isSet(String key) {
		return this.getConfigExact().isSet(key);
	}
	
	public int getInt(String key) {
		return this.getConfigExact().getInt(key);
	}
	
	public int getInt(String key, int def) {
		return this.getConfigExact().getInt(key, def);
	}
	
	public double getDouble(String key) {
		return this.getConfigExact().getDouble(key);
	}
	
	public long getLong(String key) {
		return this.getConfigExact().getLong(key);
	}
	
	public void set(String key, Object value) {
		this.getConfigExact().set(key, value);
	}
	
	public FileConfiguration getConfigExact() {
		return this.config;
	}
	
	public void reload() {
		this.config = YamlConfiguration.loadConfiguration(this.file);
	}
	
	public void save() {
		try {
			this.config.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
