package net.teamcarbon.carbonweb.commands;

import net.milkbowl.vault.permission.Permission;
import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class CarbonWebLink implements CommandExecutor {

	private CarbonWeb plugin;
	public CarbonWebLink(CarbonWeb p) { plugin = p; }

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		Permission perm = plugin.perm;

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "You must be in-game to do that!");
			return true;
		}

		Player p = (Player) sender;

		if (args.length > 0) {
			// Valid key chars = ABCDEFGHKLMNOPQRSTWXYZ234679
			String pat = "^[A-HK-TW-Z2-4679]{5}$";
			String key = args[0].toUpperCase(Locale.ENGLISH);
			if (!key.matches(pat)) {
				sender.sendMessage(ChatColor.RED + "This is not a valid key! Make sure it's typed correctly!");
			} else {

				if (CarbonWeb.linkKeyExists(key)) {
					plugin.linkDiscordUser(p, CarbonWeb.getLinkUserFromKey(key));
					CarbonWeb.removeLinkKey(key);
					sender.sendMessage(ChatColor.AQUA + "Your Discord account has been linked to your Minecraft account!");
					return true;
				}

				try {

					ResultSet res = plugin.execq(plugin.f("SELECT COUNT(*) FROM users WHERE unique_key='%s'", key));
					if (res != null) {
						res.first();
						int count = res.getInt(1);
						res.close();
						if (count < 1) {
							sender.sendMessage(ChatColor.RED + "Couldn't find that key! Make sure it's typed correctly!");
							return true;
						} else if (count > 1) {
							sender.sendMessage(ChatColor.RED + "There seems to be duplicate keys. Let an admin know!");
							plugin.getLogger().warning(plugin.f("Found duplicate key (%s) in users database when trying to link with %s's account", key, p.getName()));
							return true;
						}
					} else {
						sender.sendMessage(ChatColor.RED + "Encountered an error querying the database. Let an admin know!");
						return true;
					}

					String username = "", userid = "";
					res = plugin.execq(plugin.f("SELECT user_id,user_name FROM users WHERE unique_key='%s'", key));
					if (res != null) {
						res.first();
						username = res.getString("user_name");
						userid = res.getString("user_id");
						res.close();
					}

					String uuid = p.getUniqueId().toString().replace("-", "");
					String name = p.getName();
					String group = "default";
					int auth = 254;

					if (perm != null && perm.hasGroupSupport()) {
						String g = perm.getPrimaryGroup(p).toLowerCase(Locale.ENGLISH);
						auth = plugin.getConfig().getInt("auth-levels." + g, 254);
					}

					int updated = plugin.execu(plugin.f("UPDATE users SET minecraft_id='%s',minecraft_name='%s',auth_level='%d',unique_key='' WHERE unique_key='%s'", uuid, name, auth, key));
					if (updated > 0) {
						sender.sendMessage(ChatColor.AQUA + "Your Minecraft account has been linked to " + username + "'s account (ID: " + userid + ")");
					} else {
						sender.sendMessage(ChatColor.RED + "There was an error linking your account. Let an admin know!");
						plugin.getLogger().warning(plugin.f("An error occured trying to link minecraft account (%s / %s) with web account, no rows were updated.", p.getName(), p.getUniqueId().toString()));
					}
					return true;

				} catch (SQLException e) {
					sender.sendMessage(ChatColor.RED + "An error occured. Let an admin know!");
					plugin.getLogger().warning(plugin.f("An error occured trying to link minecraft account (%s / %s) with web account, an exception occured.", p.getName(), p.getUniqueId().toString()));
					e.printStackTrace();
					return true;
				}

			}

		}

		// Display help if not returned by now

		sender.sendMessage(ChatColor.GRAY + "Visit " + ChatColor.GOLD + "http://team-carbon.net/?p=link" + ChatColor.GRAY + " for instructions!");

		return true;
	}

}
