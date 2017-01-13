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
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
				plugin.getLogger().log(Level.WARNING, "Malformed URL in config (CarbonWeb/config.yml: vote-data.notify-urls): " + url);
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

		if (!plugin.getConfig().getStringList("vote-data.allowed-services").contains(v.getServiceName())) {
			Bukkit.getLogger().warning(v.getUsername() + " attempted to vote from an invalid service: " + v.getServiceName());
			return;
		}

		VoteInfo vi = new VoteInfo(v);
		Bukkit.getLogger().info(v.getUsername() + "(" + v.getAddress() + ") cast a vote from " + v.getServiceName());

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
				VoteListener.rewardPlayer(plugin, p);
				if (p != null && !p.isOnline()) {
					List<String> queuedRewards = plugin.getConfig().getStringList("vote-data.queued-rewards");
					if (!queuedRewards.contains(p.getUniqueId().toString())) {
						queuedRewards.add(p.getUniqueId().toString());
					}
					plugin.getConfig().set("vote-data.queued-rewards", queuedRewards);
					plugin.saveConfig();
					Bukkit.getLogger().info("Added player for deferred vote rewards: " + p.getName() + " (UUID: " + p.getUniqueId().toString() + ")");
				}
			}

			if (!urls.isEmpty()) {
				if (pass == null || pass.isEmpty()) {
					plugin.getLogger().log(Level.WARNING, "The password hasn't been set! (CarbonWeb/config.yml: vote-data.password)");
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
			for (String key : removeVotes) {
				VoteInfo vi = uuidVotes.get(key);
				Bukkit.getLogger().warning("Failed to resolve UUID for username: " + vi.user + " (IP: " + vi.addr + ", Service: " + vi.serv + ")");
				uuidVotes.remove(key);
			}
		}

	}

	public static void rewardPlayer(CarbonWeb plugin, Player p) {
		if (p == null || !p.isOnline()) return;
		String rarestItem = "";
		int rarestWeight = Integer.MAX_VALUE;
		boolean rewarded = false, rewardedMultiple = false;
		for (String tier : plugin.getConfig().getConfigurationSection("vote-data.rewards").getKeys(false)) {
			if (plugin.perm.has(p, "vote-rewards.tier." + tier)) {
				RandomCollection<String> items = new RandomCollection<>();
				for (String item : plugin.getConfig().getConfigurationSection(plugin.tierPath(tier) + ".items").getKeys(false)) {
					items.add(plugin.getConfig().getInt(plugin.itemPath(tier, item) + ".weight", 0), item);
				}

				int min = plugin.getConfig().getInt(plugin.tierPath(tier) + ".min-items", 1);
				int max = plugin.getConfig().getInt(plugin.tierPath(tier) + ".max-items", 1);
				int amount = ThreadLocalRandom.current().nextInt(min, max + 1);

				List<ItemStack> givenItems = new ArrayList<>();
				for (int i = 0; i < amount; i++) {
					String itemName = items.next();
					int minAmount = plugin.getConfig().getInt(plugin.itemPath(tier, itemName) + ".min-amount", 1);
					int maxAmount = plugin.getConfig().getInt(plugin.itemPath(tier, itemName) + ".max-amount", 1);

					int rndAmount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);

					ItemStack is = new ItemStack(plugin.getConfig().getItemStack(plugin.itemPath(tier, itemName) + ".item"));
					is.setAmount(rndAmount);

					givenItems.add(is);

					if (plugin.getConfig().getInt(plugin.itemPath(tier, itemName) + ".weight", 1) < rarestWeight) {
						rarestItem = is.getType().toString().toLowerCase().replace("_", " ");
					}
					if (!givenItems.isEmpty()) rewarded = true;
					if (givenItems.size() > 1) rewardedMultiple = true;
				}

				HashMap<Integer, ItemStack> excess = p.getInventory().addItem(givenItems.toArray(new ItemStack[] {}));
				if (!excess.isEmpty()) {
					p.sendMessage(ChatColor.RED + "Not enough room in your inventory! Dropping " + excess.size() + " items.");
					for (ItemStack is : excess.values()) {
						p.getWorld().dropItem(p.getLocation(), is);
					}
				}

				if (rewarded) {
					String msg = plugin.getConfig().getString("vote-data.broadcast", "&6{PLAYER} &avoted and received {REWARD}&a!");
					msg = msg.replace("{PLAYER}", p.getName());
					msg = msg.replace("{REWARD}", rarestItem + (rewardedMultiple ? " and more" : ""));
					msg = ChatColor.translateAlternateColorCodes('&', msg);
					Bukkit.broadcastMessage(msg);
				}

				rarestItem = "";
				rarestWeight = Integer.MAX_VALUE;
				rewarded = false;
				givenItems.clear();
				items.clear();
			}
		}
	}

}
