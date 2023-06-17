package me.pixelgames.pixelcrack3r.ps.cloudlisteners;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import me.pixelgames.pixelcrack3r.ps.objects.PrivateServerService;

public class ChannelMessageReceiveListener {

	@EventListener
	public void onChannelMessageReceive(ChannelMessageReceiveEvent e) {
		if(!e.channel().equalsIgnoreCase("private_server")) return;
		
		if(e.message().equalsIgnoreCase("send_query")) {
			JsonDocument data = JsonDocument.fromJsonString(e.content().readString());
			if(data.getString("request").equalsIgnoreCase("startup_properties")) {
				String target = data.getString("target", e.sender().name());
				PrivateServerService service = PrivateServer.getInstance().getPrivateServerHandler().getPrivateServer(target);
				if(service == null) return;
				service.setWrapperActive(true);
				e.queryResponse(ChannelMessage.buildResponseFor(e.channelMessage()).buffer(DataBuf.empty().writeString(JsonDocument.newDocument().append("properties", service.buildStartupProperties().toString()).toString())).build());
			}
		}
	}
}
