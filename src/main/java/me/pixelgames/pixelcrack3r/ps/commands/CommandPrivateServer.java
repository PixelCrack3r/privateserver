package me.pixelgames.pixelcrack3r.ps.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.pixelgames.pixelcrack3r.ps.main.PrivateServer;

public class CommandPrivateServer implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			if(player.hasPermission("privateserver.command")) {
				if(args.length > 0 && args[0].equalsIgnoreCase("setvillager") && player.hasPermission("privateserver.command.setvillager")) {
					PrivateServer.getInstance().getPSConfig().set("villager.world", player.getLocation().getWorld().getName());
					PrivateServer.getInstance().getPSConfig().set("villager.posX", player.getLocation().getX());
					PrivateServer.getInstance().getPSConfig().set("villager.posY", player.getLocation().getY());
					PrivateServer.getInstance().getPSConfig().set("villager.posZ", player.getLocation().getZ());
					PrivateServer.getInstance().getPSConfig().set("villager.yaw", player.getLocation().getYaw());
					PrivateServer.getInstance().getPSConfig().set("villager.pitch", player.getLocation().getPitch());
					
					PrivateServer.getInstance().getPSConfig().save();
					
					PrivateServer.getInstance().setupUi();
					
					player.sendMessage(PrivateServer.getInstance().getPrefix() + "You set the villager position to §e" + player.getLocation().toString() + "§7.");
				} else {
					PrivateServer.getInstance().getUi().openStartStopInv(player);
				}
			} else player.sendMessage("§cYou don't have the permission to execute this command.");
			
		}
		return true;
	}
	
}
