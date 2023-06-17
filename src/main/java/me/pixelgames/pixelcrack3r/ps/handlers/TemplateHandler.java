package me.pixelgames.pixelcrack3r.ps.handlers;

import java.util.Set;

import eu.cloudnetservice.driver.service.ServiceTemplate;
import me.pixelgames.pixelcrack3r.ps.configuration.ServerConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import com.google.gson.JsonObject;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;

public class TemplateHandler {

	public ServiceTemplate getTemplate(String template) {
		String sectionKey = this.getTemplateKey(template);
		
		// boolean isStatic = PrivateServer.getInstance().getPSConfig().getBoolean("privateservers.templates." + sectionKey + ".static");
		
		String prefix = PrivateServer.getInstance().getPSConfig().getString("privateservers.templates." + sectionKey + ".prefix");
		String templateName = PrivateServer.getInstance().getPSConfig().getString("privateservers.templates." + sectionKey + ".template_name");
		String storage = PrivateServer.getInstance().getPSConfig().getString("privateservers.templates." + sectionKey + ".storage");
		
		return new ServiceTemplate.Builder()
				.prefix(prefix)
				.name(templateName)
				.storage(storage)
				.build();
	}
	
	public ServerConfiguration getServerConfiguration(String template) {
		String sectionKey = this.getTemplateKey(template);
		
		boolean isStatic = PrivateServer.getInstance().getPSConfig().getBoolean("privateservers.templates." + sectionKey + ".static");
		
		String javaCommand = PrivateServer.getInstance().getPSConfig().getString("privateservers.templates." + sectionKey + ".javaCommand");
		String runtime = PrivateServer.getInstance().getPSConfig().getString("privateservers.templates." + sectionKey + ".runtime");
		
		int maxHeapMemory = PrivateServer.getInstance().getPSConfig().getInt("privateservers.templates." + sectionKey + ".maxHeapMemory");
		
		JsonObject properties = new JsonObject();
		if(javaCommand != null) properties.addProperty("javaCommand", javaCommand);
		if(runtime != null) properties.addProperty("runtime", runtime);
		if(maxHeapMemory != 0) properties.addProperty("maxHeapMemory", maxHeapMemory);
		
		return new ServerConfiguration(sectionKey, isStatic, properties);
	}
	
	public boolean templateExists(String template) {
		return this.getTemplateKey(template) != null;
	}
	
	public String getTemplateKey(String template) {
		if(template == null || template.equalsIgnoreCase("none")) return null;
		if(this.getTemplates().contains(template)) return template;
		if(!PrivateServer.getInstance().getPSConfig().isSet("privateservers.templates." + template.trim() + ".name")) {
			ConfigurationSection section = PrivateServer.getInstance().getPSConfig().getConfigExact().getConfigurationSection("privateservers.templates");
			for(String key : section.getKeys(false)) {
				if(ChatColor.translateAlternateColorCodes('&', section.getString(key + ".name")).equalsIgnoreCase(template)) {
					return key;
				}
			}
		}
		return null;
	}
	
	public String getTemplateName(String key) {
		return PrivateServer.getInstance().getPSConfig().getString("privateservers.templates." + key.trim() + ".name");
	}
	
	public Set<String> getTemplates() {
		ConfigurationSection section = PrivateServer.getInstance().getPSConfig().getConfigExact().getConfigurationSection("privateservers.templates");
		return section.getKeys(false);
	}
	
}
