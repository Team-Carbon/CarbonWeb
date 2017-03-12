package net.teamcarbon.carbonweb.commands;

import com.michaelwflaherty.cleverbotapi.CleverBotQuery;
import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CarbonWebCleverBot implements CommandExecutor {

	private CarbonWeb plugin;

	public CarbonWebCleverBot(CarbonWeb p) { plugin = p; }

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!plugin.perm.has(sender, "carbonweb.cleverbot")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
			return true;
		}
		if (args.length < 1) {
			sender.sendMessage(ChatColor.RED + "You must say something! (/cb phrase)");
			return true;
		}
		String query = String.join(" ", args);
		try {
			CleverBotQuery bot = new CleverBotQuery(plugin.getConfig().getString("discord.cleverbot-api-key"), query);
			bot.sendRequest();
			String response = bot.getResponse();
			sender.sendMessage(ChatColor.AQUA + "CleverBot > " + ChatColor.GREEN + response);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.AQUA + "CleverBot > " + ChatColor.RED + "Sorry! Lost track of the conversation.");
		}
		return true;
	}

}
