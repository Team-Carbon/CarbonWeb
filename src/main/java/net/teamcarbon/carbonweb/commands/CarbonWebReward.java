package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CarbonWebReward implements CommandExecutor {

	private CarbonWeb plugin;

	public CarbonWebReward(CarbonWeb p) {
		plugin = p;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		sender.sendMessage(ChatColor.RED + "Not yet implemented!");

		return true;
	}

}
