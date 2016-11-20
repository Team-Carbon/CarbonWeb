package net.teamcarbon.carbonweb.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import net.teamcarbon.carbonweb.CarbonWeb;
import net.teamcarbon.carbonweb.utils.BCrypt;
import net.teamcarbon.carbonweb.utils.UUIDFetcher;
import net.teamcarbon.carbonweb.utils.VoteInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;

public class VoteListener implements Listener {
	
	private CarbonWeb plugin;
	public VoteListener(CarbonWeb p) {
		plugin = p;
		for (String url : plugin.getConfig().getStringList("vote-data.notify-urls")) {
			try {
				URL notifyUrl = new URL(url);
				urls.add(notifyUrl);
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, "Malformed URL in config (CarbonWeb.yml: vote-data.notify-urls): " + url);
			}
		}
	}

	// Votes that need to have UUIDs resolved
	private HashMap<String, VoteInfo> uuidVotes = new HashMap<>();

	// Votes in queue to process
	private List<VoteInfo> processVotes = new ArrayList<>();

	private List<URL> urls = new ArrayList<>();

	@EventHandler
	public void onVote(VotifierEvent e) {
		Vote v = e.getVote();
		VoteInfo vi = new VoteInfo(v);
		plugin.getLogger().log(Level.FINE, v.getUsername() + "(" + v.getAddress() + ") cast a vote from " + v.getServiceName());

		// Check if the voting user is online, fetch their UUID locally, then add to the process queue.
		for (Player p :plugin.getServer().getOnlinePlayers()) {
			if (vi.user.equalsIgnoreCase(p.getName())) {
				vi.setUuid(p.getUniqueId());
				processVotes.add(vi);
			}
		}

		// If the player is offline, cache the VoteInfo to be resolved later.
		if (!vi.uuidSet()) { uuidVotes.put(vi.user, vi); }

		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() { processVotes(); }
		}, 100L, 200L);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() { fetchUuids(); }
		}, 200L, 200L);
	}

	private void processVotes() {
		String pass = plugin.getConfig().getString("vote-data.password", null);
		if (!urls.isEmpty() && !processVotes.isEmpty()) {
			if (pass == null || pass.isEmpty()) {
				plugin.getLogger().log(Level.WARNING, "The password hasn't been set! (CarbonWeb.yml: vote-data.password)");
			}
			for (URL url : urls) {

				try {

					// Setup the connection
					URLConnection con = url.openConnection();
					con.setDoOutput(true); // Triggers POST.
					con.setRequestProperty("Accept-Charset", "UTF-8");
					con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

					// Build the JSON array
					JsonObject jsonObj = new JsonObject();
					jsonObj.addProperty("timestamp", System.currentTimeMillis()/1000L);
					jsonObj.addProperty("password", BCrypt.hashpw(pass, BCrypt.gensalt()));
					JsonArray votesJson = new JsonArray();
					for (VoteInfo vi : processVotes) { votesJson.add(vi.asJson()); }
					jsonObj.add("votes", votesJson);

					// Build the payload string
					String payload = String.format("data=%s", URLEncoder.encode(jsonObj.toString(), "UTF-8"));
					plugin.getLogger().log(Level.FINE, "Notify URL " + url.toString() + " with payload: " + payload);

					// Send request
					OutputStream output = con.getOutputStream();
					output.write(payload.getBytes("UTF-8"));
					output.close();

					processVotes.clear();

				} catch (Exception e) {
					plugin.getLogger().log(Level.WARNING, "Encountered an error attempting to send vote data to notify-url: "
							+ url.toString() + " -- Details:");
					e.printStackTrace();
				}

			}
		}
	}

	private void fetchUuids() {

		// Remove names which are already resolved.
		for (VoteInfo v : uuidVotes.values()) {
			if (v.uuidSet()) {
				if (uuidVotes.containsKey(v.user)) {
					uuidVotes.remove(v.user);
				}
			}
		}

		// Query the Mojang API to resolve the remaining names if there are any.
		if (!uuidVotes.isEmpty()) {
			UUIDFetcher f = new UUIDFetcher(new ArrayList<>(uuidVotes.keySet()));
			Map<String, UUID> res;
			try {
				res = f.call();
				for (String name : res.keySet()) {
					for (String nameKey : uuidVotes.keySet()) {
						if (name.equalsIgnoreCase(nameKey)) {
							uuidVotes.get(nameKey).setUuid(res.get(name));
							processVotes.add(uuidVotes.get(nameKey));
						}
					}
				}
			} catch (Exception ignore) {}
		}

	}

}
