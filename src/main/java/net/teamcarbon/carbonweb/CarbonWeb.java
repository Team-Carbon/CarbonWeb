package net.teamcarbon.carbonweb;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import net.teamcarbon.carbonweb.commands.*;
import net.teamcarbon.carbonweb.listeners.*;
import net.teamcarbon.carbonweb.tasks.DiscordRankSyncTask;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class CarbonWeb extends JavaPlugin {

	private Plugin ess;
	private HikariDataSource hds;
	private static JDA jda;
	private File voteDataFile = new File(getDataFolder(), "vote-data.yml");
	private File discordDataFile = new File(getDataFolder(), "discord-data.yml");
	private FileConfiguration voteData = YamlConfiguration.loadConfiguration(voteDataFile);
	private FileConfiguration discordData = YamlConfiguration.loadConfiguration(discordDataFile);
	private BukkitTask discordRankSyncTask;
	public Permission perm;
	public Economy econ;
	public static HashMap<String, String> linkKeys = new HashMap<>();
	public static HashMap<String, String> revLinkKeys = new HashMap<>();

	public String getDebugPath() { return "enable-debug-logging"; }
	public void disablePlugin() {}

	public void onEnable() {
		PluginManager pm = Bukkit.getPluginManager();
		if (!setupVault()) {
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
		Bukkit.getPluginCommand("CarbonWebVote").setExecutor(new CarbonWebVote(this));
		Bukkit.getPluginCommand("CarbonWebTest").setExecutor(new CarbonWebTest(this));
		Bukkit.getPluginCommand("CarbonWebCleverBot").setExecutor(new CarbonWebCleverBot(this));

		// Find Essentials
		if (pm.isPluginEnabled("Essentials")) { ess = pm.getPlugin("Essentials"); }

		reload();

		if (jda != null) {
			for (Object o : jda.getRegisteredListeners()) {
				jda.removeEventListener(o);
			}
		}

		try {
			jda = new JDABuilder(AccountType.BOT)
					.setToken(getConfig().getString("discord.bot-token"))
					.addListener(new DiscordBotListener(this))
					.setAutoReconnect(true)
					.setAudioEnabled(false)
					.setBulkDeleteSplittingEnabled(false)
					.buildAsync();
		} catch (LoginException e) {
			System.err.println("Exception while logging in!");
			e.printStackTrace();
		} catch (RateLimitedException e) {
			System.err.println("Exception: Excessive login attempts! (Rate limit exceeded)");
			e.printStackTrace();
		}

		// Repeating tasks

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() { dumpInfo(); }
		}, 0L, 100L);

		discordRankSyncTask = new DiscordRankSyncTask(this).runTaskTimer(this, 30, 30);

	}

	public void onDisable() { if (!hds.isClosed()) { hds.close(); } }

	public void reload() {
		if (discordRankSyncTask != null && Bukkit.getScheduler().isCurrentlyRunning(discordRankSyncTask.getTaskId()))
			discordRankSyncTask.cancel();

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
		hc.addDataSourceProperty("connectionTimeout", "3000");
		hc.setJdbcUrl(jdbcUrl);
		hc.setUsername(user);
		hc.setPassword(pass);
		hc.validate();
		hds = new HikariDataSource(hc);

		discordRankSyncTask = new DiscordRankSyncTask(this).runTaskTimer(this, 30, 30);
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

	public String tierPath(String tier) { return "vote-data.rewards." + tier; }
	public String itemPath(String tier, String item) { return "vote-data.rewards." + tier + ".items." + item; }

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

	public void addVote(Player p) {
		String path = "vote-counts." + p.getUniqueId().toString();
		int vc = voteData.getInt(path, 0) + 1;
		voteData.set(path, vc);
		try { voteData.save(voteDataFile); } catch (IOException e) { e.printStackTrace(); }

		for (String key : getConfig().getConfigurationSection("vote-data.vote-promotions").getKeys(false)) {
			String keyPath = "vote-data.vote-promotions." + key + ".";
			String keyPerm = "vote-rewards.promotion." + key;
			if (!perm.has(p, keyPerm)) continue;
			int reqVotes = getConfig().getInt(keyPath + "votes", 0);
			if (vc >= reqVotes) {
				String rank = getConfig().getString(keyPath + "rank", "default");
				for (String remRank : getConfig().getStringList(keyPath + "remove-ranks")) {
					perm.playerRemoveGroup(null, p, remRank);
				}
				perm.playerAddGroup(null, p, rank);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString(keyPath + "message", "&bPromoted to &6" + rank + "&b!")));
				getLogger().info(p.getName() + " voted " + vc + " times, promoted to " + rank);
			}
		}
	}

	public void testCount(Player p, int count) {
		String path = "vote-counts." + p.getUniqueId().toString();

		for (String key : getConfig().getConfigurationSection("vote-data.vote-promotions").getKeys(false)) {
			String keyPath = "vote-data.vote-promotions." + key + ".";
			String keyPerm = "vote-rewards.promotion." + key;
			if (!perm.has(p, keyPerm)) return;
			int reqVotes = getConfig().getInt(keyPath + "votes", 0);
			if (count >= reqVotes) {
				String rank = getConfig().getString(keyPath + "rank", "default");
				for (String remRank : getConfig().getStringList(keyPath + "remove-ranks")) {
					perm.playerRemoveGroup(null, p, remRank);
				}
				perm.playerAddGroup(null, p, rank);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString(keyPath + "message", "&bPromoted to &6" + rank + "&b!")));
				getLogger().info(p.getName() + " voted " + count + " times, promoted to " + rank);
			}
		}
	}

	public int getVotes(OfflinePlayer p) {
		String path = "vote-counts." + p.getUniqueId().toString();
		return voteData.getInt(path, 0);
	}

	public void linkDiscordUser(OfflinePlayer player, User user) {
		discordData.set("minecraft-to-discord-links." + player.getUniqueId().toString(), user.getId());
		try { discordData.save(discordDataFile); } catch (Exception ignored) {}
	}

	public boolean isLinked(OfflinePlayer player) {
		return discordData != null && discordData.contains("minecraft-to-discord-links." + player.getUniqueId().toString());
	}

	public boolean isLinked(User user) {
		if (discordData == null) return false;
		ConfigurationSection linkData = discordData.getConfigurationSection("minecraft-to-discord-links");
		if (linkData == null) return false;
		for (String key : linkData.getKeys(false)) {
			if (user.getId().equalsIgnoreCase(linkData.getString(key))) {
				return true;
			}
		}
		return false;
	}

	public OfflinePlayer getLinkedPlayer(User user) {
		if (discordData == null) return null;
		ConfigurationSection linkData = discordData.getConfigurationSection("minecraft-to-discord-links");
		if (linkData == null) return null;
		for (String key : linkData.getKeys(false)) {
			if (user.getId().equalsIgnoreCase(linkData.getString(key))) {
				return Bukkit.getOfflinePlayer(UUID.fromString(key));
			}
		}
		return null;
	}

	public User getLinkedUser(OfflinePlayer player) {
		return jda.getUserById(discordData.getString("minecraft-to-discord-links." + player.getUniqueId().toString(), ""));
	}

	public void replyTo(MessageChannel channel, User user, String msg, boolean mentionPrivate) {
		MessageBuilder mb = new MessageBuilder();
		if (mentionPrivate || channel.getType() != ChannelType.PRIVATE) mb.append(user);
		mb.append(" " + msg);
		channel.sendMessage(mb.build()).queue();
	}

	public String randKey() {
		int len = 5;
		String base = "ABCDEFGHKLMNOPQRSTWXYZ234679";
		String code = "";
		Random r = new Random(System.nanoTime() * 1000L);
		while (code.length() < len) code += base.charAt(r.nextInt(base.length()));
		return code;
	}

	public static boolean linkKeyExists(String key) { return revLinkKeys.containsKey(key.toUpperCase(Locale.ENGLISH)); }

	public static boolean linkUserExists(User user) { return linkKeys.containsKey(user.getId()); }

	public static void addLinkKey(User user, String key) {
		key = key.toUpperCase(Locale.ENGLISH);
		if (linkKeys.containsKey(user.getId())) { revLinkKeys.remove(linkKeys.get(user.getId())); }
		linkKeys.put(user.getId(), key);
		revLinkKeys.put(key, user.getId());
	}

	public static String getLinkKeyFromUser(User user) {
		if (linkUserExists(user)) { return linkKeys.get(user.getId()); }
		return null;
	}

	public static User getLinkUserFromKey(String key) {
		key = key.toUpperCase(Locale.ENGLISH);
		if (linkKeyExists(key)) { return jda.getUserById(revLinkKeys.get(key)); }
		return null;
	}

	public static void removeLinkKey(String key) {
		key = key.toUpperCase(Locale.ENGLISH);
		if (linkKeyExists(key)) {
			linkKeys.remove(revLinkKeys.get(key));
			revLinkKeys.remove(key);
		}
	}

	public static String stripAltColors(String s) {
		return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s));
	}

	private boolean setupVault() {

		// Permissions
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) { perm = permissionProvider.getProvider(); }

		// Economy
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) { econ = economyProvider.getProvider(); }

		return (perm != null && econ != null);

	}

}