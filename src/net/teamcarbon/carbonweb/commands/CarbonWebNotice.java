package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonlib.Misc.Messages;
import net.teamcarbon.carbonlib.Misc.Messages.Clr;
import net.teamcarbon.carbonlib.Misc.MiscUtils;
import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CarbonWebNotice implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!MiscUtils.perm(sender, "carbonweb.notice")) {
			Messages.send(sender, Messages.Message.NO_PERM);
			return true;
		}

		if (args.length == 0) {

			sender.sendMessage(Clr.GOLD + "=====[ CarbonWeb Notice ]=====");
			sender.sendMessage(Clr.DARKAQUA + "/" + label + " off" + Clr.GRAY + " - Disable notice");
			sender.sendMessage(Clr.DARKAQUA + "/" + label + " on" + Clr.GRAY + " - Enable notice");
			sender.sendMessage(Clr.DARKAQUA + "/" + label + " set [msg]" + Clr.GRAY + " - Set and enable notice");
			sender.sendMessage(Clr.DARKAQUA + "/" + label + " test [on|off]" + Clr.GRAY + " - Toggle test mode");
			sender.sendMessage(Clr.GRAY + "---------");
			sender.sendMessage(Clr.DARKAQUA + "Notice is currently " + (CarbonWeb.getConf().getBoolean("json-data.notice-enabled", false) ? "Enabled" : "Disabled"));
			sender.sendMessage(Clr.DARKAQUA + "Testing is currently " + (CarbonWeb.getConf().getBoolean("json-data.testing-mode", false) ? "Enabled" : "Disabled"));
			sender.sendMessage(Clr.DARKAQUA + "Current Notice: " + Clr.GRAY + CarbonWeb.getConf().getString("json-data.notice-message", ""));
			return true;

		}

		if (args[0].equalsIgnoreCase("set")) {
			if (args.length < 2) {
				sender.sendMessage(Clr.RED + "Message is required! ( /" + label + " set [msg] )");
				return true;
			}
			String notice = args[1];
			for (int i = 2; i < args.length; i++) { notice += " " + args[i]; }
			CarbonWeb.getConf().set("json-data.notice-enabled", false);
			CarbonWeb.getConf().set("json-data.notice-message", notice);
			CarbonWeb.saveConf();
			sender.sendMessage(Clr.AQUA + "Message set");
			return true;
		}

		if (args[0].equalsIgnoreCase("off")) {
			CarbonWeb.getConf().set("json-data.notice-enabled", false);
			CarbonWeb.saveConf();
			sender.sendMessage(Clr.AQUA + "Message disabled");
			return true;
		}

		if (args[0].equalsIgnoreCase("on")) {
			CarbonWeb.getConf().set("json-data.notice-enabled", true);
			CarbonWeb.saveConf();
			sender.sendMessage(Clr.AQUA + "Message enabled");
			return true;
		}

		if (args[0].equalsIgnoreCase("test")) {
			if (args.length < 2 || !MiscUtils.eq(args[1], "on", "off")) {
				sender.sendMessage(Clr.RED + "/" + label + " test [on|off]");
				return true;
			}

			if (args[1].equalsIgnoreCase("on")) {
				CarbonWeb.getConf().set("json-data.testing-mode", true);
				CarbonWeb.saveConf();
				sender.sendMessage(Clr.AQUA + "Testing enabled");
				return true;
			}

			if (args[1].equalsIgnoreCase("off")) {
				CarbonWeb.getConf().set("json-data.testing-mode", false);
				CarbonWeb.saveConf();
				sender.sendMessage(Clr.AQUA + "Testing disabled");
				return true;
			}
		}

		sender.sendMessage(Clr.RED + "Unknown argument: " + args[0] + ", use /" + label + " for help.");

		return true;
	}

}
