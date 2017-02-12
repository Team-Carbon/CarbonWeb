package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class CarbonWebVote implements CommandExecutor {

	private CarbonWeb plugin;
	public CarbonWebVote(CarbonWeb p) { plugin = p; }

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!plugin.perm.has(sender, "carbonweb.vote")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
			return true;
		}

		sender.sendMessage(ChatColor.GRAY + "+====================+");
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("vote-data.vote-description", "")));

		Set<String> sites = plugin.getConfig().getConfigurationSection("vote-data.vote-sites").getKeys(false);
		if (sites != null && !sites.isEmpty()) {
			for(String site : sites) {
				String val = plugin.getConfig().getString("vote-data.vote-sites." + site, "");
				sender.sendMessage(ChatColor.GRAY + site + ": " + ChatColor.GOLD + val);
			}
			sender.sendMessage(ChatColor.GRAY + "+--------------------+");
		}


		if (sender instanceof Player) {
			Player p = (Player) sender;
			int votes = plugin.getVotes(p);
			sender.sendMessage(ChatColor.AQUA + "You have voted " + ChatColor.GREEN + votes + ChatColor.AQUA + " times!");
		}

		sender.sendMessage(ChatColor.GRAY + "+====================+");

		return true;
	}

}
