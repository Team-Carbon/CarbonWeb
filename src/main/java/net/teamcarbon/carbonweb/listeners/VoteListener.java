package net.teamcarbon.carbonweb.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import net.teamcarbon.carbonweb.CarbonWeb;
import net.teamcarbon.carbonweb.utils.BCrypt;
import net.teamcarbon.carbonweb.utils.RandomCollection;
import net.teamcarbon.carbonweb.utils.UUIDFetcher;
import net.teamcarbon.carbonweb.utils.VoteInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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

		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() { processVotes(); }
		}, 100L, 200L);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() { fetchUuids(); }
		}, 200L, 200L);
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
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			if (vi.user.equalsIgnoreCase(p.getName())) {
				vi.setUuid(p.getUniqueId());
				processVotes.add(vi);
			}
		}

		// If the player is offline, cache the VoteInfo to be resolved later and add the user to the waitlist
		if (!vi.uuidSet()) {
			uuidVotes.put(vi.user, vi);
		}
	}

	private void processVotes() {
		String pass = plugin.getConfig().getString("vote-data.password", null);
		if (!processVotes.isEmpty()) {
			for (VoteInfo vi : processVotes) {
				Player p = Bukkit.getPlayer(vi.uuid());
				if (p != null && p.isOnline()) {
					for (String tier : plugin.getConfig().getConfigurationSection("vote-data.rewards").getKeys(false)) {
						if (plugin.perm.has(p, "vote-rewards.tier." + tier)) {
							RandomCollection<String> items = new RandomCollection<>();
							for (String item : plugin.getConfig().getConfigurationSection("vote-data.rewards." + tier).getKeys(false)) {
								items.add(plugin.getConfig().getInt("vote-data.rewards." + tier, 0), item);
							}

							int min = plugin.getConfig().getInt("vote-data.rewards." + tier + ".min-items", 1);
							int max = plugin.getConfig().getInt("vote-data.rewards." + tier + ".max-items", 1);
							int amount = ThreadLocalRandom.current().nextInt(min, max + 1);

							List<ItemStack> givenItems = new ArrayList<>();
							for (int i = 0; i < amount; i++) {
								String itemName = items.next();
								int minAmount = plugin.getConfig().getInt("vote-data.rewards." + tier + "." + itemName + ".min-amount");
								int maxAmount = plugin.getConfig().getInt("vote-data.rewards." + tier + "." + itemName + ".max-amount");

								int rndAmount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);

								ItemStack is = new ItemStack(plugin.getConfig().getItemStack("vote-data.rewards." + tier + "." + itemName + ".item"));
								is.setAmount(rndAmount);

								givenItems.add(is);
							}
						}
					}
				}
			}

			if (!urls.isEmpty()) {
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

			// Remove pending votes if they've failed to parse 3 times
			List<String> removeVotes = new ArrayList<>();
			for (String key : uuidVotes.keySet()) {
				VoteInfo vi = uuidVotes.get(key);
				if (vi.getParseAttempts() > 2) { removeVotes.add(key); }
				else { vi.incParseAttempts(); }
			}
			for (String key : removeVotes) { uuidVotes.remove(key); }
		}

	}

}
