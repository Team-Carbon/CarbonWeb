package net.teamcarbon.carbonweb.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import net.teamcarbon.carbonlib.Misc.BCrypt;
import net.teamcarbon.carbonlib.Misc.CarbonException;
import net.teamcarbon.carbonlib.UUIDUtils.UUIDFetcher;
import net.teamcarbon.carbonweb.CarbonWeb;
import net.teamcarbon.carbonweb.utils.VoteInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

public class VoteListener implements Listener {

	// Votes that need to have UUIDs resolved
	private static HashMap<String, VoteInfo> uuidVotes = new HashMap<>();

	// Votes in queue to process
	private static List<VoteInfo> processVotes = new ArrayList<>();

	private static List<URL> urls = new ArrayList<>();

	static {
		for (String url : CarbonWeb.inst.getConf().getStringList("vote-data.notify-urls")) {
			try {
				URL notifyUrl = new URL(url);
				urls.add(notifyUrl);
			} catch (Exception e) {
				CarbonWeb.inst.logWarn("Malformed URL in config (CarbonWeb.yml: vote-data.notify-urls): " + url);
			}
		}
	}

	@EventHandler
	public void onVote(VotifierEvent e) {
		Vote v = e.getVote();
		VoteInfo vi = new VoteInfo(v);
		CarbonWeb.inst.logDebug(v.getUsername() + "(" + v.getAddress() + ") cast a vote from " + v.getServiceName());

		// Check if the voting user is online, fetch their UUID locally, then add to the process queue.
		for (Player p : CarbonWeb.server().getOnlinePlayers()) {
			if (vi.user.equalsIgnoreCase(p.getName())) {
				vi.setUuid(p.getUniqueId());
				processVotes.add(vi);
			}
		}

		// If the player is offline, cache the VoteInfo to be resolved later.
		if (!vi.uuidSet()) { uuidVotes.put(vi.user, vi); }
	}

	/**
	 * Processes any queued votes. All VoteInfo objects in the processVotes List are
	 * serialized into a JSON array, each entry containing 'user', 'address', 'time',
	 * 'service', and if the VoteInfo has the UUID set: 'uuid'.<br><br>
	 * POST parameters 'pass' and 'data' are sent, 'pass' being the password
	 * specified in the config, hashed with BCrypt before URLEncoder.encode().
	 * 'data' is the URLEncoded JSON array containing the VoteInfo objects.<br><br>
	 * At this point, queued VoteInfo objects should all have UUIDs set.
	 * @see net.teamcarbon.carbonlib.Misc.BCrypt
	 */
	public static void processVotes() {
		String pass = CarbonWeb.inst.getConf().getString("vote-data.password", null);
		if (!urls.isEmpty() && !processVotes.isEmpty()) {
			if (pass == null || pass.isEmpty()) {
				CarbonWeb.inst.logWarn("The password hasn't been set! (CarbonWeb.yml: vote-data.password)");
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
					CarbonWeb.inst.logDebug("Notify URL " + url.toString() + " with payload: " + payload);

					// Send request
					OutputStream output = con.getOutputStream();
					output.write(payload.getBytes("UTF-8"));
					output.close();

					processVotes.clear();

				} catch (Exception e) {
					CarbonWeb.inst.logWarn("Encountered an error attempting to send vote data to notify-url: "
							+ url.toString() + " -- Details:");
					CarbonException.print(CarbonWeb.inst, e);
				}

			}
		}
	}

	/**
	 * Called asynchronously, uses Mojang's API to resolve usernames to UUIDs
	 * where possible, updating the cached list of VoteInfo objects if they
	 * don't have their UUID set already. Will not do anything if there are
	 * no cached pending votes or names. If all pending VoteInfo objects have
	 * UUIDs associated, they are removed from the list of names to resolve.
	 */
	public static void fetchUuids() {

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
