package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CarbonWebReload implements CommandExecutor {

	private CarbonWeb plugin;
	public CarbonWebReload(CarbonWeb p) { plugin = p; }

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!plugin.perm.has(sender, "carbonweb.reload")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
			return true;
		}

		plugin.reload();
		sender.sendMessage("Reloaded " + plugin.getDescription().getName());

		return true;
	}

}
