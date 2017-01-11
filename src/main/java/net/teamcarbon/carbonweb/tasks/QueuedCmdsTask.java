package net.teamcarbon.carbonweb.tasks;

import net.teamcarbon.carbonweb.CarbonWeb;
import net.teamcarbon.carbonweb.utils.RewardCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.ResultSet;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class QueuedCmdsTask extends BukkitRunnable {

	private final CarbonWeb plugin;

	public QueuedCmdsTask(CarbonWeb p) {
		plugin = p;
	}

	public void run() {
		Bukkit.getLogger().info("Executing QueuedCmdsTask");
		List<RewardCommand> cmds = new ArrayList<>();
		ResultSet res = plugin.execq("SELECT * FROM commands WHERE executed=0");
		if (res != null) {
			try {
				while (res.next()) {
					int id = res.getInt("cmd_id");
					Time time = res.getTime("time_added");
					String cmd = res.getString("command");
					String target = res.getString("required_target");
					String source = res.getString("source");
					boolean executed = res.getBoolean("executed");
					boolean success = res.getBoolean("success");
					RewardCommand rc = new RewardCommand(id, time, cmd, target, source, executed, success);
					cmds.add(rc);
					Bukkit.getLogger().info("Cached command: " + rc.getCommand());
				}
			} catch (Exception e) {
				plugin.getLogger().warning("Encountered an error iterating over result set rows (QueuedCmdsTask):");
				e.printStackTrace();
			}
			String successIds = "";
			if (!cmds.isEmpty()) {
				for (RewardCommand rc : cmds) {
					Bukkit.getLogger().info("Executing command: " + rc.getCommand());
					if (rc.hasTarget()) {
						OfflinePlayer op = Bukkit.getPlayer(rc.getTarget());
						if (op != null && op.isOnline()) {
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rc.getCommand());
							rc.setExecuted(true);
						} else {
							Bukkit.getLogger().info("Required target not found");
						}
					} else {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rc.getCommand());
					}
					if (rc.isSuccess()) {
						successIds += (successIds.isEmpty() ? "" : ", ") + rc.getId();
					}
				}
			}
			String query = "UPDATE commands SET executed=true WHERE cmd_id in (" + successIds + ")";
			plugin.execu(query);
			cmds.clear();
		} else {
			plugin.getLogger().warning("Encountered an error querying the database. (null ResultSet)");
			return;
		}
	}

}
