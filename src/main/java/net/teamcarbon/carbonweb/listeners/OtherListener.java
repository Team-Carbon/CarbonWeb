package net.teamcarbon.carbonweb.listeners;

import net.teamcarbon.carbonweb.CarbonWeb;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.Iterator;

public class OtherListener implements Listener {

	@EventHandler
	public void onPing(ServerListPingEvent e) {
		Iterator<Player> pi = e.iterator();
		while (pi.hasNext()) {
			Player p = pi.next();
			if (CarbonWeb.ess != null) {
				com.earth2me.essentials.Essentials ep = (com.earth2me.essentials.Essentials) CarbonWeb.ess;
				com.earth2me.essentials.User eu = ep.getUser(p);
				if (eu.isHidden()) {
					pi.remove();
				}
			}
		}
	}

}
