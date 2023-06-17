package me.pixelgames.pixelcrack3r.ps.configuration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;

public class ServerConfiguration {

	private String templates;
	
	private boolean isStatic;
	
	private JsonObject properties;

	public ServerConfiguration() {
		this("none", true, new JsonObject());
	}
	
	public ServerConfiguration(String template, boolean isStatic) {
		this(template, isStatic, JsonParser.parseString("{}").getAsJsonObject());
	}
	
	public ServerConfiguration(String template, boolean isStatic, JsonDocument doc) {
		this(template, isStatic, JsonParser.parseString(doc.toString()).getAsJsonObject());
	}
	
	public ServerConfiguration(String template, boolean isStatic, JsonObject properties) {
		this.templates = template;
		this.isStatic = isStatic;
		this.properties = properties;
	}

	public String getTemplate() {
		return templates;
	}

	public void setTemplate(String template) {
		this.templates = template;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isTemplate() {
		return this.templates != null && PrivateServer.getInstance().getTemplateHandler().templateExists(templates);
	}
	
	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public JsonObject getProperties() {
		return properties;
	}

	public void setProperties(JsonObject properties) {
		this.properties = properties;
	}
	
}
