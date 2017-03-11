package net.teamcarbon.carbonweb.listeners;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class DiscordBotListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		JDA jda = e.getJDA();
		long respNum = e.getResponseNumber();
		User author = e.getAuthor();
		Message msg = e.getMessage();
		MessageChannel ch = e.getChannel();

		String m = msg.getContent();
		boolean isBot = author.isBot();
	}

}
