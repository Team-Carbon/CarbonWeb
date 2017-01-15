package net.teamcarbon.carbonweb.listeners;

import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.logging.Level;

public class PlayerListener implements Listener {

	private CarbonWeb plugin;
	public PlayerListener(CarbonWeb p) { plugin = p; }

	@EventHandler
	public void playerJoin(PlayerJoinEvent e) {

		plugin.dumpInfo();

		String id = e.getPlayer().getUniqueId().toString();
		List<String> queued = plugin.getConfig().getStringList("vote-data.queued-rewards");
		if (queued.contains(id)) {
			VoteListener.rewardPlayer(plugin, e.getPlayer(), true, false);
			queued.remove(id);
		}
		plugin.getConfig().set("vote-data.queued-rewards", queued);
		plugin.saveConfig();

	}

	@EventHandler
	public void playerQuit(PlayerQuitEvent e) { plugin.dumpInfo(); }

	@EventHandler
	public void playerLogin(PlayerLoginEvent e) {

		if (e.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
			plugin.getLogger().log(Level.FINE, "Set maintenance kick message for player: " + e.getPlayer().getName());
			e.setKickMessage("Server is undergoing maintenance! Check again later.\nThere's a new website at team-carbon.net, check there for updates!");
		}

	}

}
