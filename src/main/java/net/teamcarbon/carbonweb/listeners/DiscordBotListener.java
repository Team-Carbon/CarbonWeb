package net.teamcarbon.carbonweb.listeners;

import com.michaelwflaherty.cleverbotapi.CleverBotQuery;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.OfflinePlayer;

import java.util.Random;

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
		switch (m.toLowerCase()) {
			case "!link":
				if (e.getChannelType() != ChannelType.PRIVATE) {
					plugin.replyTo(ch, author, "Please use !link in a private message with me! (Right click my name and click 'Message')");
					return;
				}
				if (plugin.isLinked(e.getAuthor())) {
					ch.sendMessage("Your account is already linked!");
					return;
				}
				ch.sendMessage("This function is not yet implemented.");
				break;
			case "!ping":
				String pingEmoji = plugin.getConfig().getString("discord.emojis.ping-emoji", "ping_pong");
				if (!pingEmoji.isEmpty()) pingEmoji = ":" + pingEmoji + ":";
				plugin.replyTo(ch, author, "Pong! " + pingEmoji);
				break;
			case "!flip":
				String coinEmoji = plugin.getConfig().getString("discord.emojis.coin-flip-" + (rb ? "front":"back"), "");
				if (!coinEmoji.isEmpty()) coinEmoji = ":" + coinEmoji + ":";
				plugin.replyTo(ch, author, "flipped a coin and got " + (r.nextBoolean() ? "heads" : "tails") + " " + coinEmoji);
				break;
			case "!roll":
				String[] mParts = m.split(" ");
				long sides = 6;
				try {
					sides = Math.abs(Long.parseLong(mParts[1]));
					if (sides < 2) {
						plugin.replyTo(ch, author, "I can't roll something with only one side!");
						return;
					}
				} catch (Exception ignored) {}

				if (sides == 2) {
					String roll2Emoji = plugin.getConfig().getString("discord.emojis.coin-flip-" + (rb ? "front":"back"), "");
					if (!roll2Emoji.isEmpty()) roll2Emoji = ":" + roll2Emoji + ":";
					plugin.replyTo(ch, author, "flipped a coin and got " + (r.nextBoolean() ? "heads" : "tails") + " " + roll2Emoji);
				} else {
					long roll = (int) (Math.random() * sides) + 1;
					String emoji = "";
					if (roll >= 0 && roll <= 10) {
						switch ((int)roll) {
							case 0: emoji = ":zero:"; break;
							case 1: emoji = ":one:"; break;
							case 2: emoji = ":two:"; break;
							case 3: emoji = ":three:"; break;
							case 4: emoji = ":four:"; break;
							case 5: emoji = ":five:"; break;
							case 6: emoji = ":six:"; break;
							case 7: emoji = ":seven:"; break;
							case 8: emoji = ":eight:"; break;
							case 9: emoji = ":nine:"; break;
							case 10: emoji = ":ten:"; break;
						}
					}
					plugin.replyTo(ch, author, "rolled a " + sides + "-sided die and landed a " + roll + " " + emoji);
				}
				break;
			default:
				if (m.startsWith("!")) {
					String cmd = m.split(" ")[0].replace("!", "");
					if (plugin.getConfig().contains("discord.commands." + cmd)) {
						String response = plugin.getConfig().getString("discord.commands." + cmd);
						if (plugin.isLinked(author) && response.contains("{VOTE_COUNT}")) {
							plugin.replyTo(ch, author, ":exclamation: You are not linked to your Minecraft account! PM me **!link** to link accounts.");
						}
						plugin.replyTo(ch, author, parseVars(response, author));
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
				s = s.replace("{VOTE_COUNT}", "" + 0);
			}
		}
		return s;
	}

}
