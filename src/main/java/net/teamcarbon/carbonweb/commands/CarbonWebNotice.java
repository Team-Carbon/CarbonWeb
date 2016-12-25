package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CarbonWebNotice implements CommandExecutor {
	
	private CarbonWeb plugin;
	public CarbonWebNotice(CarbonWeb p) { plugin = p; }

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!plugin.perm.has(sender, "carbonweb.notice")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
			return true;
		}

		if (args.length == 0) {

			sender.sendMessage(ChatColor.GOLD + "=====[ CarbonWeb Notice ]=====");
			sender.sendMessage(ChatColor.DARK_AQUA + "/" + label + " off" + ChatColor.GRAY + " - Disable notice");
			sender.sendMessage(ChatColor.DARK_AQUA + "/" + label + " on" + ChatColor.GRAY + " - Enable notice");
			sender.sendMessage(ChatColor.DARK_AQUA + "/" + label + " set [msg]" + ChatColor.GRAY + " - Set and enable notice");
			sender.sendMessage(ChatColor.DARK_AQUA + "/" + label + " test [on|off]" + ChatColor.GRAY + " - Toggle test mode");
			sender.sendMessage(ChatColor.GRAY + "---------");
			sender.sendMessage(ChatColor.DARK_AQUA + "Notice is currently " + (plugin.getConfig().getBoolean("json-data.notice-enabled", false) ? "Enabled" : "Disabled"));
			sender.sendMessage(ChatColor.DARK_AQUA + "Testing is currently " + (plugin.getConfig().getBoolean("json-data.testing-mode", false) ? "Enabled" : "Disabled"));
			sender.sendMessage(ChatColor.DARK_AQUA + "Current Notice: " + ChatColor.GRAY + plugin.getConfig().getString("json-data.notice-message", ""));
			return true;

		}

		if (args[0].equalsIgnoreCase("set")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Message is required! ( /" + label + " set [msg] )");
				return true;
			}
			String notice = args[1];
			for (int i = 2; i < args.length; i++) { notice += " " + args[i]; }
			plugin.getConfig().set("json-data.notice-enabled", false);
			plugin.getConfig().set("json-data.notice-message", notice);
			plugin.saveConfig();
			sender.sendMessage(ChatColor.AQUA + "Message set");
			return true;
		}

		if (args[0].equalsIgnoreCase("off")) {
			plugin.getConfig().set("json-data.notice-enabled", false);
			plugin.saveConfig();
			sender.sendMessage(ChatColor.AQUA + "Message disabled");
			return true;
		}

		if (args[0].equalsIgnoreCase("on")) {
			plugin.getConfig().set("json-data.notice-enabled", true);
			plugin.saveConfig();
			sender.sendMessage(ChatColor.AQUA + "Message enabled");
			return true;
		}

		if (args[0].equalsIgnoreCase("test")) {
			if (args.length < 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
				sender.sendMessage(ChatColor.RED + "/" + label + " test [on|off]");
				return true;
			}

			if (args[1].equalsIgnoreCase("on")) {
				plugin.getConfig().set("json-data.testing-mode", true);
				plugin.saveConfig();
				sender.sendMessage(ChatColor.AQUA + "Testing enabled");
				return true;
			}

			if (args[1].equalsIgnoreCase("off")) {
				plugin.getConfig().set("json-data.testing-mode", false);
				plugin.saveConfig();
				sender.sendMessage(ChatColor.AQUA + "Testing disabled");
				return true;
			}
		}

		sender.sendMessage(ChatColor.RED + "Unknown argument: " + args[0] + ", use /" + label + " for help.");

		return true;
	}

}
