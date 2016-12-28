package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonweb.CarbonWeb;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Set;

public class CarbonWebReward implements CommandExecutor {

	private CarbonWeb plugin;
	public CarbonWebReward(CarbonWeb p) { plugin = p; }
	private final static MemoryConfiguration TIER = new MemoryConfiguration();

	static {
		TIER.set("min-items", 2);
		TIER.set("max-items", 3);
		TIER.set("items", new MemoryConfiguration());
		//TIER.set("", null);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!plugin.perm.has(sender, "carbonweb.editrewards")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
			return true;
		}

		if (args.length < 1) { return false; } // return false to list usage

		String act = args[0].toLowerCase();
		String tier = args.length > 1 ? args[1].toLowerCase() : "";
		String item = args.length > 2 ? args[2].toLowerCase() : "";

		switch (act) {
			case "listtiers":
			case "lt":
				sender.sendMessage(ChatColor.AQUA + "Available tiers:");
				sender.sendMessage(ChatColor.GREEN + getTierList());
				return true;

			case "listitems":
			case "li":
				if (tierExists(tier))
				break;

			case "newtier":
			case "addtier":
			case "at": // cwrw at tiername
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw addtier tierName");
					sender.sendMessage(ChatColor.GRAY + "This creates an empty rewards tier. Add items to it with "
							+ "/cwrw additem");
					return true;
				}
				if (tierExists(tier)) {
					sender.sendMessage(ChatColor.RED + tier + " tier already exists. To edit it, use /cwrw edittier");
					return true;
				} else {
					plugin.getConfig().set(tierPath(tier), TIER);
					plugin.saveConfig();
					sender.sendMessage(ChatColor.AQUA + tier + " tier has been created");
					return true;
				}

			case "newitem":
			case "additem":
			case "ai": // cwrw ai tier item
				if (args.length < 3) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw additem tierName itemName");
					sender.sendMessage(ChatColor.GRAY + "itemName can be anything, not just valid item names. "
							+ "The item saved will be what you're holding when you add an item. "
							+ "This allows you to save items with enchants and additional meta. "
							+ "To list available tiers, use /cwrw lt");
					return true;
				}

				if (!(sender instanceof Player)) {
					sender.sendMessage(ChatColor.RED + "You must be in-game to do that!");
					return true;
				}

				if (!tierExists(tier)) {
					sender.sendMessage(ChatColor.RED + "Tier doesn't exist. Available tiers:");
					sender.sendMessage(ChatColor.RED + getTierList());
					return true;
				}

				if (itemExists(tier, item)) {
					sender.sendMessage(ChatColor.RED + "Item already exists. To edit it, use /cwrw edititem");
					return true;
				}

				Player p = (Player) sender;
				ItemStack hand = p.getInventory().getItemInMainHand();
				if (hand != null && hand.getType() != Material.AIR) {
					hand = new ItemStack(hand);
					hand.setAmount(1);
					plugin.getConfig().set(itemPath(tier, item), hand);
					plugin.saveConfig();
					sender.sendMessage(ChatColor.AQUA + "Set " + tier + "." + item + " to the currently held item");
					return true;
				} else {
					sender.sendMessage(ChatColor.RED + "Must be holding the item to add to this tier");
					return true;
				}

			case "remtier":
			case "removetier":
			case "rt": // cwrw rt tier
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw remtier tierName");
					return true;
				}

				if (!tierExists(tier)) {
					sender.sendMessage(ChatColor.RED + "Tier doesn't exist. Available tiers:");
					sender.sendMessage(ChatColor.RED + getTierList());
					return true;
				}

				plugin.getConfig().set(tierPath(tier), null);
				plugin.saveConfig();
				sender.sendMessage(ChatColor.AQUA + tier + " tier removed from config.");
				return true;

			case "remitem":
			case "removeitem":
			case "ri": // cwrw ri tier item
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw remitem tierName itemName");
					return true;
				}

				item = args[2].toLowerCase();

				if (!tierExists(tier)) {
					sender.sendMessage(ChatColor.RED + "Tier doesn't exist. Available tiers:");
					sender.sendMessage(ChatColor.RED + getTierList());
					return true;
				}

				if (!itemExists(tier, item)) {
					sender.sendMessage(ChatColor.RED + "Item doesn't exist. Available tiers:");
					sender.sendMessage(ChatColor.RED + getTierList());
					return true;
				}

				return true;

			case "edittier":
			case "et": // cwrw et tier prop value
				break;

			case "edititem":
			case "ei": // cwrw ei tier item prop value
				break;

			default: return false;
		}

		return true;

	}

	private String tierPath(String tier) { return "vote-data.rewards." + tier; }
	private String itemPath(String tier, String item) { return "vote-data.rewards." + tier + "." + item; }

	private String getTierList() {
		Set<String> tiers = plugin.getConfig().getConfigurationSection("vote-data.rewards").getKeys(false);
		Iterator<String> ti = tiers.iterator();
		String list = ti.hasNext() ? ti.next() : "";
		while (ti.hasNext()) { list += ", " + ti.next(); }
		return list;
	}

	private boolean tierExists(String tier) {
		if (tier == null || tier.isEmpty()) return false;
		tier = tier.toLowerCase();
		return plugin.getConfig().contains(tierPath(tier));
	}

	private boolean itemExists(String tier, String item) {
		if (item == null || item.isEmpty()) return false;
		item = item.toLowerCase();
		return tierExists(tier) && plugin.getConfig().contains(itemPath(tier, item));
	}

}
