package me.pixelgames.pixelcrack3r.ps.handlers;

import java.sql.ResultSet;
import java.sql.SQLException;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import org.bukkit.OfflinePlayer;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;

public class TicketSystem {

	public boolean hasTicket(OfflinePlayer player) {
		return false;
	}
	
	public JsonDocument getTickets(OfflinePlayer player) {

		try(ResultSet result = PrivateServer.getInstance().getMySQLInstance().query("SELECT * FROM `tickets` WHERE `player`='" + player.getUniqueId().toString() + "';")) {
			if(result.next()) {
				String properties = result.getString("properties");
				return JsonDocument.fromJsonString(properties);
			}

			JsonDocument doc = JsonDocument.newDocument();

			PrivateServer.getInstance().getMySQLInstance().update("INSERT INTO `tickets` (`player`, `properties`) VALUES ('" + player.getUniqueId().toString() + "', '" + doc + "')");
			return doc;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
