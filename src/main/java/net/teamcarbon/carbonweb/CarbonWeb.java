package net.teamcarbon.carbonweb;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.milkbowl.vault.permission.Permission;
import net.teamcarbon.carbonweb.commands.*;
import net.teamcarbon.carbonweb.listeners.*;
import net.teamcarbon.carbonweb.tasks.VoteRewardsTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;

public class CarbonWeb extends JavaPlugin {

	private Plugin ess;
	private HikariDataSource hds;
	private BukkitTask voteRewardsTask;
	public Permission perm;

	public String getDebugPath() { return "enable-debug-logging"; }
	public void disablePlugin() {}

	public void onEnable() {
		PluginManager pm = Bukkit.getPluginManager();
		if (!setupPerms()) {
			getLogger().log(Level.SEVERE, "Failed to find Vault! Disabling " + getDescription().getName());
			pm.disablePlugin(this);
			return;
		}
		saveDefaultConfig();
		pm.registerEvents(new VoteListener(this), this);
		pm.registerEvents(new PlayerListener(this), this);
		Bukkit.getPluginCommand("CarbonWebReload").setExecutor(new CarbonWebReload(this));
		Bukkit.getPluginCommand("CarbonWebNotice").setExecutor(new CarbonWebNotice(this));
		Bukkit.getPluginCommand("CarbonWebLink").setExecutor(new CarbonWebLink(this));
		Bukkit.getPluginCommand("CarbonWebReward").setExecutor(new CarbonWebReward(this));
		Bukkit.getPluginCommand("CarbonWebVote").setExecutor(new CarbonWebVote());

		// Find Essentials
		if (pm.isPluginEnabled("Essentials")) { ess = pm.getPlugin("Essentials"); }

		reload();

		// Repeating tasks

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() { dumpInfo(); }
		}, 0L, 100L);

		voteRewardsTask = new VoteRewardsTask(this).runTaskTimer(this, 15, 15);

	}

	public void reload() {
		voteRewardsTask.cancel();

		reloadConfig();
		String jdbcUrl = "jdbc:mysql://"
				+ getConfig().getString("database.hostname", "localhost:3306") + "/"
				+ getConfig().getString("database.database", "databasename"),
				user = getConfig().getString("database.username", "databaseuser"),
				pass = getConfig().getString("database.password", "databasepass");
		getLogger().info("Attempting to connect to " + jdbcUrl + " as " + user);
		if (hds != null && !hds.isClosed()) hds.close();
		HikariConfig hc = new HikariConfig();
		hc.setDriverClassName("com.mysql.jdbc.Driver");
		hc.addDataSourceProperty("cachePrepStmts", "true");
		hc.addDataSourceProperty("prepStmtCacheSize", "250");
		hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		hc.setJdbcUrl(jdbcUrl);
		hc.setUsername(user);
		hc.setPassword(pass);
		hc.validate();
		hds = new HikariDataSource(hc);

		voteRewardsTask = new VoteRewardsTask(this).runTaskTimer(this, 15, 15);
	}

	public Connection getConn() {
		try {
			return hds.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String f(String f, Object ... o) { return String.format(Locale.ENGLISH, f, o); }

	public ResultSet execq(String q) {
		try {
			Connection c = getConn();
			PreparedStatement ps = c.prepareStatement(q);
			return ps.executeQuery();
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Encountered an error running query: " + q);
			e.printStackTrace();
			return null;
		}
	}

	public int execu(String q) {
		try {
			Connection c = getConn();
			PreparedStatement ps = c.prepareStatement(q);
			return ps.executeUpdate();
		} catch (SQLException e) {
			Bukkit.getLogger().warning("Encountered an error running query: " + q);
			e.printStackTrace();
			return 0;
		}
	}

	public void onDisable() { if (!hds.isClosed()) { hds.close(); } }

	public void dumpInfo() {
		JsonObject json = new JsonObject();

		json.addProperty("updated", System.currentTimeMillis()/1000L);

		json.addProperty("address", "team-carbon.net");
		json.addProperty("ip", getServer().getIp());
		json.addProperty("port", getServer().getPort());
		json.addProperty("motd", getServer().getMotd());
		json.addProperty("version", getServer().getVersion());
		json.addProperty("count", Bukkit.getOnlinePlayers().size());
		json.addProperty("capacity", Bukkit.getMaxPlayers());
		json.addProperty("testing", getConfig().getBoolean("json-data.testing-mode", false));
		json.addProperty("notice", getConfig().getBoolean("json-data.notice-enabled", false));
		json.addProperty("notice-msg", getConfig().getString("json-data.notice-message", ""));
		json.addProperty("whitelisted", getServer().hasWhitelist());

		JsonArray jsonPlrs = new JsonArray();
		if (getServer().getOnlinePlayers().size() > 0) {
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
				if (perm != null && perm.hasGroupSupport()) {
					jsonPlr.addProperty("group", perm.getPrimaryGroup(p));
				}

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

		try (JsonWriter writer = new JsonWriter(new FileWriter(new File(getDataFolder(), "data.json")))) {
			Gson gson = new GsonBuilder().create();
			gson.toJson(json, writer);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Failed to write data.json! Details: ");
			e.printStackTrace();
		}
	}

	private boolean setupPerms() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			perm = permissionProvider.getProvider();
		}
		return (perm != null);
	}

}