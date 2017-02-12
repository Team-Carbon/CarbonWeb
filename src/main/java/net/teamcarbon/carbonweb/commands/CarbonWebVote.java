package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CarbonWebVote implements CommandExecutor {

	private CarbonWeb plugin;
	public CarbonWebVote(CarbonWeb p) { plugin = p; }

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!plugin.perm.has(sender, "carbonweb.vote")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
			return true;
		}

		List<String> sites = plugin.getConfig().getStringList("vote-data.vote-sites");
		if (sites != null && !sites.isEmpty()) {
			sender.sendMessage(ChatColor.AQUA + "Current vote sites:");
			for(String site : sites) { sender.sendMessage(ChatColor.GOLD + site); }
		}

		if (sender instanceof Player) {
			Player p = (Player) sender;
			int votes = plugin.getVotes(p);
			sender.sendMessage(ChatColor.AQUA + "You have voted " + ChatColor.GREEN + votes + ChatColor.AQUA + " times!");
		}

		return true;
	}

}
