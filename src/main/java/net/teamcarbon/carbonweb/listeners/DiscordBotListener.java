package net.teamcarbon.carbonweb.listeners;

import com.michaelwflaherty.cleverbotapi.CleverBotQuery;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.teamcarbon.carbonweb.CarbonWeb;

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
				break;
			case "!ping":
				plugin.replyTo(ch, author, "Pong!");
				break;
			case "!flip":
				plugin.replyTo(ch, author, "flipped a coin and got " + (r.nextBoolean() ? "heads" : "tails"));
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
					plugin.replyTo(ch, author, "flipped a coin and got " + (r.nextBoolean() ? "heads" : "tails"));
				} else {
					long roll = (int) (Math.random() * sides) + 1;
					plugin.replyTo(ch, author, "rolled a " + sides + "-sided die and landed a " + roll);
				}
				break;
			default:
				if (plugin.getConfig().getBoolean("discord.use-cleverbot", true)) {
					if (e.getChannelType() == ChannelType.PRIVATE) {
						CleverBotQuery bot = new CleverBotQuery(plugin.getConfig().getString("discord.cleverbot-api-key"), m);
						try {
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

}
