package net.teamcarbon.carbonweb;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import net.teamcarbon.carbonlib.CarbonPlugin;
import net.teamcarbon.carbonlib.Misc.CarbonException;
import net.teamcarbon.carbonweb.commands.CarbonWebNotice;
import net.teamcarbon.carbonweb.commands.CarbonWebReload;
import net.teamcarbon.carbonweb.commands.CarbonWebVote;
import net.teamcarbon.carbonweb.listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;

public class CarbonWeb extends CarbonPlugin {

	private static Plugin ess;
	public static CarbonWeb inst;

	public String getDebugPath() { return "enable-debug-logging"; }
	public void disablePlugin() {}

	public void enablePlugin() {
		inst = (CarbonWeb) getPlugin();
		pm().registerEvents(new VoteListener(), this);
		pm().registerEvents(new PlayerListener(), this);
		server().getPluginCommand("CarbonWebReload").setExecutor(new CarbonWebReload());
		server().getPluginCommand("CarbonWebVote").setExecutor(new CarbonWebVote());
		server().getPluginCommand("CarbonWebNotice").setExecutor(new CarbonWebNotice());

		// Find Essentials
		if (pm().isPluginEnabled("Essentials")) { ess = pm().getPlugin("Essentials"); }

		// Repeating tasks

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				dumpInfo();
			}
		}, 0L, 100L);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				VoteListener.processVotes();
			}
		}, 100L, 200L);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				VoteListener.fetchUuids();
			}
		}, 200L, 200L);
	}

	public static void dumpInfo() {
		JsonObject json = new JsonObject();

		json.addProperty("updated", System.currentTimeMillis()/1000L);

		json.addProperty("address", "team-carbon.net");
		json.addProperty("ip", inst.getServer().getIp());
		json.addProperty("port", inst.getServer().getPort());
		json.addProperty("motd", inst.getServer().getMotd());
		json.addProperty("version", inst.getServer().getVersion());
		json.addProperty("count", Bukkit.getOnlinePlayers().size());
		json.addProperty("capacity", Bukkit.getMaxPlayers());
		json.addProperty("testing", inst.getConf().getBoolean("json-data.testing-mode", false));
		json.addProperty("notice", inst.getConf().getBoolean("json-data.notice-enabled", false));
		json.addProperty("notice-msg", inst.getConf().getString("json-data.notice-message", ""));
		json.addProperty("whitelisted", server().hasWhitelist());

		JsonArray jsonPlrs = new JsonArray();
		if (inst.getServer().getOnlinePlayers().size() > 0) {
			for (Player p : Bukkit.getOnlinePlayers()) {
				JsonObject jsonPlr = new JsonObject();
				jsonPlr.addProperty("name", p.getName());
				jsonPlr.addProperty("uuid", p.getUniqueId().toString());
				jsonPlr.addProperty("disp", p.getDisplayName());
				jsonPlr.addProperty("health", p.getHealth());
				jsonPlr.addProperty("level", p.getLevel());
				jsonPlr.addProperty("food", p.getFoodLevel());
				jsonPlr.addProperty("flying", p.isFlying());
				jsonPlr.addProperty("address", p.getAddress().getHostString());
				jsonPlr.addProperty("group", perm().getPrimaryGroup(p));

				if (ess != null) {
					JsonObject jsonEss = new JsonObject();
					com.earth2me.essentials.Essentials ep = (com.earth2me.essentials.Essentials) ess;
					com.earth2me.essentials.User eu = ep.getUser(p);
					jsonEss.addProperty("hidden", eu.isHidden());
					jsonEss.addProperty("balance", eu.getMoney());
					jsonEss.addProperty("muted", eu.isMuted());
					jsonEss.addProperty("jailed", eu.isJailed());
					jsonEss.addProperty("god", eu.isGodModeEnabled());
					jsonEss.addProperty("socialspy", eu.isSocialSpyEnabled());
					jsonPlr.add("Essentials", jsonEss);
				}

				jsonPlrs.add(jsonPlr);
			}
			json.add("players", jsonPlrs);
		} else {
			json.add("players", new JsonArray());
		}

		try (JsonWriter writer = new JsonWriter(new FileWriter(new File(inst.getDataFolder(), "data.json")))) {
			Gson gson = new GsonBuilder().create();
			gson.toJson(json, writer);
		} catch (Exception e) {
			inst.log.warn("Failed to write data.json! Details: ");
			CarbonException.print(inst, e);
		}
	}

}