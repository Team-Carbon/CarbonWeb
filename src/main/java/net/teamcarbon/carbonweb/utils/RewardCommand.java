package net.teamcarbon.carbonweb.utils;

import java.sql.Time;

/**
 * <p>Represents a command fetched from the database to be executed.
 * These commands are designed to be a response to voting in order
 * to issue rewards.</p>
 */

public class RewardCommand {

	private int id;
	private Time timeAdded;
	private String cmd, target, source;
	private boolean executed, success;

	public RewardCommand(int id, Time time, String cmd, String target, String source, boolean executed, boolean success) {
		this.id = id;
		this.timeAdded = time;
		this.cmd = cmd;
		this.target = target;
		this.source = source;
		this.executed = executed;
		this.success = success;
	}

	public void setExecuted(boolean e) { executed = e; }
	public void setSuccess(boolean s) { success = s; }

	public int getId() { return id; }
	public Time getTimeAdded() { return timeAdded; }
	public String getCommand() { return cmd; }
	public String getTarget() { return target; }
	public String getSource() { return source; }
	public boolean isExecuted() { return executed; }
	public boolean isSuccess() { return success; }

	public boolean hasTarget() { return target.isEmpty(); }
	public boolean hasSource() { return source.isEmpty(); }

}
