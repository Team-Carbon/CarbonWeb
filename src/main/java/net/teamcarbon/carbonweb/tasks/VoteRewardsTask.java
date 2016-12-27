package net.teamcarbon.carbonweb.tasks;

import net.teamcarbon.carbonweb.CarbonWeb;
import net.teamcarbon.carbonweb.utils.RewardCommand;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.ResultSet;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class VoteRewardsTask extends BukkitRunnable {

	private final CarbonWeb plugin;

	public VoteRewardsTask(CarbonWeb p) {
		plugin = p;
	}

	public void run() {
		List<RewardCommand> cmds = new ArrayList<>();
		ResultSet res = plugin.execq("SELECT * FROM users WHERE executed=0");
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
				}
			} catch (Exception e) {
				plugin.getLogger().warning("Encountered an error iterating over result set rows (vote reward task)");
			}
		} else {
			plugin.getLogger().warning("Encountered an error querying the database. (null ResultSet)");
			return;
		}
	}

}
