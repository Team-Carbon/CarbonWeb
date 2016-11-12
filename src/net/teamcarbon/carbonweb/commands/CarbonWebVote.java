package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonlib.Misc.Messages.Clr;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CarbonWebVote implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		sender.sendMessage(Clr.RED + "Not yet implemented");
		return true;
	}

}
