package me.pixelgames.pixelcrack3r.ps.handlers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.OfflinePlayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;

public class TicketSystem {

	public boolean hasTicket(OfflinePlayer player) {
		return false;
	}
	
	public JsonObject getTickets(OfflinePlayer player) {

		try(ResultSet result = PrivateServer.getInstance().getMySQLInstance().query("SELECT * FROM `tickets` WHERE `player`='" + player.getUniqueId().toString() + "';")) {
			if(result.next()) {
				String properties = result.getString("properties");
				return JsonParser.parseString(properties).getAsJsonObject();
			}
			
			JsonObject obj = new JsonObject();
			
			PrivateServer.getInstance().getMySQLInstance().update("INSERT INTO `tickets` (`player`, `properties`) VALUES ('" + player.getUniqueId().toString() + "', '" + obj + "')");
			return obj;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
