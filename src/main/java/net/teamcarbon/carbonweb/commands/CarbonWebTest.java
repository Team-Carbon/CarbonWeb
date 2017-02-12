package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class CarbonWebTest implements CommandExecutor {

	private CarbonWeb plugin;
	public CarbonWebTest(CarbonWeb p) { plugin = p; }

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!plugin.perm.has(sender, "carbonweb.debug")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
			return true;
		}

		if (args.length < 0) { return false; }

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Must be in game to do that!");
			return true;
		}

		Player target = (Player) sender;

		switch (args[0].toLowerCase(Locale.ENGLISH)) {
			case "vote":

				if (args.length > 1) { target = Bukkit.getPlayer(args[1]); }

				if (target != null && target.isOnline()) {
					plugin.addVote(target);
					sender.sendMessage(ChatColor.AQUA + "Executed vote");
				} else {
					sender.sendMessage(ChatColor.RED + "Player not found (must be online)");
				}
				return true;

			case "count":

				int count;

				if (args.length > 1) {
					try {
						count = Integer.parseInt(args[1]);
					} catch(Exception e) {
						sender.sendMessage(ChatColor.RED + "Invalid count: " + args[1]);
						return true;
					}
				} else {
					sender.sendMessage(ChatColor.RED + "Must provide a count");
					return true;
				}

				if (args.length > 2) { target = Bukkit.getPlayer(args[2]); }

				if (target != null && target.isOnline()) {
					plugin.testCount(target, count);
					sender.sendMessage(ChatColor.AQUA + "Executed count test. (Count will not be saved)");
				} else {
					sender.sendMessage(ChatColor.RED + "Player not found (must be online)");
				}

				return true;

			default:
				sender.sendMessage(ChatColor.RED + "Unknown argument: " + args[0]);
		}

		return true;
	}
}
