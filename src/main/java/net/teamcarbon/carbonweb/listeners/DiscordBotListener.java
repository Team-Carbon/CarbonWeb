package net.teamcarbon.carbonweb.listeners;

import com.michaelwflaherty.cleverbotapi.CleverBotQuery;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

public class DiscordBotListener extends ListenerAdapter {

	private CarbonWeb plugin;
	private Random r;
	public DiscordBotListener(CarbonWeb p) {
		plugin = p;
		r = new Random(System.currentTimeMillis());
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		JDA jda = e.getJDA();
		long respNum = e.getResponseNumber();
		User author = e.getAuthor();
		Message msg = e.getMessage();
		MessageChannel ch = e.getChannel();

		String m = msg.getContent();

		boolean isBot = author.isBot();
		if (isBot) return;

		boolean rb = r.nextBoolean();
		String[] mParts = m.split("\\s+");
		boolean isCmd = mParts[0].startsWith("!");
		String cmd = isCmd ? mParts[0].replace("!", "") : "";
		String[] args = new String[]{};
		try { args = Arrays.copyOfRange(mParts, 1, mParts.length); } catch (Exception ignored) {}

		if (plugin.getConfig().getBoolean("discord.debug-mode", false)) {
			ch.sendMessage("[DEBUG] " + m).queue();
			ch.sendMessage("[DEBUG] args.length = " + args.length + ", isCmd = " + isCmd + ", cmd = " + cmd).queue();
		}

		switch (cmd) {
			case "link":
				if (e.getChannelType() != ChannelType.PRIVATE) {
					plugin.replyTo(ch, author, "Please use `!link` in a private message with me! (Right click my name and click 'Message')", false);
					return;
				}
				OfflinePlayer player = plugin.getLinkedPlayer(author);
				if (player != null) {
					plugin.replyTo(ch, author, "Your account is linked to **" + player.getName() + "** (UUID `" + player.getUniqueId() + "`)"
							+ "\nhttps://namemc.com/profile/" + player.getUniqueId(), false);
					return;
				}
				String key = CarbonWeb.linkUserExists(author) ? CarbonWeb.getLinkKeyFromUser(author) : plugin.randKey();
				CarbonWeb.addLinkKey(author, key);
				plugin.replyTo(ch, author, "To finish linking your accounts, use this command in game: `/link " + key + "`", false);
				break;
			case "ping":
				plugin.replyTo(ch, author, "**Pong!** :ping_pong:", false);
				break;
			case "flip":
				plugin.replyTo(ch, author, "flipped a coin and got " + (r.nextBoolean() ? "`heads` :+1:" : "`tails` :-1:"), false);
				break;
			case "roll":
				long sides = 6;
				try {
					if (args.length > 0) {
						sides = Math.abs(Long.parseUnsignedLong(args[0]));
						if (sides < 2) {
							plugin.replyTo(ch, author, "I can't roll something with only one side!", false);
							return;
						}
					}
				} catch (Exception ex) {
					plugin.replyTo(ch, author, "Sorry, not a valid number!", false);
					return;
				}

				if (sides == 2) {
					plugin.replyTo(ch, author, "Flipped a coin and got " + (r.nextBoolean() ? "`heads` :+1:" : "`tails` :-1:"), false);
				} else {
					long roll = (int) (Math.random() * sides) + 1;
					String emoji = "";
					if (roll >= 0 && roll <= 10) {
						switch ((int)roll) {
							case 0: emoji = ":zero:"; break; case 1: emoji = ":one:"; break; case 2: emoji = ":two:"; break;
							case 3: emoji = ":three:"; break; case 4: emoji = ":four:"; break; case 5: emoji = ":five:"; break;
							case 6: emoji = ":six:"; break; case 7: emoji = ":seven:"; break; case 8: emoji = ":eight:"; break;
							case 9: emoji = ":nine:"; break; case 10: emoji = ":keycap_ten:"; break;case 100: emoji = ":100:"; break;
						}
					}
					plugin.replyTo(ch, author, "rolled a " + sides + "-sided die and landed " + (emoji.isEmpty() ? "`" + roll + "`" : emoji), false);
				}
				break;
			case "cleverbot":
			case "cbot":
			case "cb":
				try {
					String query = String.join(" ", args);
					CleverBotQuery bot = new CleverBotQuery(plugin.getConfig().getString("discord.cleverbot-api-key"), query);
					bot.sendRequest();
					String response = bot.getResponse();
					ch.sendMessage(response).queue();
				} catch (Exception ex) {
					ch.sendMessage("Sorry! Lost track of the conversation.").queue();
				}
				break;
			case "vote":
			case "votes":
				String message = CarbonWeb.stripAltColors(plugin.getConfig().getString("vote-data.vote-description", ""));
				Set<String> sites = plugin.getConfig().getConfigurationSection("vote-data.vote-sites").getKeys(false);
				if (sites != null && !sites.isEmpty()) {
					for (String site : sites) {
						String val = plugin.getConfig().getString("vote-data.vote-sites." + site, "");
						message += "\n**" + site + "**: " + val;
					}
				}
				OfflinePlayer op = plugin.getLinkedPlayer(author);
				if (op != null) {
					message += "\nYou have voted " + plugin.getVotes(op) + " times!";
				} else {
					message += "\nTo view your vote count, link your account! PM me `!link` to get started.";
				}
				plugin.replyTo(ch, author, message, false);
				break;
			default:
				if (isCmd) {
					if (plugin.getConfig().contains("discord.commands." + cmd)) {
						String response = plugin.getConfig().getString("discord.commands." + cmd);
						if (!plugin.isLinked(author) && response.contains("{VOTE_COUNT}")) {
							plugin.replyTo(ch, author, ":warning: You must be linked to a Minecraft account to use that! PM me `!link` to link accounts.", false);
							return;
						}
						plugin.replyTo(ch, author, parseVars(response, author), false);
					}
				} else if (plugin.getConfig().getBoolean("discord.use-cleverbot", true)) {
					if (e.getChannelType() == ChannelType.PRIVATE) {
						try {
							CleverBotQuery bot = new CleverBotQuery(plugin.getConfig().getString("discord.cleverbot-api-key"), m);
							bot.sendRequest();
							String response = bot.getResponse();
							ch.sendMessage(response).queue();
						} catch (Exception ex) {
							ch.sendMessage("Sorry! Lost track of the conversation.").queue();
						}
					}
				}
				break;
		}
	}

	private String parseVars(String s, User user) {
		if (s.contains("{VOTE_COUNT}")) {
			OfflinePlayer player = plugin.getLinkedPlayer(user);
			if (player != null) {
				int voteCount = plugin.getVotes(player);
				s = s.replace("{VOTE_COUNT}", "" + voteCount);
			}
		}
		return s;
	}

}
