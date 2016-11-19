package net.teamcarbon.carbonweb.listeners;

import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

	@EventHandler
	public void playerJoin(PlayerJoinEvent e) {
		CarbonWeb.dumpInfo();
	}

	@EventHandler
	public void playerQuit(PlayerQuitEvent e) {
		CarbonWeb.dumpInfo();
	}

	@EventHandler
	public void playerLogin(PlayerLoginEvent e) {
		if (e.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
			CarbonWeb.inst.logDebug("Set maintenance kick message for player: " + e.getPlayer().getName());
			e.setKickMessage("Server is undergoing maintenance! Check again later.\nThere's a new website at team-carbon.net, check there for updates!");
		}
	}

}
