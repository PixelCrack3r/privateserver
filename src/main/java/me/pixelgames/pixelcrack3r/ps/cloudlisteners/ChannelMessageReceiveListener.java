package me.pixelgames.pixelcrack3r.ps.cloudlisteners;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;
import me.pixelgames.pixelcrack3r.ps.objects.PrivateServerService;
import org.jetbrains.annotations.NotNull;

public class ChannelMessageReceiveListener {

	@EventListener
	public void onChannelMessageReceive(@NotNull ChannelMessageReceiveEvent e) {
		if(e.getMessage() == null) return;
		if(!e.getChannel().equalsIgnoreCase("private_server")) return;
		
		if(e.getMessage().equalsIgnoreCase("send_query")) {
			if(e.getData().getString("request").equalsIgnoreCase("startup_properties")) {
				String target = e.getData().getString("target", e.getSender().getName());
				PrivateServerService service = PrivateServer.getInstance().getPrivateServerHandler().getPrivateServer(target);
				if(service == null) return;
				service.setWrapperActive(true);
				e.setQueryResponse(ChannelMessage.buildResponseFor(e.getChannelMessage()).json(JsonDocument.newDocument().append("properties", service.buildStartupProperties().toString())).build());
			}
		}
	}
}
