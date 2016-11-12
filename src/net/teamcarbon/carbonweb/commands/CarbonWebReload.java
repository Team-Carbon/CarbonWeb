package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonlib.Misc.Messages;
import net.teamcarbon.carbonlib.Misc.Messages.Message;
import net.teamcarbon.carbonlib.Misc.MiscUtils;
import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CarbonWebReload implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!MiscUtils.perm(sender, "carbonweb.reload")) {
			Messages.send(sender, Message.NO_PERM);
			return true;
		}

		CarbonWeb.reloadConf();
		sender.sendMessage("Reloaded " + CarbonWeb.inst.getDescription().getName());

		return true;
	}

}
