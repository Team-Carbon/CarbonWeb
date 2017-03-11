package net.teamcarbon.carbonweb.tasks;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class DiscordRankSyncTask extends BukkitRunnable {

	private final CarbonWeb plugin;

	public DiscordRankSyncTask(CarbonWeb p) {
		plugin = p;
	}

	public void run() {
		JDA jda = plugin.jda();
		for (Guild g : jda.getGuilds()) {
			List<Role> roles = g.getRoles();
			for (Member m : g.getMembers()) {
				m.getUser().getId();
			}
		}
	}

}
