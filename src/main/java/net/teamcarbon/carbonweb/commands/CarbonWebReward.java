package net.teamcarbon.carbonweb.commands;

import net.teamcarbon.carbonweb.CarbonWeb;
import net.teamcarbon.carbonweb.listeners.VoteListener;
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
		//TIER.set("items", new MemoryConfiguration());
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

		String tprop = args.length > 2 ? args[2].toLowerCase() : "";
		String tval = args.length > 3 ? args[3].toLowerCase() : "";

		// cwrw edititem tier item prop val
		String iitem = args.length > 2 ? args[2].toLowerCase() : "";
		String iprop = args.length > 3 ? args[3].toLowerCase() : "";
		String ival = args.length > 4 ? args[4].toLowerCase() : "";

		switch (act) {
			case "givereward": // cwrw gr player
			case "gr":
				if (args.length > 1) {
					Player p = plugin.getServer().getPlayer(args[1]);
					if (p != null) {
						if (p.isOnline()) {
							VoteListener.rewardPlayer(plugin, p, false, true);
							sender.sendMessage(ChatColor.AQUA + "Rewarded " + p.getName() + " with their vote reward tier");
							return true;
						} else {
							sender.sendMessage(ChatColor.RED + "That player isn't online right now!");
							return true;
						}
					} else {
						sender.sendMessage(ChatColor.RED + "That player couldn't be found!");
						return true;
					}
				} else {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw givereward player");
					return true;
				}
			case "listtiers":
			case "lt":
				sender.sendMessage(ChatColor.AQUA + "Available tiers:");
				sender.sendMessage(ChatColor.GREEN + getTierList());
				return true;

			case "listitems":
			case "li":
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw listitems tier");
					return true;
				}
				if (tierExists(tier)) {
					sender.sendMessage(ChatColor.AQUA + "Items in " + tier + " tier:");
					sender.sendMessage(ChatColor.GREEN + getItemList(tier));
					return true;
				} else {
					sender.sendMessage(ChatColor.RED + "Tier doesn't exist. Available tiers:");
					sender.sendMessage(ChatColor.RED + getTierList());
					return true;
				}

			case "newtier":
			case "addtier":
			case "at": // cwrw at tiername
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw addtier tier");
					sender.sendMessage(ChatColor.GRAY + "This creates an empty rewards tier. Add items to it with "
							+ "/cwrw additem");
					return true;
				}
				if (tierExists(tier)) {
					sender.sendMessage(ChatColor.RED + tier + " tier already exists. To edit it, use /cwrw edittier");
					return true;
				} else {
					plugin.getConfig().set(plugin.tierPath(tier), TIER);
					plugin.saveConfig();
					sender.sendMessage(ChatColor.AQUA + tier + " tier has been created");
					return true;
				}

			case "newitem":
			case "additem":
			case "ai": // cwrw ai tier item
				if (args.length < 3) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw additem tier item");
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
					plugin.getConfig().set(plugin.itemPath(tier, item) + ".item", hand);
					plugin.getConfig().set(plugin.itemPath(tier, item) + ".weight", 1);
					plugin.getConfig().set(plugin.itemPath(tier, item) + ".min-amount", 1);
					plugin.getConfig().set(plugin.itemPath(tier, item) + ".max-amount", 1);
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
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw remtier tier");
					return true;
				}

				if (!tierExists(tier)) {
					sender.sendMessage(ChatColor.RED + "Tier doesn't exist. Available tiers:");
					sender.sendMessage(ChatColor.RED + getTierList());
					return true;
				}

				plugin.getConfig().set(plugin.tierPath(tier), null);
				plugin.saveConfig();
				sender.sendMessage(ChatColor.AQUA + tier + " tier removed from config.");
				return true;

			case "remitem":
			case "removeitem":
			case "ri": // cwrw ri tier item
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw " + act + " tier item");
					return true;
				}

				if (!tierExists(tier)) {
					sender.sendMessage(ChatColor.RED + "Tier doesn't exist. Available tiers:");
					sender.sendMessage(ChatColor.RED + getTierList());
					return true;
				}

				if (!itemExists(tier, item)) {
					sender.sendMessage(ChatColor.RED + "Item doesn't exist. Available items:");
					sender.sendMessage(ChatColor.RED + getItemList(tier));
					return true;
				}

				plugin.getConfig().set(plugin.itemPath(tier, item), null);
				plugin.saveConfig();
				sender.sendMessage(ChatColor.AQUA + "Removed item: " + item + " from " + tier + " tier rewards.");

				return true;

			case "edittier":
			case "et": // cwrw et tier prop value
				if (args.length < 3) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw " + act + " tier prop [val]");
					sender.sendMessage(ChatColor.GRAY + "Properties: min, max");
					sender.sendMessage(ChatColor.GRAY + "'min' and 'max' determines how many items are given. "
							+ "Both must be at least 1.");
					return true;
				}

				if (!tierExists(tier)) {
					sender.sendMessage(ChatColor.RED + "Tier doesn't exist. Available tiers:");
					sender.sendMessage(ChatColor.RED + getTierList());
					return true;
				}

				switch (tprop) {
					case "min":
					case "minitems":
					case "itemsmin":
					case "min-items":
					case "items-min":
						try {
							int min = Integer.parseInt(tval);
							if (min < 1) {
								sender.sendMessage(ChatColor.RED + "Min must be at least 1");
								return true;
							}
							int max = plugin.getConfig().getInt(plugin.tierPath(tier) + ".max-items", 1);
							if (min > max) {
								sender.sendMessage(ChatColor.RED + "Min must be less than max (max = " + max + "), "
										+ "increase max, then you can set min again.");
								return true;
							}
							plugin.getConfig().set(plugin.tierPath(tier) + ".min-items", min);
							plugin.saveConfig();
							sender.sendMessage(ChatColor.AQUA + "Set " + tier + ".min-items to " + min);
							return true;
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED + "Min must be a whole number. (You said \"" + tval + "\"");
							sender.sendMessage(ChatColor.RED + "Usage: /cwrw edittier tier prop [val]");
							return true;
						}
					case "max":
					case "maxitems":
					case "itemsmax":
					case "max-items":
					case "items-max":
						try {
							int max = Integer.parseInt(tval);
							if (max < 1) {
								sender.sendMessage(ChatColor.RED + "Max must be at least 1");
								return true;
							}
							int min = plugin.getConfig().getInt(plugin.tierPath(tier) + ".min-items", 1);
							if (max < min) {
								sender.sendMessage(ChatColor.RED + "Max must be more than min (min = " + min + "), "
										+ "decrease min, then you can set max again.");
								return true;
							}
							plugin.getConfig().set(plugin.tierPath(tier) + ".max-items", max);
							plugin.saveConfig();
							sender.sendMessage(ChatColor.AQUA + "Set " + tier + ".max-items to " + max);
							return true;
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED + "Max must be a whole number. (You said \"" + tval + "\"");
							sender.sendMessage(ChatColor.RED + "Usage: /cwrw edittier tier prop [val]");
							return true;
						}
					default:
						sender.sendMessage(ChatColor.RED + "Propery doesn't exist. To list properties, use /cwrw edittier (/cwrw et)");
						return true;
				}

			case "edititem":
			case "ei": // cwrw ei tier item prop value
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /cwrw edititem tier item prop [val]");
					sender.sendMessage(ChatColor.GRAY + "Properties: min, max");
					sender.sendMessage(ChatColor.GRAY + "'item' uses the item in-hand, no value is provided.");
					sender.sendMessage(ChatColor.GRAY + "'min' and 'max' determines how much of the item is given. "
							+ "A random value in this range is chosen. Both must be at least 1.");
					sender.sendMessage(ChatColor.GRAY + "'weight' is how likely the item is to be picked from the "
							+ "list. An item can be picked more than once. Weight values can be any number, "
							+ "higher numbers are more likely to be picked.");
					return true;
				}

				if (!tierExists(tier)) {
					sender.sendMessage(ChatColor.RED + "Tier doesn't exist. Available tiers:");
					sender.sendMessage(ChatColor.RED + getTierList());
					return true;
				}

				if (!itemExists(tier, item)) {
					sender.sendMessage(ChatColor.RED + "Item doesn't exist. Available items:");
					sender.sendMessage(ChatColor.RED + getItemList(tier));
					return true;
				}

				switch (iprop) {
					case "weight":
						try {
							int weight = Integer.parseInt(ival);
							if (weight < 1) {
								sender.sendMessage(ChatColor.RED + "Weight must be at least 1");
								return true;
							}
							plugin.getConfig().set(plugin.itemPath(tier, item) + ".weight", weight);
							plugin.saveConfig();
							sender.sendMessage(ChatColor.AQUA + "Set " + tier + "." + item + ".weight to " + weight);
							return true;
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED + "Min must be a whole number. (You said \"" + ival + "\"");
							sender.sendMessage(ChatColor.RED + "Usage: /cwrw edititem tier item prop [val]");
							return true;
						}
					case "item":

						if (!(sender instanceof Player)) {
							sender.sendMessage(ChatColor.RED + "You must be in-game to do that!");
							return true;
						}

						p = (Player) sender;
						hand = p.getInventory().getItemInMainHand();
						if (hand != null && hand.getType() != Material.AIR) {
							hand = new ItemStack(hand);
							hand.setAmount(1);
							plugin.getConfig().set(plugin.itemPath(tier, item) + ".item", hand);
							plugin.saveConfig();
							sender.sendMessage(ChatColor.AQUA + "Set " + tier + "." + item + " to the currently held item");
							return true;
						} else {
							sender.sendMessage(ChatColor.RED + "Must be holding the item to add to this tier");
							return true;
						}
					case "min":
					case "minamount":
					case "amountmin":
					case "min-amount":
					case "amount-min":
						try {
							int min = Integer.parseInt(ival);
							if (min < 1) {
								sender.sendMessage(ChatColor.RED + "Min must be at least 1");
								return true;
							}
							int max = plugin.getConfig().getInt(plugin.itemPath(tier, item) + ".max-amount", 1);
							if (min > max) {
								sender.sendMessage(ChatColor.RED + "Min must be less than max (max = " + max + "), "
										+ "increase max, then you can set min again.");
								return true;
							}
							plugin.getConfig().set(plugin.itemPath(tier, item) + ".min-amount", min);
							plugin.saveConfig();
							sender.sendMessage(ChatColor.AQUA + "Set " + tier + "." + item + ".min-amount to " + min);
							return true;
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED + "Min must be a whole number. (You said \"" + ival + "\"");
							sender.sendMessage(ChatColor.RED + "Usage: /cwrw edititem tier item prop [val]");
							return true;
						}
					case "max":
					case "maxamount":
					case "amountmax":
					case "max-amount":
					case "amount-max":
						try {
							int max = Integer.parseInt(ival);
							if (max < 1) {
								sender.sendMessage(ChatColor.RED + "Max must be at least 1");
								return true;
							}
							int min = plugin.getConfig().getInt(plugin.tierPath(tier) + ".min-items", 1);
							if (max < min) {
								sender.sendMessage(ChatColor.RED + "Max must be more than min (min = " + min + "), "
										+ "decrease min, then you can set max again.");
								return true;
							}
							plugin.getConfig().set(plugin.itemPath(tier, item) + ".max-amount", max);
							plugin.saveConfig();
							sender.sendMessage(ChatColor.AQUA + "Set " + tier + "." + item + ".max-amount to " + max);
							return true;
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED + "Max must be a whole number. (You said \"" + ival + "\"");
							sender.sendMessage(ChatColor.RED + "Usage: /cwrw edititem tier item prop [val]");
							return true;
						}
					default:
						sender.sendMessage(ChatColor.RED + "Propery doesn't exist. To list properties, use /cwrw edititem (/cwrw ei)");
						return true;
				}
			default: return false;
		}
	}

	private String getTierList() {
		Set<String> tiers = plugin.getConfig().getConfigurationSection("vote-data.rewards").getKeys(false);
		Iterator<String> ti = tiers.iterator();
		String list = ti.hasNext() ? ti.next() : "";
		while (ti.hasNext()) { list += ", " + ti.next(); }
		return list;
	}

	private String getItemList(String tier) {
		if (!tierExists(tier)) return "";
		Set<String> items = plugin.getConfig().getConfigurationSection(plugin.tierPath(tier)+".items").getKeys(false);
		Iterator<String> ii = items.iterator();
		String list = ii.hasNext() ? ii.next() : "";
		while (ii.hasNext()) { list += ", " + ii.next(); }
		return list;
	}

	private boolean tierExists(String tier) {
		if (tier == null || tier.isEmpty()) return false;
		tier = tier.toLowerCase();
		return plugin.getConfig().contains(plugin.tierPath(tier));
	}

	private boolean itemExists(String tier, String item) {
		if (item == null || item.isEmpty()) return false;
		item = item.toLowerCase();
		return tierExists(tier) && plugin.getConfig().contains(plugin.itemPath(tier, item));
	}

}
