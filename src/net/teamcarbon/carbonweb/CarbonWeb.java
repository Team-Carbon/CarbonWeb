package net.teamcarbon.carbonweb;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import net.milkbowl.vault.permission.Permission;
import net.teamcarbon.carbonlib.CarbonLib;
import net.teamcarbon.carbonlib.Misc.CarbonException;
import net.teamcarbon.carbonlib.Misc.Log;
import net.teamcarbon.carbonlib.Misc.MiscUtils;
import net.teamcarbon.carbonweb.commands.CarbonWebNotice;
import net.teamcarbon.carbonweb.commands.CarbonWebReload;
import net.teamcarbon.carbonweb.commands.CarbonWebVote;
import net.teamcarbon.carbonweb.listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.FileWriter;

public class CarbonWeb extends JavaPlugin {

	public static Log log;
	public static CarbonWeb inst;
	public static PluginManager pm;
	public static Permission perms;

	public static Plugin ess, pex;

	/*public enum ConfType {
		DATA("data.yml"), MESSAGES("messages.yml");
		private String fn;
		private ConfigAccessor ca;
		private boolean init = false;
		ConfType(String fileName) { fn = fileName; }
		public void initConfType() {
			ca = new ConfigAccessor(CarbonSpring.inst, fn);
			init = true;
		}
		public FileConfiguration getConfig() { return ca.config(); }
		public void saveConfig() { ca.save(); }
		public void reloadConfig() { ca.reload(); }
		public boolean isInitialized() { return init; }
	}*/

	public void onEnable() {

		inst = this;
		Bukkit.getScheduler().runTaskLater(this, new Runnable() {
			@Override
			public void run() {
				enablePlugin();
			}
		}, 1L);

	}

	public void enablePlugin() {
		try {
			saveDefaultConfig();
			pm = getServer().getPluginManager();
			log = new Log(this, "enable-debug-messages");
			CarbonException.setGlobalPluginScope(this, "net.teamcarbon");
			CarbonLib.notifyHook(this);
			if (!setupPerms()) {
				log.severe("Couldn't find Vault! Disabling " + getDescription().getName() + ".");
				pm.disablePlugin(this);
			}
			pm.registerEvents(new VoteListener(), this);
			pm.registerEvents(new PlayerListener(), this);
			server().getPluginCommand("CarbonWebReload").setExecutor(new CarbonWebReload());
			server().getPluginCommand("CarbonWebVote").setExecutor(new CarbonWebVote());
			server().getPluginCommand("CarbonWebNotice").setExecutor(new CarbonWebNotice());

			// Find Essentials
			if (pm.isPluginEnabled("Essentials")) { ess = pm.getPlugin("Essentials"); }
			if (pm.isPluginEnabled("PermissionsEx")) { pex = pm.getPlugin("PermissionsEx"); }

			// Repeating tasks

			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				@Override
				public void run() {
					dumpInfo();
				}
			}, 0L, 100L);

			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				@Override
				public void run() {
					VoteListener.processVotes();
				}
			}, 100L, 200L);

			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				@Override
				public void run() {
					VoteListener.fetchUuids();
				}
			}, 200L, 200L);

		} catch (Exception e) {
			log.severe("===[ An exception occurred while trying to enable " + getDescription().getName() + " ]===");
			(new CarbonException(this, e)).printStackTrace();
			log.severe("=====================================");
		}
	}

	public static FileConfiguration getConf() { return CarbonWeb.inst.getConfig(); }
	/*public static FileConfiguration getConfig(ConfType ct) { return ct.getConfig(); }
	public static void saveConfig(ConfType ct) { ct.saveConfig(); }*/
	public static void saveConf() { inst.saveConfig(); }
	/*public static void saveAllConfigs() {
		inst.saveConfig();
		for (ConfType ct : ConfType.values()) ct.saveConfig();
	}*/
	public static void reloadConf() { CarbonWeb.inst.reloadConfig(); }
	/*public static void reloadConfig(ConfType ct) { ct.reloadConfig(); }
	public static void reloadAllConfigs() {
		inst.reloadConfig();
		for (ConfType ct : ConfType.values()) ct.reloadConfig();
	}*/
	private boolean setupPerms() {
		RegisteredServiceProvider<Permission> pp = Bukkit.getServicesManager().getRegistration(Permission.class);
		if (pp != null)
			perms = pp.getProvider();
		MiscUtils.setPerms(perms);
		return perms != null;
	}

	public static Server server() { return inst.getServer(); }

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
		json.addProperty("testing", getConf().getBoolean("json-data.testing-mode", false));
		json.addProperty("notice", getConf().getBoolean("json-data.notice-enabled", false));
		json.addProperty("notice-msg", getConf().getString("json-data.notice-message", ""));

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
				jsonPlr.addProperty("group", perms.getPrimaryGroup(p));

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
			log.warn("Failed to write data.json! Details: ");
			CarbonException.print(inst, e);
		}
	}

}